package com.lsj.tingshu.album.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lsj.tingshu.model.album.AlbumInfo;
import com.lsj.tingshu.model.album.BaseCategory3;
import com.lsj.tingshu.query.album.AlbumInfoQuery;
import com.lsj.tingshu.vo.album.AlbumInfoVo;
import com.lsj.tingshu.vo.album.AlbumListVo;
import com.lsj.tingshu.vo.album.AlbumStatVo;

import java.util.List;

public interface AlbumInfoService extends IService<AlbumInfo> {


    /**
     * 保存专辑信息
     *
     * @param albumInfoVo
     */
    void saveAlbumInfo(AlbumInfoVo albumInfoVo);

    /**
     * 保存专辑统计信息
     *
     * @param albumId
     */
    void saveAlbumStat(Long albumId);

    /**
     * 分页获取用户的专辑列表信息
     *
     * @param albumInfoQuery
     * @return
     */
    IPage<AlbumListVo> findUserAlbumPage(IPage<AlbumListVo> albumListVoPage, AlbumInfoQuery albumInfoQuery);

    AlbumInfo getAlbumInfo(Long albumId);

    void updateAlbumInfo(Long albumId, AlbumInfoVo albumInfoVo);

    void removeAlbumInfo(Long albumId);

    List<AlbumInfo> findUserAllAlbumList(Long userId);

    AlbumStatVo getAlbumStatByAlbumId(long albumId);

    List<BaseCategory3> findTopBaseCategory3List(Long c1Id);

    List<Long> getAlbumInfoIds();

}
