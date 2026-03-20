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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Controller", description = "User profile endpoints")
public class UserController {

    private final UserService userService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

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
        description = """
            Cập nhật thông tin cá nhân: fullName, avatarUrl, bio, phone, dateOfBirth (yyyy-MM-dd), gender (MALE/FEMALE/OTHER), location.
            Trả về full UserResponse giống GET /users/me sau khi update thành công.
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(request));
    }

    /**
     * Upload avatar từ máy tính.
     *
     * Flow frontend:
     *   1. POST /api/users/me/avatar  (multipart)  → nhận { "avatarUrl": "https://..." }
     *   2. PUT  /api/users/me         (JSON)        → gửi avatarUrl + các field khác → nhận full UserResponse
     *
     * GlobalResponseWrapper tự bọc thêm 1 tầng, nên response thực tế:
     *   { "success": true, "status": 200, "data": { "avatarUrl": "https://..." } }
     */
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Upload avatar từ máy tính",
        description = """
            Upload file ảnh (JPG, PNG, GIF, WebP) tối đa 5MB.
            Trả về URL public của ảnh để frontend dùng tiếp trong PUT /me.
            Response (trong data): { "avatarUrl": "https://..." }
            """,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<java.util.Map<String, String>> uploadAvatar(
            @RequestParam("file") MultipartFile file) throws java.io.IOException {

        // 1. Validate: không được rỗng
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }

        // 2. Validate: chỉ chấp nhận ảnh
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Chỉ chấp nhận file ảnh (JPG, PNG, GIF, WebP)");
        }

        // 3. Tạo tên file duy nhất bằng UUID
        String originalFilename = file.getOriginalFilename();
        String ext = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase()
                : ".jpg";
        String filename = "avatar_" + java.util.UUID.randomUUID() + ext;

        // 4. Lưu file vào thư mục uploads/avatars/ (tự tạo nếu chưa có)
        java.nio.file.Path uploadDir = java.nio.file.Paths.get("uploads", "avatars");
        java.nio.file.Files.createDirectories(uploadDir);
        java.nio.file.Files.copy(
                file.getInputStream(),
                uploadDir.resolve(filename),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
        );

        // 5. Tạo URL public truy cập ảnh
        String avatarUrl = baseUrl + "/uploads/avatars/" + filename;

        // 6. Trả về { "avatarUrl": "..." } — GlobalResponseWrapper sẽ bọc thêm tầng data bên ngoài
        return ResponseEntity.ok(java.util.Map.of("avatarUrl", avatarUrl));
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
