package com.lsj.tingshu.album.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lsj.tingshu.model.album.TrackInfo;
import com.lsj.tingshu.query.album.TrackInfoQuery;
import com.lsj.tingshu.vo.album.AlbumTrackListVo;
import com.lsj.tingshu.vo.album.TrackListVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TrackInfoMapper extends BaseMapper<TrackInfo> {
    IPage<TrackListVo> findUserTrackPage(IPage page, @Param("vo") TrackInfoQuery trackInfoQuery);

    IPage<AlbumTrackListVo> findAlbumTrackPage(@Param("albumId") Long albumId, @Param("page") IPage<AlbumTrackListVo> page);



}
