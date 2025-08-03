package com.lsj.tingshu.order.config;

import com.lsj.tingshu.common.rabbit.constant.MqConst;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
public class RabbitMqAutoConfiguration {

    @Bean
    public CustomExchange customExchange() {
        HashMap<String, Object> exchangeArgsMap = new HashMap<>();
        exchangeArgsMap.put("x-delayed-type", "direct");// 交换机的类型还是direct
        return new CustomExchange(MqConst.EXCHANGE_CANCEL_ORDER, "x-delayed-message", true, false, exchangeArgsMap);

    }

    /**
     * 定义关单队列
     * String name,
     * boolean durable,
     * boolean exclusive,
     * boolean autoDelete,
     *
     * @Nullable Map<String, Object> arguments
     */

    @Bean
    public Queue closeQueue() {
        return new Queue(MqConst.QUEUE_CANCEL_ORDER, true, false, false, null);
    }

    /**
     * 定义队列和交换机的绑定
     */
    @Bean
    public Binding binding(CustomExchange customExchange, Queue closeQueue) {
        return BindingBuilder.bind(closeQueue).to(customExchange).with(MqConst.ROUTING_CANCEL_ORDER).noargs();
    }


}
