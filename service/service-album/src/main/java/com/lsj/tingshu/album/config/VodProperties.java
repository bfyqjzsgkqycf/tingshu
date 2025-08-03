package com.lsj.tingshu.album.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="vod") //读取节点
@Data
public class VodProperties {
    private Long appId;
    private String secretId;
    private String secretKey;
    // https://cloud.tencent.com/document/api/266/31756#.E5.9C.B0.E5.9F.9F.E5.88.97.E8.A1.A8
    private String region;
    private String tempPath;
}
