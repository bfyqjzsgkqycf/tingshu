package com.lsj.tingshu.order.closeorder.zset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class DelayOrderCloseZSetService {


    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String DELAY_QUEUE_KEY = "delay:orders";


    /**
     * 添加延时订单到 Redis ZSet
     *
     * @param orderId      订单ID
     * @param delaySeconds 延迟时间（秒）
     */
    public void addDelayOrder(String orderId, long delaySeconds) {
        long expireTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(delaySeconds);
        redisTemplate.opsForZSet().add(DELAY_QUEUE_KEY, orderId, expireTime);
    }

    /**
     * 订单完成时删除对应的延时记录
     */
    public void removeOrder(String orderId) {
        redisTemplate.opsForZSet().remove(DELAY_QUEUE_KEY, orderId);
    }


    /**
     * 每秒钟扫描一次过期订单
     */
    @Scheduled(fixedRate = 1000) // 1秒执行一次
    public void processExpiredOrders() {
        long currentTime = System.currentTimeMillis();
        // 查询所有过期订单（Score <= 当前时间）
        Set<String> expiredOrders = redisTemplate.opsForZSet()
                .rangeByScore(DELAY_QUEUE_KEY, 0, currentTime);

        if (expiredOrders != null && !expiredOrders.isEmpty()) {
            for (String orderId : expiredOrders) {
                // 执行关单逻辑（需根据业务实现）
                boolean success = closeOrder(orderId);
                if (success) {
                    // 关单成功后删除记录
                    removeOrder(orderId);
                }
            }
        }
    }

    /**
     * 关单逻辑
     */
    private boolean closeOrder(String orderId) {
        // 1. 查询数据库订单状态
        // 2. 如果未支付，执行关单操作
        // 3. 返回关单是否成功
        System.out.println("关闭订单: " + orderId);
        return true;
    }
}

