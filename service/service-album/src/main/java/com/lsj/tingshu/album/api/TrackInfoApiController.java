package com.lsj.tingshu.album.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lsj.tingshu.album.service.TrackInfoService;
import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.common.service.login.annotation.TingShuLogin;
import com.lsj.tingshu.common.util.AuthContextHolder;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lsj.tingshu.model.album.TrackInfo;
import com.lsj.tingshu.query.album.TrackInfoQuery;
import com.lsj.tingshu.vo.album.AlbumTrackListVo;
import com.lsj.tingshu.vo.album.TrackInfoVo;
import com.lsj.tingshu.vo.album.TrackListVo;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "声音管理")
@RestController
@RequestMapping("api/album/trackInfo")
@SuppressWarnings({"unchecked", "rawtypes"})
public class TrackInfoApiController {

    @Autowired
    private TrackInfoService trackInfoService;

    // Request URL: http://localhost:8500/api/album/trackInfo/uploadTrack
    @PostMapping("/uploadTrack")
    public Result uploadTrack(MultipartFile file) {
        Map<String, Object> map = trackInfoService.uploadTrack(file);
        // 返回音频id和url
        return Result.ok(map);
    }

    // Request URL: http://localhost:8500/api/album/trackInfo/saveTrackInfo
    @TingShuLogin
    @PostMapping("/saveTrackInfo")
    public Result saveTrackInfo(@RequestBody TrackInfoVo trackInfoVo) {
        Long userId = AuthContextHolder.getUserId();
        trackInfoService.saveTrackInfo(userId, trackInfoVo);
        return Result.ok();
    }

    // Request URL: http://localhost:8500/api/album/trackInfo/findUserTrackPage/1/10
    @TingShuLogin
    @PostMapping("/findUserTrackPage/{pn}/{pz}")
    public Result findUserTrackPage(
            @PathVariable(value = "pn") Long pn,
            @PathVariable(value = "pz") Long pz,
            @RequestBody TrackInfoQuery trackInfoQuery) {
        trackInfoQuery.setUserId(AuthContextHolder.getUserId());
        IPage<TrackListVo> page = new Page<>(pn, pz);
        page = trackInfoService.findUserTrackPage(page, trackInfoQuery);
        return Result.ok(page);
    }

    // Request URL: http://localhost:8500/api/album/trackInfo/getTrackInfo/51943
    @TingShuLogin
    @GetMapping("/getTrackInfo/{trackId}")
    public Result getTrackInfo(@PathVariable(value = "trackId") Long trackId) {
        TrackInfo trackInfo = trackInfoService.getById(trackId);
        return Result.ok(trackInfo);
    }

    // Request URL: http://localhost:8500/api/album/trackInfo/updateTrackInfo/51943
    @TingShuLogin
    @PutMapping("/updateTrackInfo/{trackId}")
    public Result updateTrackInfo(
            @PathVariable(value = "trackId") Long trackId,
            @RequestBody TrackInfoVo trackInfoVo
    ) {
        trackInfoService.updateTrackInfo(trackId, trackInfoVo);
        return Result.ok();
    }

    // Request URL: http://localhost:8500/api/album/trackInfo/removeTrackInfo/51943
    @TingShuLogin
    @DeleteMapping("/removeTrackInfo/{trackId}")
    public Result removeTrackInfo(@PathVariable(value = "trackId") Long trackId) {
        trackInfoService.removeTrackInfo(trackId);
        return Result.ok();
    }

    // Request URL: http://localhost:8500/api/album/trackInfo/findAlbumTrackPage/1429/1/10
    @TingShuLogin(required = false)
    @GetMapping("/findAlbumTrackPage/{albumId}/{pn}/{pz}")
    public Result findAlbumTrackPage(
            @PathVariable(value = "albumId") Long albumId,
            @PathVariable(value = "pn") Long pn,
            @PathVariable(value = "pz") Long pz
    ) {
        IPage<AlbumTrackListVo> page = new Page<>(pn, pz);
        page = trackInfoService.findAlbumTrackPage(albumId, page);
        return Result.ok(page);
    }

    // Request URL: http://localhost:8500/api/album/trackInfo/getTrackStatVo/51943
    @TingShuLogin
    @GetMapping("/getTrackStatVo/{trackId}")
    public Result getTrackStatVo(@PathVariable Long trackId) {
        TrackInfoVo trackInfoVo = trackInfoService.getTrackStatVo(trackId);
        return Result.ok(trackInfoVo);
    }

    // Request URL: http://localhost:8500/api/album/trackInfo/findUserTrackPaidList/49166
    @GetMapping("/findUserTrackPaidList/{trackId}")
    @TingShuLogin
    public Result findUserTrackPaidList(@PathVariable(value = "trackId") Long trackId) {

        List<Map<String, Object>> choicePayTrackMap = trackInfoService.findUserTrackPaidList(trackId);

        return Result.ok(choicePayTrackMap);

    }
}

