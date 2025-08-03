package com.lsj.tingshu.album.service;

import com.lsj.tingshu.vo.album.TrackMediaInfoVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface VodService {

    Map<String, Object> uploadTrack(MultipartFile file);

    TrackMediaInfoVo getMediainfo(String mediaFileId);

    void removeMedia(String mediaFileId);
}
