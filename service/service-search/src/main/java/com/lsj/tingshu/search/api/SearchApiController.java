package com.lsj.tingshu.search.api;

import com.cache.annotaion.CacheAble;
import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.query.search.AlbumIndexQuery;
import com.lsj.tingshu.search.service.ItemService;
import com.lsj.tingshu.search.service.SearchService;
import com.lsj.tingshu.vo.search.AlbumInfoIndexVo;
import com.lsj.tingshu.vo.search.AlbumSearchResponseVo;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Tag(name = "搜索专辑管理")
@RestController
@RequestMapping("api/search/albumInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class SearchApiController {

    @Autowired
    private SearchService searchService;

    @Autowired
    private ItemService itemService;

    @PostMapping("/onSaleAlbum/{albumId}")
    public Result onSaleAlbum(@PathVariable Long albumId) {
        itemService.onSaleAlbum(albumId);
        return Result.ok();
    }

    @PostMapping("/onOffAlbum/{albumId}")
    public Result onOffAlbum(@PathVariable Long albumId) {
        itemService.onOffAlbum(albumId);
        return Result.ok();
    }

    @PostMapping("/batch/batchOnSaleAlbum")
    public Result batchOnSaleAlbum() {
        for (int i = 1; i < 1594; i++) {
            itemService.onSaleAlbum(Long.valueOf(i));
            if (i == 1577) {
                i += 15;
            }
        }
        return Result.ok();
    }

    @PostMapping("/batch/batchOnOffAlbum")
    public Result batchOnOffAlbum() {
        itemService.batchOnOffAlbum();
        return Result.ok();
    }

    // Request URL: http://localhost:8500/api/search/albumInfo/channel/1
    @GetMapping("/channel/{c1Id}")
    public Result channel(@PathVariable Long c1Id) {
        List<Map<String, Object>> list = searchService.channel(c1Id);
        return Result.ok(list);
    }

    // Request URL: http://localhost:8500/api/search/albumInfo
    @PostMapping
    public Result search(@RequestBody AlbumIndexQuery albumIndexQuery) {
        AlbumSearchResponseVo albumSearchResponseVo = searchService.search(albumIndexQuery);
        return Result.ok(albumSearchResponseVo);
    }

    // Request URL: http://localhost:8500/api/search/albumInfo/completeSuggest/%E5%8E%86%E5%8F%B2
    @GetMapping("/completeSuggest/{input}")
    public Result completeSuggest(@PathVariable String input) {
        Set<String> set = searchService.completeSuggest(input);
        return Result.ok(set);
    }

    // Request URL: http://localhost:8500/api/search/albumInfo/1255
    @GetMapping("/{albumId}")
    @CacheAble(cacheKey = "cache:info:#{#args[0]}",
            bloomKey="#{#args[0]}",
            lockKey="cache:lock:#{#args[0]}", enableDistroLock=true, enableDistroBloom=true)   // 当程序运行的时候，我希望，程序员说用目标方法的哪个参数作为缓存key,切面逻辑就要拼接对应外部程序员指定的。
    public Result getAlbumInfo(@PathVariable(value = "albumId") Long albumId) {

        Map<String, Object> map = itemService.getAlbumInfo(albumId);

        return Result.ok(map);
    }

    // Request URL: http://localhost:8500/api/search/albumInfo/findRankingList/1/hotScore
    @GetMapping("/findRankingList/{c1Id}/{dimension}")
    public Result findRankingList(
            @PathVariable Long c1Id,
            @PathVariable String dimension
    ) {
        List<AlbumInfoIndexVo> list = itemService.findRankingList(c1Id, dimension);
        return Result.ok(list);
    }

    @PostMapping("/preAlbumToCache")
    public Result preAlbumToCache() {
        itemService.preAlbumToCache();
        return Result.ok();
    }
}

