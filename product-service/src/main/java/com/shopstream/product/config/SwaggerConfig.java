package com.shopstream.product.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ShopStream — Product Service API")
                        .description(
                                "Production-grade product catalog microservice. " +
                                        "Handles CRUD, search, and Redis-cached retrieval " +
                                        "of 1000+ products.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Arjun Sharma")
                                .email("arjun@gmail.com")
                                .url("https://github.com/yourusername/shopstream")))
                // JWT security scheme — shows Authorize button in UI
                .addSecurityItem(new SecurityRequirement()
                        .addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token from /api/auth/login")));
    }
}