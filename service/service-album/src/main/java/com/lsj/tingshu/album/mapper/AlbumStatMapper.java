package com.lsj.tingshu.album.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lsj.tingshu.model.album.AlbumStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AlbumStatMapper extends BaseMapper<AlbumStat> {


    void updateAlbumStatNum(@Param("albumId") Long albumId, @Param("statType") String statType, @Param("count") Integer count);

}
