package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.common.AuthProvider;
import org.com.story.dto.request.ChangePasswordRequest;
import org.com.story.dto.request.ForgotPasswordRequest;
import org.com.story.dto.request.ResetPasswordRequest;
import org.com.story.dto.request.SignUpRequest;
import org.com.story.dto.request.UpdateProfileRequest;
import org.com.story.dto.response.LoginResponse;
import org.com.story.dto.response.OtpResponse;
import org.com.story.dto.response.UserResponse;
import org.com.story.entity.Role;
import org.com.story.entity.User;
import org.com.story.entity.VerificationToken;
import org.com.story.entity.Wallet;
import org.com.story.exception.BadRequestException;
import org.com.story.repository.RoleRepository;
import org.com.story.repository.UserRepository;
import org.com.story.repository.VerificationTokenRepository;
import org.com.story.repository.WalletRepository;
import org.com.story.repository.ChapterPurchaseRepository;
import org.com.story.security.JwtUtil;
import org.com.story.service.MailService;
import org.com.story.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenRepository verificationTokenRepository;
    private final MailService mailService;
    private final WalletRepository walletRepository;
    private final ChapterPurchaseRepository chapterPurchaseRepository;
    private final JwtUtil jwtUtil;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // ===================== REGISTER =====================
    @Override
    public UserResponse registerUser(SignUpRequest request) {
        // Check email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        // Get READER role
        Role readerRole = roleRepository.findByName("READER")
                .orElseThrow(() -> new IllegalStateException("Default role READER not found in database. Please ensure database is properly initialized with seed data."));

        // Create user with DISABLED status (enabled = false)
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setProvider(AuthProvider.LOCAL);
        user.setEnabled(false); // ⚠️ DISABLED until email verified
        user.setRoles(Set.of(readerRole));
        user.setFollowedStories(new HashSet<>());

        // Optional profile fields
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getLocation() != null) {
            user.setLocation(request.getLocation());
        }

        User savedUser = userRepository.save(user);

        // Create wallet for user
        Wallet wallet = new Wallet();
        wallet.setUser(savedUser);
        wallet.setBalance(0L);
        wallet.setLockedBalance(0L);
        walletRepository.save(wallet);

        // Send verification email
        sendVerificationEmail(savedUser);

        return mapToResponse(savedUser);
    }

    // ===================== REGISTER VIA OTP =====================

    /**
     * Step 1: Lưu user tạm (enabled=false), gửi OTP 6 số về email.
     */
    @Override
    public OtpResponse sendRegisterOtp(SignUpRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            // Nếu user tồn tại nhưng chưa verify, cho phép gửi lại OTP
            User existing = userRepository.findByEmail(request.getEmail()).get();
            if (existing.getEnabled()) {
                throw new BadRequestException("Email already exists");
            }
            // Gửi lại OTP cho user chưa verify
            String otp = generateOtp();
            saveOtp(existing, otp, "REGISTER_OTP", 10);
            sendRegisterOtpEmail(existing, otp);
            return OtpResponse.builder()
                    .message("Mã OTP đã được gửi lại đến email " + request.getEmail() + ". Vui lòng kiểm tra hòm thư.")
                    .email(request.getEmail())
                    .devOtp(otp) // TODO: xóa khi deploy production
                    .build();
        }

        // Get READER role
        Role readerRole = roleRepository.findByName("READER")
                .orElseThrow(() -> new IllegalStateException("Default role READER not found in database."));

        // Tạo user với DISABLED status
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setProvider(AuthProvider.LOCAL);
        user.setEnabled(false); // ⚠️ DISABLED until OTP verified
        user.setRoles(Set.of(readerRole));
        user.setFollowedStories(new HashSet<>());

        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getDateOfBirth() != null) user.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getLocation() != null) user.setLocation(request.getLocation());

        User savedUser = userRepository.save(user);

        // Tạo wallet
        Wallet wallet = new Wallet();
        wallet.setUser(savedUser);
        wallet.setBalance(0L);
        wallet.setLockedBalance(0L);
        walletRepository.save(wallet);

        // Gửi OTP
        String otp = generateOtp();
        saveOtp(savedUser, otp, "REGISTER_OTP", 10);
        sendRegisterOtpEmail(savedUser, otp);

        return OtpResponse.builder()
                .message("Mã OTP đã được gửi đến email " + request.getEmail() + ". Vui lòng nhập mã để hoàn tất đăng ký.")
                .email(request.getEmail())
                .devOtp(otp) // TODO: xóa khi deploy production
                .build();
    }

    /**
     * Step 2: Xác nhận OTP → kích hoạt tài khoản, trả về UserResponse.
     */
    @Override
    public UserResponse verifyRegisterOtp(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy tài khoản với email này"));

        if (user.getEnabled()) {
            throw new BadRequestException("Tài khoản đã được kích hoạt trước đó. Vui lòng đăng nhập.");
        }

        verifyOtp(user, otp, "REGISTER_OTP");

        user.setEnabled(true);
        userRepository.save(user);

        return mapToResponse(user);
    }

    /**
     * Gửi lại OTP đăng ký (khi user chưa verify).
     */
    @Override
    public OtpResponse resendRegisterOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy tài khoản với email này"));

        if (user.getEnabled()) {
            throw new BadRequestException("Tài khoản đã được kích hoạt. Không cần gửi lại OTP.");
        }

        String otp = generateOtp();
        saveOtp(user, otp, "REGISTER_OTP", 10);
        sendRegisterOtpEmail(user, otp);

        return OtpResponse.builder()
                .message("Mã OTP đã được gửi lại đến email " + email + ". Vui lòng kiểm tra hòm thư.")
                .email(email)
                .devOtp(otp) // TODO: xóa khi deploy production
                .build();
    }

    private void sendRegisterOtpEmail(User user, String otp) {
        String subject = "Mã OTP xác thực đăng ký - Story Platform";
        String content = String.format(
                "Xin chào %s,\n\n" +
                "Cảm ơn bạn đã đăng ký tài khoản tại Story Platform!\n\n" +
                "Mã OTP xác thực của bạn là:\n\n" +
                "  ► %s ◄\n\n" +
                "Mã này có hiệu lực trong 10 phút.\n" +
                "Vui lòng nhập mã này để hoàn tất quá trình đăng ký.\n" +
                "Nếu không phải bạn, hãy bỏ qua email này.\n\n" +
                "Trân trọng,\nStory Platform Team",
                user.getFullName() != null ? user.getFullName() : user.getEmail(),
                otp);
        mailService.sendTextMail(user.getEmail(), subject, content);
    }
    @Override
    public UserResponse updateProfile(UpdateProfileRequest request) {
        User currentUser = getCurrentUser();

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            currentUser.setFullName(request.getFullName());
        }
        if (request.getAvatarUrl() != null) {
            currentUser.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getBio() != null) {
            currentUser.setBio(request.getBio());
        }
        if (request.getPhone() != null) {
            currentUser.setPhone(request.getPhone());
        }
        if (request.getDateOfBirth() != null) {
            currentUser.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            currentUser.setGender(request.getGender());
        }
        if (request.getLocation() != null) {
            currentUser.setLocation(request.getLocation());
        }

        return mapToResponse(userRepository.save(currentUser));
    }

    @Override
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Override
    public UserResponse getUserProfile() {
        return mapToResponse(getCurrentUser());
    }

    private UserResponse mapToResponse(User user) {
        Long walletBalance = walletRepository.findByUserId(user.getId())
                .map(Wallet::getBalance)
                .orElse(0L);

        int followedStories = userRepository.countFollowedStories(user.getId());
        int purchasedChapters = (int) chapterPurchaseRepository.countByUserId(user.getId());

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .provider(user.getProvider().name())
                .enabled(user.getEnabled())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .phone(user.getPhone())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .location(user.getLocation())
                .walletBalance(walletBalance)
                .banUntil(user.getBanUntil())
                .totalFollowedStories(followedStories)
                .totalPurchasedChapters(purchasedChapters)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    // ===================== EMAIL VERIFICATION =====================
    @Override
    public void sendVerificationEmail(User user) {
        // Delete old token if exists
        verificationTokenRepository.findByUserId(user.getId())
                .ifPresent(verificationTokenRepository::delete);

        // Generate new verification token
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(token, user);
        verificationTokenRepository.save(verificationToken);

        // Create verification link
        String verificationLink = baseUrl + "/api/auth/verify?token=" + token;

        // Send email
        String subject = "Xác thực tài khoản Story Platform";
        String content = String.format(
                "Xin chào %s,\n\n" +
                "Cảm ơn bạn đã đăng ký tài khoản tại Story Platform!\n\n" +
                "Vui lòng click vào link dưới đây để xác thực email của bạn:\n" +
                "%s\n\n" +
                "Link này sẽ hết hạn sau 24 giờ.\n\n" +
                "Trân trọng,\nStory Platform Team",
                user.getFullName() != null ? user.getFullName() : user.getEmail(),
                verificationLink);

        mailService.sendTextMail(user.getEmail(), subject, content);
    }

    @Override
    @Transactional
    public boolean verifyEmail(String token) {
        VerificationToken vt = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid verification token"));

        // Check if token is expired
        if (vt.isExpired()) {
            throw new BadRequestException("Verification token has expired. Please request a new one.");
        }

        // Check if token was already used
        if (vt.isUsed()) {
            throw new BadRequestException("Verification token has already been used");
        }

        // Enable user
        User user = vt.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        // Mark token as used
        vt.setUsed(true);
        verificationTokenRepository.save(vt);
        return true;
    }

    @Override
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (user.getEnabled()) {
            throw new BadRequestException("Email is already verified");
        }

        sendVerificationEmail(user);
    }

    // ===================== OAUTH2 OTP =====================
    @Override
    public OtpResponse sendOAuth2Otp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        String otp = generateOtp();
        saveOtp(user, otp, "OAUTH2_LOGIN", 10);

        String subject = "Mã OTP đăng nhập Google - Story Platform";
        String content = String.format(
                "Xin chào %s,\n\n" +
                "Bạn vừa đăng nhập bằng tài khoản Google.\n\n" +
                "Mã OTP xác thực của bạn là:\n\n" +
                "  ► %s ◄\n\n" +
                "Mã này có hiệu lực trong 10 phút.\n" +
                "Nếu không phải bạn, hãy bỏ qua email này.\n\n" +
                "Trân trọng,\nStory Platform Team",
                user.getFullName() != null ? user.getFullName() : email,
                otp);

        mailService.sendTextMail(email, subject, content);

        return OtpResponse.builder()
                .message("Mã OTP đã được gửi đến email " + email + ". Vui lòng kiểm tra hòm thư và nhập mã trong 10 phút.")
                .email(email)
                .devOtp(otp) // TODO: xóa dòng này khi deploy production
                .build();
    }

    @Override
    public LoginResponse verifyOAuth2Otp(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        verifyOtp(user, otp, "OAUTH2_LOGIN");

        String accessToken = jwtUtil.generateAccessToken(email);
        return new LoginResponse(accessToken, null);
    }

    // ===================== CHANGE PASSWORD =====================
    @Override
    public void changePassword(ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BadRequestException("Mật khẩu mới và xác nhận mật khẩu không khớp");
        }

        User currentUser = getCurrentUser();

        if (currentUser.getPassword() == null) {
            throw new BadRequestException("Tài khoản này chưa có mật khẩu. Vui lòng dùng chức năng Quên mật khẩu để tạo mật khẩu mới.");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new BadRequestException("Mật khẩu hiện tại không đúng");
        }

        if (passwordEncoder.matches(request.getNewPassword(), currentUser.getPassword())) {
            throw new BadRequestException("Mật khẩu mới không được trùng với mật khẩu hiện tại");
        }

        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);
    }

    // ===================== FORGOT PASSWORD =====================
    @Override
    public OtpResponse forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Email không tồn tại trong hệ thống"));

        String otp = generateOtp();
        saveOtp(user, otp, "FORGOT_PASSWORD", 10);

        String subject = "Mã OTP đặt lại mật khẩu - Story Platform";
        String content = String.format(
                "Xin chào %s,\n\n" +
                "Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.\n\n" +
                "Mã OTP xác thực của bạn là:\n\n" +
                "  ╔══════════════╗\n" +
                "  ║   %s   ║\n" +
                "  ╚══════════════╝\n\n" +
                "Mã này có hiệu lực trong 10 phút.\n" +
                "Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này.\n\n" +
                "Trân trọng,\nStory Platform Team",
                user.getFullName() != null ? user.getFullName() : user.getEmail(),
                otp);

        mailService.sendTextMail(user.getEmail(), subject, content);

        return OtpResponse.builder()
                .message("Mã OTP đã được gửi đến email " + user.getEmail() + ". Vui lòng kiểm tra hòm thư trong vòng 10 phút.")
                .email(user.getEmail())
                .devOtp(otp) // TODO: xóa dòng này khi deploy production
                .build();
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BadRequestException("Mật khẩu mới và xác nhận mật khẩu không khớp");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Email không tồn tại trong hệ thống"));

        verifyOtp(user, request.getOtp(), "FORGOT_PASSWORD");

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // ===================== HELPERS =====================
    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000); // 6 chữ số
        return String.valueOf(otp);
    }

    private void saveOtp(User user, String otp, String otpType, int expiryMinutes) {
        // Xóa OTP cũ nếu tồn tại, flush ngay để tránh duplicate key khi insert mới
        verificationTokenRepository.findByUserId(user.getId())
                .ifPresent(vt -> {
                    verificationTokenRepository.delete(vt);
                    verificationTokenRepository.flush();
                });

        String token = UUID.randomUUID().toString();
        VerificationToken vt = new VerificationToken(token, otp, otpType, user, expiryMinutes);
        verificationTokenRepository.save(vt);
    }

    private void verifyOtp(User user, String inputOtp, String expectedType) {
        VerificationToken vt = verificationTokenRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy OTP. Vui lòng yêu cầu gửi lại."));

        if (!expectedType.equals(vt.getOtpType())) {
            throw new BadRequestException("OTP không hợp lệ cho hành động này.");
        }
        if (vt.isExpired()) {
            throw new BadRequestException("OTP đã hết hạn. Vui lòng yêu cầu gửi lại.");
        }
        if (vt.isUsed()) {
            throw new BadRequestException("OTP đã được sử dụng.");
        }
        if (!inputOtp.equals(vt.getOtp())) {
            throw new BadRequestException("OTP không chính xác.");
        }

        vt.setUsed(true);
        verificationTokenRepository.save(vt);
    }
}
