package com.cache.config;

import com.cache.aspect.CacheAspect;
import com.cache.constant.CacheAbleConstant;
import com.cache.service.CacheOpsService;
import com.cache.service.impl.CacheOpsServiceImpl;
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
public class CacheAbleAutoConfiguration {
    Logger logger = LoggerFactory.getLogger(this.getClass());


    @Bean
    public RedissonClient redissonClient(RedisProperties redisProperties) {


        // 1.创建配置对象
        Config config = new Config();

        // 2.设置配置信息
        config.useSingleServer().setAddress(CacheAbleConstant.CACHE_PROTOC + redisProperties.getHost() + CacheAbleConstant.CACHE_PROTOC_SPLIT + redisProperties.getPort()).setPassword(redisProperties.getPassword());

        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }


    @Bean
    public RBloomFilter rBloomFilter(RedissonClient redissonClient, StringRedisTemplate redisTemplate) {

        // 1.获取布隆过滤器对象
        // redissonClient.getBloomFilter()如果是第一次调用，会创建这个key【创建布隆过滤器】 如果第二次直接获取这个已经有的布隆过滤器
        RBloomFilter<Object> albumInfoIdsBloomFilter = redissonClient.getBloomFilter(CacheAbleConstant.DISTRO_BLOOM_FILTER_NAME);

        String bloomLockKey = CacheAbleConstant.DISTRO_BLOOM_FILTER_LOCK_KEY;
        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(bloomLockKey, CacheAbleConstant.DISTRO_BLOOM_FILTER_LOCK_VALUE);
        if (aBoolean) {
            // 2.初始化布隆过滤器(期望插入元素个数  误判率)
            boolean tryInitFlag = albumInfoIdsBloomFilter.tryInit(CacheAbleConstant.DISTRO_BLOOM_INSERT, CacheAbleConstant.DISTRO_BLOOM_FPP);
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


    /**
     * 定义切面组件
     */

    @Bean
    public CacheAspect cacheAspect() {
        return new CacheAspect();
    }


    @Bean
    public CacheOpsService cacheOpsService() {
        return new CacheOpsServiceImpl();
    }
}
