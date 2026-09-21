package com.lms.apigateway.controller;

import com.lms.apigateway.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(GatewayFallbackController.class)
class GatewayFallbackControllerIntegrationTest {

    @Autowired
    WebTestClient webTestClient;

    @Test
    void fallbackReturnsServiceUnavailableJsonForTargetService() {
        webTestClient.get()
                .uri("/fallback/assignment-service")
                .exchange()
                .expectStatus().isEqualTo(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE.getHttpStatusCode())
                .expectHeader().contentTypeCompatibleWith("application/json")
                .expectBody()
                .jsonPath("$.code").isEqualTo(1405)
                .jsonPath("$.service").isEqualTo("assignment-service")
                .jsonPath("$.path").isEqualTo("/fallback/assignment-service");
    }
}
