package com.lsj.tingshu.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lsj.tingshu.model.payment.PaymentInfo;

public interface PaymentInfoService extends IService<PaymentInfo> {

    /**
     * 保存订单支付相关信息
     * @param tradeType
     * @param orderNo
     * @param userId
     */
    PaymentInfo saveOrderPaymentInfo(String tradeType, String orderNo, Long userId);



}
