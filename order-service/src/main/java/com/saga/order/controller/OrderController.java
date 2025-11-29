package com.saga.order.controller;

import com.saga.order.dto.CreateOrderRequest;
import com.saga.order.entity.Order;
import com.saga.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Order Management", description = "APIs for managing pizza orders and orchestrating the distributed saga workflow")
public class OrderController {

        private final OrderService orderService;

        @Operation(summary = "Create a new pizza order", description = """
                        Creates a new pizza order and initiates the Saga orchestration process.

                        **Saga Flow:**
                        1. Order is created with status CREATED
                        2. Payment processing is initiated (PAYMENT_PENDING)
                        3. If payment succeeds → Kitchen preparation starts (KITCHEN_PENDING)
                        4. If kitchen succeeds → Delivery assignment starts (DELIVERY_PENDING)
                        5. If delivery succeeds → Order marked as COMPLETED ✅

                        **Failure Handling:**
                        - If any step fails, appropriate compensation occurs
                        - Payment failures result in PAYMENT_FAILED status
                        - Kitchen/Delivery failures trigger payment refunds and order cancellation

                        **Note:** The order is created immediately, but the saga runs asynchronously.
                        Use GET /api/orders/{orderId} to track the order status.
                        """)
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Order created successfully and saga initiated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class), examples = @ExampleObject(name = "Successful Order Creation", value = """
                                        {
                                          "id": "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6",
                                          "customerId": "CUST001",
                                          "pizzaType": "Margherita",
                                          "quantity": 2,
                                          "amount": 31.98,
                                          "deliveryAddress": "123 Main Street",
                                          "status": "CREATED",
                                          "createdAt": "2024-01-15T10:30:00",
                                          "paymentTransactionId": null,
                                          "kitchenId": null,
                                          "driverId": null
                                        }
                                        """))),
                        @ApiResponse(responseCode = "400", description = "Invalid request - validation failed", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "Validation Error", value = """
                                        {
                                          "timestamp": "2024-01-15T10:30:00",
                                          "status": 400,
                                          "error": "Bad Request",
                                          "message": "Validation failed",
                                          "errors": [
                                            "Pizza type is required",
                                            "Quantity must be at least 1"
                                          ]
                                        }
                                        """)))
        })
        @PostMapping
        public ResponseEntity<Order> createOrder(
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Order details including customer ID, pizza type, quantity, and delivery address", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreateOrderRequest.class), examples = {
                                        @ExampleObject(name = "Single Margherita Pizza", value = """
                                                        {
                                                          "customerId": "CUST001",
                                                          "pizzaType": "Margherita",
                                                          "quantity": 1,
                                                          "deliveryAddress": "123 Main Street"
                                                        }
                                                        """),
                                        @ExampleObject(name = "Multiple Pepperoni Pizzas", value = """
                                                        {
                                                          "customerId": "CUST002",
                                                          "pizzaType": "Pepperoni",
                                                          "quantity": 3,
                                                          "deliveryAddress": "456 Oak Avenue"
                                                        }
                                                        """),
                                        @ExampleObject(name = "Vegetarian Special", value = """
                                                        {
                                                          "customerId": "CUST003",
                                                          "pizzaType": "Vegetarian",
                                                          "quantity": 2,
                                                          "deliveryAddress": "789 Pine Road"
                                                        }
                                                        """)
                        })) @Valid @RequestBody CreateOrderRequest request) {
                log.info("Received create order request: {}", request);
                Order order = orderService.createOrder(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(order);
        }

        @Operation(summary = "Get order by ID", description = """
                        Retrieves a specific order by its unique identifier.

                        **Order Status Values:**
                        - `CREATED` - Order just created
                        - `PAYMENT_PENDING` - Waiting for payment confirmation
                        - `PAYMENT_COMPLETED` - Payment successful
                        - `PAYMENT_FAILED` - Payment failed (terminal state)
                        - `KITCHEN_PENDING` - Waiting for kitchen to prepare
                        - `KITCHEN_COMPLETED` - Pizza prepared
                        - `KITCHEN_FAILED` - Kitchen preparation failed
                        - `DELIVERY_PENDING` - Waiting for driver assignment
                        - `COMPLETED` - Order successfully delivered ✅
                        - `DELIVERY_FAILED` - Delivery failed
                        - `CANCELLED` - Order cancelled due to failure (after compensation)

                        **Tracking Saga Progress:**
                        Poll this endpoint to track the saga's progress through the status field.
                        """)
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Order found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class), examples = {
                                        @ExampleObject(name = "Completed Order", value = """
                                                        {
                                                          "id": "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6",
                                                          "customerId": "CUST001",
                                                          "pizzaType": "Margherita",
                                                          "quantity": 2,
                                                          "amount": 31.98,
                                                          "deliveryAddress": "123 Main Street",
                                                          "status": "COMPLETED",
                                                          "createdAt": "2024-01-15T10:30:00",
                                                          "paymentTransactionId": "PAY-123456",
                                                          "kitchenId": "KITCHEN-001",
                                                          "driverId": "DRIVER-042"
                                                        }
                                                        """),
                                        @ExampleObject(name = "Pending Order", value = """
                                                        {
                                                          "id": "b2c3d4e5-f6g7-8h9i-0j1k-l2m3n4o5p6q7",
                                                          "customerId": "CUST002",
                                                          "pizzaType": "Pepperoni",
                                                          "quantity": 1,
                                                          "amount": 15.99,
                                                          "deliveryAddress": "456 Oak Avenue",
                                                          "status": "KITCHEN_PENDING",
                                                          "createdAt": "2024-01-15T11:15:00",
                                                          "paymentTransactionId": "PAY-789012",
                                                          "kitchenId": null,
                                                          "driverId": null
                                                        }
                                                        """),
                                        @ExampleObject(name = "Cancelled Order (After Compensation)", value = """
                                                        {
                                                          "id": "c3d4e5f6-g7h8-9i0j-1k2l-m3n4o5p6q7r8",
                                                          "customerId": "CUST003",
                                                          "pizzaType": "Hawaiian",
                                                          "quantity": 1,
                                                          "amount": 15.99,
                                                          "deliveryAddress": "789 Pine Road",
                                                          "status": "CANCELLED",
                                                          "createdAt": "2024-01-15T12:00:00",
                                                          "paymentTransactionId": "PAY-345678",
                                                          "kitchenId": null,
                                                          "driverId": null
                                                        }
                                                        """)
                        })),
                        @ApiResponse(responseCode = "404", description = "Order not found", content = @Content)
        })
        @GetMapping("/{orderId}")
        public ResponseEntity<Order> getOrder(
                        @Parameter(description = "Unique identifier of the order (UUID format)", example = "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6", required = true) @PathVariable @NonNull String orderId) {
                log.info("Fetching order with ID: {}", orderId);
                return orderService.getOrder(orderId)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
        }

        @Operation(summary = "Get all orders", description = """
                        Retrieves all orders in the system.

                        **Use Cases:**
                        - Monitor all orders and their statuses
                        - View saga success/failure rates
                        - Track system performance
                        - Debug distributed transaction flows

                        **Note:** In production, this endpoint would be paginated.
                        """)
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "List of all orders retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class), examples = @ExampleObject(name = "Multiple Orders", value = """
                                        [
                                          {
                                            "id": "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6",
                                            "customerId": "CUST001",
                                            "pizzaType": "Margherita",
                                            "quantity": 2,
                                            "amount": 31.98,
                                            "deliveryAddress": "123 Main Street",
                                            "status": "COMPLETED",
                                            "createdAt": "2024-01-15T10:30:00",
                                            "paymentTransactionId": "PAY-123456",
                                            "kitchenId": "KITCHEN-001",
                                            "driverId": "DRIVER-042"
                                          },
                                          {
                                            "id": "b2c3d4e5-f6g7-8h9i-0j1k-l2m3n4o5p6q7",
                                            "customerId": "CUST002",
                                            "pizzaType": "Pepperoni",
                                            "quantity": 1,
                                            "amount": 15.99,
                                            "deliveryAddress": "456 Oak Avenue",
                                            "status": "PAYMENT_PENDING",
                                            "createdAt": "2024-01-15T11:15:00",
                                            "paymentTransactionId": null,
                                            "kitchenId": null,
                                            "driverId": null
                                          }
                                        ]
                                        """)))
        })
        @GetMapping
        public ResponseEntity<Iterable<Order>> getAllOrders() {
                log.info("Fetching all orders");
                return ResponseEntity.ok(orderService.getAllOrders());
        }
}