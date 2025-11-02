package com.saga.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for creating a new pizza order")
public class CreateOrderRequest {

    @Schema(
            description = "Unique identifier of the customer placing the order",
            example = "CUST001",
            required = true
    )
    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @Schema(
            description = "Type/name of the pizza to order",
            example = "Margherita",
            allowableValues = {"Margherita", "Pepperoni", "Vegetarian", "Hawaiian", "BBQ Chicken"},
            required = true
    )
    @NotBlank(message = "Pizza type is required")
    private String pizzaType;

    @Schema(
            description = "Number of pizzas to order",
            example = "2",
            minimum = "1",
            required = true
    )
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @Schema(
            description = "Full delivery address for the order",
            example = "123 Main Street, Apt 4B, New York, NY 10001",
            required = true
    )
    @NotBlank(message = "Delivery address is required")
    private String deliveryAddress;
}