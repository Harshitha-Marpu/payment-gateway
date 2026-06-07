package com.payment.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentGatewayOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Payment Gateway API")
                .description("""
                    A production-grade payment gateway built with Spring Boot.
                    
                    Features:
                    - Payment authorization with fraud detection
                    - Capture, void, and refund operations
                    - Idempotency support
                    - API key authentication
                    - Real-time transaction status tracking
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("Payment Gateway")
                    .email("support@paymentgateway.com"))
                .license(new License()
                    .name("MIT License")))
            .addSecurityItem(new SecurityRequirement()
                .addList("X-API-Key"))
            .components(new Components()
                .addSecuritySchemes("X-API-Key", new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-API-Key")
                    .description("API key for authentication. Use: test-api-key-12345")));
    }
}