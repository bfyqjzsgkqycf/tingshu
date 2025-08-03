package com.lsj.tingshu.album.client.impl;

import com.lsj.tingshu.album.client.AlbumInfoFeignClient;
import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.model.album.AlbumInfo;
import com.lsj.tingshu.model.album.TrackInfo;
import com.lsj.tingshu.vo.album.TrackListVo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AlbumInfoDegradeFeignClient implements AlbumInfoFeignClient {

    @Override
    public Result getAlbumInfoAndTag(String albumId) {
        return Result.fail();
    }

    @Override
    public Result getCategoryInfoByAlbumId(Long albumId) {
        return Result.fail();
    }

    @Override
    public Result getAlbumStatByAlbumId(Long albumId) {
        return Result.fail();
    }

    @Override
    public Result findTopBaseCategory3List(Long c1Id) {
        return Result.fail();
    }

    @Override
    public Result<List<Long>> getAllCategory1Ids() {
        return Result.fail();
    }

    @Override
    public Result<List<TrackListVo>> getTrackVoListByTrackIds(List<Long> userCollectTrackIdList) {
        return Result.fail();
    }

    @Override
    public Result<List<Long>> getAlbumInfoIds() {
        return Result.fail();
    }

    @Override
    public Result<List<TrackInfo>> getNeedPayTrackList(Long userId, Long currentTrackId, Integer trackCount) {
        return null;
    }

    @Override
    public Result<AlbumInfo> getAlbumInfoByTrackId(Long currentTrackId) {
        return null;
    }

}
