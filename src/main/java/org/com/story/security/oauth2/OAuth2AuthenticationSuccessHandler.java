package org.com.story.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.com.story.entity.User;
import org.com.story.repository.UserRepository;
import org.com.story.security.JwtUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler
        implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public OAuth2AuthenticationSuccessHandler(
            JwtUtil jwtUtil,
            UserRepository userRepository
    ) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = (String) oAuth2User.getAttribute("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow();

        String accessToken = jwtUtil.generateAccessToken(user.getEmail());

        // Redirect về trang hiển thị token (dùng để test khi chưa có frontend)
        // Khi có frontend thật, đổi lại thành: "http://localhost:3000/oauth2/success?token=" + accessToken
        String frontendUrl = System.getenv("FRONTEND_URL");
        String redirectUrl = (frontendUrl != null && !frontendUrl.isBlank())
                ? frontendUrl + "/oauth2/success?token=" + accessToken
                : "/api/auth/oauth2/success?token=" + accessToken;

        response.sendRedirect(redirectUrl);
    }
}
