package com.lsj.tingshu.order.client;

import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.model.order.OrderInfo;
import com.lsj.tingshu.order.client.impl.OrderInfoDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@FeignClient(value = "service-order", fallback = OrderInfoDegradeFeignClient.class)
public interface OrderInfoFeignClient {

    @GetMapping("/api/inner/orderinfo/getOrderInfoByOrderNoAndUserId/{userId}/{orderNo}")
    Result<OrderInfo> getOrderInfoByOrderNoAndUserId(
            @PathVariable(value = "userId") Long userId,
            @PathVariable(value = "orderNo") String orderNo
    );

}