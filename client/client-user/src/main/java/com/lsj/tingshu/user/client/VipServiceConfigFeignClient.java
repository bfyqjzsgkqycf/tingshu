package com.lsj.tingshu.user.client;

import com.lsj.tingshu.user.client.impl.VipServiceConfigDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@FeignClient(value = "service-user", fallback = VipServiceConfigDegradeFeignClient.class, contextId = "vipServiceConfigFeignClient")
public interface VipServiceConfigFeignClient {


}