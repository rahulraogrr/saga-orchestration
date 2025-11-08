package com.saga.delivery.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignDeliveryCommand implements Serializable {
    private String orderId;
    private String deliveryAddress;
}