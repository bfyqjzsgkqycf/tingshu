package com.lsj.tingshu.account.service;

import com.lsj.tingshu.model.account.RechargeInfo;
import com.lsj.tingshu.vo.account.RechargeInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.Map;

public interface RechargeInfoService extends IService<RechargeInfo> {


    /**
     * 根据用户id和订单编号查询充值金额
     *
     * @param userId
     * @param orderNo
     * @return
     */
    BigDecimal getRechargeAmountByOrderNoAndUserId(Long userId, String orderNo);





    /**
     * 零钱充值
     *
     * @param rechargeInfoVo
     * @return
     */
    Map<String, Object> submitRecharge(RechargeInfoVo rechargeInfoVo);


}
