package com.saga.payment.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundPaymentCommand implements Serializable {
    private String orderId;
    private String reason;
}