package com.lsj.tingshu.user.api;

import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.common.service.login.annotation.TingShuLogin;
import com.lsj.tingshu.user.service.UserListenProcessService;
import com.lsj.tingshu.vo.user.UserListenProcessVo;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "用户声音播放进度管理接口")
@RestController
@RequestMapping("api/user/userListenProcess")
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserListenProcessApiController {

    @Autowired
    private UserListenProcessService userListenProcessService;

    // Request URL: http://localhost:8500/api/user/userListenProcess/getTrackBreakSecond/51943
    @GetMapping("/getTrackBreakSecond/{trackId}")
    @TingShuLogin
    public Result getTrackBreakSecond(@PathVariable Long trackId) {
        BigDecimal trackBreakSecond = userListenProcessService.getTrackBreakSecond(trackId);
        return Result.ok(trackBreakSecond);
    }

    // Request URL: http://localhost:8500/api/user/userListenProcess/updateListenProcess
    @PostMapping("/updateListenProcess")
    @TingShuLogin
    public Result updateListenProcess(@RequestBody UserListenProcessVo userListenProcess) {
        userListenProcessService.updateListenProcess(userListenProcess);
        return Result.ok();
    }

    // Request URL: http://localhost:8500/api/user/userListenProcess/getLatelyTrack
    @GetMapping("/getLatelyTrack")
    @TingShuLogin
    public Result getLatelyTrack() {
        Map<String, Object> map = userListenProcessService.getLatelyTrack();
        return Result.ok(map);
    }
}

