package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.LoginRequest;
import org.com.story.dto.request.SignUpRequest;
import org.com.story.dto.response.LoginResponse;
import org.com.story.dto.response.UserResponse;
import org.com.story.entity.RefreshToken;
import org.com.story.security.JwtUtil;
import org.com.story.service.RefreshTokenService;
import org.com.story.service.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth Controller", description = "Authentication and authorization endpoints")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;

    @PostMapping("/sign-up")
    @Operation(summary = "Register new user", description = "Create a new user account")
    public UserResponse signUp(@Valid @RequestBody SignUpRequest request) {
        return userService.registerUser(request);
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Login with email and password to get access token")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                req.getEmail(),
                                req.getPassword()
                        )
                );
        String accessToken =
                jwtUtil.generateAccessToken(req.getEmail());

        RefreshToken refreshToken =
                refreshTokenService.create(req.getEmail());

        return new LoginResponse(
                accessToken,
                refreshToken.getToken()
        );
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Get new access token using refresh token")
    public LoginResponse refresh(@RequestParam String refreshToken) {
        RefreshToken rt =
                refreshTokenService.verify(refreshToken);

        String newAccessToken =
                jwtUtil.generateAccessToken(rt.getUsername());

        return new LoginResponse(
                newAccessToken,
                refreshToken
        );
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Logout and invalidate refresh token")
    public void logout(@RequestParam String refreshToken) {

        refreshTokenService.logout(refreshToken);
    }

    @GetMapping("/verify")
    @Operation(summary = "Verify email", description = "Verify user email with token from email")
    public String verifyEmail(@RequestParam String token) {
        boolean success = userService.verifyEmail(token);
        if (success) {
            return "Email verified successfully! You can now login.";
        }
        return "Email verification failed!";
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend verification email", description = "Resend verification email to user")
    public String resendVerification(@RequestParam String email) {
        userService.resendVerificationEmail(email);
        return "Verification email sent! Please check your inbox.";
    }
}
