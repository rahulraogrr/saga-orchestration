package com.saga.payment.service;

import com.saga.payment.command.ProcessPaymentCommand;
import com.saga.payment.command.RefundPaymentCommand;
import com.saga.payment.domain.Payment;

import java.util.Optional;

public interface PaymentService {

    /**
     * Process payment for an order
     */
    void processPayment(ProcessPaymentCommand command);

    /**
     * Refund payment for an order (compensation)
     */
    void refundPayment(RefundPaymentCommand command);

    /**
     * Get payment by order ID
     */
    Optional<Payment> getPaymentByOrderId(String orderId);

    /**
     * Get all payments
     */
    Iterable<Payment> getAllPayments();
}