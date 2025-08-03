package com.lsj.tingshu.order.rpc;

import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.model.order.OrderInfo;
import com.lsj.tingshu.order.service.OrderInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inner/orderinfo")
public class OrderInfoRpcController {

    @Autowired
    private OrderInfoService orderInfoService;

    @GetMapping("/getOrderInfoByOrderNoAndUserId/{userId}/{orderNo}")
    Result<OrderInfo> getOrderInfoByOrderNoAndUserId(@PathVariable(value = "userId") Long userId,
                                                     @PathVariable(value = "orderNo") String orderNo) {

        OrderInfo orderInfo = orderInfoService.getOrderInfoByOrderNoAndUserId(userId, orderNo);

        return Result.ok(orderInfo);
    }

}
