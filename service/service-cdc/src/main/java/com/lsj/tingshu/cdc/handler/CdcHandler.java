package com.lsj.tingshu.cdc.handler;

import com.lsj.tingshu.cdc.entity.CdcEntity;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import top.javatool.canal.client.annotation.CanalTable;
import top.javatool.canal.client.handler.EntryHandler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@CanalTable("album_info") //监听变更表
public class CdcHandler implements EntryHandler<CdcEntity> {

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 像album_info表中插入一条新数据记录时，该方法会被回调到。
     *
     * @param cdcEntity
     */
    @Override
    public void insert(CdcEntity cdcEntity) {
        log.info("监听到指定album_info中有新数据插入，且插入数据的主键id:{}", cdcEntity.getId());
    }


    /**
     * 像album_info表中有数据记录发生修改的时候，该方法会被回调到。
     *
     * @param before---修改之前的主键id
     * @param after----修改之后的主键id
     *                  <p>
     *                  如果修改的不是监听的这一列，那么这一列是没有旧值，但是有新值。 如果修改的是监听的这一列，那么这一列是有旧值，有新值。
     */
    @Override
    @SneakyThrows
    public void update(CdcEntity before, CdcEntity after) {
        log.info("监听到指定album_info中有记录的主键列发生了数据修改，修改之前的主键id{}，修改后的新主键值:{}", before.getId(), after.getId());


        // 删除Redis中的缓存(一删缓存)
        String cacheKey = "cache:info:" + after.getId();
        redisTemplate.delete(cacheKey);


        // 二删除
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

        try {
            scheduledExecutorService.schedule(new Runnable() {
                @Override
                public void run() {
                    redisTemplate.delete(cacheKey);  // 异步删除
                }
            }, 300, TimeUnit.MICROSECONDS);
        } catch (Exception e) {

            // 发送消息给RabbitMQ  TODO
        }
    }

    /**
     * 删除album_info表中有数据记录的时候，该方法会被回调到。
     *
     * @param cdcEntity
     */
    @Override
    public void delete(CdcEntity cdcEntity) {
        log.info("监听到指定album_info中有记录被删除,删除记录对应的主键：{}", cdcEntity.getId());
    }
}
