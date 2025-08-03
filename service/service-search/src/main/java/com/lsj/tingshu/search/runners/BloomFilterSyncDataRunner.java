package com.lsj.tingshu.search.runners;

import com.lsj.tingshu.search.service.impl.ItemServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.BeansException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.List;

//@Component
@Slf4j
public class BloomFilterSyncDataRunner implements ApplicationRunner, ApplicationContextAware {

    ApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        // 将数据库的数据同步到布隆过滤器中

        // 1.获取布隆过滤器对象
        RBloomFilter rBloomFilter = applicationContext.getBean("rBloomFilter", RBloomFilter.class);

        // 2.获取ItemServiceImpl的Bean对象
        ItemServiceImpl itemServiceImpl = applicationContext.getBean("itemServiceImpl", ItemServiceImpl.class);

        List<Long> albumIdsFromDb = itemServiceImpl.getAlbumIdsFromDb();

        // 3.同步数据
        for (Long albumId : albumIdsFromDb) {
            rBloomFilter.add(albumId);
        }

        log.info("分布式布隆过滤器数据同步完毕...个数是：{}", rBloomFilter.count());

    }

    //    @Override
    public void run(String... args) throws Exception {
        // 接收到参数 但是需要需要额外的获取出来key value
        log.info("CommandLineRunner的run方法后调到...");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
