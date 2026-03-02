package org.com.story.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.com.story.dto.response.OtpResponse;
import org.com.story.repository.UserRepository;
import org.com.story.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;

    public OAuth2AuthenticationSuccessHandler(@Lazy UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        // Gửi OTP 6 số về email thay vì trả JWT trực tiếp
        OtpResponse otpResponse = userService.sendOAuth2Otp(email);

        String frontendUrl = System.getenv("FRONTEND_URL");

        if (frontendUrl != null && !frontendUrl.isBlank()) {
            response.sendRedirect(frontendUrl + "/oauth2/verify-otp?email=" + email);
        } else {
            response.sendRedirect("/api/auth/oauth2/otp-page?email=" + email
                    + "&devOtp=" + otpResponse.getDevOtp());
        }
    }
}
