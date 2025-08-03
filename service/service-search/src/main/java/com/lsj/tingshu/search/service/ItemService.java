package com.lsj.tingshu.search.service;

import com.lsj.tingshu.vo.search.AlbumInfoIndexVo;

import java.util.List;
import java.util.Map;

public interface ItemService {

    void onSaleAlbum(Long albumId);

    void onOffAlbum(Long albumId);

    void batchOnOffAlbum();

    Map<String, Object> getAlbumInfo(Long albumId);

    void preAlbumToCache();

    List<AlbumInfoIndexVo> findRankingList(Long c1Id, String dimension);

    List<Long> getAlbumIdsFromDb();
}
