package com.transit.delay_prediction.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.transit.delay_prediction.entity.VehiclePosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import jakarta.annotation.PostConstruct;

/**
 * Configuration for Redis connectivity and serialization.
 */
@Configuration
public class RedisConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    /**
     * Log configuration initialization for debugging.
     */
    @PostConstruct
    public void init() {
        logger.info("RedisConfig initialized with host: {}, port: {}", redisHost, redisPort);
    }

    /**
     * Redis connection factory using Lettuce.
     */
    @Bean
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        logger.info("Creating Redis connection factory with host: {}, port: {}", redisHost, redisPort);
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        return new LettuceConnectionFactory(config);
    }

    /**
     * Configures ReactiveRedisTemplate for storing VehiclePosition objects.
     * Uses Jackson2JsonRedisSerializer with JavaTimeModule for LocalDateTime support.
     */
    @Bean
    public ReactiveRedisTemplate<String, VehiclePosition> reactiveRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        Jackson2JsonRedisSerializer<VehiclePosition> valueSerializer =
                new Jackson2JsonRedisSerializer<>(VehiclePosition.class);
        valueSerializer.setObjectMapper(objectMapper);

        StringRedisSerializer keySerializer = new StringRedisSerializer();

        RedisSerializationContext<String, VehiclePosition> context = RedisSerializationContext
                .<String, VehiclePosition>newSerializationContext(keySerializer)
                .value(valueSerializer)
                .hashKey(keySerializer)
                .hashValue(valueSerializer)
                .build();

        logger.info("ReactiveRedisTemplate configured for VehiclePosition with connection factory host: {}", redisHost);
        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }
}