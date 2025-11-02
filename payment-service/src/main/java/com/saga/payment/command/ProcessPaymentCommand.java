package com.saga.payment.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessPaymentCommand implements Serializable {
    private String orderId;
    private Double amount;
    private String customerId;
}