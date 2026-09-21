package com.lms.chatbotservice.configuration;

import io.lettuce.core.ClientOptions;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RedisConfigTest {

    @Test
    void lettuceConnectionFactoryValidationIsEnabled() {
        BeanPostProcessor postProcessor = RedisConfig.lettuceConnectionFactoryValidator();
        LettuceConnectionFactory factory = new LettuceConnectionFactory("localhost", 6379);

        postProcessor.postProcessBeforeInitialization(factory, "redisConnectionFactory");

        assertThat(factory.getValidateConnection()).isTrue();
    }

    @Test
    void lettuceCustomizerEnablesKeepAliveReconnectAndPoolValidation() {
        RedisProperties properties = new RedisProperties();
        properties.setTimeout(Duration.ofSeconds(5));
        properties.setConnectTimeout(Duration.ofSeconds(3));
        RedisProperties.Pool pool = properties.getLettuce().getPool();
        pool.setMaxActive(30);
        pool.setMaxIdle(5);
        pool.setMinIdle(0);
        pool.setMaxWait(Duration.ofSeconds(5));
        pool.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));

        LettuceClientConfigurationBuilderCustomizer customizer =
                new RedisConfig().resilientLettuceClientCustomizer(properties);
        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder =
                LettucePoolingClientConfiguration.builder();

        customizer.customize(builder);
        LettucePoolingClientConfiguration configuration = builder.build();

        ClientOptions clientOptions = configuration.getClientOptions().orElseThrow();
        assertThat(clientOptions.isAutoReconnect()).isTrue();
        assertThat(clientOptions.isPingBeforeActivateConnection()).isTrue();
        assertThat(clientOptions.getSocketOptions().isKeepAlive()).isTrue();
        assertThat(clientOptions.getSocketOptions().getConnectTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(configuration.getCommandTimeout()).isEqualTo(Duration.ofSeconds(5));

        GenericObjectPoolConfig<?> poolConfig = configuration.getPoolConfig();
        assertThat(poolConfig.getTestOnBorrow()).isTrue();
        assertThat(poolConfig.getTestWhileIdle()).isTrue();
        assertThat(poolConfig.getMaxTotal()).isEqualTo(30);
        assertThat(poolConfig.getMaxIdle()).isEqualTo(5);
        assertThat(poolConfig.getMinIdle()).isZero();
        assertThat(poolConfig.getTimeBetweenEvictionRuns()).isEqualTo(Duration.ofSeconds(30));
        assertThat(poolConfig.getMinEvictableIdleDuration()).isEqualTo(Duration.ofMinutes(2));
    }
}
