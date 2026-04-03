package com.guo.guopicturebackend.outpainting;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
@ConditionalOnProperty(name = "outpainting.mq-enabled", havingValue = "true")
public class OutPaintingRabbitConfig {

    public static final String EXCHANGE = "guo.outpainting.exchange";
    public static final String QUEUE = "guo.outpainting.submit";
    public static final String ROUTING_KEY = "outpainting.submit";

    @Bean
    public TopicExchange outPaintingExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue outPaintingQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding outPaintingBinding(Queue outPaintingQueue, TopicExchange outPaintingExchange) {
        return BindingBuilder.bind(outPaintingQueue).to(outPaintingExchange).with(ROUTING_KEY);
    }
}
