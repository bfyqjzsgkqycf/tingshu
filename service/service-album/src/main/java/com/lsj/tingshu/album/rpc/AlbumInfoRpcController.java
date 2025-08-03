package com.lsj.tingshu.album.rpc;

import com.lsj.tingshu.album.service.AlbumInfoService;
import com.lsj.tingshu.album.service.BaseCategoryService;
import com.lsj.tingshu.album.service.TrackInfoService;
import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.model.album.AlbumInfo;
import com.lsj.tingshu.model.album.BaseCategory3;
import com.lsj.tingshu.model.album.BaseCategoryView;
import com.lsj.tingshu.model.album.TrackInfo;
import com.lsj.tingshu.vo.album.AlbumStatVo;
import com.lsj.tingshu.vo.album.TrackListVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inner/rpc/albuminfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class AlbumInfoRpcController {

    @Autowired
    private AlbumInfoService albumInfoService;

    @Autowired
    private TrackInfoService trackInfoService;

    @Autowired
    private BaseCategoryService baseCategoryService;

    @GetMapping("/getAlbumInfoAndTag/{albumId}")
    public Result<AlbumInfo> getAlbumInfoAndTag(@PathVariable String albumId) {
        AlbumInfo albumInfo = albumInfoService.getAlbumInfo(Long.parseLong(albumId));
        return Result.ok(albumInfo);
    }

    @GetMapping("/getCategoryInfoByAlbumId/{albumId}")
    public Result<BaseCategoryView> getCategoryInfoByAlbumId(@PathVariable String albumId) {
        BaseCategoryView categoryInfoByAlbumId = baseCategoryService.getCategoryInfoByAlbumId(Long.parseLong(albumId));
        return Result.ok(categoryInfoByAlbumId);
    }

    @GetMapping("/getAlbumStatByAlbumId/{albumId}")
    public Result<AlbumStatVo> getAlbumStatByAlbumId(@PathVariable String albumId) {
        AlbumStatVo albumStat = albumInfoService.getAlbumStatByAlbumId(Long.parseLong(albumId));
        return Result.ok(albumStat);
    }

    @GetMapping("/findTopBaseCategory3List/{c1Id}")
    public Result findTopBaseCategory3List(@PathVariable Long c1Id) {
        List<BaseCategory3> list = albumInfoService.findTopBaseCategory3List(c1Id);
        return Result.ok(list);
    }

    @GetMapping("/getAllCategory1Ids")
    Result<List<Long>> getAllCategory1Ids() {
        List<Long> category1Ids = baseCategoryService.getAllCategory1Ids();
        return Result.ok(category1Ids);
    }

    @PostMapping("/getTrackVoListByTrackIds")
    Result<List<TrackListVo>> getTrackVoListByTrackIds(@RequestBody List<Long> userCollectTrackIdList) {
        List<TrackListVo> trackVoList = trackInfoService.getTrackVoListByTrackIds(userCollectTrackIdList);
        return Result.ok(trackVoList);
    }

    @GetMapping("/getAlbumInfoIds")
    Result<List<Long>> getAlbumInfoIds() {
        List<Long> albumIds = albumInfoService.getAlbumInfoIds();
        return Result.ok(albumIds);
    }

    @GetMapping("/getNeedPayTrackList/{userId}/{currentTrackId}/{trackCount}")
    Result<List<TrackInfo>> getNeedPayTrackList(@PathVariable(value = "userId") Long userId,
                                                @PathVariable(value = "currentTrackId") Long currentTrackId,
                                                @PathVariable(value = "trackCount") Integer trackCount) {


        List<TrackInfo> trackInfoList = trackInfoService.getNeedPayTrackList(userId, currentTrackId, trackCount);
        return Result.ok(trackInfoList);
    }

    @GetMapping("/getTrackInfoByTrackId/{currentTrackId}")
    Result<AlbumInfo> getAlbumInfoByTrackId(@PathVariable(value = "currentTrackId") Long currentTrackId) {

        AlbumInfo albumInfo = trackInfoService.getAlbumInfoByTrackId(currentTrackId);

        return Result.ok(albumInfo);

    }

}

