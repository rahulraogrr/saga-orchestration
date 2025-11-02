package com.saga.order.service;

import com.saga.order.config.RabbitMQConfig;
import com.saga.order.dto.CreateOrderRequest;
import com.saga.order.dto.OrderStatus;
import com.saga.order.entity.Order;
import com.saga.order.event.*;
import com.saga.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

//THE ORCHESTRATOR
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;
    private final OrderHelper orderHelper;

    @Override
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        log.info("=== Creating new order for customer: {} ===", request.getCustomerId());

        // Create the order entity
        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setPizzaType(request.getPizzaType());
        order.setQuantity(request.getQuantity());
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setAmount(orderHelper.calculateAmount(request));
        order.setStatus(OrderStatus.CREATED);

        // Save to database
        order = orderRepository.save(order);
        log.info("Order created with ID: {}", order.getId());

        // Start the Saga by initiating payment
        orderHelper.startPaymentProcess(order);

        return order;
    }

    @Override
    public Optional<Order> getOrder(String orderId) {
        log.info("Fetching order with ID: {}", orderId);
        return orderRepository.findById(orderId);
    }

    @Override
    public Iterable<Order> getAllOrders() {
        log.info("Fetching all orders");
        return orderRepository.findAll();
    }

    @Override
    @RabbitListener(queues = RabbitMQConfig.ORDER_EVENT_QUEUE)
    @Transactional
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        log.info("<<< Received PaymentProcessedEvent: {}", event);

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + event.getOrderId()));

        order.setStatus(OrderStatus.PAYMENT_COMPLETED);
        order.setPaymentTransactionId(event.getTransactionId());
        orderRepository.save(order);

        log.info("Payment completed for order: {}", order.getId());

        // Continue Saga - proceed to kitchen
        orderHelper.startKitchenProcess(order);
    }

    @Override
    @RabbitListener(queues = RabbitMQConfig.ORDER_EVENT_QUEUE)
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.error("<<< Received PaymentFailedEvent: {}", event);

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + event.getOrderId()));

        order.setStatus(OrderStatus.PAYMENT_FAILED);
        orderRepository.save(order);

        log.error("❌❌❌ SAGA FAILED - Payment failed for order: {} - Reason: {} ❌❌❌",
                order.getId(), event.getReason());
    }

    @Override
    @RabbitListener(queues = RabbitMQConfig.ORDER_EVENT_QUEUE)
    @Transactional
    public void handlePaymentRefunded(PaymentRefundedEvent event) {
        log.info("<<< Received PaymentRefundedEvent: {}", event);
        log.info("✅ Refund completed for order: {}", event.getOrderId());
    }

    @Override
    @RabbitListener(queues = RabbitMQConfig.ORDER_EVENT_QUEUE)
    @Transactional
    public void handlePizzaPrepared(PizzaPreparedEvent event) {
        log.info("<<< Received PizzaPreparedEvent: {}", event);

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + event.getOrderId()));

        order.setStatus(OrderStatus.KITCHEN_COMPLETED);
        order.setKitchenId(event.getKitchenId());
        orderRepository.save(order);

        log.info("Pizza prepared for order: {}", order.getId());

        // Continue Saga - proceed to delivery
        orderHelper.startDeliveryProcess(order);
    }

    @Override
    @RabbitListener(queues = RabbitMQConfig.ORDER_EVENT_QUEUE)
    @Transactional
    public void handleKitchenFailed(KitchenFailedEvent event) {
        log.error("<<< Received KitchenFailedEvent: {}", event);

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + event.getOrderId()));

        order.setStatus(OrderStatus.KITCHEN_FAILED);
        orderRepository.save(order);

        log.warn("⚠️ Kitchen failed for order: {} - Payment was successful, initiating refund...",
                order.getId());

        // COMPENSATE: Refund the payment
        orderHelper.compensatePayment(order, event.getReason());
    }

    @Override
    @RabbitListener(queues = RabbitMQConfig.ORDER_EVENT_QUEUE)
    @Transactional
    public void handleDeliveryAssigned(DeliveryAssignedEvent event) {
        log.info("<<< Received DeliveryAssignedEvent: {}", event);

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + event.getOrderId()));

        order.setStatus(OrderStatus.COMPLETED);
        order.setDriverId(event.getDriverId());
        orderRepository.save(order);

        log.info("✅✅✅ SAGA COMPLETED SUCCESSFULLY for order: {} ✅✅✅", order.getId());
    }

    @Override
    @RabbitListener(queues = RabbitMQConfig.ORDER_EVENT_QUEUE)
    @Transactional
    public void handleDeliveryFailed(DeliveryFailedEvent event) {
        log.error("<<< Received DeliveryFailedEvent: {}", event);

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + event.getOrderId()));

        order.setStatus(OrderStatus.DELIVERY_FAILED);
        orderRepository.save(order);

        log.warn("⚠️ Delivery failed for order: {} - Payment was successful, initiating refund...",
                order.getId());

        // COMPENSATE: Refund the payment
        orderHelper.compensatePayment(order, event.getReason());
    }

}
