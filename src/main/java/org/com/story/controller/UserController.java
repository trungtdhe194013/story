package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/me")
    @Operation(
        summary = "Get current user profile",
        description = "Lấy toàn bộ thông tin của user đang đăng nhập: id, email, fullName, roles, provider, enabled, walletBalance, totalFollowedStories, totalPurchasedChapters, createdAt, updatedAt",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<UserResponse> getMe() {
        return ResponseEntity.ok(userService.getUserProfile());
    }

    @PutMapping("/me")
    @Operation(
        summary = "Update current user profile",
        description = "Cập nhật thông tin cá nhân: fullName, password (optional)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<UserResponse> updateProfile(@RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(request));
    }
}


