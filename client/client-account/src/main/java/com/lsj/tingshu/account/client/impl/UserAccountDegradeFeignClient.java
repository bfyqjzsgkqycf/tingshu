package com.lsj.tingshu.account.client.impl;

import com.lsj.tingshu.account.client.UserAccountFeignClient;
import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.vo.account.AccountLockResultVo;
import com.lsj.tingshu.vo.account.AccountLockVo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class UserAccountDegradeFeignClient implements UserAccountFeignClient {
    @Override
    public Result<AccountLockResultVo> checkAndLockUserAccount(AccountLockVo accountLockVo) {
        return Result.fail();
    }

    @Override
    public Result<BigDecimal> getRechargeAmountByOrderNoAndUserId(Long userId, String orderNo) {
        return Result.fail();
    }
}
