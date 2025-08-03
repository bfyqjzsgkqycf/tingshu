package com.lsj.tingshu.order.closeorder.delay;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class DelayOrderTask implements Delayed {

    private final String orderId;       // 订单ID
    private final long expireTime;      // 过期时间（时间戳）

    public DelayOrderTask(String orderId, long delaySeconds) {
        this.orderId = orderId;
        this.expireTime = System.currentTimeMillis() + delaySeconds * 1000;
    }

    /**
     * 计算当前任务距离过期还有多长时间
     * 返回剩余时间的数值，并转换为调用方指定的时间单位（
     *
     * @param unit the time unit
     * Delayed 接口要求实现此方法，供延迟队列（如 DelayQueue）内部使用。
     * 当队列调用 take() 或 poll() 方法时，会通过 getDelay 检查队首任务是否已到期：
     * 如果返回值 <= 0，表示任务已到期，可以取出。
     * 如果返回值 > 0，表示任务未到期，队列会阻塞或跳过。
     * @return
     */
    @Override
    public long getDelay(@NotNull TimeUnit unit) {
        long remaining = expireTime - System.currentTimeMillis();
        return unit.convert(remaining, TimeUnit.MILLISECONDS);
    }

    /**
     * 定义任务的排序规则，比较当前任务和另一个任务的到期时间
     * 确保到期时间早的任务在队列中排在前面。
     *
     * @param o the object to be compared.
     * @return
     */
    @Override
    public int compareTo(@NotNull Delayed o) {
        // 到期时间早的任务排在队列前面，确保调用 take() 方法时，优先取出最早到期的任务
        return Long.compare(this.expireTime,((DelayOrderTask) o).expireTime);
    }

    public String getOrderId() {
        return orderId;
    }
}
