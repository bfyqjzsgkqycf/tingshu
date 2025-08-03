package com.lsj.tingshu.user.stategy.impl;

import com.lsj.tingshu.album.client.AlbumInfoFeignClient;
import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.model.album.AlbumInfo;
import com.lsj.tingshu.model.user.UserPaidTrack;
import com.lsj.tingshu.user.mapper.UserPaidTrackMapper;
import com.lsj.tingshu.user.service.UserPaidTrackService;
import com.lsj.tingshu.user.stategy.DiffItemTypePaidRecordProcess;
import com.lsj.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component(value = "1002")
public class TrackItemTypePaidRecord implements DiffItemTypePaidRecordProcess {


    @Autowired
    private UserPaidTrackMapper userPaidTrackMapper;
    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Autowired
    private UserPaidTrackService userPaidTrackService;


    @Override
    public void processPaidRecord(UserPaidRecordVo userPaidRecordVo) {
        // 1.根据订单编号查询付款项是声音支付流水
        LambdaQueryWrapper<UserPaidTrack> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPaidTrack::getOrderNo, userPaidRecordVo.getOrderNo());
        wrapper.eq(UserPaidTrack::getUserId, userPaidRecordVo.getUserId());
        List<UserPaidTrack> userPaidTrackList = userPaidTrackMapper.selectList(wrapper);
        if (userPaidTrackList != null && userPaidTrackList.size() > 0) {
            log.error("付款项类型是声音的支付流水已经存在。");
            return;
        }
        // 2.根据声音id 查询专辑对象
        Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfoByTrackId(userPaidRecordVo.getItemIdList().get(0));
        AlbumInfo albumInfoData = albumInfoResult.getData();
        Assert.notNull(albumInfoData, "远程查询专辑微服务获取专辑对象失败");

        List<UserPaidTrack> userPaidTracks = userPaidRecordVo.getItemIdList().stream().map(trackId -> {
            UserPaidTrack userPaidTrack = new UserPaidTrack();
            userPaidTrack.setOrderNo(userPaidRecordVo.getOrderNo());
            userPaidTrack.setUserId(userPaidRecordVo.getUserId());
            userPaidTrack.setAlbumId(albumInfoData.getId());
            userPaidTrack.setTrackId(trackId);
            return userPaidTrack;
        }).collect(Collectors.toList());

        userPaidTrackService.saveBatch(userPaidTracks);
    }
}
