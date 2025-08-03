package com.lsj.tingshu.user.client;

import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.model.user.UserInfo;
import com.lsj.tingshu.model.user.VipServiceConfig;
import com.lsj.tingshu.user.client.impl.UserInfoDegradeFeignClient;
import com.lsj.tingshu.vo.user.UserInfoVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Primary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@Primary
@FeignClient(value = "service-user", fallback = UserInfoDegradeFeignClient.class, contextId = "userInfoFeignClient")
public interface UserInfoFeignClient {

    @GetMapping("/inner/rpc/userinfo/getUserInfo/{userId}")
    Result<UserInfoVo> getUserInfo(@PathVariable String userId);

    @GetMapping("/inner/rpc/userinfo/getIsPaidAlbum/{userId}/{albumId}")
    Result<Boolean> getIsPaidAlbum(@PathVariable(value = "userId") Long userId, @PathVariable Long albumId);

    @GetMapping("/inner/rpc/userinfo/getIsPaidAlbumTrack/{userId}/{albumId}")
    Result<Map<Long, String>> getIsPaidAlbumTrack(@PathVariable Long userId, @PathVariable Long albumId);

    @GetMapping("/inner/rpc/userinfo/getVipServiceById/{itemId}")
    Result<VipServiceConfig> getVipServiceById(@PathVariable(value = "itemId") Long itemId);

    @GetMapping("/inner/rpc/userinfo/getUserInfoByOpenId/{openId}")
    Result<UserInfo> getUserInfoByOpenId(@PathVariable(value = "openId") String openId);

    @GetMapping("/inner/rpc/userinfo/updateExpireTimeVip")
    void updateExpireTimeVip();
}