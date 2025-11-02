package com.saga.kitchen.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PizzaPreparedEvent implements Serializable {
    private String orderId;
    private String kitchenId;
}