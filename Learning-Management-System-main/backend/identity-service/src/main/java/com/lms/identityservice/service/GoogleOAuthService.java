package com.lms.identityservice.service;

import com.lms.identityservice.dto.response.SocialUserInfo;

public interface GoogleOAuthService {
    SocialUserInfo getUserInfo(String code, String redirectUri);
}
