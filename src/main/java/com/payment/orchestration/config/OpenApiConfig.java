package com.payment.orchestration.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration for OpenAPI (Swagger) documentation.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentOrchestrationOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(servers())
                .components(new Components()
                        .addSecuritySchemes("idempotency-key", idempotencyKeyScheme()))
                .addSecurityItem(new SecurityRequirement().addList("idempotency-key"));
    }

    private Info apiInfo() {
        return new Info()
                .title("Payment Orchestration API")
                .description("""
                        A simplified payment orchestration system that provides:
                        
                        **Features:**
                        - Create and fetch payments
                        - Intelligent routing to payment providers
                        - Retry mechanism with exponential backoff
                        - Failover to alternate providers
                        - Idempotency support for safe retries
                        - Payment status tracking
                        
                        **Routing Rules:**
                        - CARD, NET_BANKING → Provider A
                        - UPI, WALLET → Provider B
                        
                        **Idempotency:**
                        All payment creation requests require an `Idempotency-Key` header.
                        The same key with the same request will return the original response.
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("Payment Team")
                        .email("payments@example.com"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    private List<Server> servers() {
        return List.of(
                new Server()
                        .url("http://localhost:8080")
                        .description("Local Development Server"),
                new Server()
                        .url("https://api.payments.example.com")
                        .description("Production Server")
        );
    }

    private SecurityScheme idempotencyKeyScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("Idempotency-Key")
                .description("Unique key to ensure idempotent payment processing");
    }
}
