package com.lsj.tingshu.common.service.threadpool.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Data
@Component
@ConfigurationProperties(prefix = "app.threadpool")
public class ThreadPoolProperties {
    private int corePoolSize = 2 * Runtime.getRuntime().availableProcessors();
    private int maxPoolSize = 2 * corePoolSize;
    private int queueSize = 200;
    private Long ttl = 30L;
    private TimeUnit timeUnit = TimeUnit.SECONDS;
}
