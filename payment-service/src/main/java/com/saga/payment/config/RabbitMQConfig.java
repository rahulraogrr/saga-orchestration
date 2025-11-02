package com.saga.payment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange
    public static final String SAGA_EXCHANGE = "saga.exchange";

    // Queues
    public static final String PAYMENT_COMMAND_QUEUE = "payment.command.queue";
    public static final String ORDER_EVENT_QUEUE = "order.event.queue";

    // Routing Keys
    public static final String PAYMENT_COMMAND_ROUTING_KEY = "payment.command";
    public static final String ORDER_EVENT_ROUTING_KEY = "order.event";

    @Bean
    public TopicExchange sagaExchange() {
        return new TopicExchange(SAGA_EXCHANGE);
    }

    @Bean
    public Queue paymentCommandQueue() {
        return new Queue(PAYMENT_COMMAND_QUEUE, true);
    }

    @Bean
    public Queue orderEventQueue() {
        return new Queue(ORDER_EVENT_QUEUE, true);
    }

    @Bean
    public Binding paymentCommandBinding() {
        return BindingBuilder
                .bind(paymentCommandQueue())
                .to(sagaExchange())
                .with(PAYMENT_COMMAND_ROUTING_KEY);
    }

    @Bean
    public Binding orderEventBinding() {
        return BindingBuilder
                .bind(orderEventQueue())
                .to(sagaExchange())
                .with(ORDER_EVENT_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}