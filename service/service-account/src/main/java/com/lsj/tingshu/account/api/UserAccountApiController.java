package com.lsj.tingshu.account.api;

import com.lsj.tingshu.account.service.UserAccountService;
import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.common.service.login.annotation.TingShuLogin;
import com.lsj.tingshu.common.util.AuthContextHolder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsj.tingshu.model.account.UserAccountDetail;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Tag(name = "用户账户管理")
@RestController
@RequestMapping("api/account/userAccount")
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserAccountApiController {

    @Autowired
    private UserAccountService userAccountService;

    @GetMapping("/getAvailableAmount")
    @TingShuLogin
    public Result getAvailableAmount() {

        BigDecimal availableAmount = userAccountService.getAvailableAmount(AuthContextHolder.getUserId());

        return Result.ok(availableAmount);
    }

    @GetMapping("/findUserConsumePage/{pn}/{pz}")
    @TingShuLogin
    public Result findUserConsumePage(@PathVariable(value = "pn") Long pn,
                                      @PathVariable(value = "pz") Long pz) {

        Page<UserAccountDetail> detailPage = new Page<>(pn, pz);
        Long userId = AuthContextHolder.getUserId();
        detailPage = userAccountService.findUserConsumePage(detailPage, userId);
        return Result.ok(detailPage);
    }

    // Request URL: http://localhost:8500/api/account/userAccount/findUserRechargePage/1/10
    @GetMapping("/findUserRechargePage/{pn}/{pz}")
    @TingShuLogin
    public Result findUserRechargePage(@PathVariable(value = "pn") Long pn,
                                      @PathVariable(value = "pz") Long pz) {

        Page<UserAccountDetail> detailPage = new Page<>(pn, pz);
        Long userId = AuthContextHolder.getUserId();
        detailPage = userAccountService.findUserRechargePage(detailPage, userId);
        return Result.ok(detailPage);
    }


}

