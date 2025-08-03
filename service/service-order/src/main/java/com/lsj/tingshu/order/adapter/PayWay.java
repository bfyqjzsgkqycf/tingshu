package com.lsj.tingshu.order.adapter;

import com.lsj.tingshu.vo.order.OrderInfoVo;

public interface PayWay {

    /**
     * 根据支付方式判断是否适配
     */
    public boolean supportPayWay(String payWay);


    /**
     * 适配支付方式之后的支付逻辑
     */
    void payWay(String orderNo, OrderInfoVo orderInfoVo, Long userId);

}
