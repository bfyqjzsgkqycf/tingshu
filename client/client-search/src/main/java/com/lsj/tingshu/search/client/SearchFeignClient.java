package com.lsj.tingshu.search.client;

import com.lsj.tingshu.search.client.impl.SearchDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@FeignClient(value = "service-search", fallback = SearchDegradeFeignClient.class)
public interface SearchFeignClient {
    @GetMapping("/api/inner/searchinfo/preElasticSearchAlbumToRedis")
    void preElasticSearchAlbumToRedis();
}