package com.lsj.tingshu.payment.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.lsj.tingshu.common.rabbit.constant.MqConst;
import com.lsj.tingshu.common.rabbit.service.RabbitService;
import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.common.util.AuthContextHolder;
import com.lsj.tingshu.model.payment.PaymentInfo;
import com.lsj.tingshu.payment.config.WxPayV3Config;
import com.lsj.tingshu.payment.mapper.PaymentInfoMapper;
import com.lsj.tingshu.payment.service.PaymentInfoService;
import com.lsj.tingshu.payment.service.WxPayService;
import com.lsj.tingshu.payment.util.PayUtil;
import com.lsj.tingshu.user.client.UserInfoFeignClient;
import com.lsj.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.core.exception.ValidationException;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import com.wechat.pay.java.service.payments.model.Transaction;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {
    @Autowired
    private PaymentInfoService paymentInfoService;
    @Autowired
    private RSAAutoCertificateConfig certificateConfig;

    @Autowired
    private WxPayV3Config wxPayV3Config;

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;
    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private RabbitService rabbitService;


    @Autowired
    private StringRedisTemplate redisTemplate;


    @Override
    public Map<String, Object> createJsapi(String tradeType, String orderNo) {

        HashMap<String, Object> payNeedMap = new HashMap<>();
        Long userId = AuthContextHolder.getUserId();


        // 1.插入用户支付流水表
        PaymentInfo paymentInfo = paymentInfoService.saveOrderPaymentInfo(tradeType, orderNo, userId);

        // 2.构建微信小程序支付用到的参数
        // 2.1 使用微信支付公钥的RSA配置
        // 2.2 构建JsapiService对象
        JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(certificateConfig).build();
        // 2.3 构建预订单支付的请求对象
        PrepayRequest request = new PrepayRequest();
        Amount amount = new Amount();
//        amount.setTotal(paymentInfo.getAmount());//线上
        amount.setTotal(1);//测试
        request.setAmount(amount);
        request.setAppid(wxPayV3Config.getAppid());
        request.setMchid(wxPayV3Config.getMerchantId());
        request.setDescription("测试商品:" + paymentInfo.getContent());
        request.setNotifyUrl(wxPayV3Config.getNotifyUrl());
        request.setOutTradeNo(paymentInfo.getOutTradeNo()); // 给微信平台我自己的一个订单唯一的编号

        Result<UserInfoVo> userInfo = userInfoFeignClient.getUserInfo(userId.toString());
        UserInfoVo userInfoData = userInfo.getData();
        Assert.notNull(userInfoData, "远程查询用户微服务获取信息失败");
        String userOpenId = userInfoData.getWxOpenId();
        Payer payer = new Payer();
        payer.setOpenid(userOpenId);
        request.setPayer(payer); // 付款者信息

        // 2.4 发送预下单请求
        // response包含了调起支付所需的所有参数，可直接用于前端调起支付
        PrepayWithRequestPaymentResponse response = service.prepayWithRequestPayment(request);

        payNeedMap.put("timeStamp", response.getTimeStamp());
        payNeedMap.put("nonceStr", response.getNonceStr());
        payNeedMap.put("package", response.getPackageVal());  // pre_pay_id
        payNeedMap.put("signType", response.getSignType());
        payNeedMap.put("paySign", response.getPaySign()); // 直接使用签名


        // 3.返回
        return payNeedMap;
    }

    @Override
    public Transaction queryPayStatus(String orderNo) {

        // 1.构建查询订单的请求对象
        QueryOrderByOutTradeNoRequest outTradeNoRequest = new QueryOrderByOutTradeNoRequest();
        outTradeNoRequest.setMchid(wxPayV3Config.getMerchantId()); // 唯一标识
        outTradeNoRequest.setOutTradeNo(orderNo); // 在微信支付平台不一定是唯一标识

        // 2.创建Service对象
        JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(certificateConfig).build();
        try {
            Transaction transaction = service.queryOrderByOutTradeNo(outTradeNoRequest);

            return transaction;
        } catch (ServiceException e) {
            System.out.printf("code=[%s], message=[%s]\n", e.getErrorCode(), e.getErrorMessage());
            System.out.printf("reponse body=[%s]\n", e.getResponseBody());
            return null;
        }
    }

    @Override
    public void processWxPaidSuccess(String orderNo, Long userId) {


        // 1.修改支付表的订单的支付状态（未支付修改已支付）
        paymentInfoMapper.updateOrderPayStatus(orderNo, userId);

        // 2.交易类型判断要做的事情
        // 2.1 如果交易类型是下单操作
        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(new LambdaQueryWrapper<PaymentInfo>().eq(PaymentInfo::getOrderNo, orderNo).eq(PaymentInfo::getUserId, userId));
        String paymentType = paymentInfo.getPaymentType();

        String wxPaidKey = "wx:paid:ordrno:" + orderNo;
        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(wxPaidKey, userId.toString(), 1, TimeUnit.MINUTES);
        if (aBoolean) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("orderNo", orderNo);
            jsonObject.put("userId", userId.toString());
            String routingKey = "1301".equals(paymentType) ? MqConst.ROUTING_ORDER_PAY_SUCCESS : MqConst.ROUTING_RECHARGE_PAY_SUCCESS;
            rabbitService.sendMessage(MqConst.EXCHANGE_ORDER, routingKey, JSONObject.toJSONString(jsonObject));
        }

    }

    @Override
    public Transaction asyncNotify(HttpServletRequest httpServletRequest) {

        // 1.构造 RequestParam对象
        // 1.1 获取微信平台证书的序列号
        String wechatPaySerial = httpServletRequest.getHeader("Wechatpay-Serial");
        // 1.2 获取随机数
        String wechatpayNonce = httpServletRequest.getHeader("Wechatpay-Nonce");
        // 1.3 获取微信支付的签名
        String wechatSignature = httpServletRequest.getHeader("Wechatpay-Signature");
        // 1.4 获取时间戳
        String wechatTimestamp = httpServletRequest.getHeader("Wechatpay-Timestamp");
        // 1.5 从请求中获取请求体的原始报文
        String originBodyStr = PayUtil.readData(httpServletRequest);

        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(wechatPaySerial)
                .nonce(wechatpayNonce)
                .signature(wechatSignature)
                .timestamp(wechatTimestamp)
                .body(originBodyStr).build();

        // 2.初始化 NotificationParser
        NotificationParser parser = new NotificationParser(certificateConfig);

        try {
            // 3.以支付通知回调为例，验签、解密并转换成 Transaction
            Transaction transaction = parser.parse(requestParam, Transaction.class);
            return transaction;
        } catch (ValidationException e) {
            // 签名验证失败，返回 401 UNAUTHORIZED 状态码
            log.error("签名验证失败", e);
            return null;
        }
    }
}
