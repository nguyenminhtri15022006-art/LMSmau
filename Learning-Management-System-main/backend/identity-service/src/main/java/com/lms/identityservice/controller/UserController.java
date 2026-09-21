package com.lms.identityservice.controller;

import com.lms.identityservice.dto.ApiResponse;
import com.lms.identityservice.dto.request.UserRoleUpdateRequest;
import com.lms.identityservice.dto.request.UserUpdatePasswordRequest;
import com.lms.identityservice.dto.request.UserUpdateRequest;
import com.lms.identityservice.dto.response.UserFullResponse;
import com.lms.identityservice.dto.response.UserPublicProfileResponse;
import com.lms.identityservice.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    UserService userService;

    @GetMapping("/myinfo")
    ApiResponse<UserFullResponse> getMyinfo() {
        return ApiResponse.<UserFullResponse>builder()
                .data(userService.getMyinfo())
                .build();
    }

    @PutMapping("/myinfo")
    ApiResponse<UserFullResponse> updateInfo(@RequestBody @Valid UserUpdateRequest request) {
        return ApiResponse.<UserFullResponse>builder()
                .data(userService.updateInfo(request))
                .build();
    }

    @PatchMapping("/myinfo/password")
    ApiResponse<UserFullResponse> updatePasswordInfo(@RequestBody @Valid UserUpdatePasswordRequest request) {
        return ApiResponse.<UserFullResponse>builder()
                .data(userService.updatePassword(request))
                .build();
    }

    @PatchMapping("/myinfo/role")
    ApiResponse<UserFullResponse> updateRole(@RequestBody @Valid UserRoleUpdateRequest request) {
        return ApiResponse.<UserFullResponse>builder()
                .data(userService.updateRole(request))
                .build();
    }

    @GetMapping("/{userId}/profile")
    ApiResponse<UserPublicProfileResponse> getPublicProfile(@PathVariable String userId) {
        return ApiResponse.<UserPublicProfileResponse>builder()
                .data(userService.getPublicProfile(userId))
                .build();
    }

}
