package com.lsj.tingshu.order.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lsj.tingshu.album.client.AlbumInfoFeignClient;
import com.lsj.tingshu.common.rabbit.constant.MqConst;
import com.lsj.tingshu.common.rabbit.service.RabbitService;
import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.common.result.ResultCodeEnum;
import com.lsj.tingshu.common.service.constant.SystemConstant;
import com.lsj.tingshu.common.service.execption.TingShuException;
import com.lsj.tingshu.common.util.AuthContextHolder;
import com.lsj.tingshu.common.util.MD5;
import com.lsj.tingshu.model.album.AlbumInfo;
import com.lsj.tingshu.model.album.TrackInfo;
import com.lsj.tingshu.model.order.OrderDerate;
import com.lsj.tingshu.model.order.OrderDetail;
import com.lsj.tingshu.model.order.OrderInfo;
import com.lsj.tingshu.model.user.VipServiceConfig;
import com.lsj.tingshu.order.adapter.PayWay;
import com.lsj.tingshu.order.helper.SignHelper;
import com.lsj.tingshu.order.mapper.OrderDerateMapper;
import com.lsj.tingshu.order.mapper.OrderDetailMapper;
import com.lsj.tingshu.order.mapper.OrderInfoMapper;
import com.lsj.tingshu.order.service.OrderInfoService;
import com.lsj.tingshu.user.client.UserInfoFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.lsj.tingshu.vo.order.OrderDerateVo;
import com.lsj.tingshu.vo.order.OrderDetailVo;
import com.lsj.tingshu.vo.order.OrderInfoVo;
import com.lsj.tingshu.vo.order.TradeVo;
import com.lsj.tingshu.vo.user.UserInfoVo;
import com.lsj.tingshu.vo.user.UserPaidRecordVo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private OrderDerateMapper orderDerateMapper;

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;

    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private List<PayWay> payWaysList; // spring自动从容器中找到PayWay接口的所有实现类Bean对象赋值给payWaysList属性

    @Autowired
    private RabbitService rabbitService;

    @Override
    public OrderInfoVo trade(TradeVo tradeVo) {

        // 1.处理三种付款项类型
        OrderInfoVo orderInfoVo = null;
        Long userId = AuthContextHolder.getUserId();
        String itemType = tradeVo.getItemType();
        // 2.分别构建不同付款项类型的结算页

        switch (itemType) {
            case "1001": // 专辑类型付款项
                orderInfoVo = processItemTypeAlbum(tradeVo, userId);
                break;
            case "1002": // 声音类型付款项
                orderInfoVo = processItemTypeTrack(tradeVo, userId);
                break;
            case "1003": // vip套餐类型付款项
                orderInfoVo = processItemTypeVip(tradeVo, userId);

        }

        List<OrderDetailVo> orderDetailVoList = orderInfoVo.getOrderDetailVoList();
        if (!CollectionUtils.isEmpty(orderDetailVoList)) {
            String orderInfoStr = JSONObject.toJSONString(orderInfoVo); // 序列化
            Map orderInfoMap = JSONObject.parseObject(orderInfoStr, Map.class); // 反序列化
            String sign = SignHelper.getSign(orderInfoMap);
            orderInfoVo.setSign(sign); // 订单结算页数据的签名(提交订单的时候完整性校验)
            String orderRepeatSubmitNo = MD5.encrypt(orderDetailVoList.toString() + userId);// 集合中对象的内容做MD5(集合中对象的内容就算id一样 对象中其它属性一定不一样)
            // 对orderRepeatSubmitNo存储到Redis.
            redisTemplate.opsForValue().set(orderRepeatSubmitNo, "x");
            redisTemplate.opsForValue().set(orderInfoVo.getTradeNo(), "1");
        }


        //  TODO:像防重表中插入记录
        // 买1-10声音：
        // 3.返回构建的结算页
        return orderInfoVo;
    }


    /**
     * 并发情况：
     * 1.几乎一起进来的（压测工具）
     * 2.手动多次点击提交订单（ms:100）
     *
     * @param orderInfoVo
     * @return 标准写法：
     * 1.幂等性校验一定要放在干活逻辑的前面
     * 2.判断和删除放在一起执行，保证原子性
     */
    @Override
    public Map<String, Object> submitOrder(OrderInfoVo orderInfoVo) {

        // 1.获取局部变量
        HashMap<String, Object> orderNoMap = new HashMap<>();
        Long userId = AuthContextHolder.getUserId();

        // 2.对提交来的订单数据做各种校验
        // 2.1 提交订单的请求是否是合法渠道过来。（不合法的请求）
        String tradeNo = orderInfoVo.getTradeNo();
        if (!redisTemplate.hasKey(tradeNo)) {
            throw new TingShuException(ResultCodeEnum.ILLEGAL_REQUEST);
        }

        // 2.2 黑白名单的对请求的域名做限制..
        // 2.3 基础数据校验---Spring的校验包自动完成校验Validated
        // 2.4 订单数据的时效性。（生成结算页的时间和提交订单的时间是否很长。【1-2分钟】)
        // 2.5 订单数据的完整性(订单的三个价格都不能修改)
        // a) 查询数据库对比要买的商品价格是否和数据库中存储的商品一致，只要是不一致 就代表修改过。（性能低，提交订单的接口耗时 并发下降）
        // b) 从内存中做对比。
        String currentOrderInfoVoStr = JSONObject.toJSONString(orderInfoVo);
        Map currentOrderInfoVoMap = JSONObject.parseObject(currentOrderInfoVoStr, Map.class);
        currentOrderInfoVoMap.put("payWay", ""); // 移除掉payWay
        SignHelper.checkSign(currentOrderInfoVoMap);  // 既有订单时效性又有订单数据完整性校验。
        // 2.6 订单的重复提交：订单的幂等性保证：同一个订单不管点击立即结算多少次，数据库中有且只存储一份订单。
        // a)单端重复提交（pc（1、先点击立即结算 2、又点击一次））
        // b)多端重复提交 (pc:1.先点击立即结算在pc  app:1.在次点击立即结算在app)
        List<OrderDetailVo> orderDetailVoList = orderInfoVo.getOrderDetailVoList().stream().collect(Collectors.toList());
        String orderRepeatSubmitNoKey = MD5.encrypt(orderDetailVoList.toString() + userId);
        String script = "if redis.call(\"exists\",KEYS[1])\n" + "then\n" + "    return redis.call(\"del\",KEYS[1])\n" + "else\n" + "    return 0\n" + "end";
        Long execute = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList(orderRepeatSubmitNoKey));
        if (execute == 0) {
            throw new TingShuException(ResultCodeEnum.REPEAT_SUBMIT);
        }

        // 3. 生成订单编码
        String orderNo = RandomStringUtils.random(18, true, false);

        // 4.判断支付方式
        String payWay = orderInfoVo.getPayWay();
        for (PayWay way : payWaysList) {
            if (way.supportPayWay(payWay)) {
                way.payWay(orderNo, orderInfoVo, userId); // 真正适配之后的具体支付方式的支付逻辑
            }
        }

        // 5.返回订单编号
        orderNoMap.put("orderNo", orderNo);
        return orderNoMap;
    }

    @Override
    public void processPaySuccess(String orderNo, Long userId) {

        // 1.更新订单的状态（未支付修改已支付）
        // 1.1 根据订单编号查询订单状态 是未支付
        // 1.2 将未支付订单状态修改为已支付
        orderInfoMapper.updateOrderStatus(orderNo, userId, SystemConstant.ORDER_STATUS_PAID);

        // 2.记录用户支付的流水（user_paid_album user_paid_track user_vip_service）--insert

        UserPaidRecordVo userPaidRecordVo = prepareUserPaidRecordVo(orderNo, userId);
        rabbitService.sendMessage(MqConst.EXCHANGE_USER, MqConst.ROUTING_USER_PAY_RECORD, JSONObject.toJSONString(userPaidRecordVo));

        // 3.[付款项类型是专辑/声音（vip）不用修改]修改专辑的购买量
        rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM, MqConst.ROUTING_ALBUM_STAT_UPDATE, JSONObject.toJSONString(userPaidRecordVo));

        // 4.ElasticSearch[1.未来在做MySQL数据同步到Es自动更新 2.手动修改Es中专辑的购买量]
        rabbitService.sendMessage(MqConst.EXCHANGE_ES_UPDATE, MqConst.ROUTING_ES_UPDATE, JSONObject.toJSONString(userPaidRecordVo));


    }

    @Override
    public OrderInfo getOrderInfoAndDetailByOrderNo(String orderNo) {

        // 1.根据订单编号查询基本信息
        OrderInfo orderInfo = orderInfoMapper.selectOne(new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo));
        if (orderInfo == null) {
            throw new TingShuException(500, "该订单信息不存在");
        }

        // 2.查询订单详情信息
        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, orderInfo.getId()));
        orderInfo.setOrderDetailList(orderDetailList);

        // 3.查询订单减免信息
        List<OrderDerate> orderDeratelList = orderDerateMapper.selectList(new LambdaQueryWrapper<OrderDerate>().eq(OrderDerate::getOrderId, orderInfo.getId()));

        orderInfo.setOrderDerateList(orderDeratelList);


        // 4.封装订单的状态名字
        String orderStatus = orderInfo.getOrderStatus();

        String orderStatusName = getOrderStatusName(orderStatus);
        orderInfo.setOrderStatusName(orderStatusName);

        // 5.封装订单对应支付方式名字
        String payWay = orderInfo.getPayWay();
        String payWayName = getPayWayName(payWay);
        orderInfo.setPayWayName(payWayName);

        return orderInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderInfo saveOrderInfo(String orderNo, OrderInfoVo orderInfoVo, Long userId) {
        // 1.保存订单基本信息
        OrderInfo orderInfo = null;
        try {
            orderInfo = saveOrderBasicInfo(orderNo, orderInfoVo, userId);

            // 2.保存订单详情信息
            saveOrderDetail(orderInfo.getId(), orderInfoVo);

            // 3.保存订单减免信息
            saveOrderDerate(orderInfo.getId(), orderInfoVo);
//            int i = 1 / 0;
        } catch (Exception e) {
            throw new TingShuException(405, "数据库操作失败");
        }

        return orderInfo;
    }

    @Override
    public OrderInfo getOrderInfoByOrderNoAndUserId(Long userId, String orderNo) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getOrderNo, orderNo).eq(OrderInfo::getUserId, userId);

        // 1.根据订单编号查询基本信息
        OrderInfo orderInfo = orderInfoMapper.selectOne(wrapper);
        if (orderInfo == null) {
            throw new TingShuException(500, "该订单信息不存在");
        }

        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, orderInfo.getId()));
        orderInfo.setOrderDetailList(orderDetailList);
        return orderInfo;
    }

    @Override
    public IPage<OrderInfo> findUserPage(IPage<OrderInfo> page, Long userId) {

        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getUserId, userId);
        wrapper.orderByAsc(OrderInfo::getCreateTime);
        IPage<OrderInfo> result = this.page(page, wrapper);

        List<OrderInfo> orderInfoList = result.getRecords().stream().map(orderInfo -> {
            String orderStatus = orderInfo.getOrderStatus();
            String orderStatusName = getOrderStatusName(orderStatus);
            orderInfo.setOrderStatusName(orderStatusName);
            String orderInfoPayWay = orderInfo.getPayWay();
            String payWayName = getPayWayName(orderInfoPayWay);
            orderInfo.setPayWayName(payWayName);


            orderInfo.setOrderDetailList( orderDetailMapper.selectList(new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, orderInfo.getId())));
            orderInfo.setOrderDerateList( orderDerateMapper.selectList(new LambdaQueryWrapper<OrderDerate>().eq(OrderDerate::getOrderId, orderInfo.getId())));
            return orderInfo;
        }).collect(Collectors.toList());


        return result.setRecords(orderInfoList);
    }

    private void saveOrderDerate(Long orderId, OrderInfoVo orderInfoVo) {

        for (OrderDerateVo orderDerateVo : orderInfoVo.getOrderDerateVoList()) {

            OrderDerate orderDerate = new OrderDerate();
            orderDerate.setOrderId(orderId);
            orderDerate.setDerateType(orderDerateVo.getDerateType());
            orderDerate.setDerateAmount(orderDerateVo.getDerateAmount());
            orderDerate.setRemarks(orderDerateVo.getRemarks());
            orderDerateMapper.insert(orderDerate);
        }

    }

    private void saveOrderDetail(Long orderId, OrderInfoVo orderInfoVo) {

        for (OrderDetailVo orderDetailVo : orderInfoVo.getOrderDetailVoList()) {

            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orderId);
            orderDetail.setItemId(orderDetailVo.getItemId());
            orderDetail.setItemName(orderDetailVo.getItemName());
            orderDetail.setItemUrl(orderDetailVo.getItemUrl());
            orderDetail.setItemPrice(orderDetailVo.getItemPrice());
            orderDetailMapper.insert(orderDetail);
        }

    }

    private OrderInfo saveOrderBasicInfo(String orderNo, OrderInfoVo orderInfoVo, Long userId) {


        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setUserId(userId);
        orderInfo.setOrderTitle(orderInfoVo.getOrderDetailVoList().get(0).getItemName());
        orderInfo.setOrderNo(orderNo);
        orderInfo.setOrderStatus(SystemConstant.ORDER_STATUS_UNPAID); // 未支付
        orderInfo.setOriginalAmount(orderInfoVo.getOriginalAmount());
        orderInfo.setDerateAmount(orderInfoVo.getDerateAmount());
        orderInfo.setOrderAmount(orderInfoVo.getOrderAmount());
        orderInfo.setItemType(orderInfoVo.getItemType());// 付款类型
        orderInfo.setPayWay(orderInfoVo.getPayWay());
        orderInfoMapper.insert(orderInfo);

        return orderInfo;

    }


    private String getPayWayName(String payWay) {
        String payWayName = "";

        switch (payWay) {
            case "1101":
                payWayName = "微信支付";
                break;
            case "1102":
                payWayName = "支付宝支付";
                break;
            case "1103":
                payWayName = "余额支付";

        }

        return payWayName;
    }

    private String getOrderStatusName(String orderStatus) {
        String orderStatusName = "";

        switch (orderStatus) {
            case "0901":
                orderStatusName = "未支付";
                break;
            case "0902":
                orderStatusName = "已支付";
                break;
            case "0903":
                orderStatusName = "已关闭";

        }

        return orderStatusName;
    }

    private UserPaidRecordVo prepareUserPaidRecordVo(String orderNo, Long userId) {

        // 1.构建对象
        UserPaidRecordVo userPaidRecordVo = new UserPaidRecordVo();
        // 1.1 基础属性赋值
        userPaidRecordVo.setOrderNo(orderNo);   // 订单编号
        userPaidRecordVo.setUserId(userId); // 用户id

        // 1.2 根据订单编号查询订单信息
        OrderInfo orderInfo = getOrderInfoAndDetailByOrderNo(orderNo);

        userPaidRecordVo.setItemType(orderInfo.getItemType());// 付款项类型(订单表)
        List<Long> itemIds = orderInfo.getOrderDetailList().stream().map(OrderDetail::getItemId).collect(Collectors.toList());
        userPaidRecordVo.setItemIdList(itemIds); // 付款项id

        return userPaidRecordVo;
    }


    /**
     * 处理付款项是专辑类型
     *
     * @param tradeVo
     * @param userId
     * @return
     */
    @SneakyThrows
    private OrderInfoVo processItemTypeAlbum(TradeVo tradeVo, Long userId) {

        // 1.构建订单结算页对象
        OrderInfoVo orderInfoVo = new OrderInfoVo();
        String itemType = tradeVo.getItemType();  // 付款项类型是专辑
        Long albumId = tradeVo.getItemId();// 专辑id


        // 2.做幂等性校验
        // 2.1 校验专辑是否支付过。（user_paid_album）
        Result<Boolean> isPaidAlbumResult = userInfoFeignClient.getIsPaidAlbum(userId, albumId);
        Boolean isPaidAlbumFlag = isPaidAlbumResult.getData();
        Assert.notNull(isPaidAlbumFlag, "远程查询用户微服务获取用户是否购买专辑失败");
        if (isPaidAlbumFlag) {
            // 构建一个支付过的集合（集合中存放的是支付过的专辑id）
            log.error("该用户:{}已经购买过专辑:{},请勿重复购买!~", userId, albumId);
            orderInfoVo.setExitPaidItemIds(Lists.newArrayList(albumId));
            return orderInfoVo;
        }

        // 2.2 校验专辑是否下单了但是未支付。(order_info以及order_detail表)
        long count = orderInfoMapper.getItemTypeAlbumOrderInfo(userId, itemType, albumId);
        if (count > 0) {
            log.error("该用户:{}已经下单过该专辑:{}，请勿重复下单!~", userId, albumId);
            orderInfoVo.setExitOrdedItemIds(Lists.newArrayList(albumId));
            return orderInfoVo;
        }

        // 3.为订单结算页对象赋值
        String tradeNo = RandomStringUtils.random(15, true, true);
        orderInfoVo.setTradeNo(tradeNo);   // 订单交易号（追踪订单流向的编号[在订单编号不存在的时候，追踪订单]）
        orderInfoVo.setPayWay("");  // (进入结算页 不知道) 支付方式(零钱支付 支付宝支付（未做） 微信支付)
        orderInfoVo.setItemType(itemType); // 付款项类型

        Result<AlbumInfo> albumInfoAndTag = albumInfoFeignClient.getAlbumInfoAndTag(albumId.toString());
        AlbumInfo albumInfoAndTagData = albumInfoAndTag.getData();
        Assert.notNull(albumInfoAndTagData, "远程查询专辑微服务获取专辑信息失败");

        BigDecimal originalAmount = albumInfoAndTagData.getPrice(); // 专辑价格
        orderInfoVo.setOriginalAmount(originalAmount.setScale(2, BigDecimal.ROUND_HALF_UP));  // 订单的原始金额
        Result<UserInfoVo> userInfoResult = userInfoFeignClient.getUserInfo(userId.toString());
        UserInfoVo userInfoData = userInfoResult.getData();
        Assert.notNull(userInfoData, "远程查询用户微服务失败");
        Integer isVip = userInfoData.getIsVip();
        BigDecimal vipDiscount = new BigDecimal("0.00");
        Date vipExpireTime = userInfoData.getVipExpireTime();
        if ("1".equals(isVip + "") && vipExpireTime.after(new Date())) {
            vipDiscount = albumInfoAndTagData.getVipDiscount();
        } else {
            vipDiscount = albumInfoAndTagData.getDiscount();
        }
        if (vipDiscount.intValue() == -1) {
            // 不打折
            vipDiscount = new BigDecimal("10.00");
        }
        //  实际金额：原价100: 打折：7折 实际金额70  不打折：实际金额100
        BigDecimal orderAmount = originalAmount.multiply(vipDiscount).divide(new BigDecimal("10.00"));
        orderInfoVo.setDerateAmount(originalAmount.subtract(orderAmount).setScale(2, BigDecimal.ROUND_HALF_UP)); // 订单的减免金额（原价金额-实际金额）
        orderInfoVo.setOrderAmount(orderAmount.setScale(2, BigDecimal.ROUND_HALF_UP)); // 订单的实际金额


        OrderDetailVo orderDetailVo = new OrderDetailVo();
        orderDetailVo.setItemId(albumId);
        orderDetailVo.setItemName(albumInfoAndTagData.getAlbumTitle());
        orderDetailVo.setItemUrl(albumInfoAndTagData.getCoverUrl());
        orderDetailVo.setItemPrice(originalAmount.setScale(2, BigDecimal.ROUND_HALF_UP)); // 原始金额
        orderInfoVo.setOrderDetailVoList(Lists.newArrayList(orderDetailVo)); // 订单详情信息---到底购买的是什么（具体商品）


        OrderDerateVo orderDerateVo = new OrderDerateVo();
        orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_ALBUM_DISCOUNT);
        orderDerateVo.setDerateAmount(orderInfoVo.getDerateAmount());
        orderDerateVo.setRemarks("专辑减免");
        orderInfoVo.setOrderDerateVoList(Lists.newArrayList(orderDerateVo)); // 订单减免信息---到底减免的是什么 （具体商品）


        orderInfoVo.setTimestamp(System.currentTimeMillis()); // 订单结算页数据的时间戳（提交订单的时候做订单时效性检验）
        // 4.返回订单结算页对象
        return orderInfoVo;

    }


    private OrderInfoVo processItemTypeVip(TradeVo tradeVo, Long userId) {

        OrderInfoVo orderInfoVo = new OrderInfoVo();
        Long itemId = tradeVo.getItemId(); // 1 2 3
        String itemType = tradeVo.getItemType(); // vip套餐
        // 1.幂等性校验
        // 1.1 已支付 不用校验。
        // 1.2 已下单未支付
        long count = orderInfoMapper.getItemTypeVipOrderInfo(userId, itemType, itemId);
        if (count > 0) {
            log.error("该用户:{}已经下单过该套餐:{}，请勿重复下单!~", userId, itemId);
            orderInfoVo.setExitOrdedItemIds(Lists.newArrayList(itemId));
            return orderInfoVo;
        }

        String tradeNo = RandomStringUtils.random(15, true, true);
        orderInfoVo.setTradeNo(tradeNo);
        orderInfoVo.setPayWay("");
        orderInfoVo.setItemType(itemType);

        Result<VipServiceConfig> vipServiceConfigResult = userInfoFeignClient.getVipServiceById(itemId);
        VipServiceConfig serviceConfigData = vipServiceConfigResult.getData();
        Assert.notNull(serviceConfigData, "远程调用用户微服务获取套餐信息失败");

        BigDecimal originalAmount = serviceConfigData.getPrice();  // 套餐原价
        BigDecimal discountPrice = serviceConfigData.getDiscountPrice(); // 实际价格
        orderInfoVo.setOriginalAmount(originalAmount.setScale(2, BigDecimal.ROUND_HALF_UP));
        orderInfoVo.setDerateAmount(originalAmount.subtract(discountPrice).setScale(2, BigDecimal.ROUND_HALF_UP));
        orderInfoVo.setOrderAmount(discountPrice.setScale(2, BigDecimal.ROUND_HALF_UP));

        OrderDetailVo orderDetailVo = new OrderDetailVo();
        orderDetailVo.setItemId(itemId);
        orderDetailVo.setItemName(serviceConfigData.getName());
        orderDetailVo.setItemUrl(serviceConfigData.getImageUrl());
        orderDetailVo.setItemPrice(originalAmount.setScale(2, BigDecimal.ROUND_HALF_UP));

        orderInfoVo.setOrderDetailVoList(Lists.newArrayList(orderDetailVo));


        OrderDerateVo orderDerateVo = new OrderDerateVo();
        orderDerateVo.setDerateType(SystemConstant.ORDER_DERATE_VIP_SERVICE_DISCOUNT);
        orderDerateVo.setDerateAmount(originalAmount.subtract(discountPrice).setScale(2, BigDecimal.ROUND_HALF_UP));
        orderDerateVo.setRemarks("vip减免了");


        orderInfoVo.setOrderDerateVoList(Lists.newArrayList(orderDerateVo));
        orderInfoVo.setTimestamp(System.currentTimeMillis());
        return orderInfoVo;
    }

    private OrderInfoVo processItemTypeTrack(TradeVo tradeVo, Long userId) {

        // 1.创建OrderInfoVo对象
        OrderInfoVo orderInfoVo = new OrderInfoVo();

        Long currentTrackId = tradeVo.getItemId();
        String itemType = tradeVo.getItemType();
        Integer trackCount = tradeVo.getTrackCount();   // 0[本集] 或者其他[后n集]

        Result<AlbumInfo> trackInfoResult = albumInfoFeignClient.getAlbumInfoByTrackId(currentTrackId);
        AlbumInfo albumInfoData = trackInfoResult.getData();
        Assert.notNull(albumInfoData, "远程查询专辑微服务获取声音信息失败");
        BigDecimal singleTrackPrice = albumInfoData.getPrice();// 单集声音价格

        // 2.查询要购买的声音
        Result<List<TrackInfo>> needPayTrackListResult = albumInfoFeignClient.getNeedPayTrackList(userId, currentTrackId, trackCount);
        List<TrackInfo> needPayTrackListData = needPayTrackListResult.getData();
        Assert.notNull(needPayTrackListData, "远程查询专辑微服务获取购买的声音列表失败");


        // 3.查询用户购买的专辑对应的声音Map
        Result<Map<Long, String>> isPaidAlbumTrackMapResult = userInfoFeignClient.getIsPaidAlbumTrack(userId, albumInfoData.getId());
        Map<Long, String> isPaidAlbumTrackMap = isPaidAlbumTrackMapResult.getData();
        Assert.notNull(isPaidAlbumTrackMap, "远程查询用户微服务获取已经购买过的声音失败");

        // 4.幂等性校验【第一种把已经支付过的过滤掉，返回没有支付过的】 【第二种，要买的声音中只要有已经支付过的，返回，不让你再把能买声音构建出来】
        // 4.1过滤要买的声音是否已经支付过
        // 策略一：
        List<TrackInfo> readPaidTrackList = needPayTrackListData.stream().filter(trackInfo1 -> StringUtils.isEmpty(isPaidAlbumTrackMap.get(trackInfo1.getId()))).collect(Collectors.toList());

        // 4.2 校验声音是否下单未支付(order_info以及order_detail中联合查询)把下单未支付的声音id列表返回出来
        List<Long> currentOrderedTrackIds = orderInfoMapper.getItemTypeTrackOrderInfo(userId, itemType);

        // 策略一：
        List<TrackInfo> readOrderedUnPayTrackList = readPaidTrackList.stream().filter(trackInfo -> !currentOrderedTrackIds.contains(trackInfo.getId())).collect(Collectors.toList());

        // 5.给订单结算页对象赋值
        String tradeNo = RandomStringUtils.random(15, true, true);
        orderInfoVo.setTradeNo(tradeNo);
        orderInfoVo.setPayWay("");
        orderInfoVo.setItemType(itemType);
        BigDecimal originalAmount = trackCount == 0 ? singleTrackPrice : singleTrackPrice.multiply(new BigDecimal(readOrderedUnPayTrackList.size()));
        orderInfoVo.setOriginalAmount(originalAmount.setScale(2, BigDecimal.ROUND_HALF_UP));// 原始金额 声音的价格（专辑对象中）
        orderInfoVo.setDerateAmount(new BigDecimal("0.00"));
        orderInfoVo.setOrderAmount(originalAmount.setScale(2, BigDecimal.ROUND_HALF_UP));


        ArrayList<OrderDetailVo> orderDetailVos = new ArrayList<>();
        for (TrackInfo needPayTrack : readOrderedUnPayTrackList) { // 策略一
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemId(needPayTrack.getId());
            orderDetailVo.setItemName(needPayTrack.getTrackTitle());
            orderDetailVo.setItemUrl(needPayTrack.getCoverUrl());
            orderDetailVo.setItemPrice(singleTrackPrice.setScale(2, BigDecimal.ROUND_HALF_UP));
            orderDetailVos.add(orderDetailVo);
        }
        orderInfoVo.setOrderDetailVoList(Lists.newArrayList(orderDetailVos));
        orderInfoVo.setOrderDerateVoList(Lists.newArrayList());   // 无
        orderInfoVo.setTimestamp(System.currentTimeMillis());
        return orderInfoVo;
    }

}
