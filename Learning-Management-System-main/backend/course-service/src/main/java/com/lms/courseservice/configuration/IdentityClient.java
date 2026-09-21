package com.lms.courseservice.configuration;

import com.lms.courseservice.dto.response.UserInfoDto;
import com.lms.courseservice.exception.CourseException;
import com.lms.courseservice.exception.ErrorCode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "identity-service",
        url = "${services.identity-service.url}",
        fallback = IdentityClient.Fallback.class
)
public interface IdentityClient {

    @GetMapping("/internal/users/{userId}/exists")
    boolean existsById(@PathVariable String userId);

    @GetMapping("/internal/user/{userId}")
    UserInfoDto getUserById(@PathVariable String userId);

    @GetMapping("/internal/users/search/by-email")
    UserInfoDto findByEmail(@RequestParam String email);

    @Component
    class Fallback implements IdentityClient {
        @Override
        public boolean existsById(String userId) {
            throw new CourseException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }

        @Override
        public UserInfoDto getUserById(String userId) {
            return null;
        }

        @Override
        public UserInfoDto findByEmail(String email) {
            return null;
        }
    }
}
