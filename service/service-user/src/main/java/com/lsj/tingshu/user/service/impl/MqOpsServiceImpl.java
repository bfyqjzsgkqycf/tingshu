package com.lsj.tingshu.user.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.lsj.tingshu.album.client.AlbumInfoFeignClient;
import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.common.service.execption.TingShuException;
import com.lsj.tingshu.model.album.AlbumInfo;
import com.lsj.tingshu.model.user.*;
import com.lsj.tingshu.user.mapper.*;
import com.lsj.tingshu.user.service.MqOpsService;
import com.lsj.tingshu.user.service.UserPaidTrackService;
import com.lsj.tingshu.user.stategy.DiffItemTypePaidRecordProcess;
import com.lsj.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MqOpsServiceImpl implements MqOpsService {

    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;

    @Autowired
    private UserPaidTrackService userPaidTrackService;

    @Autowired
    private UserVipServiceMapper userVipServiceMapper;

    @Autowired
    private UserPaidTrackMapper userPaidTrackMapper;

    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private VipServiceConfigMapper vipServiceConfigMapper;

    @Autowired
    // 第一对：1001， AlbumItemTypePaidRecord Bean对象
    // 第二对：1002， TrackItemTypePaidRecord Bean对象
    // 第三对：1003， VipItemTypePaidRecord Bean对象
    private Map<String, DiffItemTypePaidRecordProcess> diffItemTypeMap;


    @Override

    public void userPaidRecordUpdate(String content) {

        // 1.反序列化
        UserPaidRecordVo userPaidRecordVo = JSONObject.parseObject(content, UserPaidRecordVo.class);


        // 2.获取属性
        String itemType = userPaidRecordVo.getItemType();

        // 3.根据付款项类型处理不同的流水
        // 策略模式优化 if...else if... else
        DiffItemTypePaidRecordProcess diffItemTypePaidRecordProcess = diffItemTypeMap.get(itemType);
        diffItemTypePaidRecordProcess.processPaidRecord(userPaidRecordVo);
    }

    private void processItemAlbumType(Long userId, String orderNo, List<Long> itemIdList) {

        // 1.根据订单编号查询付款项是专辑支付流水
        LambdaQueryWrapper<UserPaidAlbum> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPaidAlbum::getOrderNo, orderNo);
        wrapper.eq(UserPaidAlbum::getUserId, userId);
        UserPaidAlbum userPaidAlbum = userPaidAlbumMapper.selectOne(wrapper);
        if (userPaidAlbum != null) {
            log.error("付款项类型是专辑的支付流水已经存在。");
            return;
        }
        // 2.插入
        userPaidAlbum = new UserPaidAlbum();
        userPaidAlbum.setOrderNo(orderNo);
        userPaidAlbum.setUserId(userId);
        userPaidAlbum.setAlbumId(itemIdList.get(0));
        userPaidAlbumMapper.insert(userPaidAlbum);

    }


    @Transactional(rollbackFor = Exception.class)
    public void processItemVipType(Long userId, String orderNo, List<Long> itemIdList) {

        // 1.根据订单编号查询付款项是vip支付流水
        LambdaQueryWrapper<UserVipService> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserVipService::getOrderNo, orderNo);
        wrapper.eq(UserVipService::getUserId, userId);
        UserVipService userVipService = userVipServiceMapper.selectOne(wrapper);
        if (userVipService != null) {
            log.error("付款项类型是vip的支付流水已经存在。");
            return;
        }

        // 2.构建对象
        userVipService = new UserVipService();
        userVipService.setOrderNo(orderNo);
        userVipService.setUserId(userId);
        userVipService.setStartTime(new Date()); // vip套餐的开始时间


        // 2.1 根据用户id 查询用户对象
        UserInfo userInfo = userInfoMapper.selectById(userId);
        if (userInfo == null) {
            log.error("用户不存在");
            throw new TingShuException(500, "用户信息不存在");
        }
        Integer isVip = userInfo.getIsVip();
        Date vipExpireTime = userInfo.getVipExpireTime();

        // 2.2 根据套餐id 获取套餐信息
        VipServiceConfig vipServiceConfig = vipServiceConfigMapper.selectById(itemIdList.get(0));
        if (vipServiceConfig == null) {
            log.error("套餐不存在");
            throw new TingShuException(500, "套餐不存在");
        }
        Integer serviceMonth = vipServiceConfig.getServiceMonth();
        Calendar instance = Calendar.getInstance();  // 得到日历对象

        // 2.3 判断用户vip身份和
        if ("1".equals(isVip + "") && vipExpireTime.after(new Date())) {
            instance.setTime(vipExpireTime);
        } else {
            instance.setTime(new Date());
        }

        instance.add(Calendar.MONTH, serviceMonth);
        userVipService.setExpireTime(instance.getTime());  // vip套餐到期时间

        // 2.4 记录流水
        userVipServiceMapper.insert(userVipService);

        userInfo.setIsVip(1); // 用户的身份
        userInfo.setVipExpireTime(userVipService.getExpireTime());
        // 2.5 修改用户信息（vip身份 vip过期时间）
        userInfoMapper.updateById(userInfo);


    }

    @SneakyThrows
    private void processItemTrackType(Long userId, String orderNo, List<Long> itemIdList) {

        // 1.根据订单编号查询付款项是声音支付流水
        LambdaQueryWrapper<UserPaidTrack> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPaidTrack::getOrderNo, orderNo);
        wrapper.eq(UserPaidTrack::getUserId, userId);
        List<UserPaidTrack> userPaidTrackList = userPaidTrackMapper.selectList(wrapper);
        if (userPaidTrackList != null && userPaidTrackList.size() > 0) {
            log.error("付款项类型是声音的支付流水已经存在。");
            return;
        }
        // 2.根据声音id 查询专辑对象
        Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfoByTrackId(itemIdList.get(0));
        AlbumInfo albumInfoData = albumInfoResult.getData();
        Assert.notNull(albumInfoData, "远程查询专辑微服务获取专辑对象失败");

        List<UserPaidTrack> userPaidTracks = itemIdList.stream().map(trackId -> {
            UserPaidTrack userPaidTrack = new UserPaidTrack();
            userPaidTrack.setOrderNo(orderNo);
            userPaidTrack.setUserId(userId);
            userPaidTrack.setAlbumId(albumInfoData.getId());
            userPaidTrack.setTrackId(trackId);
            return userPaidTrack;
        }).collect(Collectors.toList());

        userPaidTrackService.saveBatch(userPaidTracks);


    }

}
