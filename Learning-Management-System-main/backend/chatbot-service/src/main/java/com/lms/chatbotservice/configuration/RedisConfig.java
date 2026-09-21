package com.lms.chatbotservice.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfig {

    private static final Duration DEFAULT_COMMAND_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration DEFAULT_EVICTION_INTERVAL = Duration.ofSeconds(30);
    private static final Duration MIN_EVICTABLE_IDLE_TIME = Duration.ofMinutes(2);

    @Bean
    public ObjectMapper redisObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // Template riêng để serialize ChatMessage thành JSON
    // Tách riêng với StringRedisTemplate dùng cho RateLimiter
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory factory,
            ObjectMapper redisObjectMapper) {

        Jackson2JsonRedisSerializer<Object> serializer =
                new Jackson2JsonRedisSerializer<>(redisObjectMapper, Object.class);

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public static BeanPostProcessor lettuceConnectionFactoryValidator() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof LettuceConnectionFactory factory) {
                    factory.setValidateConnection(true);
                }
                return bean;
            }
        };
    }

    @Bean
    public LettuceClientConfigurationBuilderCustomizer resilientLettuceClientCustomizer(
            RedisProperties redisProperties) {
        return builder -> {
            Duration commandTimeout = valueOrDefault(redisProperties.getTimeout(), DEFAULT_COMMAND_TIMEOUT);
            Duration connectTimeout = valueOrDefault(redisProperties.getConnectTimeout(), DEFAULT_CONNECT_TIMEOUT);

            SocketOptions socketOptions = SocketOptions.builder()
                    .connectTimeout(connectTimeout)
                    .keepAlive(true)
                    .tcpNoDelay(true)
                    .build();
            ClientOptions clientOptions = ClientOptions.builder()
                    .autoReconnect(true)
                    .pingBeforeActivateConnection(true)
                    .socketOptions(socketOptions)
                    .timeoutOptions(TimeoutOptions.enabled(commandTimeout))
                    .build();
            builder.commandTimeout(commandTimeout);
            builder.clientOptions(clientOptions);

            if (builder instanceof LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder poolingBuilder) {
                RedisProperties.Pool pool = redisProperties.getLettuce().getPool();
                GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
                poolConfig.setMaxTotal(pool.getMaxActive());
                poolConfig.setMaxIdle(pool.getMaxIdle());
                poolConfig.setMinIdle(pool.getMinIdle());
                poolConfig.setMaxWait(valueOrDefault(pool.getMaxWait(), Duration.ofSeconds(5)));
                poolConfig.setTimeBetweenEvictionRuns(
                        valueOrDefault(pool.getTimeBetweenEvictionRuns(), DEFAULT_EVICTION_INTERVAL));
                poolConfig.setMinEvictableIdleDuration(MIN_EVICTABLE_IDLE_TIME);
                poolConfig.setTestOnBorrow(true);
                poolConfig.setTestWhileIdle(true);
                poolingBuilder.poolConfig(poolConfig);
            }
        };
    }

    private static Duration valueOrDefault(Duration value, Duration defaultValue) {
        return value != null ? value : defaultValue;
    }
}
