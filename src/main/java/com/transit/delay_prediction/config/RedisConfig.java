package com.transit.delay_prediction.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.transit.delay_prediction.entity.VehiclePosition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuration for Redis connectivity and serialization.
 */
@Configuration
public class RedisConfig {

    /**
     * Configures ReactiveRedisTemplate for storing VehiclePosition objects.
     * Uses Jackson2JsonRedisSerializer with JavaTimeModule for LocalDateTime support.
     * @param connectionFactory Redis connection factory.
     * @return ReactiveRedisTemplate for VehiclePosition.
     */
    @Bean
    public ReactiveRedisTemplate<String, VehiclePosition> reactiveRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Register JavaTimeModule for LocalDateTime serialization

        Jackson2JsonRedisSerializer<VehiclePosition> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, VehiclePosition.class);
        RedisSerializationContext.RedisSerializationContextBuilder<String, VehiclePosition> builder =
                RedisSerializationContext.newSerializationContext(new StringRedisSerializer());
        RedisSerializationContext<String, VehiclePosition> context = builder.value(serializer).build();
        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }
}