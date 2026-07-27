package com.example.transactionmonitoring.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata displayed by Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI transactionMonitoringOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Transaction Monitoring System API")
                        .description("""
                                REST API documentation for transaction monitoring \
                                and alert management system.
                                """)
                        .version("1.0.0"));
    }
}
