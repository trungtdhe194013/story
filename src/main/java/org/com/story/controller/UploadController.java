package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * Generic image upload endpoint.
 * Frontend dùng endpoint này để upload bất kỳ ảnh nào (avatar, cover, thumbnail...).
 *
 * Flow:
 *   1. POST /api/upload/image?type=avatar|cover|other  → nhận { "url": "https://..." }
 *   2. Dùng URL đó để gán vào field tương ứng (avatarUrl, coverUrl, ...)
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Tag(name = "Upload Controller", description = "Upload ảnh lên server")
public class UploadController {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload ảnh (avatar / cover / other)",
            description = """
                    Upload file ảnh (JPG, PNG, GIF, WebP) tối đa 5MB.
                    
                    **Query param** `type`:
                    - `avatar`  → lưu vào `uploads/avatars/`
                    - `cover`   → lưu vào `uploads/covers/`
                    - (khác)    → lưu vào `uploads/images/`
                    
                    **Response** (trong data):
                    ```json
                    { "url": "http://localhost:8080/uploads/covers/cover_xxx.jpg" }
                    ```
                    
                    Sau khi nhận URL, frontend dùng tiếp trong:
                    - PUT /api/users/me          → trường `avatarUrl`
                    - PUT /api/stories/{id}      → trường `coverUrl`
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "images") String type) throws IOException {

        // 1. Validate: không rỗng
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File không được để trống");
        }

        // 2. Validate: chỉ chấp nhận ảnh
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Chỉ chấp nhận file ảnh (JPG, PNG, GIF, WebP)");
        }

        // 3. Validate file size (5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File quá lớn. Giới hạn 5MB.");
        }

        // 4. Xác định thư mục lưu theo type
        String subDir = switch (type.toLowerCase()) {
            case "avatar" -> "avatars";
            case "cover"  -> "covers";
            default       -> "images";
        };

        // 5. Tạo tên file duy nhất
        String originalFilename = file.getOriginalFilename();
        String ext = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase()
                : ".jpg";
        String filename = subDir.replaceAll("s$", "") + "_" + UUID.randomUUID() + ext;

        // 6. Lưu file
        java.nio.file.Path uploadDir = Paths.get("uploads", subDir);
        Files.createDirectories(uploadDir);
        Files.copy(file.getInputStream(), uploadDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);

        // 7. Trả về URL
        // Nếu dùng ngrok free tier, phải thêm query param để skip browser warning page
        // Vì <img> tag không gửi được request header, chỉ query param mới work
        String path = "/uploads/" + subDir + "/" + filename;
        String url = baseUrl.contains("ngrok")
                ? baseUrl + path + "?ngrok-skip-browser-warning=true"
                : baseUrl + path;
        return ResponseEntity.ok(Map.of("url", url));
    }
}

