package com.lsj.tingshu.order.closeorder.zset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
public class OrderInfoController {

    @Autowired
    private DelayOrderCloseZSetService delayOrderCloseService;


    @PostMapping("/createOrder/{orderId}")
    public String createOrder(@PathVariable(value = "orderId") String orderId) {

        delayOrderCloseService.addDelayOrder(orderId, 60);

        return "success";
    }
}
