package com.lsj.tingshu.search.runnable;

import com.lsj.tingshu.search.factory.ScheduledExecutorFactory;
import com.lsj.tingshu.search.service.impl.ItemServiceImpl;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RebuildBloomRunnable implements Runnable {
    Logger logger = LoggerFactory.getLogger(this.getClass());


    private RedissonClient redissonClient;
    private StringRedisTemplate redisTemplate;
    private ItemServiceImpl itemServiceImpl;
    private RBloomFilter rBloomFilter;

    public RebuildBloomRunnable(RedissonClient redissonClient,
                                StringRedisTemplate redisTemplate,
                                ItemServiceImpl itemServiceImpl,
                                RBloomFilter rBloomFilter) {
        this.redissonClient = redissonClient;
        this.redisTemplate = redisTemplate;
        this.itemServiceImpl = itemServiceImpl;
        this.rBloomFilter = rBloomFilter;
    }






    @Override
    public void run() {

        // 1.新创建一个布隆，初始化
        RBloomFilter<Object> albumInfoIdsBloomFilterNew = redissonClient.getBloomFilter("albumInfoIdsBloomFilterNew");
        albumInfoIdsBloomFilterNew.tryInit(1000000l, 0.01);

        // 2.将新数据同步到新布隆中
        List<Long> albumIdsFromDb = itemServiceImpl.getAlbumIdsFromDb();

        for (Long albumId : albumIdsFromDb) {
            albumInfoIdsBloomFilterNew.add(albumId);
        }
        albumInfoIdsBloomFilterNew.add(20000L);

        // 3.345三步用lua脚本  用布隆数据key名字替换老数据key的名字  用布隆配置key名字替换老配置key的名字    //  rename oldKey[新布隆]  newKey[老布隆]
        String script = " redis.call(\"del\",KEYS[1])" +
                "  redis.call(\"del\",KEYS[2])" +
                "  redis.call(\"rename\",KEYS[3],KEYS[1])" +
                "  redis.call(\"rename\",KEYS[4],KEYS[2]) return 1";
        List<String> asList = Arrays.asList("albumInfoIdsBloomFilter", "{albumInfoIdsBloomFilter}:config", "albumInfoIdsBloomFilterNew", "{albumInfoIdsBloomFilterNew}:config");
        Long execute = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), asList);
        if (execute == 1) {
            logger.info("新布隆已上线，且元素的个数:{}", rBloomFilter.count());
        } else {
            logger.info("老布隆依然在线,正常使用");
        }


        // 在让你隔10s执行一次。
//        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
//        scheduledExecutorService.schedule(this,10, TimeUnit.SECONDS);  // 延时任务
        // 工厂+单例
        ScheduledExecutorFactory instance = ScheduledExecutorFactory.getInstance();
        instance.execute(this,7l, TimeUnit.DAYS);   // 以后每隔7天在执行一次。
    }
}
