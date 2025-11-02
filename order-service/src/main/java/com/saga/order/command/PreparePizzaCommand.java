package com.saga.order.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreparePizzaCommand implements Serializable {
    private String orderId;
    private String pizzaType;
    private Integer quantity;
}
