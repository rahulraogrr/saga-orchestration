package com.saga.kitchen.service;

import com.saga.kitchen.command.PreparePizzaCommand;
import com.saga.kitchen.domain.Kitchen;

import java.util.Optional;

public interface KitchenService {

    /**
     * Prepare pizza for an order
     */
    void preparePizza(PreparePizzaCommand command);

    /**
     * Get kitchen order by order ID
     */
    Optional<Kitchen> getKitchenOrderByOrderId(String orderId);

    /**
     * Get all kitchen orders
     */
    Iterable<Kitchen> getAllKitchenOrders();
}