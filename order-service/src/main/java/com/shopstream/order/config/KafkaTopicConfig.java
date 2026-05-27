package com.shopstream.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    /*
     * Creates the Kafka topic programmatically on startup.
     * If topic already exists — silently skips creation.
     * numPartitions(3) — 3 partitions = 3 consumers can
     * read in parallel = 3× throughput.
     * replicas(1) — only 1 broker in dev. In production: 3.
     */
    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name("order.created")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderUpdatedTopic() {
        return TopicBuilder.name("order.updated")
                .partitions(3)
                .replicas(1)
                .build();
    }
}