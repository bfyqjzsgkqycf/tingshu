package com.lsj.tingshu.album.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsj.tingshu.model.album.AlbumInfo;
import com.lsj.tingshu.model.album.TrackInfo;
import com.lsj.tingshu.query.album.TrackInfoQuery;
import com.lsj.tingshu.vo.album.AlbumTrackListVo;
import com.lsj.tingshu.vo.album.TrackInfoVo;
import com.lsj.tingshu.vo.album.TrackListVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface TrackInfoService extends IService<TrackInfo> {

    Map<String, Object> uploadTrack(MultipartFile file);

    void saveTrackInfo(Long userId, TrackInfoVo trackInfoVo);

    IPage<TrackListVo> findUserTrackPage(IPage page, TrackInfoQuery trackInfoQuery);

    void updateTrackInfo(Long trackId, TrackInfoVo trackInfoVo);

    void removeTrackInfo(Long trackId);

    IPage<AlbumTrackListVo> findAlbumTrackPage(Long albumId, IPage<AlbumTrackListVo> page);

    TrackInfoVo getTrackStatVo(Long trackId);

    List<TrackListVo> getTrackVoListByTrackIds(List<Long> userCollectTrackIdList);

    List<Map<String, Object>> findUserTrackPaidList(Long trackId);

    List<TrackInfo> getNeedPayTrackList(Long userId, Long currentTrackId, Integer trackCount);

    AlbumInfo getAlbumInfoByTrackId(Long currentTrackId);
}
