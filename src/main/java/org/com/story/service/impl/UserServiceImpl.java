package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.common.AuthProvider;
import org.com.story.dto.request.SignUpRequest;
import org.com.story.dto.response.UserResponse;
import org.com.story.entity.Role;
import org.com.story.entity.User;
import org.com.story.entity.VerificationToken;
import org.com.story.entity.Wallet;
import org.com.story.exception.BadRequestException;
import org.com.story.repository.RoleRepository;
import org.com.story.repository.UserRepository;
import org.com.story.repository.VerificationTokenRepository;
import org.com.story.service.MailService;
import org.com.story.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

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
        user.setPurchasedChapters(new HashSet<>());

        User savedUser = userRepository.save(user);

        // Create wallet for user
        Wallet wallet = new Wallet();
        wallet.setUser(savedUser);
        wallet.setBalance(0L);
        // Wallet will be saved by cascade or separate repository

        // Send verification email
        sendVerificationEmail(savedUser);

        return mapToResponse(savedUser);
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
        User user = getCurrentUser();
        return mapToResponse(user);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .provider(user.getProvider().name())
                .enabled(user.getEnabled())
                .build();
    }

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
                "Nếu bạn không đăng ký tài khoản này, vui lòng bỏ qua email này.\n\n" +
                "Trân trọng,\n" +
                "Story Platform Team",
                user.getFullName(),
                verificationLink
        );

        mailService.sendTextMail(user.getEmail(), subject, content);
    }

    @Override
    @Transactional
    public boolean verifyEmail(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid verification token"));

        // Check if token is expired
        if (verificationToken.isExpired()) {
            throw new BadRequestException("Verification token has expired. Please request a new one.");
        }

        // Check if token was already used
        if (verificationToken.isUsed()) {
            throw new BadRequestException("Verification token has already been used");
        }

        // Enable user
        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        // Mark token as used
        verificationToken.setUsed(true);
        verificationTokenRepository.save(verificationToken);

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
}
