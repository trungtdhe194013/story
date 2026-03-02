package org.com.story.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.ForgotPasswordRequest;
import org.com.story.dto.request.LoginRequest;
import org.com.story.dto.request.ResetPasswordRequest;
import org.com.story.dto.request.SignUpRequest;
import org.com.story.dto.request.VerifyOtpRequest;
import org.com.story.dto.response.LoginResponse;
import org.com.story.dto.response.OtpResponse;
import org.com.story.dto.response.UserResponse;
import org.com.story.entity.RefreshToken;
import org.com.story.security.JwtUtil;
import org.com.story.service.RefreshTokenService;
import org.com.story.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
    @Operation(
        summary = "Đăng ký tài khoản - Step 1: Gửi OTP",
        description = "Nhập thông tin đăng ký, hệ thống sẽ gửi mã OTP 6 số về email. Tài khoản chưa được kích hoạt cho đến khi xác thực OTP."
    )
    public OtpResponse signUp(@Valid @RequestBody SignUpRequest request) {
        return userService.sendRegisterOtp(request);
    }

    @PostMapping("/sign-up/verify-otp")
    @Operation(
        summary = "Đăng ký tài khoản - Step 2: Xác nhận OTP",
        description = "Nhập email và mã OTP 6 số nhận qua email để kích hoạt tài khoản. OTP có hiệu lực 10 phút."
    )
    public UserResponse verifyRegisterOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return userService.verifyRegisterOtp(request.getEmail(), request.getOtp());
    }

    @PostMapping("/sign-up/resend-otp")
    @Operation(
        summary = "Đăng ký tài khoản - Gửi lại OTP",
        description = "Gửi lại mã OTP 6 số về email trong trường hợp OTP cũ đã hết hạn hoặc không nhận được."
    )
    public OtpResponse resendRegisterOtp(@RequestParam String email) {
        return userService.resendRegisterOtp(email);
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Login with email and password to get access token")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
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

    // ===================== EMAIL VERIFICATION =====================
    @GetMapping("/verify")
    @Operation(summary = "Verify email", description = "Verify user email with token from email")
    public String verifyEmail(@RequestParam String token) {
        boolean success = userService.verifyEmail(token);
        if (success) return "Email verified successfully! You can now login.";
        return "Email verification failed!";
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend verification email", description = "Resend verification email to user")
    public String resendVerification(@RequestParam String email) {
        userService.resendVerificationEmail(email);
        return "Verification email sent! Please check your inbox.";
    }

    // ===================== FORGOT / RESET PASSWORD =====================

    @PostMapping("/forgot-password")
    @Operation(
        summary = "Quên mật khẩu - Step 1: Gửi OTP",
        description = """
            Nhập email tài khoản. Hệ thống sẽ gửi mã OTP 6 số về email đó.
            OTP có hiệu lực trong **10 phút**.
            """
    )
    public OtpResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return userService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    @Operation(
        summary = "Quên mật khẩu - Step 2: Đặt lại mật khẩu",
        description = """
            Nhập email, mã OTP 6 số nhận qua email, mật khẩu mới và xác nhận mật khẩu mới.
            - `email`: email tài khoản
            - `otp`: mã OTP 6 số nhận qua email
            - `newPassword`: mật khẩu mới (ít nhất 6 ký tự, có chữ hoa, chữ thường, số)
            - `confirmNewPassword`: xác nhận mật khẩu mới (phải trùng với newPassword)
            """
    )
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    // ===================== OAUTH2 OTP =====================
    @Hidden
    @PostMapping("/oauth2/send-otp")
    public OtpResponse resendOAuth2Otp(@RequestParam String email) {
        return userService.sendOAuth2Otp(email);
    }

    @PostMapping("/oauth2/verify-otp")
    @Operation(summary = "Verify OAuth2 OTP", description = "Nhập mã OTP 6 số để hoàn tất đăng nhập Google và nhận JWT token")
    public LoginResponse verifyOAuth2Otp(@Valid @RequestBody VerifyOtpRequest request) {
        return userService.verifyOAuth2Otp(request.getEmail(), request.getOtp());
    }

    @Hidden
    @GetMapping(value = "/oauth2/otp-page", produces = MediaType.TEXT_HTML_VALUE)
    public String oauth2OtpPage(
            @RequestParam String email,
            @RequestParam(required = false) String devOtp
    ) {
        String devNote = (devOtp != null && !devOtp.isBlank())
                ? "<div class='dev-box'>🛠 <b>[DEV MODE]</b> OTP của bạn: <b style='font-size:22px;letter-spacing:4px'>" + devOtp + "</b><br><small>Xóa devOtp khỏi response khi deploy production</small></div>"
                : "";

        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                  <meta charset="UTF-8">
                  <title>Xác thực OTP - Google Login</title>
                  <style>
                    * { box-sizing: border-box; }
                    body { font-family: Arial, sans-serif; background: #f0f4f8; display: flex; justify-content: center; align-items: center; min-height: 100vh; margin: 0; }
                    .card { background: white; border-radius: 12px; padding: 36px 32px; box-shadow: 0 4px 20px rgba(0,0,0,0.12); width: 100%%; max-width: 440px; }
                    h2 { color: #1a73e8; margin-bottom: 6px; }
                    p { color: #555; margin-bottom: 20px; }
                    .email-badge { background: #e8f0fe; color: #1a73e8; padding: 6px 12px; border-radius: 20px; font-size: 14px; display: inline-block; margin-bottom: 20px; }
                    .otp-input { display: flex; gap: 10px; justify-content: center; margin-bottom: 24px; }
                    .otp-input input { width: 48px; height: 56px; text-align: center; font-size: 24px; font-weight: bold; border: 2px solid #ddd; border-radius: 8px; outline: none; transition: border-color .2s; }
                    .otp-input input:focus { border-color: #1a73e8; }
                    .btn { width: 100%%; padding: 13px; background: #1a73e8; color: white; border: none; border-radius: 8px; font-size: 16px; cursor: pointer; font-weight: bold; transition: background .2s; }
                    .btn:hover { background: #1557b0; }
                    .btn-resend { width: 100%%; padding: 10px; background: transparent; color: #1a73e8; border: 1px solid #1a73e8; border-radius: 8px; font-size: 14px; cursor: pointer; margin-top: 10px; }
                    .btn-resend:hover { background: #e8f0fe; }
                    .result { margin-top: 20px; padding: 14px; border-radius: 8px; display: none; word-break: break-all; }
                    .result.success { background: #e6f4ea; color: #1e7e34; }
                    .result.error { background: #fce8e6; color: #c62828; }
                    .token-box { background: #1e1e1e; color: #00ff88; padding: 12px; border-radius: 6px; font-family: monospace; font-size: 12px; margin-top: 10px; word-break: break-all; }
                    .copy-btn { margin-top: 8px; padding: 8px 16px; background: #43a047; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 13px; }
                    .dev-box { background: #fff8e1; border-left: 4px solid #ffc107; padding: 12px 16px; border-radius: 6px; margin-bottom: 20px; font-size: 14px; }
                  </style>
                </head>
                <body>
                  <div class="card">
                    <h2>🔐 Xác thực OTP</h2>
                    <p>Mã OTP 6 số đã được gửi đến email của bạn</p>
                    <div class="email-badge">📧 %s</div>
                    %s
                    <div class="otp-input">
                      <input type="text" maxlength="1" id="d1" oninput="focusNext(this,'d2')" onkeydown="focusPrev(event,this,'')">
                      <input type="text" maxlength="1" id="d2" oninput="focusNext(this,'d3')" onkeydown="focusPrev(event,this,'d1')">
                      <input type="text" maxlength="1" id="d3" oninput="focusNext(this,'d4')" onkeydown="focusPrev(event,this,'d2')">
                      <input type="text" maxlength="1" id="d4" oninput="focusNext(this,'d5')" onkeydown="focusPrev(event,this,'d3')">
                      <input type="text" maxlength="1" id="d5" oninput="focusNext(this,'d6')" onkeydown="focusPrev(event,this,'d4')">
                      <input type="text" maxlength="1" id="d6" oninput="focusNext(this,'')"   onkeydown="focusPrev(event,this,'d5')">
                    </div>
                    <button class="btn" onclick="verifyOtp()">✅ Xác nhận OTP</button>
                    <button class="btn-resend" onclick="resendOtp()">🔄 Gửi lại OTP</button>
                    <div id="result" class="result"></div>
                  </div>
                  <script>
                    const EMAIL = '%s';
                    function getOtp() {
                      return ['d1','d2','d3','d4','d5','d6'].map(id => document.getElementById(id).value).join('');
                    }
                    function focusNext(el, nextId) {
                      if (el.value.length === 1 && nextId) document.getElementById(nextId).focus();
                    }
                    function focusPrev(e, el, prevId) {
                      if (e.key === 'Backspace' && el.value === '' && prevId) document.getElementById(prevId).focus();
                    }
                    async function verifyOtp() {
                      const otp = getOtp();
                      if (otp.length !== 6) { showResult('error','Vui lòng nhập đủ 6 chữ số OTP'); return; }
                      try {
                        const res = await fetch('/api/auth/oauth2/verify-otp', {
                          method: 'POST', headers: {'Content-Type':'application/json'},
                          body: JSON.stringify({ email: EMAIL, otp: otp })
                        });
                        const data = await res.json();
                        if (res.ok) {
                          showResult('success', '✅ Đăng nhập thành công!<br><div class="token-box" id="tokenBox">' + data.accessToken + '</div><button class="copy-btn" onclick="copyToken()">📋 Copy Bearer Token</button>');
                        } else {
                          showResult('error', '❌ ' + (data.message || 'OTP không hợp lệ'));
                        }
                      } catch(e) { showResult('error', '❌ Lỗi kết nối server'); }
                    }
                    async function resendOtp() {
                      try {
                        const res = await fetch('/api/auth/oauth2/send-otp?email=' + encodeURIComponent(EMAIL), { method: 'POST' });
                        const data = await res.json();
                        if (res.ok) showResult('success', '📨 ' + data.message);
                        else showResult('error', '❌ ' + (data.message || 'Gửi OTP thất bại'));
                      } catch(e) { showResult('error', '❌ Lỗi kết nối server'); }
                    }
                    function showResult(type, msg) {
                      const el = document.getElementById('result');
                      el.className = 'result ' + type;
                      el.innerHTML = msg;
                      el.style.display = 'block';
                    }
                    function copyToken() {
                      const text = document.getElementById('tokenBox').innerText.trim();
                      navigator.clipboard.writeText('Bearer ' + text).then(() => alert('✅ Đã copy Bearer token!'));
                    }
                  </script>
                </body>
                </html>
                """.formatted(email, devNote, email);
    }
}
