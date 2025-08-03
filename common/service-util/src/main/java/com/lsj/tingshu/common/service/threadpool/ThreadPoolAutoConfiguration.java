package com.lsj.tingshu.common.service.threadpool;

import com.lsj.tingshu.common.service.threadpool.properties.ThreadPoolProperties;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ThreadPoolAutoConfiguration {

    @Value("${spring.application.name}")
    private String serviceName;

    @Bean
    public ThreadPoolExecutor threadPoolExecutor(ThreadPoolProperties threadPoolProperties) {

        ThreadFactory threadFactory = new ThreadFactory() {
            AtomicInteger atomicInteger = new AtomicInteger(0);
            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("customer-" + serviceName + "-" + atomicInteger.incrementAndGet());
                return thread;
            }
        };

        RejectedExecutionHandler rejectedExecutionHandler = new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                throw new RejectedExecutionException("线程池已满");
            }
        };

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                threadPoolProperties.getCorePoolSize(),
                threadPoolProperties.getMaxPoolSize(),
                threadPoolProperties.getTtl(),
                threadPoolProperties.getTimeUnit(),
                new LinkedBlockingDeque<Runnable>(threadPoolProperties.getQueueSize()),
                threadFactory,
                rejectedExecutionHandler
        );

        return threadPoolExecutor;
    }
}
