package com.lsj.tingshu.album.receiver.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.lsj.tingshu.album.mapper.AlbumStatMapper;
import com.lsj.tingshu.album.mapper.TrackStatMapper;
import com.lsj.tingshu.album.receiver.service.MqOpsService;
import com.lsj.tingshu.album.service.TrackInfoService;
import com.lsj.tingshu.common.service.constant.SystemConstant;
import com.lsj.tingshu.common.service.execption.TingShuException;
import com.lsj.tingshu.model.album.AlbumInfo;
import com.lsj.tingshu.vo.album.TrackStatMqVo;
import com.lsj.tingshu.vo.user.UserPaidRecordVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class MqOpsServiceImpl implements MqOpsService {


    @Autowired
    private TrackStatMapper trackStatMapper;

    @Autowired
    private AlbumStatMapper albumStatMapper;


    @Autowired
    private TrackInfoService trackInfoService;

    @Override
    public void albumAndTrackStatUpdate(String content) {

        // 1.处理消息
        TrackStatMqVo trackStatMqVo = JSONObject.parseObject(content, TrackStatMqVo.class);
        Long albumId = trackStatMqVo.getAlbumId();  // 有可能是空  （收藏量对应的是空 如果是播放量不为空)
        Long trackId = trackStatMqVo.getTrackId();
        String statType = trackStatMqVo.getStatType();
        Integer count = trackStatMqVo.getCount();


        try {
            // 2.处理业务
            // 2.1 修改专辑的播放量(不修改收藏量)
            if (albumId != null) {
                albumStatMapper.updateAlbumStatNum(albumId, SystemConstant.ALBUM_STAT_PLAY, count);
            }
            // 2.2 修改声音的播放量/收藏量
            trackStatMapper.updateTrackStatNum(trackId, statType, count);
        } catch (Exception e) {
            throw new TingShuException(500, "数据库操作失败");
        }


    }

    @Override
    public void albumStatUpdate(String content) {

        // 1.反序列化
        UserPaidRecordVo userPaidRecordVo = JSONObject.parseObject(content, UserPaidRecordVo.class);

        // 2.获取属性
        List<Long> itemIdList = userPaidRecordVo.getItemIdList();
        String itemType = userPaidRecordVo.getItemType();
        Long albumId = 0L;
        // 3.判断付款项类型
        if ("1001".equals(itemType)) {
            albumId = itemIdList.get(0);

        } else if ("1002".equals(itemType)) {
            Long trackId = itemIdList.get(0);
            AlbumInfo albumInfo = trackInfoService.getAlbumInfoByTrackId(trackId);
            albumId = albumInfo.getId();
        } else {
            return;
        }

        // 修改专辑的购买量
        albumStatMapper.updateAlbumStatNum(albumId, "0403", itemIdList.size());

    }
}
