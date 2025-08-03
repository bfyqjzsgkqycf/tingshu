package com.lsj.tingshu.common.service.minio.config;

import com.lsj.tingshu.common.service.execption.TingShuException;
import com.lsj.tingshu.common.service.minio.service.FileUploadService;
import com.lsj.tingshu.common.service.minio.service.impl.FileUploadServiceImpl;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
@ConditionalOnProperty(prefix = "minio", value = "enable", havingValue = "true")
public class MinioAutoConfiguration {

    @Bean
    public MinioClient minioClient(MinioProperties minioProperties) {
        Logger log = LoggerFactory.getLogger(this.getClass());
        // 创建 minio 上传客户端
        MinioClient minioClient =
                MinioClient.builder()
                        .endpoint(minioProperties.getEndpointUrl())
                        .credentials(minioProperties.getAccessKey(), minioProperties.getSecreKey())
                        .build();
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(minioProperties.getBucketName()).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(minioProperties.getBucketName()).build());
            } else {
                log.error("Bucket {} already exists.", minioProperties.getBucketName());
            }

        } catch (Exception e) {
            throw new TingShuException(500, "minio 对象创建失败");
        }
        return minioClient;
    }

    @Bean
    public FileUploadService fileUploadService() {
        return new FileUploadServiceImpl();
    }
}
