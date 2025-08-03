package com.lsj.tingshu.album.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lsj.tingshu.model.album.AlbumInfo;
import com.lsj.tingshu.query.album.AlbumInfoQuery;
import com.lsj.tingshu.vo.album.AlbumListVo;
import com.lsj.tingshu.vo.album.AlbumStatVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AlbumInfoMapper extends BaseMapper<AlbumInfo> {

    IPage<AlbumListVo> findUserAlbumPage(@Param("albumListVoPage") IPage<AlbumListVo> albumListVoPage, @Param("vo") AlbumInfoQuery albumInfoQuery);

    AlbumStatVo getAlbumStatByAlbumId(@Param("albumId") long albumId);


}
