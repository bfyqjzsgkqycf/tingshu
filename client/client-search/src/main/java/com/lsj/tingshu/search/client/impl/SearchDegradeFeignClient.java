package com.lsj.tingshu.search.client.impl;

import com.lsj.tingshu.search.client.SearchFeignClient;
import org.springframework.stereotype.Component;

@Component
public class SearchDegradeFeignClient implements SearchFeignClient {

    @Override
    public void preElasticSearchAlbumToRedis() {

    }
}
