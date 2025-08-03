package com.lsj.tingshu.account.client;

import com.lsj.tingshu.account.client.impl.RechargeInfoDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@FeignClient(value = "service-account", fallback = RechargeInfoDegradeFeignClient.class, contextId = "rechargeInfoFeignClient")
public interface RechargeInfoFeignClient {


}