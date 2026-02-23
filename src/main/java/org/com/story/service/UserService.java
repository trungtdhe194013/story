package org.com.story.service;

import org.com.story.entity.User;
import org.com.story.dto.response.UserResponse;
import org.com.story.dto.request.SignUpRequest;

public interface UserService {

    UserResponse getUserProfile();

    User getCurrentUser();

    UserResponse registerUser(SignUpRequest request);

    // Email verification methods
    void sendVerificationEmail(User user);

    boolean verifyEmail(String token);

    void resendVerificationEmail(String email);
}



