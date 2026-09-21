package com.lms.identityservice.mapper;

import com.lms.identityservice.dto.request.UserCreationRequest;
import com.lms.identityservice.dto.request.UserUpdateRequest;
import com.lms.identityservice.dto.response.UserCreationResponse;
import com.lms.identityservice.dto.response.UserFullResponse;
import com.lms.identityservice.dto.response.UserSummaryResponse;
import com.lms.identityservice.entity.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(UserCreationRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUser(@MappingTarget User user, UserUpdateRequest request);

    UserFullResponse toUserFullResponse(User user);

    UserSummaryResponse toUserSummaryResponse(User user);

    UserCreationResponse toUserCreationResponse(User user);

    List<UserFullResponse> toListUserFullResponse(List<User> userList);

    List<UserSummaryResponse> toListUserSummaryResponse(List<User> userList);
}
