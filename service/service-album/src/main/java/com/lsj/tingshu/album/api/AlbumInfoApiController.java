package com.lsj.tingshu.album.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lsj.tingshu.album.service.AlbumInfoService;
import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.common.service.login.annotation.TingShuLogin;
import com.lsj.tingshu.common.util.AuthContextHolder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsj.tingshu.model.album.AlbumInfo;
import com.lsj.tingshu.query.album.AlbumInfoQuery;
import com.lsj.tingshu.vo.album.AlbumInfoVo;
import com.lsj.tingshu.vo.album.AlbumListVo;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "专辑管理")
@RestController
@RequestMapping("api/album/albumInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class AlbumInfoApiController {

    @Autowired
    private AlbumInfoService albumInfoService;

    // http://localhost:8500/api/album/albumInfo/saveAlbumInfo
    @TingShuLogin
    @PostMapping("/saveAlbumInfo")
    public Result saveAlbumInfo(@RequestBody AlbumInfoVo albumInfoVo) {
        albumInfoService.saveAlbumInfo(albumInfoVo);
        return Result.ok();
    }

    // http://localhost:8500/api/album/albumInfo/findUserAlbumPage/1/10
    @TingShuLogin
    @PostMapping("/findUserAlbumPage/{pn}/{pz}")
    public Result findUserAlbumPage(
            @PathVariable(value = "pn") Long pn,
            @PathVariable(value = "pz") Long pz,
            @RequestBody AlbumInfoQuery albumInfoQuery
    ) {
        Long userId = AuthContextHolder.getUserId();
        albumInfoQuery.setUserId(userId);
        // 创建分页对象
        IPage<AlbumListVo> albumListVoPage = new Page<>(pn, pz);
        // 执行分页查询
        albumListVoPage = albumInfoService.findUserAlbumPage(albumListVoPage, albumInfoQuery);
        return Result.ok(albumListVoPage);
    }

    // Request URL: http://localhost:8500/api/album/albumInfo/getAlbumInfo/1595
    @TingShuLogin
    @GetMapping("/getAlbumInfo/{albumId}")
    public Result getAlbumInfo(@PathVariable(value = "albumId") Long albumId) {
        AlbumInfo albumInfo = albumInfoService.getAlbumInfo(albumId);
        return Result.ok(albumInfo);
    }

    // Request URL: http://localhost:8500/api/album/albumInfo/updateAlbumInfo/1600
    @TingShuLogin
    @PutMapping("/updateAlbumInfo/{albumId}")
    public Result updateAlbumInfo(
            @PathVariable(value = "albumId") Long albumId,
            @RequestBody AlbumInfoVo albumInfoVo
    ) {
        albumInfoService.updateAlbumInfo(albumId, albumInfoVo);
        return Result.ok();
    }

    // Request URL: http://localhost:8500/api/album/albumInfo/removeAlbumInfo/1600
    @TingShuLogin
    @DeleteMapping("/removeAlbumInfo/{albumId}")
    public Result removeAlbumInfo(@PathVariable(value = "albumId") Long albumId) {
        albumInfoService.removeAlbumInfo(albumId);
        return Result.ok();
    }

    // Request URL: http://localhost:8500/api/album/albumInfo/findUserAllAlbumList
    @TingShuLogin
    @GetMapping("/findUserAllAlbumList")
    public Result findUserAllAlbumList() {
        Long userId = AuthContextHolder.getUserId();
        List<AlbumInfo> albumInfoList = albumInfoService.findUserAllAlbumList(userId);
        return Result.ok(albumInfoList);
    }
}

