package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.ChangePasswordRequest;
import org.com.story.dto.request.UpdateProfileRequest;
import org.com.story.dto.response.UserResponse;
import org.com.story.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Controller", description = "User profile endpoints")
public class UserController {

    private final UserService userService;

    // ===================== PROFILE =====================

    @GetMapping("/me")
    @Operation(
        summary = "Get current user profile",
        description = "Lấy toàn bộ thông tin profile của user đang đăng nhập.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<UserResponse> getMe() {
        return ResponseEntity.ok(userService.getUserProfile());
    }

    @PutMapping("/me")
    @Operation(
        summary = "Update current user profile",
        description = "Cập nhật thông tin cá nhân: fullName, avatarUrl, bio, phone, dateOfBirth (yyyy-MM-dd), gender (MALE/FEMALE/OTHER), location.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(request));
    }

    // ===================== CHANGE PASSWORD =====================

    @PutMapping("/me/change-password")
    @Operation(
        summary = "Đổi mật khẩu",
        description = """
            Dành cho user **đang đăng nhập** muốn đổi mật khẩu.
            Body cần truyền:
            - `currentPassword`: mật khẩu hiện tại
            - `newPassword`: mật khẩu mới (ít nhất 6 ký tự, có chữ hoa, chữ thường, số)
            - `confirmNewPassword`: xác nhận mật khẩu mới (phải trùng với newPassword)
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.noContent().build();
    }
}
