package com.lsj.tingshu.user.receiver;

import com.lsj.tingshu.common.rabbit.constant.MqConst;
import com.lsj.tingshu.user.service.MqOpsService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Slf4j
public class UserInfoReceiver {
    @Autowired
    private MqOpsService mqOpsService;

    @RabbitListener(bindings = @QueueBinding(value = @Queue(value = MqConst.QUEUE_USER_PAY_RECORD, durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_USER), key = MqConst.ROUTING_USER_PAY_RECORD))
    @SneakyThrows  // 绕开编译时候的异常 但是在真正运行期间 出现了异常还是抛出来
    public void listenUserPaidRecordUpdate(String content, Message message, Channel channel) {

        log.info("下游用户微服务监听到消息{}", content);

        // 1.判读消息是否存在
        if (StringUtils.isEmpty(content)) {
            return;
        }

        // 2.获取消息标识
        long deliveryTag = message.getMessageProperties().getDeliveryTag();


        // 3.真正干活
        mqOpsService.userPaidRecordUpdate(content);
        // 4.手动应答
        channel.basicAck(deliveryTag, false);


    }

}
