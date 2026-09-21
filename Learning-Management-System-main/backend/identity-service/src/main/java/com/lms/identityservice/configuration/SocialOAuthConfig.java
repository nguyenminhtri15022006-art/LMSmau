package com.lms.identityservice.configuration;

import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.social")
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class SocialOAuthConfig {

    ProviderConfig google;

    @Data
    public static class ProviderConfig {
        String clientId;
        String clientSecret;
        String tokenUrl;
        String userInfoUrl;
        List<String> allowedRedirectUris = Collections.emptyList();
    }
}
