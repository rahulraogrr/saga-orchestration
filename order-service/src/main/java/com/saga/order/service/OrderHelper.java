package com.saga.order.service;

import com.saga.order.command.AssignDeliveryCommand;
import com.saga.order.command.PreparePizzaCommand;
import com.saga.order.command.ProcessPaymentCommand;
import com.saga.order.command.RefundPaymentCommand;
import com.saga.order.config.RabbitMQConfig;
import com.saga.order.dto.CreateOrderRequest;
import com.saga.order.dto.OrderStatus;
import com.saga.order.entity.Order;
import com.saga.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderHelper {

    private final RabbitTemplate rabbitTemplate;
    private final OrderRepository orderRepository;

    /**
     * Calculate order amount based on quantity
     */
    public Double calculateAmount(CreateOrderRequest request) {
        final double PRICE_PER_PIZZA = 15.99;
        return request.getQuantity() * PRICE_PER_PIZZA;
    }

    /**
     * SAGA STEP 1: Initiate Payment
     */
    public void startPaymentProcess(Order order) {
        log.info(">>> SAGA Step 1: Starting payment process for order: {}", order.getId());

        order.setStatus(OrderStatus.PAYMENT_PENDING);
        orderRepository.save(order);

        ProcessPaymentCommand command = new ProcessPaymentCommand(
                order.getId(),
                order.getAmount(),
                order.getCustomerId()
        );

        log.info("Sending ProcessPaymentCommand: {}", command);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SAGA_EXCHANGE,
                RabbitMQConfig.PAYMENT_COMMAND_ROUTING_KEY,
                command
        );
    }

    /**
     * SAGA STEP 2: Initiate Kitchen Preparation
     */
    public void startKitchenProcess(Order order) {
        log.info(">>> SAGA Step 2: Starting kitchen process for order: {}", order.getId());

        order.setStatus(OrderStatus.KITCHEN_PENDING);
        orderRepository.save(order);

        PreparePizzaCommand command = new PreparePizzaCommand(
                order.getId(),
                order.getPizzaType(),
                order.getQuantity()
        );

        log.info("Sending PreparePizzaCommand: {}", command);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SAGA_EXCHANGE,
                RabbitMQConfig.KITCHEN_COMMAND_ROUTING_KEY,
                command
        );
    }

    /**
     * SAGA STEP 3: Initiate Delivery Assignment
     */
    public void startDeliveryProcess(Order order) {
        log.info(">>> SAGA Step 3: Starting delivery process for order: {}", order.getId());

        order.setStatus(OrderStatus.DELIVERY_PENDING);
        orderRepository.save(order);

        AssignDeliveryCommand command = new AssignDeliveryCommand(
                order.getId(),
                order.getDeliveryAddress()
        );

        log.info("Sending AssignDeliveryCommand: {}", command);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SAGA_EXCHANGE,
                RabbitMQConfig.DELIVERY_COMMAND_ROUTING_KEY,
                command
        );
    }

    /**
     * COMPENSATION: Refund Payment
     */
    public void compensatePayment(Order order, String reason) {
        log.info("🔄🔄🔄 COMPENSATION: Refunding payment for order: {} 🔄🔄🔄", order.getId());

        RefundPaymentCommand command = new RefundPaymentCommand(
                order.getId(),
                reason
        );

        log.info("Sending RefundPaymentCommand: {}", command);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SAGA_EXCHANGE,
                RabbitMQConfig.PAYMENT_COMMAND_ROUTING_KEY,
                command
        );

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        log.info("❌❌❌ SAGA COMPENSATED - Order cancelled: {} ❌❌❌", order.getId());
    }


}
