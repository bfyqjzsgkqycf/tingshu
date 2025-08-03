package com.lsj.tingshu.album.client;

import com.lsj.tingshu.album.client.impl.AlbumInfoDegradeFeignClient;
import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.model.album.AlbumInfo;
import com.lsj.tingshu.model.album.BaseCategory3;
import com.lsj.tingshu.model.album.BaseCategoryView;
import com.lsj.tingshu.model.album.TrackInfo;
import com.lsj.tingshu.vo.album.AlbumStatVo;
import com.lsj.tingshu.vo.album.TrackListVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Primary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 * @author qy
 */
@Primary
@FeignClient(value = "service-album", fallback = AlbumInfoDegradeFeignClient.class, contextId = "albumInfoFeignClient")
public interface AlbumInfoFeignClient {


    @GetMapping("/inner/rpc/albuminfo/getAlbumInfoAndTag/{albumId}")
    Result<AlbumInfo> getAlbumInfoAndTag(@PathVariable String albumId);

    @GetMapping("/inner/rpc/albuminfo/getCategoryInfoByAlbumId/{albumId}")
    Result<BaseCategoryView> getCategoryInfoByAlbumId(@PathVariable Long albumId);

    @GetMapping("/inner/rpc/albuminfo/getAlbumStatByAlbumId/{albumId}")
    Result<AlbumStatVo> getAlbumStatByAlbumId(@PathVariable Long albumId);

    @GetMapping("/inner/rpc/albuminfo/findTopBaseCategory3List/{c1Id}")
    Result<List<BaseCategory3>> findTopBaseCategory3List(@PathVariable Long c1Id);

    @GetMapping("/inner/rpc/albuminfo/getAllCategory1Ids")
    Result<List<Long>> getAllCategory1Ids();

    @PostMapping("/inner/rpc/albuminfo/getTrackVoListByTrackIds")
    Result<List<TrackListVo>> getTrackVoListByTrackIds(List<Long> userCollectTrackIdList);

    @GetMapping("/inner/rpc/albuminfo/getAlbumInfoIds")
    Result<List<Long>> getAlbumInfoIds();

    @GetMapping("/inner/rpc/albuminfo/getNeedPayTrackList/{userId}/{currentTrackId}/{trackCount}")
    Result<List<TrackInfo>> getNeedPayTrackList(@PathVariable(value = "userId") Long userId,
                                                @PathVariable(value = "currentTrackId") Long currentTrackId,
                                                @PathVariable(value = "trackCount") Integer trackCount);

    @GetMapping("/inner/rpc/albuminfo/getTrackInfoByTrackId/{currentTrackId}")
    Result<AlbumInfo> getAlbumInfoByTrackId(@PathVariable(value = "currentTrackId") Long currentTrackId);

}