package com.saga.kitchen.service;

import com.saga.kitchen.command.PreparePizzaCommand;
import com.saga.kitchen.config.RabbitMQConfig;
import com.saga.kitchen.domain.Kitchen;
import com.saga.kitchen.domain.KitchenStatus;
import com.saga.kitchen.event.KitchenFailedEvent;
import com.saga.kitchen.event.PizzaPreparedEvent;
import com.saga.kitchen.repository.KitchenRepository;
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
public class KitchenServiceImpl implements KitchenService {

    private final KitchenRepository kitchenRepository;
    private final RabbitTemplate rabbitTemplate;
    private final Random random = new Random();

    @Value("${kitchen.failure.simulation.enabled:false}")
    private boolean failureSimulationEnabled;

    @Value("${kitchen.failure.simulation.rate:0.2}")
    private double failureRate;

    /**
     * Listen for PreparePizzaCommand from Order Service
     */
    @Override
    @RabbitListener(queues = RabbitMQConfig.KITCHEN_COMMAND_QUEUE)
    @Transactional
    public void preparePizza(PreparePizzaCommand command) {
        log.info("<<< Received PreparePizzaCommand: {}", command);

        try {
            // Check if already processed (idempotency)
            Optional<Kitchen> existingOrder = kitchenRepository.findByOrderId(command.getOrderId());
            if (existingOrder.isPresent()) {
                log.warn("Pizza order already processed for order: {}", command.getOrderId());

                Kitchen kitchen = existingOrder.get();
                if (kitchen.getStatus() == KitchenStatus.PREPARED) {
                    publishPizzaPreparedEvent(kitchen);
                } else if (kitchen.getStatus() == KitchenStatus.FAILED) {
                    publishKitchenFailedEvent(kitchen);
                }
                return;
            }

            // Create kitchen order record
            Kitchen kitchen = new Kitchen();
            kitchen.setOrderId(command.getOrderId());
            kitchen.setPizzaType(command.getPizzaType());
            kitchen.setQuantity(command.getQuantity());
            kitchen.setStatus(KitchenStatus.PENDING);

            kitchen = kitchenRepository.save(kitchen);
            log.info("Kitchen order created: {}", kitchen.getId());

            // Simulate pizza preparation delay
            kitchen.setStatus(KitchenStatus.PREPARING);
            kitchenRepository.save(kitchen);

            log.info("🍕 Preparing {} {} pizza(s)...", command.getQuantity(), command.getPizzaType());
            Thread.sleep(2000); // 2 seconds to "prepare" pizza

            // Simulate kitchen failure
            boolean preparationSuccessful = simulateKitchenPreparation(command);

            if (preparationSuccessful) {
                // Pizza prepared successfully
                kitchen.setStatus(KitchenStatus.PREPARED);
                kitchen.setPreparedAt(LocalDateTime.now());
                kitchen = kitchenRepository.save(kitchen);

                log.info("✅ Pizza PREPARED for order: {} | Kitchen ID: {}",
                        command.getOrderId(), kitchen.getId());

                publishPizzaPreparedEvent(kitchen);

            } else {
                // Kitchen failed
                kitchen.setStatus(KitchenStatus.FAILED);
                kitchen.setFailureReason("Out of ingredients or kitchen capacity full");
                kitchen = kitchenRepository.save(kitchen);

                log.error("❌ Kitchen FAILED for order: {} | Reason: {}",
                        command.getOrderId(), kitchen.getFailureReason());

                publishKitchenFailedEvent(kitchen);
            }

        } catch (Exception e) {
            log.error("Error preparing pizza for order: {}", command.getOrderId(), e);

            // Publish failure event
            KitchenFailedEvent event = new KitchenFailedEvent(
                    command.getOrderId(),
                    "Kitchen processing error: " + e.getMessage()
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SAGA_EXCHANGE,
                    RabbitMQConfig.ORDER_EVENT_ROUTING_KEY,
                    event
            );
        }
    }

    /**
     * Simulate kitchen preparation with possible failure
     */
    private boolean simulateKitchenPreparation(PreparePizzaCommand command) {
        log.info("Kitchen preparing order...");
        log.info("Pizza Type: {} | Quantity: {}", command.getPizzaType(), command.getQuantity());

        // Simulate failure based on configuration
        if (failureSimulationEnabled) {
            boolean shouldFail = random.nextDouble() < failureRate;
            if (shouldFail) {
                log.warn("Simulated kitchen failure ({}% failure rate)", (int)(failureRate * 100));
                return false;
            }
        }

        // In production, this would check actual kitchen capacity, ingredients, etc.
        return true;
    }

    /**
     * Publish PizzaPreparedEvent to Order Service
     */
    private void publishPizzaPreparedEvent(Kitchen kitchen) {
        PizzaPreparedEvent event = new PizzaPreparedEvent(
                kitchen.getOrderId(),
                kitchen.getId()
        );

        log.info(">>> Sending PizzaPreparedEvent: {}", event);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SAGA_EXCHANGE,
                RabbitMQConfig.ORDER_EVENT_ROUTING_KEY,
                event
        );
    }

    /**
     * Publish KitchenFailedEvent to Order Service
     */
    private void publishKitchenFailedEvent(Kitchen kitchen) {
        KitchenFailedEvent event = new KitchenFailedEvent(
                kitchen.getOrderId(),
                kitchen.getFailureReason()
        );

        log.info(">>> Sending KitchenFailedEvent: {}", event);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.SAGA_EXCHANGE,
                RabbitMQConfig.ORDER_EVENT_ROUTING_KEY,
                event
        );
    }

    @Override
    public Optional<Kitchen> getKitchenOrderByOrderId(String orderId) {
        return kitchenRepository.findByOrderId(orderId);
    }

    @Override
    public Iterable<Kitchen> getAllKitchenOrders() {
        return kitchenRepository.findAll();
    }
}