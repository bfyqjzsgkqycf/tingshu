package com.lsj.tingshu.user.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(value = "wx.login")
@Data
public class WxLoginProperties {
    private String appId;
    private String secret;
}
