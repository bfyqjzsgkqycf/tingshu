package com.lsj.tingshu.user.rpc;

import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.model.user.UserInfo;
import com.lsj.tingshu.model.user.VipServiceConfig;
import com.lsj.tingshu.user.service.UserInfoService;
import com.lsj.tingshu.user.service.VipServiceConfigService;
import com.lsj.tingshu.vo.user.UserInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/inner/rpc/userinfo")
public class UserInfoRpcController {

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private VipServiceConfigService vipServiceConfigService;


    @GetMapping("/getUserInfo/{userId}")
    Result<UserInfoVo> getUserInfo(@PathVariable(value = "userId") String userId) {

        UserInfoVo userInfo = userInfoService.getUserInfo(Long.parseLong(userId));
        return Result.ok(userInfo);

    }

    @GetMapping("/getIsPaidAlbum/{userId}/{albumId}")
    Result<Boolean> getIsPaidAlbum(@PathVariable(value = "userId") Long userId, @PathVariable(value = "albumId") Long albumId) {

        Boolean isPaidAlbum = userInfoService.getIsPaidAlbum(userId, albumId);

        return Result.ok(isPaidAlbum);
    }

    @GetMapping("/getIsPaidAlbumTrack/{userId}/{albumId}")
    Result<Map<Long, String>> getIsPaidAlbumTrack(@PathVariable(value = "userId") Long userId, @PathVariable(value = "albumId") Long albumId) {
        Map<Long, String> isPaidAlbumTrack = userInfoService.getIsPaidAlbumTrack(userId, albumId);
        return Result.ok(isPaidAlbumTrack);
    }

    @GetMapping("/getVipServiceById/{itemId}")
    Result<VipServiceConfig> getVipServiceById(@PathVariable(value = "itemId") Long itemId) {

        VipServiceConfig vipServiceConfig = vipServiceConfigService.getById(itemId);
        return Result.ok(vipServiceConfig);
    }


    @GetMapping("/getUserInfoByOpenId/{openId}")
    Result<UserInfo> getUserInfoByOpenId(@PathVariable(value = "openId") String openId) {
        UserInfo userInfo = userInfoService.getUserInfoByOpenId(openId);
        return Result.ok(userInfo);
    }

    @GetMapping("/updateExpireTimeVip")
    void updateExpireTimeVip() {
        userInfoService.updateExpireTimeVip();
    }
}
