package com.saga.payment.service;

import com.saga.payment.command.ProcessPaymentCommand;
import com.saga.payment.command.RefundPaymentCommand;
import com.saga.payment.config.RabbitMQConfig;
import com.saga.payment.domain.Payment;
import com.saga.payment.domain.PaymentStatus;
import com.saga.payment.event.PaymentFailedEvent;
import com.saga.payment.event.PaymentProcessedEvent;
import com.saga.payment.event.PaymentRefundedEvent;
import com.saga.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final RabbitTemplate rabbitTemplate;
    private final Random random = new Random();

    @Value("${payment.failure.simulation.enabled:false}")
    private boolean failureSimulationEnabled;

    @Value("${payment.failure.simulation.rate:0.3}")
    private double failureRate;

    /**
     * Listen for ProcessPaymentCommand from Order Service
     */
    @Override
    @RabbitListener(queues = RabbitMQConfig.PAYMENT_COMMAND_QUEUE)
    @Transactional
    public void processPayment(ProcessPaymentCommand command) {
        log.info("<<< Received ProcessPaymentCommand: {}", command);

        try {
            // Check if payment already exists (idempotency)
            Optional<Payment> existingPayment = paymentRepository.findByOrderId(command.getOrderId());
            if (existingPayment.isPresent()) {
                log.warn("Payment already processed for order: {}", command.getOrderId());

                Payment payment = existingPayment.get();
                if (payment.getStatus() == PaymentStatus.COMPLETED) {
                    // Re-send success event (idempotent)
                    publishPaymentProcessedEvent(payment);
                } else if (payment.getStatus() == PaymentStatus.FAILED) {
                    // Re-send failure event (idempotent)
                    publishPaymentFailedEvent(payment);
                }
                return;
            }

            // Create payment record
            Payment payment = new Payment();
            payment.setOrderId(command.getOrderId());
            payment.setCustomerId(command.getCustomerId());
            payment.setAmount(command.getAmount());
            payment.setStatus(PaymentStatus.PENDING);

            payment = paymentRepository.save(payment);
            log.info("Payment record created: {}", payment.getId());

            // Simulate payment processing delay
            Thread.sleep(1000); // 1 second delay

            // Simulate payment processing
            boolean paymentSuccessful = processPaymentWithExternalGateway(command);

            if (paymentSuccessful) {
                // Payment succeeded
                payment.setStatus(PaymentStatus.COMPLETED);
                payment = paymentRepository.save(payment);

                log.info("✅ Payment SUCCESSFUL for order: {} | Transaction ID: {}",
                        command.getOrderId(), payment.getId());

                publishPaymentProcessedEvent(payment);

            } else {
                // Payment failed
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Insufficient funds or card declined");
                payment = paymentRepository.save(payment);

                log.error("❌ Payment FAILED for order: {} | Reason: {}",
                        command.getOrderId(), payment.getFailureReason());

                publishPaymentFailedEvent(payment);
            }

        } catch (Exception e) {
            log.error("Error processing payment for order: {}", command.getOrderId(), e);

            // Publish failure event
            PaymentFailedEvent event = new PaymentFailedEvent(
                    command.getOrderId(),
                    "Payment processing error: " + e.getMessage()
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SAGA_EXCHANGE,
                    RabbitMQConfig.ORDER_EVENT_ROUTING_KEY,
                    event
            );
        }
    }

    /**
     * Listen for RefundPaymentCommand from Order Service (COMPENSATION)
     */
    @Override
    @RabbitListener(queues = RabbitMQConfig.PAYMENT_COMMAND_QUEUE)
    @Transactional
    public void refundPayment(RefundPaymentCommand command) {
        log.info("<<< Received RefundPaymentCommand: {}", command);

        try {
            // Find the original payment
            Optional<Payment> paymentOpt = paymentRepository.findByOrderId(command.getOrderId());

            if (paymentOpt.isEmpty()) {
                log.warn("No payment found for order: {} - Cannot refund", command.getOrderId());
                return;
            }

            Payment payment = paymentOpt.get();

            if (payment.getStatus() == PaymentStatus.REFUNDED) {
                log.warn("Payment already refunded for order: {}", command.getOrderId());
                publishPaymentRefundedEvent(payment);
                return;
            }

            if (payment.getStatus() != PaymentStatus.COMPLETED) {
                log.warn("Cannot refund payment with status: {} for order: {}",
                        payment.getStatus(), command.getOrderId());
                return;
            }

            // Simulate refund processing delay
            Thread.sleep(500);

            // Process refund
            log.info("🔄 Processing refund for order: {} | Amount: ${}",
                    command.getOrderId(), payment.getAmount());

            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setFailureReason(command.getReason());
            paymentRepository.save(payment);

            log.info("✅ Refund SUCCESSFUL for order: {}", command.getOrderId());

            publishPaymentRefundedEvent(payment);

        } catch (Exception e) {
            log.error("Error refunding payment for order: {}", command.getOrderId(), e);
        }
    }

    /**
     * Simulate external payment gateway processing
     */
    private boolean processPaymentWithExternalGateway(ProcessPaymentCommand command) {
        log.info("Processing payment with external gateway...");
        log.info("Customer: {} | Amount: ${}", command.getCustomerId(), command.getAmount());

        // Simulate failure based on configuration
        if (failureSimulationEnabled) {
            boolean shouldFail = random.nextDouble() < failureRate;
            if (shouldFail) {
                log.warn("Simulated payment failure ({}% failure rate)", (int)(failureRate * 100));
                return false;
            }
        }

        // In production, this would call actual payment gateway (Stripe, PayPal, etc.)
        // For now, we always succeed unless failure simulation is enabled
        return true;
    }

    /**
     * Publish PaymentProcessedEvent to Order Service
     */
    private void publishPaymentProcessedEvent(Payment payment) {
        PaymentProcessedEvent event = new PaymentProcessedEvent(
                payment.getOrderId(),
                payment.getId()
        );

        log.info(">>> Sending PaymentProcessedEvent: {}", event);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SAGA_EXCHANGE,
                RabbitMQConfig.ORDER_EVENT_ROUTING_KEY,
                event
        );
    }

    /**
     * Publish PaymentFailedEvent to Order Service
     */
    private void publishPaymentFailedEvent(Payment payment) {
        PaymentFailedEvent event = new PaymentFailedEvent(
                payment.getOrderId(),
                payment.getFailureReason()
        );

        log.info(">>> Sending PaymentFailedEvent: {}", event);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SAGA_EXCHANGE,
                RabbitMQConfig.ORDER_EVENT_ROUTING_KEY,
                event
        );
    }

    /**
     * Publish PaymentRefundedEvent to Order Service
     */
    private void publishPaymentRefundedEvent(Payment payment) {
        PaymentRefundedEvent event = new PaymentRefundedEvent(payment.getOrderId());

        log.info(">>> Sending PaymentRefundedEvent: {}", event);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SAGA_EXCHANGE,
                RabbitMQConfig.ORDER_EVENT_ROUTING_KEY,
                event
        );
    }

    @Override
    public Optional<Payment> getPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    @Override
    public Iterable<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }
}