package com.lsj.tingshu.order.adapter.impl;

import com.lsj.tingshu.order.adapter.PayWay;
import com.lsj.tingshu.vo.order.OrderInfoVo;
import org.springframework.stereotype.Service;

@Service
public class ZhifubaoPayWayImp implements PayWay {

    @Override
    public boolean supportPayWay(String payWay) {
        return "1102".equals(payWay);
    }

    @Override
    public void payWay(String orderNo, OrderInfoVo orderInfoVo, Long userId) {
        // 支付宝支付逻辑
    }
}
