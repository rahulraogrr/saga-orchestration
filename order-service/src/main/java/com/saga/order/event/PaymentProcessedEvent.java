package com.saga.order.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessedEvent implements Serializable {
    private String orderId;
    private String transactionId;
}
