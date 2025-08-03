package com.lsj.tingshu.common.service.minio.service.impl;

import com.lsj.tingshu.common.service.execption.TingShuException;
import com.lsj.tingshu.common.service.minio.config.MinioProperties;
import com.lsj.tingshu.common.service.minio.service.FileUploadService;
import com.lsj.tingshu.common.util.MD5;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class FileUploadServiceImpl implements FileUploadService {

    @Autowired
    private MinioProperties minioProperties;

    @Autowired
    private MinioClient minioClient;

    @Override
    public String fileUpload(MultipartFile file) {
        String fileUrl = "";
        try {
            // 相同文件去重
            byte[] bytes = file.getBytes();
            String bytesToString = new String(bytes);
            String encrypt = MD5.encrypt(bytesToString);
            // 获取文件后缀
            String fileSuffix = file.getOriginalFilename()
                    .substring(file.getOriginalFilename().lastIndexOf("."), file.getOriginalFilename().length());
            String fileName = encrypt + fileSuffix;
            // minio 查询缓存
            StatObjectArgs.Builder statbuilder = StatObjectArgs.builder();
            StatObjectArgs statObjectArgs = statbuilder
                    .bucket(minioProperties.getBucketName())
                    .object(fileName)
                    .build();
            try {
                minioClient.statObject(statObjectArgs);// 问询对象在不在桶中
                return minioProperties.getEndpointUrl()
                        + "/" + minioProperties.getBucketName()
                        + "/" + fileName;
            } catch (Exception e) {
                log.error("将上传的文件于 minio 里不存在, 可以上传");
            }

            // 构建文件对象 上传到 minio 桶
            PutObjectArgs.Builder builder = PutObjectArgs.builder();
            PutObjectArgs putObjectArgs = builder
                    .bucket(minioProperties.getBucketName())
                    .object(fileName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .build();
            minioClient.putObject(putObjectArgs);

            System.out.println("上传成功");
            log.info("文件上传 minio 成功");

            fileUrl = minioProperties.getEndpointUrl()
                    + "/" + minioProperties.getBucketName()
                    + "/" + fileName;
        } catch (Exception e) {
            log.error("上传文件出错：{}", e.getMessage());
            throw new TingShuException(500, "文件上传 minio 失败");
        }
        return fileUrl;
    }
}
