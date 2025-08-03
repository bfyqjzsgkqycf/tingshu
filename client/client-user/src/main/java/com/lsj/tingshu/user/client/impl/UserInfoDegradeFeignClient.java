package com.lsj.tingshu.user.client.impl;

import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.model.user.UserInfo;
import com.lsj.tingshu.model.user.VipServiceConfig;
import com.lsj.tingshu.user.client.UserInfoFeignClient;
import com.lsj.tingshu.vo.user.UserInfoVo;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UserInfoDegradeFeignClient implements UserInfoFeignClient {

    @Override
    public Result<UserInfoVo> getUserInfo(String userId) {
        return Result.fail();
    }

    @Override
    public Result<Boolean> getIsPaidAlbum(Long userId, Long albumId) {
        return Result.fail();
    }

    @Override
    public Result<Map<Long, String>> getIsPaidAlbumTrack(Long userId, Long albumId) {
        return Result.fail();
    }

    @Override
    public Result<VipServiceConfig> getVipServiceById(Long itemId) {
        return Result.fail();
    }

    @Override
    public Result<UserInfo> getUserInfoByOpenId(String openId) {
        return Result.fail();
    }

    @Override
    public void updateExpireTimeVip() {

    }

}
