package com.lsj.tingshu.user.service;

import com.lsj.tingshu.model.user.UserPaidAlbum;
import com.lsj.tingshu.model.user.UserPaidTrack;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface UserPaidTrackService extends IService<UserPaidTrack> {

    UserPaidAlbum getUserPaidAlbum(Long userId, Long albumId);

    List<UserPaidTrack> getUserPaidTrack(Long userId, Long albumId);
}
