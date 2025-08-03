package com.lsj.tingshu.order.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lsj.tingshu.common.service.constant.SystemConstant;
import com.lsj.tingshu.model.order.LocalMsg;
import com.lsj.tingshu.order.mapper.LocalMsgMapper;
import com.lsj.tingshu.order.mapper.OrderInfoMapper;
import com.lsj.tingshu.order.service.MqOpsService;
import com.lsj.tingshu.order.service.OrderInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MqOpsServiceImpl implements MqOpsService {
    @Autowired
    private LocalMsgMapper localMsgMapper;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderInfoService orderInfoService;

    @Override
    public void localMsgStatusUpdate(String orderNo) {
        LocalMsg localMsg = localMsgMapper.selectOne(new LambdaQueryWrapper<LocalMsg>().eq(LocalMsg::getMsgContent, orderNo));
        if (localMsg != null) {
            localMsg.setStatus(1);
            localMsgMapper.updateById(localMsg);
        }
    }

    @Override
    public void closeOrder(String content) {

        // 1.反序列化
        Map map = JSONObject.parseObject(content, Map.class);
        Integer userId = (Integer) map.get("userId");
        String orderNo = (String) map.get("orderNo");

        // 2.将订单状态修改为已关闭
        orderInfoMapper.closeOrder(orderNo, Long.parseLong(userId.toString()), SystemConstant.ORDER_STATUS_CANCEL);

    }

    @Override
    public void wxPaidSuccess(String content) {

        // 1.反序列化
        JSONObject jsonObject = JSONObject.parseObject(content, JSONObject.class);
        String userId = (String) jsonObject.get("userId");
        String orderNo = (String) jsonObject.get("orderNo");

        // 2.直接调用支付成功后的逻辑
        orderInfoService.processPaySuccess(orderNo, Long.parseLong(userId));
    }
}