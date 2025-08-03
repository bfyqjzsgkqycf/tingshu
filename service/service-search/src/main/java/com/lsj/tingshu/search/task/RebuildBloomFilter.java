package com.lsj.tingshu.search.task;

import com.lsj.tingshu.search.service.impl.ItemServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class RebuildBloomFilter {


    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private ItemServiceImpl itemServiceImpl;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private  RBloomFilter rBloomFilter;

    public void schedule() {

        // 重建布隆---每7天的凌晨2点钟完成一个布隆重建。
        log.info("布隆重建定时任务开始执行...");

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
            log.info("新布隆已上线，且元素的个数:{}",rBloomFilter.count());
        }else{
            log.info("老布隆依然在线,正常使用");
        }

    }
}
