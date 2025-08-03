package com.lsj.tingshu.account.api;

import com.lsj.tingshu.account.service.RechargeInfoService;
import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.common.service.login.annotation.TingShuLogin;
import com.lsj.tingshu.vo.account.RechargeInfoVo;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "充值管理")
@RestController
@RequestMapping("api/account/rechargeInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class RechargeInfoApiController {

    @Autowired
    private RechargeInfoService rechargeInfoService;

    @PostMapping("/submitRecharge")
    @TingShuLogin
    public Result submitRecharge(@RequestBody RechargeInfoVo rechargeInfoVo) {
        Map<String, Object> orderNoMap = rechargeInfoService.submitRecharge(rechargeInfoVo);
        return Result.ok(orderNoMap);
    }

}

