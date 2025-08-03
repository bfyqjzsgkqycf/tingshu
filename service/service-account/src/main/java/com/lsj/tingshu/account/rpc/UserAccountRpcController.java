package com.lsj.tingshu.account.rpc;

import com.lsj.tingshu.account.service.RechargeInfoService;
import com.lsj.tingshu.account.service.UserAccountService;
import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.vo.account.AccountLockResultVo;
import com.lsj.tingshu.vo.account.AccountLockVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/inner/accountinfo")
public class UserAccountRpcController {

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private RechargeInfoService rechargeInfoService;

    @PostMapping("/checkAndLockUserAccount")
    Result<AccountLockResultVo> checkAndLockUserAccount(@RequestBody AccountLockVo accountLockVo) {

        Result<AccountLockResultVo> accountLockResultVoResult = userAccountService.checkAndLockUserAccount(accountLockVo);
        // 99
        return accountLockResultVoResult;

    }


    @GetMapping("/getRechargeInfoByOrderNoAndUserId/{userId}/{orderNo}")
    Result<BigDecimal> getRechargeAmountByOrderNoAndUserId(@PathVariable(value = "userId") Long userId,
                                                           @PathVariable(value = "orderNo") String orderNo) {
        BigDecimal rechargeAmount = rechargeInfoService.getRechargeAmountByOrderNoAndUserId(userId, orderNo);
        return Result.ok(rechargeAmount);
    }
}
