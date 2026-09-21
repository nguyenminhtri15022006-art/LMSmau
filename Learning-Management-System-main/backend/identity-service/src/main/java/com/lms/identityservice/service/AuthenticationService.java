package com.lms.identityservice.service;

import com.lms.identityservice.dto.request.*;
import com.lms.identityservice.dto.response.AuthenticationResponse;
import com.lms.identityservice.dto.response.IntrospectResponse;
import com.lms.identityservice.dto.response.UserCreationResponse;

public interface AuthenticationService {
    UserCreationResponse createUser(UserCreationRequest request);

    AuthenticationResponse authenticate(AuthenticationRequest request);

    AuthenticationResponse socialLogin(SocialLoginRequest request, String provider);

    IntrospectResponse introspect(IntrospectRequest request);

    AuthenticationResponse refreshToken(RefreshTokenRequest request);

    AuthenticationResponse issueTokensForUser(String userId);

    void logout(LogoutRequest request);
}
