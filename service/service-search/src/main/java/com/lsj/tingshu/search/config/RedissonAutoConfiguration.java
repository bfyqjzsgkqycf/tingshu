package com.lsj.tingshu.search.config;

import com.lsj.tingshu.common.service.constant.RedisConstant;
import org.redisson.Redisson;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedissonAutoConfiguration {
    Logger logger = LoggerFactory.getLogger(this.getClass());


    @Bean
    public RedissonClient redissonClient(RedisProperties redisProperties) {


        // 1.创建配置对象
        Config config = new Config();

        // 2.设置配置信息
        config.useSingleServer().setAddress("redis://" + redisProperties.getHost()
                + ":" + redisProperties.getPort()).setPassword(redisProperties.getPassword());

        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }


    @Bean
    public RBloomFilter rBloomFilter(RedissonClient redissonClient, StringRedisTemplate redisTemplate) {

        // 1.获取布隆过滤器对象
        // redissonClient.getBloomFilter()如果是第一次调用，会创建这个key【创建布隆过滤器】 如果第二次直接获取这个已经有的布隆过滤器
        RBloomFilter<Object> albumInfoIdsBloomFilter = redissonClient.getBloomFilter("albumInfoIdsBloomFilter");

        String bloomLockKey = RedisConstant.ALBUM_BLOOM_FILTER + "lock:albumInfoIdsBloomFilter";
        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(bloomLockKey, "v");
        if (aBoolean) {
            // 2.初始化布隆过滤器(期望插入元素个数  误判率)
            boolean tryInitFlag = albumInfoIdsBloomFilter.tryInit(1000000l, 0.01);
            // 3.如果初始化分布式布隆成功

            if (tryInitFlag) {
                // 同步数据给布隆过滤器 传统方式不使用
                logger.info("分布式布隆初始化完毕，等待数据同步...");
            } else {
                logger.error("分布式布隆初始化失败...");
            }
        }

        return albumInfoIdsBloomFilter;


    }
}
