package com.lsj.tingshu.album.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lsj.tingshu.model.album.TrackStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TrackStatMapper extends BaseMapper<TrackStat> {

    void updateTrackStatNum(@Param("trackId") Long trackId, @Param("statType") String statType, @Param("count") Integer count);

}
