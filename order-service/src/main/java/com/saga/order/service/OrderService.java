package com.saga.order.service;

import com.saga.order.dto.CreateOrderRequest;
import com.saga.order.entity.Order;
import com.saga.order.event.*;

import java.util.Optional;

public interface OrderService {

    /**
     * Create a new order and initiate the Saga
     */
    Order createOrder(CreateOrderRequest request);

    /**
     * Get order by ID
     */
    Optional<Order> getOrder(String orderId);

    /**
     * Get all orders
     */
    Iterable<Order> getAllOrders();

    // ==================== SAGA EVENT HANDLERS ====================

    /**
     * Handle payment processed event
     */
    void handlePaymentProcessed(PaymentProcessedEvent event);

    /**
     * Handle payment failed event
     */
    void handlePaymentFailed(PaymentFailedEvent event);

    /**
     * Handle payment refunded event
     */
    void handlePaymentRefunded(PaymentRefundedEvent event);

    /**
     * Handle pizza prepared event
     */
    void handlePizzaPrepared(PizzaPreparedEvent event);

    /**
     * Handle kitchen failed event
     */
    void handleKitchenFailed(KitchenFailedEvent event);

    /**
     * Handle delivery assigned event
     */
    void handleDeliveryAssigned(DeliveryAssignedEvent event);

    /**
     * Handle delivery failed event
     */
    void handleDeliveryFailed(DeliveryFailedEvent event);
}
