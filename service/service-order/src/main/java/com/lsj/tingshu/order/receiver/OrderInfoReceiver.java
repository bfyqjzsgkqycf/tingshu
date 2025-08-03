package com.lsj.tingshu.order.receiver;

import com.lsj.tingshu.common.rabbit.constant.MqConst;
import com.lsj.tingshu.common.service.execption.TingShuException;
import com.lsj.tingshu.order.service.MqOpsService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Slf4j
public class OrderInfoReceiver {

    @Autowired
    private MqOpsService mqOpsService;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.QUEUE_LOCAL_MSG, durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_LOCAL_MSG), key = MqConst.ROUTING_LOCAL_MSG))
    @SneakyThrows  // 绕开编译时候的异常 但是在真正运行期间 出现了异常还是抛出来
    public void listenLocalMsgStatusUpdate(String content, Message message, Channel channel) {

        log.info("下游订单微服务监听到消息{}", content);

        // 1.判读消息是否存在
        if (StringUtils.isEmpty(content)) {
            return;
        }

        // 2.获取消息标识
        long deliveryTag = message.getMessageProperties().getDeliveryTag();


        // 3.真正干活
        mqOpsService.localMsgStatusUpdate(content);
        // 4.手动应答
        channel.basicAck(deliveryTag, false);


    }


    @RabbitListener(queues = MqConst.QUEUE_CANCEL_ORDER)
    @SneakyThrows  // 绕开编译时候的异常 但是在真正运行期间 出现了异常还是抛出来
    public void listenCloseOrder(String content, Message message, Channel channel) {

        log.info("下游订单微服务监听到消息{},准备关单", content);

        // 1.判读消息是否存在
        if (StringUtils.isEmpty(content)) {
            return;
        }

        // 2.获取消息标识
        long deliveryTag = message.getMessageProperties().getDeliveryTag();


        // 3.真正干活
        mqOpsService.closeOrder(content);
        // 4.手动应答
        channel.basicAck(deliveryTag, false);
    }


    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.QUEUE_ORDER_PAY_SUCCESS, durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_ORDER), key = MqConst.ROUTING_ORDER_PAY_SUCCESS))
    @SneakyThrows  // 绕开编译时候的异常 但是在真正运行期间 出现了异常还是抛出来
    public void listenWxPaidSuccess(String content, Message message, Channel channel) {

        log.info("下游订单微服务监听到消息{}", content);

        // 1.判读消息是否存在
        if (StringUtils.isEmpty(content)) {
            return;
        }

        // 2.获取消息标识
        long deliveryTag = message.getMessageProperties().getDeliveryTag();


        // 3.真正干活
        try {
            mqOpsService.wxPaidSuccess(content);
            // 4.手动应答
            channel.basicAck(deliveryTag, false);
        } catch (TingShuException e) {
            // 做个判断  如果三次都是失败的  不用在让消费者消费这个消息 可以让消息不进入队列了
            String messageRetryKey = "message:retry:" + content;
            Long count = redisTemplate.opsForValue().increment(messageRetryKey);
            if (count > 3) {
                log.info("消息{}:重试次数已经达到最大{},异常原因是:{}，请人工及时处理", content, count - 1, e.getMessage());
                channel.basicNack(deliveryTag, false, false);
                redisTemplate.delete(messageRetryKey);
            } else {
                log.info("消息{}:进行重试{}", content, count);
                channel.basicNack(deliveryTag, false, true);
            }
            // 如果在三次之内有一次是成功的 则不能将消息从队列删除  还得让消费者消费

        }


    }

}
