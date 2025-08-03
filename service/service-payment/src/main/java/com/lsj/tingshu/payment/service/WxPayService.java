package com.lsj.tingshu.payment.service;

import com.wechat.pay.java.service.payments.model.Transaction;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface WxPayService {

    /**
     * 预下单处理
     *
     * @param tradeType
     * @param orderNo
     * @return
     */
    Map<String, Object> createJsapi(String tradeType, String orderNo);


    /**
     * 根据订单编号主动查询订单的支付状态
     *
     * @param orderNo
     * @return
     */
    Transaction  queryPayStatus(String orderNo);

    /***
     * 微信支付成功之后做的事
     * @param orderNo
     */
    void processWxPaidSuccess(String orderNo,Long userId);

    /**
     * 支付成功的异步回调
     * @param httpServletRequest
     * @return
     */
    Transaction asyncNotify(HttpServletRequest httpServletRequest);




}
