package com.lms.identityservice.controller;

import com.lms.identityservice.configuration.SecurityConfig;
import com.lms.identityservice.dto.request.UserCreationRequest;
import com.lms.identityservice.dto.response.AuthenticationResponse;
import com.lms.identityservice.dto.response.UserCreationResponse;
import com.lms.identityservice.dto.response.UserFullResponse;
import com.lms.identityservice.dto.response.UserPublicProfileResponse;
import com.lms.identityservice.service.AuthenticationService;
import com.lms.identityservice.service.EmailVerificationService;
import com.lms.identityservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AuthenticationController.class,
        EmailVerificationController.class,
        UserController.class
})
@Import(SecurityConfig.class)
class IdentityApiIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AuthenticationService authenticationService;

    @MockBean
    EmailVerificationService emailVerificationService;

    @MockBean
    UserService userService;

    @Test
    void registerValidationErrorsReturnFieldMapWithoutCallingService() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "ab",
                                  "password": "secret123",
                                  "fullname": "Student One",
                                  "email": "student@example.com"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(9998))
                .andExpect(jsonPath("$.data.username").exists());

        verify(authenticationService, never()).createUser(any());
    }

    @Test
    void loginDelegatesToServiceAndSerializesTokenResponse() throws Exception {
        when(authenticationService.authenticate(any())).thenReturn(AuthenticationResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .user(UserFullResponse.builder()
                        .userId("user-1")
                        .username("student")
                        .build())
                .build());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "student",
                                  "password": "secret123",
                                  "ipAddress": "127.0.0.1",
                                  "deviceInfo": "JUnit"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.user.userId").value("user-1"));
    }

    @Test
    void privateEndpointRejectsRequestWithoutGatewayUserHeader() throws Exception {
        mockMvc.perform(get("/users/myinfo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1001));

        verify(userService, never()).getMyinfo();
    }

    @Test
    void privateEndpointAcceptsGatewayHeadersAndCallsService() throws Exception {
        when(userService.getMyinfo()).thenReturn(UserFullResponse.builder()
                .userId("user-1")
                .email("student@example.com")
                .role("STUDENT")
                .build());

        mockMvc.perform(get("/users/myinfo")
                        .header("X-User-Id", "user-1")
                        .header("X-User-Role", "ROLE_STUDENT")
                        .header("X-User-Email", "student@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value("user-1"))
                .andExpect(jsonPath("$.data.email").value("student@example.com"));

        verify(userService).getMyinfo();
    }

    @Test
    void publicProfileEndpointDoesNotRequireGatewayHeader() throws Exception {
        when(userService.getPublicProfile("user-1")).thenReturn(UserPublicProfileResponse.builder()
                .userId("user-1")
                .fullname("Student One")
                .role("STUDENT")
                .build());

        mockMvc.perform(get("/users/user-1/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value("user-1"))
                .andExpect(jsonPath("$.data.fullname").value("Student One"));
    }

    @Test
    void registerDelegatesValidRequestToAuthenticationService() throws Exception {
        when(authenticationService.createUser(any())).thenReturn(UserCreationResponse.builder()
                .userId("user-1")
                .username("student01")
                .email("student@example.com")
                .role("STUDENT")
                .build());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "student01",
                                  "password": "secret123",
                                  "fullname": "Student One",
                                  "email": "student@example.com",
                                  "role": "STUDENT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value("user-1"))
                .andExpect(jsonPath("$.data.username").value("student01"));

        ArgumentCaptor<UserCreationRequest> requestCaptor = ArgumentCaptor.forClass(UserCreationRequest.class);
        verify(authenticationService).createUser(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getUsername()).isEqualTo("student01");
    }
}
