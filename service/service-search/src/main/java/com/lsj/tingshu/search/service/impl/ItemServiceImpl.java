package com.lsj.tingshu.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.alibaba.fastjson.JSONObject;
import com.lsj.tingshu.album.client.AlbumInfoFeignClient;
import com.lsj.tingshu.common.result.Result;
import com.lsj.tingshu.common.service.constant.RedisConstant;
import com.lsj.tingshu.common.service.execption.TingShuException;
import com.lsj.tingshu.common.util.PinYinUtils;
import com.lsj.tingshu.model.album.AlbumAttributeValue;
import com.lsj.tingshu.model.album.AlbumInfo;
import com.lsj.tingshu.model.album.BaseCategoryView;
import com.lsj.tingshu.model.search.AlbumInfoIndex;
import com.lsj.tingshu.model.search.AttributeValueIndex;
import com.lsj.tingshu.model.search.SuggestIndex;
import com.lsj.tingshu.search.executor.ExpireThreadExecutor;
import com.lsj.tingshu.search.factory.ScheduledExecutorFactory;
import com.lsj.tingshu.search.repository.AlbumInfoIndexRepository;
import com.lsj.tingshu.search.repository.SuggestIndexRepository;
import com.lsj.tingshu.search.runnable.RebuildBloomRunnable;
import com.lsj.tingshu.search.service.ItemService;
import com.lsj.tingshu.user.client.UserInfoFeignClient;
import com.lsj.tingshu.vo.album.AlbumStatVo;
import com.lsj.tingshu.vo.search.AlbumInfoIndexVo;
import com.lsj.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.google.common.hash.BloomFilter;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class ItemServiceImpl implements ItemService {

    @Autowired
    private AlbumInfoIndexRepository albumInfoIndexRepository;

    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private SuggestIndexRepository suggestIndexRepository;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RBloomFilter rBloomFilter;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private ItemServiceImpl itemServiceImpl;

    @PostConstruct   // 该方法就在容器启动的时候【创建当前这个Bean对象的时候回调到】
    public void initRebuildBloomTask() {
        // 工厂 + 单例
        ScheduledExecutorFactory instance = ScheduledExecutorFactory.getInstance();
        // 每 7 天凌晨两天中执行一个定时任务 做重建。
        long diffTime = instance.diffTime(System.currentTimeMillis());
        /*instance.execute(new RebuildBloomRunnable(redissonClient, redisTemplate, itemServiceImpl, rBloomFilter),
                diffTime, TimeUnit.MILLISECONDS);*/
        // 测试
        instance.execute(new RebuildBloomRunnable(redissonClient, redisTemplate, itemServiceImpl, rBloomFilter),
                10l, TimeUnit.MILLISECONDS);

    }

    BloomFilter<Long> longBloomFilter = null;

    @Override
    @SneakyThrows
    public void onSaleAlbum(Long albumId) {
        // 1.构建Elastic数据模型
        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        CountDownLatch countDownLatch = new CountDownLatch(4);
        // 2.追加对象数据
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                // tingshu_album album_attribute_value
                Result<BaseCategoryView> categoryInfoByAlbumId = albumInfoFeignClient.getCategoryInfoByAlbumId(albumId);
                albumInfoIndex.setCategory1Id(categoryInfoByAlbumId.getData().getCategory1Id());
                albumInfoIndex.setCategory2Id(categoryInfoByAlbumId.getData().getCategory2Id());
                albumInfoIndex.setCategory3Id(categoryInfoByAlbumId.getData().getCategory3Id());
                countDownLatch.countDown();
            }
        });

        executorService.submit(new Runnable() {
            @Override
            public void run() {
                // tingshu_album album_stat
                Result<AlbumStatVo> albumStatByAlbumId = albumInfoFeignClient.getAlbumStatByAlbumId(albumId);
                albumInfoIndex.setPlayStatNum(albumStatByAlbumId.getData().getPlayStatNum());
                albumInfoIndex.setSubscribeStatNum(albumStatByAlbumId.getData().getSubscribeStatNum());
                albumInfoIndex.setBuyStatNum(albumStatByAlbumId.getData().getBuyStatNum());
                albumInfoIndex.setCommentStatNum(albumStatByAlbumId.getData().getCommentStatNum());
                countDownLatch.countDown();
            }
        });

        Future<Long> future = executorService.submit(new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                // tingshu_album album_info
                Result<AlbumInfo> albumInfoAndTag = albumInfoFeignClient.getAlbumInfoAndTag(albumId.toString());
                AlbumInfo data = albumInfoAndTag.getData();
                albumInfoIndex.setId(data.getId());
                albumInfoIndex.setAlbumTitle(data.getAlbumTitle());
                albumInfoIndex.setAlbumIntro(data.getAlbumIntro());
                albumInfoIndex.setCoverUrl(data.getCoverUrl());
                albumInfoIndex.setIncludeTrackCount(data.getIncludeTrackCount());
                albumInfoIndex.setIsFinished(data.getIsFinished().toString());
                albumInfoIndex.setPayType(data.getPayType());
                albumInfoIndex.setCreateTime(data.getCreateTime());
                List<AlbumAttributeValue> albumAttributeValueVoList = data.getAlbumAttributeValueVoList();
                List<AttributeValueIndex> attributeValueIndexList = albumAttributeValueVoList.stream().map(albumAttributeValue -> {
                    AttributeValueIndex attributeValueIndex = new AttributeValueIndex();
                    attributeValueIndex.setAttributeId(albumAttributeValue.getAttributeId());
                    attributeValueIndex.setValueId(albumAttributeValue.getValueId());
                    return attributeValueIndex;
                }).collect(Collectors.toList());
                albumInfoIndex.setAttributeValueIndexList(attributeValueIndexList);
                countDownLatch.countDown();
                return data.getUserId();
            }
        });

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Long userId = future.get();
                    // tingshu_user user_info
                    Result<UserInfoVo> userInfo = userInfoFeignClient.getUserInfo(userId.toString());
                    UserInfoVo data = userInfo.getData();
                    if (data == null) {
                        throw new TingShuException(500, "远程调用用户微服务获取用户信息失败");
                    }
                    albumInfoIndex.setAnnouncerName(data.getNickname());
                    countDownLatch.countDown();
                } catch (Exception e) {
                    log.error("远程调用用户微服务获取用户信息失败！");
                }
            }
        });

        countDownLatch.await();
        // 热度值
        albumInfoIndex.setHotScore(new Random().nextDouble());
        // 3.保存文档对象
        albumInfoIndexRepository.save(albumInfoIndex);
        log.info("保存成功：{}", albumInfoIndex.getId());
        // 4.保存提示词
        SuggestIndex suggestIndex = new SuggestIndex();
        suggestIndex.setId(albumId.toString());
        suggestIndex.setTitle(albumInfoIndex.getAlbumTitle());
        suggestIndex.setKeyword(new Completion(new String[]{albumInfoIndex.getAlbumTitle()}));
        suggestIndex.setKeywordPinyin(new Completion(new String[]{
                PinYinUtils.toHanyuPinyin(albumInfoIndex.getAlbumTitle())
        }));
        suggestIndex.setKeywordSequence(new Completion(new String[]{
                PinYinUtils.getFirstLetter(albumInfoIndex.getAlbumTitle())
        }));

        suggestIndexRepository.save(suggestIndex);

    }

    @Override
    public void onOffAlbum(Long albumId) {
        albumInfoIndexRepository.deleteById(albumId);
    }

    @Override
    public void batchOnOffAlbum() {
        albumInfoIndexRepository.deleteAll();
    }

    /**
     * 获取专辑信息
     * 性能优化:
     * 线程: new Thread() / 线程池 + countDownLatch / 异步编排 CompletableFuture (已使用)
     */
    ThreadLocal<String> threadLocal = new ThreadLocal<>();

    @SneakyThrows
    @Override
    public Map<String, Object> getAlbumInfo(Long albumId) {
        return getAlbumInfoRedissonVersion(albumId);
    }

    private Map getAlbumInfoRedissonVersion(Long albumId) throws InterruptedException {
        String cacheKey = RedisConstant.CACHE_INFO_PREFIX + albumId;
        String lockKey = RedisConstant.CACHE_LOCK_SUFFIX + albumId; // 锁的粒度
        long ttl = 0l;

        // 1.查询分布式布隆过滤器
        if (!rBloomFilter.contains(albumId)) {
            return new HashMap<>();
        }

        // 2.布隆有，查询缓存
        String resultFromCache = redisTemplate.opsForValue().get(cacheKey);
        if (!org.springframework.util.StringUtils.isEmpty(resultFromCache)) {
            return JSONObject.parseObject(resultFromCache, Map.class);
        }

        // 3.缓存没有，查询数据库
        // 3.1 添加分布式锁（redisson下的tryLock()）
        RLock lock = redissonClient.getLock(lockKey);

        // 3.2 抢锁
        boolean acquireFlag = lock.tryLock();
        // a) 抢到锁
        if (acquireFlag) {
            Map<String, Object> albumInfoFromDB;
            try {
                albumInfoFromDB = getAlbumInfoFromDB(albumId);

                // Thread.sleep(40000);// 模拟任务的时间
                if (albumInfoFromDB != null && albumInfoFromDB.size() > 0) {
                    ttl = 60 * 60 * 24 * 7;
                } else {
                    ttl = 60 * 60 * 2l;
                }
                // b) 将数据同步到缓存中 // 解决缓存穿透固定值攻击
                redisTemplate.opsForValue().set(cacheKey, JSONObject.toJSONString(albumInfoFromDB), ttl, TimeUnit.SECONDS);
            } finally {
                lock.unlock();// 释放锁
            }
            return albumInfoFromDB;

        } else {
            // c)没有抢到锁
            Thread.sleep(200); // 同步数据到缓存时间
            String syncResultFromCache = redisTemplate.opsForValue().get(cacheKey);

            // d)  继续查询缓存(开发足够)
            if (!org.springframework.util.StringUtils.isEmpty(syncResultFromCache)) { // 99%的线程在200ms之后都可以从缓存中查询到数据 1%？
                return JSONObject.parseObject(syncResultFromCache, Map.class);
            }


            // e) 继续查询一下数据库
            Map<String, Object> albumInfoFromDB = getAlbumInfoFromDB(albumId);

            return albumInfoFromDB;


        }
    }

    /**
     * v5遗留的问题，抢到锁的线程在干活期间，可能别的线程又抢到锁也干活。如果干活是一个追加写，就会导致数据不完整或者出错。
     * 思路：给抢到锁的线程对应的锁key 续期。从而来保证，抢到锁的线程不管干活需要多久时间，只要活没干完，其它线程永远抢不到锁。满足锁的本质（互斥）
     * 最终版本。
     */
    private @Nullable Map getAlbumInfoRedisVersion(Long albumId) throws InterruptedException {
        // 1.定义局部变量
        String cacheKey = RedisConstant.CACHE_INFO_PREFIX + albumId;
        String lockKey = RedisConstant.CACHE_LOCK_SUFFIX + albumId;
        String dataFromCache = redisTemplate.opsForValue().get(cacheKey);
        String token = "";
        Boolean accquiredFlag = false;

//        boolean bloomContainFlag = longBloomFilter.mightContain(albumId);
        boolean bloomContainFlag = rBloomFilter.contains(albumId);
        if (!bloomContainFlag) {
            return new HashMap();
        }

        // 2.判断缓存是否命中(解决缓存穿透随机值攻击)
        // 2.1 缓存命中
        if (!StringUtils.isEmpty(dataFromCache)) {
            return JSONObject.parseObject(dataFromCache, Map.class);
        }
        // 2.2 从ThreadLocal获取递归线程的执行结果
        String secondToken = threadLocal.get();
        // 2.2.1 如果没有获取到，第一次进入。
        if (org.springframework.util.StringUtils.isEmpty(secondToken)) {
            // 1) 生成令牌
            token = UUID.randomUUID().toString().replace("-", "");
            // 2) 抢锁
            accquiredFlag = redisTemplate.opsForValue().setIfAbsent(lockKey, token, 30, TimeUnit.SECONDS);
        } else {
            // 2.2.2  如果没有获取到，递归进入
            accquiredFlag = true;
            token = secondToken;
        }
        // 2.3 抢锁成功
        if (accquiredFlag) {

            // 2.3.1 续期：用线程池启动了一个线程执行对锁key续期。(异步启动一个线程，不能引用业务的线程)
            ExpireThreadExecutor expireThreadExecutor = new ExpireThreadExecutor(redisTemplate, albumId.toString());
            expireThreadExecutor.renewal(30l, TimeUnit.SECONDS);

            // 2.3.2 回源查询数据库
            Map<String, Object> albumInfoFromDB;
            try {
                long ttl = 0l;
                albumInfoFromDB = getAlbumInfoFromDB(albumId);
                if (albumInfoFromDB != null && albumInfoFromDB.size() > 0) {
                    ttl = 60 * 60 * 24 * 7;
                } else {
                    ttl = 60 * 60 * 2l;
                }
                // 2.3.3.将数据同步到缓存中 // 解决缓存穿透固定值攻击
                redisTemplate.opsForValue().set(cacheKey, JSONObject.toJSONString(albumInfoFromDB));

            } finally {
                // 2.3.4 定义lua脚本 判断和删除保证原子操作
                String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                DefaultRedisScript<Long> longDefaultRedisScript = new DefaultRedisScript<>(luaScript, Long.class);
                Long execute = redisTemplate.execute(longDefaultRedisScript, Arrays.asList(lockKey), token);
                if (execute != 0) {
                    log.info("删除锁成功");
                } else {
                    log.error("删除锁失败");
                }

                // 2.3.5 及时将令牌从ThreadLocal中移除
                threadLocal.remove();// 防止内存泄漏

                // 2.3.6 取消续期任务
                expireThreadExecutor.cancelExpireTask(); // 其实可以不用写取消任务
            }
            // 2.3.7 返回结果
            return albumInfoFromDB;

        } else {
            // 2.4 抢不到锁
            Thread.sleep(200); // 一定压测给到（同步数据的时间）
            String syncResultFromCache = redisTemplate.opsForValue().get(cacheKey);

            // 2.5 继续查询缓存(99%正常情况)
            if (!org.springframework.util.StringUtils.isEmpty(syncResultFromCache)) { // 99%的线程在200ms之后都可以从缓存中查询到数据 1%？
                return JSONObject.parseObject(syncResultFromCache, Map.class);
            }

            // 2.6 自旋抢锁(1%极端情况) 目的：为了让其他线程能够在次去查询数据库同步缓存操作
            while (true) {
                // 尽可能去减少栈溢出的风险---自旋（死循环）
                String doubleCache = redisTemplate.opsForValue().get(cacheKey);
                // 2.6.1 二次查询缓存(递归中查询目的:让其他在自旋的线程及时获取到缓存结果)
                if (!org.springframework.util.StringUtils.isEmpty(doubleCache)) {   //  非常重要
                    return JSONObject.parseObject(syncResultFromCache, Map.class);
                }
                // 2.6.2 继续抢锁
                Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(lockKey, token, 30, TimeUnit.SECONDS);
                if (aBoolean) {
                    // map.put(Thread.currentThread(), true);// 我已经加过锁了
                    // threadLocal.set(true);
                    threadLocal.set(token);
                    break;
                }
            }
            // 2.7 递归
            return getAlbumInfo(albumId);
        }
    }

    /**
     * v4版本遗留的问题：如何优化抢不到锁的线程。
     * 场景：抢到锁的线程执行任务期间（同步缓存）未做到，那么抢不到锁的线程200ms 依然不能获取到数据，实际数据库是有数据，导致不满足业务场景。
     * 思路：抢不到锁的线程在去做一次查询数据库同步缓存操作。
     * v5版本如何解决。
     * 通过自旋+递归+可重入+ThreadLocal+双缓存，最终优化v4版本的问题。
     */
    private @Nullable Map getAlbumInfoOptimizeV5(Long albumId) throws InterruptedException {
        // 1.查询分布式缓存Redis
        String cacheKey = RedisConstant.CACHE_INFO_PREFIX + albumId;
        String lockKey = RedisConstant.CACHE_LOCK_SUFFIX + albumId;
        String dataFromCache = redisTemplate.opsForValue().get(cacheKey);
        String token = "";
        Boolean accquiredFlag = false;

        // 2.判断缓存是否命中
        // 2.1 缓存命中
        if (!org.springframework.util.StringUtils.isEmpty(dataFromCache)) {
            return JSONObject.parseObject(dataFromCache, Map.class);
        }

//        Boolean aBoolean1 = map.get(Thread.currentThread());
//        if (aBoolean1 == null || aBoolean1 == false) {
//            // 不是递归进来的
//            // 抢锁
//            accquiredFlag = redisTemplate.opsForValue().setIfAbsent(lockKey, token, 50, TimeUnit.SECONDS);
//        } else {
//            // 是递归进来的 不用抢锁 直接干活
//            accquiredFlag = true;
//        }
        String secondToken = threadLocal.get();
        if (org.springframework.util.StringUtils.isEmpty(secondToken)) {
            // 生成令牌
            token = UUID.randomUUID().toString().replace("-", "");
            accquiredFlag = redisTemplate.opsForValue().setIfAbsent(lockKey, token, 50, TimeUnit.SECONDS);
        } else {
            accquiredFlag = true;
            token = secondToken;
        }
        // 2.2 缓存未命中(加锁)


        // 当下用锁是为了解决缓存击穿的。
        // 正常用锁。控制受保护资源。一个抢到锁的线程在干活期间，另外的线程不允许干活（等待）

        // 锁的续期。(当前这种场景下可以不续期，因为抢到锁的线程执行的任务是覆盖写 但是如过抢到锁的线程是追加写，那么就需要续期。)
        // 续期的目的：保证抢到锁的线程在干活期间（不管干多久） 其它的线程都要阻塞。

        // 3.抢锁成功
        if (accquiredFlag) {

            // 续期：TODO.[手写一个续期线程] redisson框架的分布式自带续期功能。


            // 3.1.回源查询数据库
            Map<String, Object> albumInfoFromDB;
            try {
                albumInfoFromDB = getAlbumInfoFromDB(albumId);
                // 3.2.将数据同步到缓存中
                redisTemplate.opsForValue().set(cacheKey, JSONObject.toJSONString(albumInfoFromDB));

            } finally {
                // 3.3 释放锁
                //lua脚本 判断和删除。
                String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

                DefaultRedisScript<Long> longDefaultRedisScript = new DefaultRedisScript<>(luaScript);

                Long execute = redisTemplate.execute(longDefaultRedisScript, Arrays.asList(lockKey), token);
                if (execute != 0) {
                    log.info("删除锁成功");
                } else {
                    log.error("删除锁失败");
                }
                threadLocal.remove();// 防止内存泄漏
            }
            // 3.4 返回结果
            return albumInfoFromDB;
        } else {
            // 4.抢不到锁
            Thread.sleep(200); // 一定压测给到
            String syncResultFromCache = redisTemplate.opsForValue().get(cacheKey);
            if (!org.springframework.util.StringUtils.isEmpty(syncResultFromCache)) { // 99%的线程在200ms之后都可以从缓存中查询到数据 1%？
                return JSONObject.parseObject(syncResultFromCache, Map.class);
            }

            // 缓存没有之后： 目的：为了让其他线程能够在次去查询数据库同步缓存操作
            while (true) {
                // 尽可能去减少栈溢出的风险---自旋（死循环）
                String doubleCache = redisTemplate.opsForValue().get(cacheKey);
                if (!org.springframework.util.StringUtils.isEmpty(doubleCache)) {   //  非常重要
                    return JSONObject.parseObject(syncResultFromCache, Map.class);
                }
                Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(lockKey, token, 50, TimeUnit.SECONDS);
                if (aBoolean) {
//                    map.put(Thread.currentThread(), true);// 我已经加过锁了
//                    threadLocal.set(true);
                    threadLocal.set(token);
                    break;
                }
            }

            return getAlbumInfo(albumId);
        }
    }

    /**
     * v3版本遗留的问题：锁的误删。 由于锁没有被抢到锁的线程释放掉，导致在到了过期时间另外的线程又加了锁。接着没有手动释放锁的线程删掉了过期时候之后新进来线程加的锁。
     * 并发高的情况下，源源不断有大量请求查询数据库，导致数据库压力过大，可能出现数据库服务不可用。
     * v4版本如何解决。
     * 刻客户端删除锁的时候，判断锁是否是自己加的。
     * ①：先判断，在删除：非原子操作，极端情况下，依然会出现锁的误删
     * ②：将判断和删除放到Lua基本。
     */
    private @Nullable Map getAlbumInfoOptimizeV4(Long albumId) throws InterruptedException {
        // 1.查询分布式缓存Redis
        String cacheKey = RedisConstant.CACHE_INFO_PREFIX + albumId;
        String lockKey = RedisConstant.CACHE_LOCK_SUFFIX + albumId;
        String dataFromCache = redisTemplate.opsForValue().get(cacheKey);
        String token = UUID.randomUUID().toString().replace("-", "");

        // 2.判断缓存是否命中
        // 2.1 缓存命中
        if (!org.springframework.util.StringUtils.isEmpty(dataFromCache)) {
            return JSONObject.parseObject(dataFromCache, Map.class);
        }

        // 2.2 缓存未命中(加锁)
        Boolean accquiredFlag = redisTemplate.opsForValue().setIfAbsent(lockKey, token, 50, TimeUnit.SECONDS);

        // 3.抢锁成功
        if (accquiredFlag) {

            // 3.1.回源查询数据库
            Map<String, Object> albumInfoFromDB;
            try {
                albumInfoFromDB = getAlbumInfoFromDB(albumId);
                // 3.2.将数据同步到缓存中
                redisTemplate.opsForValue().set(cacheKey, JSONObject.toJSONString(albumInfoFromDB));

            } finally {
                // 3.3 释放锁
//                if(token.equals(redisTemplate.opsForValue().get(lockKey))){
//                    // 自己加的锁，自己删除
//                    redisTemplate.delete(lockKey);   // 客户端执行删除锁操作
////                }
                //lua脚本 判断和删除。
                String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

                DefaultRedisScript<Long> longDefaultRedisScript = new DefaultRedisScript<>(luaScript);

                Long execute = redisTemplate.execute(longDefaultRedisScript, Arrays.asList(lockKey), token);
                if (execute != 0) {
                    log.info("删除锁成功");
                } else {
                    log.error("删除锁失败");
                }
            }
            // 3.4 返回结果
            return albumInfoFromDB;
        } else {
            // 4.抢不到锁
            Thread.sleep(200); // 一定压测给到
            String syncResultFromCache = redisTemplate.opsForValue().get(cacheKey);
            return JSONObject.parseObject(syncResultFromCache, Map.class);
        }
    }

    // 优化3: 双缓存架构
    Map<Long, Map<String, Object>> localCache2 = new HashMap<>();

    private Map getAlbumInfoOptimizeV3(Long albumId) {
        long startTime = System.currentTimeMillis();
        // 1.查询缓存(Map)-本地
        if (localCache2.containsKey(albumId)) {
            long endTime = System.currentTimeMillis();
            log.info("命中本地缓存耗时：{}", endTime - startTime); // 0
            return localCache2.get(albumId);
        }
        // 2.本地缓存没命中 查询分布式缓存
        String redisCache = redisTemplate.opsForValue().get(albumId.toString());
        if (!StringUtils.isEmpty(redisCache)) {
            long endTime = System.currentTimeMillis();
            log.info("命中分布式缓存耗时：{}", endTime - startTime); //
            return JSONObject.parseObject(redisCache, Map.class);
        }
        // 3.分布式缓存没命中 查询数据库 同步到分布式缓存 再同步到本地缓存
        Map<String, Object> albumInfoFromDB = getAlbumInfoFromDB(albumId);
        localCache2.put(albumId, albumInfoFromDB);
        redisTemplate.opsForValue().set(albumId.toString(), JSONObject.toJSONString(albumInfoFromDB));
        long endTime = System.currentTimeMillis();
        log.info("数据库同步到双缓存耗时：{}", endTime - startTime); // 508
        return albumInfoFromDB;
    }

    // 优化2: 分布式缓存
    private Map getAlbumInfoOptimizeV2(Long albumId) {
        long startTime = System.currentTimeMillis();
        // 1.查询缓存-分布式
        String redisCache = redisTemplate.opsForValue().get(albumId.toString());
        if (!StringUtils.isEmpty(redisCache)) {
            long endTime = System.currentTimeMillis();
            log.info("命中分布式缓存耗时：{}", endTime - startTime); // 2
            return JSONObject.parseObject(redisCache, Map.class);
        }
        // 2.查询数据库(回源)
        Map<String, Object> albumInfoFromDB = getAlbumInfoFromDB(albumId);
        // 3.数据库同步到缓存
        redisTemplate.opsForValue().set(albumId.toString(), JSONObject.toJSONString(albumInfoFromDB));
        long endTime = System.currentTimeMillis();
        log.info("数据库同步到分布式缓存耗时：{}", endTime - startTime); // 883
        return albumInfoFromDB;
    }

    // 优化1: 本地缓存
    Map<Long, Map<String, Object>> localCache = new HashMap<>();

    private Map<String, Object> getAlbumInfoOptimizeV1(Long albumId) {
        // 耗时 第一次访问: 两个本地缓存没作用 异步起到加速作用(异步本质是压榨CPU)
        // 第二次访问: 两个本地缓存有效[本地缓存的是接口的业务数据 openFeign缓存的是实时服务发现数据]
        long startTime = System.currentTimeMillis();
        // 1.查询缓存(Map)-本地
        if (localCache.containsKey(albumId)) {
            long endTime = System.currentTimeMillis();
            log.info("命中缓存耗时：{}", endTime - startTime); // 0
            return localCache.get(albumId);
        }
        // 2.查询数据库(回源)
        Map<String, Object> albumInfoFromDB = getAlbumInfoFromDB(albumId);
        // 3.数据库同步到缓存
        localCache.put(albumId, albumInfoFromDB);
        long endTime = System.currentTimeMillis();
        log.info("数据库同步到缓存耗时：{}", endTime - startTime); // 26
        return albumInfoFromDB;
    }

    private @NotNull HashMap<String, Object> getAlbumInfoFromDB(Long albumId) {
        HashMap<String, Object> map = new HashMap<>();

        CompletableFuture albumInfoFuture1 = CompletableFuture.supplyAsync(() -> {
            // 1.查询专辑基本信息
            Result<AlbumInfo> albumInfoAndTag = albumInfoFeignClient.getAlbumInfoAndTag(albumId.toString());
            AlbumInfo albumInfoData = albumInfoAndTag.getData();
            Assert.notNull(albumInfoData, "远程调用微服务获取专辑基本信息:专辑基本不存在");
            map.put("albumInfo", albumInfoData);
            return albumInfoData.getUserId();
        }, threadPoolExecutor);

        CompletableFuture albumInfoFuture2 = albumInfoFuture1.thenAcceptAsync(userId -> {
            // 2.查询专辑对应的主播信息
            Result<UserInfoVo> userInfoResult = userInfoFeignClient.getUserInfo(userId.toString());
            UserInfoVo userInfoData = userInfoResult.getData();
            Assert.notNull(userInfoData, "远程调用微服务获取专辑对应的主播信息:主播信息不存在");
            map.put("announcer", userInfoData);
        }, threadPoolExecutor);

        CompletableFuture albumInfoFuture3 = CompletableFuture.runAsync(() -> {
            // 3.查询专辑统计信息
            Result<AlbumStatVo> albumStatResult = albumInfoFeignClient.getAlbumStatByAlbumId(albumId);
            AlbumStatVo albumStatData = albumStatResult.getData();
            Assert.notNull(albumStatData, "远程调用微服务获取专辑统计信息:专辑统计信息不存在");
            map.put("albumStatVo", albumStatData);
        }, threadPoolExecutor);

        CompletableFuture albumInfoFuture4 = CompletableFuture.runAsync(() -> {
            // 4.查询专辑分类信息
            Result<BaseCategoryView> categoryInfoResult = albumInfoFeignClient.getCategoryInfoByAlbumId(albumId);
            BaseCategoryView categoryInfoData = categoryInfoResult.getData();
            Assert.notNull(categoryInfoData, "远程调用微服务获取专辑分类信息:专辑分类信息不存在");
            map.put("baseCategoryView", categoryInfoData);
        }, threadPoolExecutor);

        CompletableFuture.allOf(albumInfoFuture2, albumInfoFuture3, albumInfoFuture4).join();

        return map;
    }

    @Override
    @SneakyThrows
    public void preAlbumToCache() {
        // 1.查询所有一级分类
        Result<List<Long>> category1Ids = albumInfoFeignClient.getAllCategory1Ids();
        List<Long> category1IdsData = category1Ids.getData();
        // 2.根据一级分类id 从Elastic中查询56个维度比较高的前10张专辑
        String[] dimensions = {"hotScore", "playStatNum", "subscribeStatNum", "buyStatNum", "commentStatNum"};
        for (Long category1Id : category1IdsData) {
            for (String dimension : dimensions) {
                SearchRequest searchRequest = new SearchRequest.Builder()
                        .index("albuminfo")
                        .query(qb -> qb
                                .term(tqb -> tqb
                                        .field("category1Id")
                                        .value(category1Id)
                                )
                        )
                        .sort(sob -> sob
                                .field(fqb -> fqb
                                        .field(dimension)
                                        .order(SortOrder.Desc)
                                )
                        )
                        .size(10)
                        .build();
                System.out.println(searchRequest);
                SearchResponse<AlbumInfoIndex> response = elasticsearchClient.search(searchRequest, AlbumInfoIndex.class);

                List<Hit<AlbumInfoIndex>> albumInfoIndexList = response.hits().hits();
                List<AlbumInfoIndex> collect = albumInfoIndexList.stream().map(hit -> {
                    AlbumInfoIndex albumInfoIndex = hit.source();
                    return albumInfoIndex;
                }).collect(Collectors.toList());

                String categoryKey = RedisConstant.RANKING_KEY_PREFIX + category1Id;
                redisTemplate.opsForHash().put(categoryKey, dimension, JSONObject.toJSONString(collect));
            }
        }

        // 3.将结果存到redis
    }

    @Override
    public List<AlbumInfoIndexVo> findRankingList(Long c1Id, String dimension) {
        String categoryKey = RedisConstant.RANKING_KEY_PREFIX + c1Id;
        Object obj = redisTemplate.opsForHash().get(categoryKey, dimension);
        List<AlbumInfoIndex> albumInfoIndexList = JSONObject.parseArray(obj.toString(), AlbumInfoIndex.class);
        List<AlbumInfoIndexVo> collect = albumInfoIndexList.stream().map(albumInfoIndex -> {
            AlbumInfoIndexVo albumInfoIndexVo = new AlbumInfoIndexVo();
            BeanUtils.copyProperties(albumInfoIndex, albumInfoIndexVo);
            return albumInfoIndexVo;
        }).collect(Collectors.toList());
        return collect;
    }

    @Override
    public List<Long> getAlbumIdsFromDb() {
        Result<List<Long>> albumInfoIds = albumInfoFeignClient.getAlbumInfoIds();
        List<Long> albumIds = albumInfoIds.getData();
        return albumIds;
    }
}
