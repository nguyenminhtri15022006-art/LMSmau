package com.lms.apigateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@Order(-2)
@RequiredArgsConstructor
public class GlobalErrorExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        log.info(ex.getLocalizedMessage());
        log.info(ex.getMessage());

        if (exchange.getResponse().isCommitted()) {
            // Gói tin đã gửi đi rồi (như cái 200 OK ban nãy) thì kệ nó, không ghi đè lỗi nữa
            return Mono.error(ex);
        }

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = ex.getMessage();
        Integer code = ErrorCode.INTERNAL_SERVER_ERROR.getCode();

        // CHỈ xử lý các lỗi rớt mạng hoặc sai định tuyến WebFlux
        if (ex instanceof ResponseStatusException responseStatusEx) {
            status = HttpStatus.resolve(responseStatusEx.getStatusCode().value());
            if (status == HttpStatus.NOT_FOUND) {
                code = ErrorCode.RESOURCE_NOT_FOUND.getCode();
                message = ErrorCode.RESOURCE_NOT_FOUND.getMessage();
            }
        } else if (ex instanceof ConnectException || (ex.getMessage() != null && ex.getMessage().contains("Connection refused"))) {
            status = ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE.getHttpStatusCode();
            code = ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE.getCode();
            message = ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE.getMessage();
        }

        String path = exchange.getRequest().getURI().getPath();
        log.error("Gateway Infrastructure Error: [Path: {}] - Status: {} - Code: {} - Message: {}", path, status, code, message);

        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("timestamp", ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT));
        errorResponse.put("status", status != null ? status.value() : 500);
        errorResponse.put("error", status != null ? status.getReasonPhrase() : "Internal Server Error");
        errorResponse.put("code", code);
        errorResponse.put("message", message);
        errorResponse.put("path", path);

        return writeResponse(exchange, status, errorResponse);
    }

    private Mono<Void> writeResponse(ServerWebExchange exchange, HttpStatus status, Map<String, Object> errorResponse) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            return exchange.getResponse().writeWith(
                    Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
            );
        } catch (JsonProcessingException e) {
            String fallback = "{\"code\":\"INTERNAL_SERVER_ERROR\",\"message\":\"Unknown parsing error occurred\"}";
            return exchange.getResponse().writeWith(
                    Mono.just(exchange.getResponse().bufferFactory().wrap(fallback.getBytes()))
            );
        }
    }
}