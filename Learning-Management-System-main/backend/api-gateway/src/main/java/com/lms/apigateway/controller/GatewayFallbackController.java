package com.lms.apigateway.controller;

import com.lms.apigateway.exception.ErrorCode;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@RestController
public class GatewayFallbackController {

    @RequestMapping(value = "/fallback/{serviceName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> fallback(@PathVariable String serviceName,
                                                              ServerWebExchange exchange) {
        ErrorCode errorCode = ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE;
        String originalPath = resolveOriginalPath(exchange);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", errorCode.getHttpStatusCode().value());
        body.put("error", errorCode.getHttpStatusCode().getReasonPhrase());
        body.put("code", errorCode.getCode());
        body.put("message", errorCode.getMessage());
        body.put("path", originalPath);
        body.put("service", serviceName);

        return Mono.just(ResponseEntity.status(errorCode.getHttpStatusCode()).body(body));
    }

    private String resolveOriginalPath(ServerWebExchange exchange) {
        Set<URI> originalUris = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR);
        if (originalUris != null && !originalUris.isEmpty()) {
            return originalUris.iterator().next().getPath();
        }

        return exchange.getRequest().getPath().value();
    }
}
