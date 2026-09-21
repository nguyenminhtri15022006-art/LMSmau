package com.lms.identityservice.service.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.lms.identityservice.configuration.SocialOAuthConfig;
import com.lms.identityservice.dto.response.SocialUserInfo;
import com.lms.identityservice.exception.ErrorCode;
import com.lms.identityservice.exception.IdentityException;
import com.lms.identityservice.service.GoogleOAuthService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class GoogleOAuthServiceImpl implements GoogleOAuthService {

    private final SocialOAuthConfig socialOAuthConfig;
    private final RestClient restClient;

    public GoogleOAuthServiceImpl(SocialOAuthConfig socialOAuthConfig, RestClient.Builder restClientBuilder) {
        this.socialOAuthConfig = socialOAuthConfig;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public SocialUserInfo getUserInfo(String code, String redirectUri) {
        SocialOAuthConfig.ProviderConfig config = socialOAuthConfig.getGoogle();
        validateRedirectUri(redirectUri, config.getAllowedRedirectUris());
        GoogleTokenResponse tokenResponse = exchangeCodeForToken(code, redirectUri, config);

        if (tokenResponse == null || tokenResponse.getIdToken() == null) {
            throw new IdentityException(ErrorCode.INVALID_SOCIAL_TOKEN);
        }

        return verifyAndDecodeIdToken(tokenResponse.getIdToken(), config.getClientId());
    }

    private void validateRedirectUri(String redirectUri, List<String> allowedRedirectUris) {
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new IdentityException(ErrorCode.INVALID_SOCIAL_TOKEN);
        }

        if (allowedRedirectUris == null || allowedRedirectUris.isEmpty()) {
            return;
        }

        boolean allowed = allowedRedirectUris.stream()
                .filter(uri -> uri != null && !uri.isBlank())
                .map(String::trim)
                .anyMatch(uri -> uri.equals(redirectUri.trim()));

        if (!allowed) {
            log.warn("Rejected Google OAuth redirect URI: {}", redirectUri);
            throw new IdentityException(ErrorCode.INVALID_SOCIAL_TOKEN);
        }
    }

    private GoogleTokenResponse exchangeCodeForToken(String code, String redirectUri, SocialOAuthConfig.ProviderConfig config) {
        String body = "code=" + encode(code)
                + "&client_id=" + encode(config.getClientId())
                + "&client_secret=" + encode(config.getClientSecret())
                + "&redirect_uri=" + encode(redirectUri)
                + "&grant_type=authorization_code";

        return restClient.post()
                .uri(config.getTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(GoogleTokenResponse.class);
    }

    private SocialUserInfo verifyAndDecodeIdToken(String idToken, String clientId) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(clientId))
                    .build();

            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                throw new IdentityException(ErrorCode.INVALID_SOCIAL_TOKEN);
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();

            return SocialUserInfo.builder()
                    .providerId(payload.getSubject())
                    .email(payload.getEmail())
                    .fullname((String) payload.get("name"))
                    .avatarUrl((String) payload.get("picture"))
                    .build();

        } catch (IdentityException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi verify Google ID Token", e);
            throw new IdentityException(ErrorCode.INVALID_SOCIAL_TOKEN);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class GoogleTokenResponse {
        @JsonProperty("id_token")
        private String idToken;

        @JsonProperty("access_token")
        private String accessToken;
    }
}
