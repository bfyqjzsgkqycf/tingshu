package com.lsj.tingshu.album.client;

import com.lsj.tingshu.album.client.impl.TrackInfoDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@FeignClient(value = "service-track", fallback = TrackInfoDegradeFeignClient.class, contextId = "trackInfoFeignClient")
public interface TrackInfoFeignClient {

}