package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.StoryRequest;
import org.com.story.dto.response.ChapterStatsResponse;
import org.com.story.dto.response.StoryDetailResponse;
import org.com.story.dto.response.StoryResponse;
import org.com.story.service.StoryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import java.util.List;

@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
@Tag(name = "Story Controller", description = "Quản lý truyện")
public class StoryController {

    private final StoryService storyService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // Public - Get all published stories
    @GetMapping
    @Operation(summary = "Get all published stories",
            description = "Danh sách truyện đã được duyệt, phân trang và có hỗ trợ filter. Tham số: size, page, sort, categories (tên thể loại), status (ONGOING, COMPLETED), keyword, year.")
    public org.springframework.data.domain.Page<StoryResponse> getAllStories(
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer year,
            org.springframework.data.domain.Pageable pageable) {
        return storyService.getAllPublishedStories(categories, status, keyword, year, pageable);
    }

    // Public - Search stories
    @GetMapping("/search")
    @Operation(summary = "Search stories", description = "Tìm kiếm truyện theo tên")
    public List<StoryResponse> searchStories(@RequestParam String keyword) {
        return storyService.searchStories(keyword);
    }

    // Protected - Get my stories (author)
    @GetMapping("/my")
    @Operation(summary = "Get my stories", description = "Xem tất cả truyện của tôi (AUTHOR), kể cả đã bị xóa mềm")
    public List<StoryResponse> getMyStories() {
        return storyService.getMyStories();
    }

    // Public/Protected - Get story basic info
    @GetMapping("/{id}")
    @Operation(summary = "Get story", description = "Thông tin cơ bản của truyện")
    public StoryResponse getStory(@PathVariable Long id) {
        return storyService.getStory(id);
    }

    // Public - Get story full detail
    @GetMapping("/{id}/detail")
    @Operation(summary = "Get story detail",
            description = "Chi tiết đầy đủ: thông tin truyện + danh sách chương PUBLISHED + tổng số chương")
    public StoryDetailResponse getStoryDetail(@PathVariable Long id) {
        return storyService.getStoryDetail(id);
    }

    // Public - Get chapter stats
    @GetMapping("/{id}/chapter-stats")
    @Operation(summary = "Get chapter statistics",
            description = "Thống kê số chương: publishedCount (đã xuất bản) và totalCount (tổng tất cả trạng thái)")
    public ChapterStatsResponse getChapterStats(@PathVariable Long id) {
        return storyService.getChapterStats(id);
    }

    // Protected - Create new story
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create story",
            description = "Tạo truyện mới. Truyền `categoryIds` để gắn thể loại ngay khi tạo.")
    public StoryResponse createStory(@Valid @RequestBody StoryRequest request) {
        return storyService.createStory(request);
    }

    // Protected - Update story
    @PutMapping("/{id}")
    @Operation(summary = "Update story",
            description = "Cập nhật thông tin truyện. Truyền `categoryIds` để thay đổi thể loại.")
    public StoryResponse updateStory(
            @PathVariable Long id,
            @Valid @RequestBody StoryRequest request) {
        return storyService.updateStory(id, request);
    }

    // Protected - Soft delete story
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft delete story",
            description = "Xóa mềm truyện — ẩn khỏi trang chủ nhưng dữ liệu vẫn còn trong DB. Author vẫn thấy trong /my.")
    public void deleteStory(@PathVariable Long id) {
        storyService.deleteStory(id);
    }

    // Protected - Submit story for review
    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit story for review")
    public StoryResponse submitForReview(@PathVariable Long id) {
        return storyService.submitForReview(id);
    }

    /**
     * Author (hoặc Admin) chuyển trạng thái bộ truyện giữa "Đang ra" (Ongoing) ↔ "Hoàn thành" (Completed).
     * Badge trạng thái xuất hiện trên trang chi tiết và trong kết quả tìm kiếm/filter.
     *
     * PATCH /api/stories/{id}/completion-status?completed=true  → Hoàn thành
     * PATCH /api/stories/{id}/completion-status?completed=false → Đang ra
     */
    @PatchMapping("/{id}/completion-status")
    @Operation(summary = "Set story completion status (AUTHOR / ADMIN)",
            description = """
                    Cập nhật trạng thái bộ truyện:
                    - `completed=true`  → "Hoàn thành" (Completed)
                    - `completed=false` → "Đang ra" (Ongoing)
                    Badge xuất hiện trên trang chi tiết và trong filter.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    public StoryResponse setCompletionStatus(
            @PathVariable Long id,
            @RequestParam boolean completed) {
        return storyService.setCompletionStatus(id, completed);
    }

    /**
     * Upload ảnh bìa truyện.
     *
     * Flow frontend:
     *   1. POST /api/stories/{id}/cover  (multipart)  → nhận { "coverUrl": "https://..." }
     *   2. PUT  /api/stories/{id}         (JSON)        → gửi coverUrl + các field khác → nhận StoryResponse
     */
    @PostMapping(value = "/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload ảnh bìa truyện",
            description = """
                    Upload file ảnh (JPG, PNG, GIF, WebP) tối đa 5MB.
                    Trả về URL public của ảnh để frontend dùng tiếp trong PUT /stories/{id}.
                    Response (trong data): { "coverUrl": "https://..." }
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, String>> uploadCover(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) throw new IllegalArgumentException("File không được để trống");

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Chỉ chấp nhận file ảnh (JPG, PNG, GIF, WebP)");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase()
                : ".jpg";
        String filename = "cover_" + id + "_" + java.util.UUID.randomUUID() + ext;

        java.nio.file.Path uploadDir = java.nio.file.Paths.get("uploads", "covers");
        java.nio.file.Files.createDirectories(uploadDir);
        java.nio.file.Files.copy(
                file.getInputStream(),
                uploadDir.resolve(filename),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
        );

        String coverUrl = baseUrl + "/uploads/covers/" + filename;
        return ResponseEntity.ok(Map.of("coverUrl", coverUrl));
    }
}
