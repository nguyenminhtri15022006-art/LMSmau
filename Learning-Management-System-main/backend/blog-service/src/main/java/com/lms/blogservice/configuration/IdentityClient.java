package com.lms.blogservice.configuration;

import com.lms.blogservice.exception.BlogException;
import com.lms.blogservice.exception.ErrorCode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "identity-service",
        url = "${services.identity-service.url}",
        fallback = IdentityClient.Fallback.class
)
public interface IdentityClient {

    @GetMapping("/internal/users/{userId}/exists")
    boolean existsById(@PathVariable String userId);

    @Component
    class Fallback implements IdentityClient {
        @Override
        public boolean existsById(String userId) {
            throw new BlogException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }
    }
}