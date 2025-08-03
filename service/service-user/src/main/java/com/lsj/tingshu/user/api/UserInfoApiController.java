package com.lsj.tingshu.user.api;

import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.common.result.ResultCodeEnum;
import com.lsj.tingshu.common.service.login.annotation.TingShuLogin;
import com.lsj.tingshu.common.util.AuthContextHolder;
import com.lsj.tingshu.user.service.UserInfoService;
import com.lsj.tingshu.user.service.UserPaidTrackService;
import com.lsj.tingshu.vo.user.UserCollectVo;
import com.lsj.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户管理接口")
@RestController
@RequestMapping("api/user/userInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserInfoApiController {

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private UserPaidTrackService userPaidTrackService;

    @Operation(summary = "模拟弹窗")
    @GetMapping("/findUserSubscribePage/{pn}/{pz}")
    public Result findUserSubscribePage(@PathVariable(value = "pn") Long pn,
                                        @PathVariable(value = "pz") Long pz) {
        return Result.build(null, ResultCodeEnum.LOGIN_AUTH);
    }

    @GetMapping("/test")
    public Result test() {
        return Result.ok();
    }

    @GetMapping("/getUserInfo/{userId}")
    public Result<UserInfoVo> getUserInfo(@PathVariable String userId) {
        UserInfoVo userInfo = userInfoService.getUserInfo(Long.parseLong(userId));
        return Result.ok(userInfo);
    }

    // Request URL: http://localhost:8500/api/user/userInfo/isCollect/51943
    @GetMapping("/isCollect/{trackId}")
    @TingShuLogin
    public Result isCollect(@PathVariable Long trackId) {
        Long userId = AuthContextHolder.getUserId();
        Boolean isCollect = userInfoService.isCollect(trackId);
        return Result.ok(isCollect);
    }

    // Request URL: http://localhost:8500/api/user/userInfo/isSubscribe/1593
    @GetMapping("/isSubscribe/{albumId}")
    @TingShuLogin
    public Result isSubscribe(@PathVariable Long albumId) {
        Long userId = AuthContextHolder.getUserId();
        Boolean isSubscribe = userInfoService.isSubscribe(albumId);
        return Result.ok(isSubscribe);
    }

    // Request URL: http://localhost:8500/api/user/userInfo/collect/51943
    @GetMapping("/collect/{trackId}")
    @TingShuLogin
    public Result collect(@PathVariable Long trackId) {
        Boolean isCollect = userInfoService.isCollect(trackId);
        return Result.ok(isCollect);
    }

    // Request URL: http://localhost:8500/api/user/userInfo/findUserCollectPage/1/10
    @GetMapping("/findUserCollectPage/{pn}/{pz}")
    @TingShuLogin
    public Result findUserCollectPage(@PathVariable Long pn, @PathVariable Long pz) {
        IPage<UserCollectVo> userCollectVoPage = new Page<>(pn, pz);
        userCollectVoPage = userInfoService.findUserCollectPage(userCollectVoPage);
        return Result.ok(userCollectVoPage);
    }
}

