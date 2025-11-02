package com.saga.kitchen.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "kitchen_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Kitchen {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String orderId;

    @Column(nullable = false)
    private String pizzaType;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KitchenStatus status;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    private LocalDateTime preparedAt;

    private String failureReason;

    @PrePersist
    public void prePersist() {
        this.receivedAt = LocalDateTime.now();
    }
}