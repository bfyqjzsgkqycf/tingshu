package com.lsj.tingshu.account.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.model.account.UserAccount;
import com.lsj.tingshu.model.account.UserAccountDetail;
import com.lsj.tingshu.vo.account.AccountLockResultVo;
import com.lsj.tingshu.vo.account.AccountLockVo;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

public interface UserAccountService extends IService<UserAccount> {


    /**
     * 检查并且锁定账户余额
     *
     * @param accountLockVo
     * @return
     */
    Result<AccountLockResultVo> checkAndLockUserAccount(@Param("accountLockVo") AccountLockVo accountLockVo);


    /**
     * 用户账户记录流水表
     */

    void log(Long userId, String title, String tradeType, BigDecimal amount, String orderNo);


    /**
     * 根据用户id 查询用户可用余额
     *
     * @param userId
     * @return
     */
    BigDecimal getAvailableAmount(Long userId);

    /**
     * 分页查询用户消费记录
     *
     * @param detailPage
     * @param userId
     * @return
     */
    Page<UserAccountDetail> findUserConsumePage(Page<UserAccountDetail> detailPage, Long userId);


    /**
     * 分页查询用户充值记录
     *
     * @param detailPage
     * @param userId
     * @return
     */
    Page<UserAccountDetail> findUserRechargePage(Page<UserAccountDetail> detailPage, Long userId);


}
