package com.saga.kitchen.controller;

import com.saga.kitchen.domain.Kitchen;
import com.saga.kitchen.service.KitchenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kitchen")
@RequiredArgsConstructor
@Slf4j
public class KitchenController {

    private final KitchenService kitchenService;

    /**
     * Get kitchen order by order ID
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<Kitchen> getKitchenOrderByOrderId(@PathVariable String orderId) {
        return kitchenService.getKitchenOrderByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all kitchen orders
     */
    @GetMapping
    public ResponseEntity<Iterable<Kitchen>> getAllKitchenOrders() {
        return ResponseEntity.ok(kitchenService.getAllKitchenOrders());
    }
}