package com.lsj.tingshu.order.client.impl;

import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.model.order.OrderInfo;
import com.lsj.tingshu.order.client.OrderInfoFeignClient;
import org.springframework.stereotype.Component;

@Component
public class OrderInfoDegradeFeignClient implements OrderInfoFeignClient {

    @Override
    public Result<OrderInfo> getOrderInfoByOrderNoAndUserId(Long userId, String orderNo) {
        return Result.fail();
    }
}
