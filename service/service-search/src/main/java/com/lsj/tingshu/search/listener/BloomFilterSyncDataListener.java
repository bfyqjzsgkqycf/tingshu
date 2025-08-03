package com.lsj.tingshu.search.listener;

import com.lsj.tingshu.search.service.impl.ItemServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.Duration;
import java.util.List;

@Slf4j
public class BloomFilterSyncDataListener implements SpringApplicationRunListener {

    /**
     * started方法在第一次被回调的时候，ConfigurableApplicationContext对象中没有应用定义的Bean对象
     * started方法在第二次被回调的时候：，ConfigurableApplicationContext对象才有应用定义的Bean对象
     * @param context the application context.
     * @param timeTaken the time taken to start the application or {@code null} if unknown
     */


    @Override
    public void started(ConfigurableApplicationContext context, Duration timeTaken) {
        System.out.println("SpringApplicationRunListener的started被调用到了...");

        // 将数据库的数据同步到布隆过滤器中

        if (context.containsBean("rBloomFilter")) {

            // 1.获取布隆过滤器对象
            RBloomFilter rBloomFilter = context.getBean("rBloomFilter", RBloomFilter.class);


            // 2.获取ItemServiceImpl的Bean对象
            ItemServiceImpl itemServiceImpl = context.getBean("itemServiceImpl", ItemServiceImpl.class);


            List<Long> albumIdsFromDb = itemServiceImpl.getAlbumIdsFromDb();

            if(rBloomFilter.count()==0){
                // 3.同步数据
                for (Long albumId : albumIdsFromDb) {
                    rBloomFilter.add(albumId);
                }
                log.info("分布式布隆过滤器数据同步完毕...个数是：{}", rBloomFilter.count());
            }
            log.info("分布式布隆过滤器数据已经同步完毕...个数是：{}", rBloomFilter.count());

        }

        log.info("started方法第一次被调到，容器中没有应用的Bean对象");


    }
}
