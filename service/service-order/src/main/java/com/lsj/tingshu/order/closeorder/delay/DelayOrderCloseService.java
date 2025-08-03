package com.lsj.tingshu.order.closeorder.delay;

import org.springframework.stereotype.Service;

import java.util.concurrent.DelayQueue;

@Service
public class DelayOrderCloseService {


    private static final DelayQueue<DelayOrderTask> delayQueue = new DelayQueue<>();

    /**
     * 生产者：添加延时任务
     */
    public static void addDelayOrder(String orderId, long delaySeconds) {
        delayQueue.put(new DelayOrderTask(orderId, delaySeconds));
        System.out.println("订单添加成功: " + orderId + "时间：" + System.currentTimeMillis() + "，延迟时间: " + delaySeconds + "秒");
    }

    /**
     * 消费者：处理到期任务
     */
    public static void processExpiredOrders() {
        while (true) {
            try {
                // 阻塞直到有元素到期
                System.out.println("线程准备取元素");
                DelayOrderTask task = delayQueue.take();
                System.out.println("进一步走下去");
                String orderId = task.getOrderId();
                closeOrder(orderId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * 关单逻辑
     */
    private static void closeOrder(String orderId) {
        System.out.println("订单已关闭: " + orderId + "，时间: " + System.currentTimeMillis());
    }

    public static void main(String[] args) throws InterruptedException {

        // 启动消费者线程
        new Thread(DelayOrderCloseService::processExpiredOrders).start();
        Thread.sleep(3000);
        // 添加测试任务（延迟20秒）
        addDelayOrder("1002", 30);
        addDelayOrder("1001", 20);


    }
}
