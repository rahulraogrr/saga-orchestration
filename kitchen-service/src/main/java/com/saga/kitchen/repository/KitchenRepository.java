package com.saga.kitchen.repository;

import com.saga.kitchen.domain.Kitchen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KitchenRepository extends JpaRepository<Kitchen, String> {
    Optional<Kitchen> findByOrderId(String orderId);
}