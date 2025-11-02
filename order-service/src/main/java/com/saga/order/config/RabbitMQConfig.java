package com.saga.order.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange
    public static final String SAGA_EXCHANGE = "saga.exchange";

    // Command Queues (Order Service sends TO these)
    public static final String PAYMENT_COMMAND_QUEUE = "payment.command.queue";
    public static final String KITCHEN_COMMAND_QUEUE = "kitchen.command.queue";
    public static final String DELIVERY_COMMAND_QUEUE = "delivery.command.queue";

    // Event Queue (Order Service receives FROM this)
    public static final String ORDER_EVENT_QUEUE = "order.event.queue";

    // Routing Keys
    public static final String PAYMENT_COMMAND_ROUTING_KEY = "payment.command";
    public static final String KITCHEN_COMMAND_ROUTING_KEY = "kitchen.command";
    public static final String DELIVERY_COMMAND_ROUTING_KEY = "delivery.command";
    public static final String ORDER_EVENT_ROUTING_KEY = "order.event";

    // Exchange
    @Bean
    public TopicExchange sagaExchange() {
        return new TopicExchange(SAGA_EXCHANGE);
    }

    // Queues
    @Bean
    public Queue paymentCommandQueue() {
        return new Queue(PAYMENT_COMMAND_QUEUE, true);
    }

    @Bean
    public Queue kitchenCommandQueue() {
        return new Queue(KITCHEN_COMMAND_QUEUE, true);
    }

    @Bean
    public Queue deliveryCommandQueue() {
        return new Queue(DELIVERY_COMMAND_QUEUE, true);
    }

    @Bean
    public Queue orderEventQueue() {
        return new Queue(ORDER_EVENT_QUEUE, true);
    }

    // Bindings
    @Bean
    public Binding paymentCommandBinding() {
        return BindingBuilder
                .bind(paymentCommandQueue())
                .to(sagaExchange())
                .with(PAYMENT_COMMAND_ROUTING_KEY);
    }

    @Bean
    public Binding kitchenCommandBinding() {
        return BindingBuilder
                .bind(kitchenCommandQueue())
                .to(sagaExchange())
                .with(KITCHEN_COMMAND_ROUTING_KEY);
    }

    @Bean
    public Binding deliveryCommandBinding() {
        return BindingBuilder
                .bind(deliveryCommandQueue())
                .to(sagaExchange())
                .with(DELIVERY_COMMAND_ROUTING_KEY);
    }

    @Bean
    public Binding orderEventBinding() {
        return BindingBuilder
                .bind(orderEventQueue())
                .to(sagaExchange())
                .with(ORDER_EVENT_ROUTING_KEY);
    }

    // Message Converter
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // RabbitTemplate
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
