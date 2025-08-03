package com.lsj.tingshu.search.rpc;

import com.lsj.tingshu.search.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inner/searchinfo")
public class SearchInfoRpcController {

    @Autowired
    ItemService itemService;

    @GetMapping("/preElasticSearchAlbumToRedis")
    void preElasticSearchAlbumToRedis() {
        itemService.preAlbumToCache();
    }

}
