package com.lsj.tingshu.album.api;

import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.common.service.minio.config.MinioProperties;
import com.lsj.tingshu.common.service.minio.service.FileUploadService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "上传管理接口")
@RestController
@RequestMapping("api/album")
public class FileUploadApiController {

    @Autowired
    private MinioProperties minioProperties;

    @Autowired
    private FileUploadService fileUploadService;

    // http://localhost:8500/api/album/fileUpload
    @PostMapping("/fileUpload")
    public Result fileUpload(@RequestPart(value = "file") MultipartFile file) {
        String fileUrl = fileUploadService.fileUpload(file);
        return Result.ok(fileUrl);
    }

}