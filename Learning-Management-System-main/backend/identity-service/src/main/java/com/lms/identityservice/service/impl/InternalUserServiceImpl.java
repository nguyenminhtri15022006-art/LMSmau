package com.lms.identityservice.service.impl;

import com.lms.identityservice.dto.response.UserFullResponse;
import com.lms.identityservice.entity.User;
import com.lms.identityservice.exception.ErrorCode;
import com.lms.identityservice.exception.IdentityException;
import com.lms.identityservice.mapper.UserMapper;
import com.lms.identityservice.repository.UserRepository;
import com.lms.identityservice.service.InternalUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InternalUserServiceImpl implements InternalUserService {

    UserRepository userRepository;
    UserMapper userMapper;

    @Override
    public UserFullResponse getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IdentityException(ErrorCode.USER_NOT_EXISTED));
        return userMapper.toUserFullResponse(user);
    }

    @Override
    public List<UserFullResponse> getUsersByIds(Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        Set<String> normalizedIds = userIds.stream()
                .filter(Objects::nonNull)
                .filter(userId -> !userId.isBlank())
                .collect(Collectors.toSet());
        if (normalizedIds.isEmpty()) {
            return List.of();
        }

        return userMapper.toListUserFullResponse(userRepository.findAllById(normalizedIds));
    }

    @Override
    public boolean existsById(String userId) {
        return userRepository.existsById(userId);
    }

    @Override
    public UserFullResponse findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toUserFullResponse)
                .orElse(null);
    }

}
