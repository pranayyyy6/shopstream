package com.shopstream.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication

/*
 * @EnableEurekaServer is the only thing that makes this a registry.
 * Without it — just a normal Spring Boot app.
 * With it — a full service discovery server.
 * Every other service will register itself here on startup.
 */
@EnableEurekaServer
public class ServiceRegistryApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceRegistryApplication.class, args);
    }
}