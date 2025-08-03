package com.lsj.tingshu.search.executor;

import com.lsj.tingshu.common.service.constant.RedisConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Description:
 * <p>
 * 1.续期方法
 * <p>
 * 2.结束方法
 */

public class ExpireThreadExecutor {


    private StringRedisTemplate redisTemplate;// 续期工具

    private String taskId;  // 任务

    ScheduledFuture<?> scheduledFuture = null;


    static ScheduledExecutorService scheduledExecutorService = null;

    static {
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
    }

    Logger logger = LoggerFactory.getLogger(this.getClass());


    public ExpireThreadExecutor(StringRedisTemplate redisTemplate, String taskId) {

        this.redisTemplate = redisTemplate;
        this.taskId = taskId;
    }


    // 锁的key:30s过期------->20s
    // 续期线程每隔10s----续期到30s.
    public void renewal(Long ttl, TimeUnit timeUnit) {

        scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            String task = RedisConstant.CACHE_LOCK_SUFFIX + taskId;

            @Override
            public void run() {
                logger.info("续期线程:{}开始执行续期任务:{}", Thread.currentThread().getName(), task);
                redisTemplate.expire(task, 30, TimeUnit.SECONDS);
            }

        }, ttl/3, ttl/3, TimeUnit.SECONDS);

    }


    public boolean cancelExpireTask() {

        boolean cancel = scheduledFuture.cancel(true);
        logger.info("续期任务{}结束", cancel ? "成功" : "失败");

        return cancel;

    }


}
