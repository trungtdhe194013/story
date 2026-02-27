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
import org.springframework.http.MediaType;
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

    /**
     * ✅ Trang nhận token sau khi đăng nhập Google thành công.
     * Dùng để test khi chưa có frontend.
     * Truy cập: http://localhost:8080/oauth2/success?token=<JWT>
     */
    @GetMapping(value = "/oauth2/success", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "OAuth2 success page (test only)", description = "Hiển thị JWT token sau khi đăng nhập Google thành công. Dùng cho mục đích test.")
    public String oauth2SuccessPage(@RequestParam(required = false) String token) {
        if (token == null || token.isBlank()) {
            return "<html><body><h2 style='color:red'>Không có token. Hãy đăng nhập qua Google trước.</h2></body></html>";
        }
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                  <meta charset="UTF-8">
                  <title>Google Login Success</title>
                  <style>
                    body { font-family: Arial, sans-serif; max-width: 800px; margin: 40px auto; padding: 20px; background: #f5f5f5; }
                    .card { background: white; border-radius: 8px; padding: 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                    h2 { color: #2e7d32; }
                    .token-box { background: #1e1e1e; color: #00ff88; padding: 16px; border-radius: 6px; word-break: break-all; font-family: monospace; font-size: 13px; }
                    .btn { margin-top: 16px; padding: 10px 20px; background: #1976d2; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 15px; }
                    .btn:hover { background: #1565c0; }
                    .guide { margin-top: 20px; background: #e3f2fd; padding: 16px; border-radius: 6px; }
                    .guide ol { margin: 8px 0; padding-left: 20px; }
                  </style>
                </head>
                <body>
                  <div class="card">
                    <h2>✅ Đăng nhập Google thành công!</h2>
                    <p>Copy token bên dưới để dùng trong Swagger:</p>
                    <div class="token-box" id="tokenBox">""" + token + """
                    </div>
                    <button class="btn" onclick="copyToken()">📋 Copy Token</button>
                    <div class="guide">
                      <strong>📌 Cách dùng trong Swagger:</strong>
                      <ol>
                        <li>Mở <a href="/swagger-ui/index.html" target="_blank">Swagger UI</a></li>
                        <li>Nhấn nút <b>Authorize 🔒</b> (góc phải trên)</li>
                        <li>Nhập vào ô <b>bearerAuth</b>: <code>Bearer &lt;token&gt;</code></li>
                        <li>Nhấn <b>Authorize</b> rồi <b>Close</b></li>
                        <li>Giờ bạn có thể gọi tất cả API cần xác thực!</li>
                      </ol>
                    </div>
                  </div>
                  <script>
                    function copyToken() {
                      const text = document.getElementById('tokenBox').innerText.trim();
                      navigator.clipboard.writeText('Bearer ' + text).then(() => {
                        alert('✅ Đã copy "Bearer token" vào clipboard!');
                      });
                    }
                  </script>
                </body>
                </html>
                """;
    }
}
