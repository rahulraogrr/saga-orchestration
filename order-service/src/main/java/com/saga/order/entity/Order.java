package com.saga.order.entity;

import com.saga.order.dto.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pizza order entity representing a customer's order in the saga workflow")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Schema(description = "Unique identifier of the order", example = "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6")
    private String id;

    @Column(nullable = false)
    @Schema(description = "Customer identifier", example = "CUST001")
    private String customerId;

    @Column(nullable = false)
    @Schema(description = "Type of pizza ordered", example = "Margherita")
    private String pizzaType;

    @Column(nullable = false)
    @Schema(description = "Number of pizzas ordered", example = "2")
    private Integer quantity;

    @Column(nullable = false)
    @Schema(description = "Total amount in USD", example = "31.98")
    private Double amount;

    @Column(nullable = false)
    @Schema(description = "Delivery address", example = "123 Main Street")
    private String deliveryAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Schema(
            description = "Current status of the order in the saga workflow",
            example = "COMPLETED",
            allowableValues = {
                    "CREATED", "PAYMENT_PENDING", "PAYMENT_COMPLETED", "PAYMENT_FAILED",
                    "KITCHEN_PENDING", "KITCHEN_COMPLETED", "KITCHEN_FAILED",
                    "DELIVERY_PENDING", "COMPLETED", "DELIVERY_FAILED", "CANCELLED"
            }
    )
    private OrderStatus status;

    @Column(nullable = false)
    @Schema(description = "Timestamp when the order was created", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Payment transaction ID (populated after successful payment)", example = "PAY-123456")
    private String paymentTransactionId;

    @Schema(description = "Kitchen identifier that prepared the order", example = "KITCHEN-001")
    private String kitchenId;

    @Schema(description = "Driver identifier assigned for delivery", example = "DRIVER-042")
    private String driverId;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.status = OrderStatus.CREATED;
    }
}