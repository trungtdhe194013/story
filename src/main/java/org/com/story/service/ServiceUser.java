package org.com.story.service;

import org.com.story.entity.User;
import org.com.story.dto.response.UserResponse;
import org.com.story.dto.request.SignUpRequest;
import org.com.story.dto.request.UpdateProfileRequest;
import org.com.story.dto.request.ChangePasswordRequest;
import org.com.story.dto.request.ForgotPasswordRequest;
import org.com.story.dto.request.ResetPasswordRequest;
import org.com.story.dto.response.LoginResponse;
import org.com.story.dto.response.OtpResponse;

public interface UserService {

    UserResponse getUserProfile();

    User getCurrentUser();

    UserResponse registerUser(SignUpRequest request);

    UserResponse updateProfile(UpdateProfileRequest request);

    // ===== Đăng ký bằng OTP =====
    // Step 1: Lưu user tạm, gửi OTP 6 số về email
    OtpResponse sendRegisterOtp(SignUpRequest request);

    // Step 2: Xác nhận OTP → kích hoạt tài khoản
    UserResponse verifyRegisterOtp(String email, String otp);

    // Gửi lại OTP đăng ký
    OtpResponse resendRegisterOtp(String email);

    // Email verification methods (legacy link - vẫn giữ)
    void sendVerificationEmail(User user);

    boolean verifyEmail(String token);

    void resendVerificationEmail(String email);

    // OAuth2: gửi OTP sau khi login Google
    OtpResponse sendOAuth2Otp(String email);

    // OAuth2: xác nhận OTP → trả JWT
    LoginResponse verifyOAuth2Otp(String email, String otp);

    // Đổi mật khẩu khi đang đăng nhập: nhập mật khẩu cũ + mới + xác nhận
    void changePassword(ChangePasswordRequest request);

    // Quên mật khẩu – Step 1: gửi OTP 6 số về email
    OtpResponse forgotPassword(ForgotPasswordRequest request);

    // Quên mật khẩu – Step 2: xác nhận OTP + đặt mật khẩu mới
    void resetPassword(ResetPasswordRequest request);
}
