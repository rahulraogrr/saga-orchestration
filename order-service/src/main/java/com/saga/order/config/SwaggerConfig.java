package com.saga.order.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI orderServiceAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Pizza Order Service - Saga Orchestrator API")
                        .description(getApiDescription())
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Pizza Order Team")
                                .email("support@pizzaorder.com")
                                .url("https://pizzaorder.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081")
                                .description("Development Server")
                ));
    }

    private String getApiDescription() {
        return "# Pizza Order Service - Distributed Saga Pattern\n\n" +
                "## Overview\n" +
                "This service implements the **Saga Orchestration Pattern** for managing distributed transactions across multiple microservices in a pizza ordering system.\n\n" +
                "## Architecture\n\n" +
                "### Services Involved:\n" +
                "1. **Order Service** (This service - Port 8081) - Saga Orchestrator\n" +
                "2. **Payment Service** (Port 8082) - Handles payment processing\n" +
                "3. **Kitchen Service** (Port 8083) - Manages pizza preparation\n" +
                "4. **Delivery Service** (Port 8084) - Assigns delivery drivers\n\n" +
                "---\n\n" +
                "## Saga Flow - Happy Path\n\n" +
                "```\n" +
                "Customer places order\n" +
                "    ↓\n" +
                "[1] Order Service creates order (Status: CREATED)\n" +
                "    ↓\n" +
                "[2] Order Service → Payment Service: ProcessPaymentCommand\n" +
                "    ↓ (Status: PAYMENT_PENDING)\n" +
                "[3] Payment Service processes payment\n" +
                "    ↓\n" +
                "[4] Payment Service → Order Service: PaymentProcessedEvent\n" +
                "    ↓ (Status: PAYMENT_COMPLETED)\n" +
                "[5] Order Service → Kitchen Service: PreparePizzaCommand\n" +
                "    ↓ (Status: KITCHEN_PENDING)\n" +
                "[6] Kitchen Service prepares pizza\n" +
                "    ↓\n" +
                "[7] Kitchen Service → Order Service: PizzaPreparedEvent\n" +
                "    ↓ (Status: KITCHEN_COMPLETED)\n" +
                "[8] Order Service → Delivery Service: AssignDeliveryCommand\n" +
                "    ↓ (Status: DELIVERY_PENDING)\n" +
                "[9] Delivery Service assigns driver\n" +
                "    ↓\n" +
                "[10] Delivery Service → Order Service: DeliveryAssignedEvent\n" +
                "    ↓ (Status: COMPLETED) ✅\n" +
                "Order Complete!\n" +
                "```\n\n" +
                "---\n\n" +
                "## Saga Flow - Failure & Compensation\n\n" +
                "### Scenario 1: Payment Fails\n" +
                "```\n" +
                "Customer places order\n" +
                "    ↓\n" +
                "Order Service creates order (Status: CREATED)\n" +
                "    ↓\n" +
                "Order Service → Payment Service: ProcessPaymentCommand\n" +
                "    ↓\n" +
                "Payment Service fails (insufficient funds)\n" +
                "    ↓\n" +
                "Payment Service → Order Service: PaymentFailedEvent\n" +
                "    ↓\n" +
                "Order Status: PAYMENT_FAILED ❌\n" +
                "(No compensation needed - nothing to rollback)\n" +
                "```\n\n" +
                "### Scenario 2: Kitchen Fails (Compensation Required)\n" +
                "```\n" +
                "Order created → Payment succeeds ✅\n" +
                "    ↓\n" +
                "Order Service → Kitchen Service: PreparePizzaCommand\n" +
                "    ↓\n" +
                "Kitchen Service fails (out of ingredients)\n" +
                "    ↓\n" +
                "Kitchen Service → Order Service: KitchenFailedEvent\n" +
                "    ↓\n" +
                "🔄 COMPENSATION STARTS\n" +
                "    ↓\n" +
                "Order Service → Payment Service: RefundPaymentCommand\n" +
                "    ↓\n" +
                "Payment Service refunds customer\n" +
                "    ↓\n" +
                "Payment Service → Order Service: PaymentRefundedEvent\n" +
                "    ↓\n" +
                "Order Status: CANCELLED ❌\n" +
                "```\n\n" +
                "### Scenario 3: Delivery Fails (Compensation Required)\n" +
                "```\n" +
                "Order created → Payment succeeds ✅ → Kitchen succeeds ✅\n" +
                "    ↓\n" +
                "Order Service → Delivery Service: AssignDeliveryCommand\n" +
                "    ↓\n" +
                "Delivery Service fails (no drivers available)\n" +
                "    ↓\n" +
                "Delivery Service → Order Service: DeliveryFailedEvent\n" +
                "    ↓\n" +
                "🔄 COMPENSATION STARTS\n" +
                "    ↓\n" +
                "Order Service → Payment Service: RefundPaymentCommand\n" +
                "    ↓\n" +
                "Payment Service refunds customer\n" +
                "    ↓\n" +
                "Order Status: CANCELLED ❌\n" +
                "(Pizza prepared but order cancelled - handled separately)\n" +
                "```\n\n" +
                "---\n\n" +
                "## Order Status Flow\n\n" +
                "```\n" +
                "CREATED\n" +
                "    ↓\n" +
                "PAYMENT_PENDING\n" +
                "    ├→ PAYMENT_COMPLETED (success)\n" +
                "    │       ↓\n" +
                "    │   KITCHEN_PENDING\n" +
                "    │       ├→ KITCHEN_COMPLETED (success)\n" +
                "    │       │       ↓\n" +
                "    │       │   DELIVERY_PENDING\n" +
                "    │       │       ├→ COMPLETED ✅ (success)\n" +
                "    │       │       └→ DELIVERY_FAILED → CANCELLED ❌\n" +
                "    │       └→ KITCHEN_FAILED → CANCELLED ❌\n" +
                "    └→ PAYMENT_FAILED ❌\n" +
                "```\n\n" +
                "---\n\n" +
                "## Message Flow (RabbitMQ)\n\n" +
                "### Commands (Order Service → Other Services):\n" +
                "- `payment.command.queue` - ProcessPaymentCommand, RefundPaymentCommand\n" +
                "- `kitchen.command.queue` - PreparePizzaCommand\n" +
                "- `delivery.command.queue` - AssignDeliveryCommand\n\n" +
                "### Events (Other Services → Order Service):\n" +
                "- `order.event.queue` - All response events\n" +
                "  - PaymentProcessedEvent\n" +
                "  - PaymentFailedEvent\n" +
                "  - PaymentRefundedEvent\n" +
                "  - PizzaPreparedEvent\n" +
                "  - KitchenFailedEvent\n" +
                "  - DeliveryAssignedEvent\n" +
                "  - DeliveryFailedEvent\n\n" +
                "---\n\n" +
                "## Key Concepts\n\n" +
                "### 1. Saga Orchestration\n" +
                "- Order Service acts as the **central coordinator**\n" +
                "- Maintains the state of the entire workflow\n" +
                "- Decides the next step based on events received\n\n" +
                "### 2. Compensation\n" +
                "- When a step fails after previous steps succeeded, we must **undo** those steps\n" +
                "- Example: If kitchen fails, we refund the payment\n" +
                "- Compensation happens in **reverse order**\n\n" +
                "### 3. Eventual Consistency\n" +
                "- System may be temporarily inconsistent\n" +
                "- Eventually reaches a consistent state (COMPLETED or CANCELLED)\n\n" +
                "### 4. Asynchronous Communication\n" +
                "- Services communicate via message queues (RabbitMQ)\n" +
                "- No blocking calls between services\n" +
                "- High scalability and resilience\n\n" +
                "---\n\n" +
                "## Testing the API\n\n" +
                "### 1. Create an Order (Happy Path)\n" +
                "```bash\n" +
                "POST /api/orders\n" +
                "{\n" +
                "  \"customerId\": \"CUST001\",\n" +
                "  \"pizzaType\": \"Margherita\",\n" +
                "  \"quantity\": 2,\n" +
                "  \"deliveryAddress\": \"123 Main Street\"\n" +
                "}\n" +
                "```\n\n" +
                "### 2. Check Order Status\n" +
                "```bash\n" +
                "GET /api/orders/{orderId}\n" +
                "```\n\n" +
                "### 3. View All Orders\n" +
                "```bash\n" +
                "GET /api/orders\n" +
                "```\n\n" +
                "---\n\n" +
                "## Additional Resources\n\n" +
                "- RabbitMQ Management UI: http://localhost:15672 (guest/guest)\n" +
                "- H2 Database Console: http://localhost:8081/h2-console\n" +
                "- Swagger UI: http://localhost:8081/swagger-ui.html\n\n" +
                "---\n\n" +
                "## Technologies Used\n" +
                "- Spring Boot 3.3.0\n" +
                "- Spring Data JPA\n" +
                "- RabbitMQ (AMQP)\n" +
                "- H2 Database\n" +
                "- Lombok\n" +
                "- SpringDoc OpenAPI 3\n";
    }
}
