package com.lms.assignmentservice.repository.httpClient;

import com.lms.assignmentservice.dto.response.InternalUserResponse;
import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Set;

@FeignClient(
        name = "identity-service",
        url = "${services.identity-service.url}",
        fallbackFactory = IdentityClient.FallbackFactoryImpl.class
)
public interface IdentityClient {

    @GetMapping("/internal/user/{userId}")
    @CircuitBreaker(name = "identityService")
    InternalUserResponse getUser(@PathVariable String userId);

    @PostMapping("/internal/users/batch")
    @CircuitBreaker(name = "identityService")
    List<InternalUserResponse> getUsersByIds(@RequestBody Set<String> userIds);

    @Component
    class FallbackFactoryImpl implements FallbackFactory<IdentityClient> {
        @Override
        public IdentityClient create(Throwable cause) {
            return new IdentityClient() {
                @Override
                public InternalUserResponse getUser(String userId) {
                    if (cause instanceof FeignException feignException && feignException.status() == 404) {
                        return null;
                    }
                    throw new AssignmentException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
                }

                @Override
                public List<InternalUserResponse> getUsersByIds(Set<String> userIds) {
                    throw new AssignmentException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
                }
            };
        }
    }
}
