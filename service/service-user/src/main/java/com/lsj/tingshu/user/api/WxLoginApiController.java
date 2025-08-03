package com.lsj.tingshu.user.api;

import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.common.service.login.annotation.TingShuLogin;
import com.lsj.tingshu.common.util.AuthContextHolder;
import com.lsj.tingshu.user.service.UserInfoService;
import com.lsj.tingshu.vo.user.UserInfoVo;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "微信授权登录接口")
@RestController
@RequestMapping("/api/user/wxLogin")
@Slf4j
public class WxLoginApiController {

    @Autowired
    private UserInfoService userInfoService;

    //http://localhost:8500/api/user/wxLogin/wxLogin/0d3uA10008oxwU19ak0004qGfH0uA10u
    @GetMapping("/wxLogin/{code}")
    public Result wxLogin(@PathVariable(value = "code") String code) {
        Map<String, Object> map = userInfoService.wxLogin(code);
        return Result.ok(map);
    }

    //http://localhost:8500/api/user/wxLogin/getUserInfo
    @GetMapping("/getUserInfo")
    @TingShuLogin
    public Result getUserInfo() {
        Long userId = AuthContextHolder.getUserId();
        UserInfoVo userInfoVo = userInfoService.getUserInfo(userId);
        return Result.ok(userInfoVo);
    }

    //http://localhost:8500/api/user/wxLogin/updateUser
    @PostMapping("/updateUser")
    @TingShuLogin
    public Result updateUser(@RequestBody UserInfoVo userInfoVo) {
        userInfoService.updateUser(userInfoVo);
        return Result.ok();
    }

    @GetMapping("/auth/getNewAccessToken")
    @TingShuLogin
    public Result getNewAccessToken(HttpServletRequest request) {
        String refreshToken = request.getHeader("refreshToken");
        Map<String, Object> map = userInfoService.getNewAccessToken(refreshToken);
        return Result.ok(map);
    }
}
