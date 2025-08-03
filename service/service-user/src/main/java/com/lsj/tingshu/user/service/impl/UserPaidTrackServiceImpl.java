package com.lsj.tingshu.user.service.impl;

import com.lsj.tingshu.model.user.UserPaidAlbum;
import com.lsj.tingshu.model.user.UserPaidTrack;
import com.lsj.tingshu.user.mapper.UserPaidAlbumMapper;
import com.lsj.tingshu.user.mapper.UserPaidTrackMapper;
import com.lsj.tingshu.user.service.UserPaidTrackService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserPaidTrackServiceImpl extends ServiceImpl<UserPaidTrackMapper, UserPaidTrack> implements UserPaidTrackService {

    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;

    @Autowired
    private UserPaidTrackMapper userPaidTrackMapper;

    @Override
    public UserPaidAlbum getUserPaidAlbum(Long userId, Long albumId) {
        LambdaQueryWrapper<UserPaidAlbum> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPaidAlbum::getUserId, userId);
        wrapper.eq(UserPaidAlbum::getAlbumId, albumId);
        return userPaidAlbumMapper.selectOne(wrapper);
    }

    @Override
    public List<UserPaidTrack> getUserPaidTrack(Long userId, Long albumId) {
        LambdaQueryWrapper<UserPaidTrack> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPaidTrack::getUserId, userId);
        wrapper.eq(UserPaidTrack::getAlbumId, albumId);
        return userPaidTrackMapper.selectList(wrapper);
    }
}
