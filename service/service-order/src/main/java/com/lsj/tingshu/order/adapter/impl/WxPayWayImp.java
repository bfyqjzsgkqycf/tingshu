package com.lsj.tingshu.order.adapter.impl;

import com.alibaba.fastjson.JSONObject;
import com.lsj.tingshu.common.rabbit.constant.MqConst;
import com.lsj.tingshu.common.rabbit.service.RabbitService;
import com.lsj.tingshu.order.adapter.PayWay;
import com.lsj.tingshu.order.service.OrderInfoService;
import com.lsj.tingshu.vo.order.OrderInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class WxPayWayImp implements PayWay {

    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private RabbitService rabbitService;

    @Override
    public boolean supportPayWay(String payWay) {
        return "1101".equals(payWay);
    }

    @Override
    public void payWay(String orderNo, OrderInfoVo orderInfoVo, Long userId) {
        // 微信支付逻辑
        // 1.保存订单信息
        orderInfoService.saveOrderInfo(orderNo, orderInfoVo, userId);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("userId", userId);
        map.put("orderNo", orderNo);

        // 2.指定订单的过期时间
//        rabbitService.sendDealyMessage(MqConst.EXCHANGE_CANCEL_ORDER, MqConst.ROUTING_CANCEL_ORDER, JSONObject.toJSONString(map), 60);
        rabbitService.sendDealyMessage(MqConst.EXCHANGE_CANCEL_ORDER, MqConst.ROUTING_CANCEL_ORDER, JSONObject.toJSONString(map), MqConst.CANCEL_ORDER_DELAY_TIME);// 线上用

    }
}
