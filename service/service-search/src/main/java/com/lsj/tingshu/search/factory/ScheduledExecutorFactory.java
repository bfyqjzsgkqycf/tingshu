package com.lsj.tingshu.search.factory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledExecutorFactory {


    static ScheduledExecutorService scheduledExecutorService;

    static {
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
    }


    /**
     * 私有构造器
     */
    private ScheduledExecutorFactory() {

    }

    // 饿汉式（提前创建）
    private static ScheduledExecutorFactory INSTANCE = new ScheduledExecutorFactory();   // 提前创建一个对象  空间也不会占用很多。

    /**
     * 自己提供唯一一个能获取实例的方法
     */

    public static ScheduledExecutorFactory getInstance() {

//     return new  ScheduledExecutorFactory();  // 懒汉式 并发下，出现线程不安全，new 对象也不是一个原子操作 会分为三步
        return INSTANCE;
    }


    /**
     * 提供延时任务的方法
     */

    public void execute(Runnable runnable, Long ttl, TimeUnit timeUnit) {

        scheduledExecutorService.schedule(runnable, ttl, timeUnit);  // 延时任务
    }


    /**
     * 计算时间的方法
     * 当前时间到7天之后凌晨2点的毫秒值
     */

    public  long diffTime(Long currentTime) {

        // 1.获取7天后的日期对象
        LocalDate localDate = LocalDate.now().plusDays(7);

        // 2.获取7天之后凌晨两点的时间对象
        LocalDateTime localDateTime = LocalDateTime.of(localDate, LocalTime.of(2, 0, 0));

        // 3.将localDateTime转为毫秒

        long epochMilli = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();


        // 4.计算和当前时间差值

        long diffTime = epochMilli -currentTime;

        return  diffTime;

    }


}
