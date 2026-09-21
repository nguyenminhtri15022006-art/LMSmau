package com.lms.courseservice.configuration;

import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import javax.net.ssl.SSLContext;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableMongoAuditing
public class MongoConfig {

    @Value("${course.mongodb.tls-version:TLSv1.2}")
    String tlsVersion;

    @Value("${course.mongodb.connect-timeout-seconds:30}")
    int connectTimeoutSeconds;

    @Value("${course.mongodb.read-timeout-seconds:30}")
    int readTimeoutSeconds;

    @Value("${course.mongodb.pool.max-size:20}")
    int maxPoolSize;

    @Value("${course.mongodb.pool.min-size:5}")
    int minPoolSize;

    @Value("${course.mongodb.pool.max-wait-time-seconds:30}")
    int maxWaitTimeSeconds;

    @Bean
    public MongoClientSettingsBuilderCustomizer mongoClientSettingsCustomizer() {
        return builder -> {
            builder.applyToSslSettings(ssl -> {
                try {
                    // Force TLSv1.2 to fix Java 21 compatibility with MongoDB Atlas
                    SSLContext sslContext = SSLContext.getInstance(tlsVersion);
                    sslContext.init(null, null, null);
                    ssl.context(sslContext);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to initialize " + tlsVersion + " SSLContext", e);
                }
            });

            builder.applyToSocketSettings(socket -> {
                socket.connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS);
                socket.readTimeout(readTimeoutSeconds, TimeUnit.SECONDS);
            });

            builder.applyToConnectionPoolSettings(pool -> {
                pool.maxSize(maxPoolSize);
                pool.minSize(minPoolSize);
                pool.maxWaitTime(maxWaitTimeSeconds, TimeUnit.SECONDS);
            });
        };
    }
}
