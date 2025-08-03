package com.lsj.tingshu.account.client;

import com.lsj.tingshu.account.client.impl.UserAccountDegradeFeignClient;
import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.vo.account.AccountLockResultVo;
import com.lsj.tingshu.vo.account.AccountLockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@FeignClient(value = "service-account", fallback = UserAccountDegradeFeignClient.class, contextId = "userAccountFeignClient")
public interface UserAccountFeignClient {

    @PostMapping("/api/inner/accountinfo/checkAndLockUserAccount")
    Result<AccountLockResultVo> checkAndLockUserAccount(@RequestBody AccountLockVo accountLockVo);

    @GetMapping("/api/inner/accountinfo/getRechargeInfoByOrderNoAndUserId/{userId}/{orderNo}")
    Result<BigDecimal> getRechargeAmountByOrderNoAndUserId(
            @PathVariable(value = "userId") Long userId,
            @PathVariable(value = "orderNo") String orderNo
    );

}