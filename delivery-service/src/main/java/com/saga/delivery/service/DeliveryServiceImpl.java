package com.saga.delivery.service;

import com.saga.delivery.command.AssignDeliveryCommand;
import com.saga.delivery.config.RabbitMQConfig;
import com.saga.delivery.domain.Delivery;
import com.saga.delivery.domain.DeliveryStatus;
import com.saga.delivery.event.DeliveryAssignedEvent;
import com.saga.delivery.event.DeliveryFailedEvent;
import com.saga.delivery.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final RabbitTemplate rabbitTemplate;
    private final Random random = new Random();

    @Value("${delivery.failure.simulation.enabled:false}")
    private boolean failureSimulationEnabled;

    @Value("${delivery.failure.simulation.rate:0.15}")
    private double failureRate;

    /**
     * Listen for AssignDeliveryCommand from Order Service
     */
    @Override
    @RabbitListener(queues = RabbitMQConfig.DELIVERY_COMMAND_QUEUE)
    @Transactional
    public void assignDelivery(AssignDeliveryCommand command) {
        log.info("<<< Received AssignDeliveryCommand: {}", command);

        try {
            // Check if already processed (idempotency)
            Optional<Delivery> existingDelivery = deliveryRepository.findByOrderId(command.getOrderId());
            if (existingDelivery.isPresent()) {
                log.warn("Delivery already processed for order: {}", command.getOrderId());

                Delivery delivery = existingDelivery.get();
                if (delivery.getStatus() == DeliveryStatus.ASSIGNED) {
                    publishDeliveryAssignedEvent(delivery);
                } else if (delivery.getStatus() == DeliveryStatus.FAILED) {
                    publishDeliveryFailedEvent(delivery);
                }
                return;
            }

            // Create delivery record
            Delivery delivery = new Delivery();
            delivery.setOrderId(command.getOrderId());
            delivery.setDeliveryAddress(command.getDeliveryAddress());
            delivery.setStatus(DeliveryStatus.PENDING);

            delivery = deliveryRepository.save(delivery);
            log.info("Delivery record created: {}", delivery.getId());

            // Simulate driver assignment delay
            log.info("🚗 Finding available driver for delivery to: {}", command.getDeliveryAddress());
            Thread.sleep(1500); // 1.5 seconds to find driver

            // Simulate driver assignment
            boolean assignmentSuccessful = simulateDriverAssignment(command);

            if (assignmentSuccessful) {
                // Driver assigned successfully
                String driverId = "DRIVER-" + String.format("%03d", random.nextInt(100));
                delivery.setStatus(DeliveryStatus.ASSIGNED);
                delivery.setDriverId(driverId);
                delivery.setAssignedAt(LocalDateTime.now());
                delivery = deliveryRepository.save(delivery);

                log.info("✅ Driver ASSIGNED for order: {} | Driver ID: {}",
                        command.getOrderId(), driverId);

                publishDeliveryAssignedEvent(delivery);

            } else {
                // No drivers available
                delivery.setStatus(DeliveryStatus.FAILED);
                delivery.setFailureReason("No drivers available in the area");
                delivery = deliveryRepository.save(delivery);

                log.error("❌ Delivery FAILED for order: {} | Reason: {}",
                        command.getOrderId(), delivery.getFailureReason());

                publishDeliveryFailedEvent(delivery);
            }

        } catch (Exception e) {
            log.error("Error assigning delivery for order: {}", command.getOrderId(), e);

            // Publish failure event
            DeliveryFailedEvent event = new DeliveryFailedEvent(
                    command.getOrderId(),
                    "Delivery processing error: " + e.getMessage()
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SAGA_EXCHANGE,
                    RabbitMQConfig.ORDER_EVENT_ROUTING_KEY,
                    event
            );
        }
    }

    /**
     * Simulate driver assignment with possible failure
     */
    private boolean simulateDriverAssignment(AssignDeliveryCommand command) {
        log.info("Checking driver availability...");
        log.info("Delivery Address: {}", command.getDeliveryAddress());

        // Simulate failure based on configuration
        if (failureSimulationEnabled) {
            boolean shouldFail = random.nextDouble() < failureRate;
            if (shouldFail) {
                log.warn("Simulated delivery failure ({}% failure rate)", (int)(failureRate * 100));
                return false;
            }
        }

        // In production, this would check actual driver availability, location, etc.
        return true;
    }

    /**
     * Publish DeliveryAssignedEvent to Order Service
     */
    private void publishDeliveryAssignedEvent(Delivery delivery) {
        DeliveryAssignedEvent event = new DeliveryAssignedEvent(
                delivery.getOrderId(),
                delivery.getDriverId()
        );

        log.info(">>> Sending DeliveryAssignedEvent: {}", event);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SAGA_EXCHANGE,
                RabbitMQConfig.ORDER_EVENT_ROUTING_KEY,
                event
        );
    }

    /**
     * Publish DeliveryFailedEvent to Order Service
     */
    private void publishDeliveryFailedEvent(Delivery delivery) {
        DeliveryFailedEvent event = new DeliveryFailedEvent(
                delivery.getOrderId(),
                delivery.getFailureReason()
        );

        log.info(">>> Sending DeliveryFailedEvent: {}", event);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SAGA_EXCHANGE,
                RabbitMQConfig.ORDER_EVENT_ROUTING_KEY,
                event
        );
    }

    @Override
    public Optional<Delivery> getDeliveryByOrderId(String orderId) {
        return deliveryRepository.findByOrderId(orderId);
    }

    @Override
    public Iterable<Delivery> getAllDeliveries() {
        return deliveryRepository.findAll();
    }
}