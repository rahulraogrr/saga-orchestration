package com.saga.delivery.service;

import com.saga.delivery.command.AssignDeliveryCommand;
import com.saga.delivery.domain.Delivery;

import java.util.Optional;

public interface DeliveryService {

    /**
     * Assign delivery driver for an order
     */
    void assignDelivery(AssignDeliveryCommand command);

    /**
     * Get delivery by order ID
     */
    Optional<Delivery> getDeliveryByOrderId(String orderId);

    /**
     * Get all deliveries
     */
    Iterable<Delivery> getAllDeliveries();
}