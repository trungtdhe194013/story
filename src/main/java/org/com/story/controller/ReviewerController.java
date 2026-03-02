package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.ReviewChapterRequest;
import org.com.story.dto.request.ReviewStoryRequest;
import org.com.story.dto.response.ChapterResponse;
import org.com.story.dto.response.StoryResponse;
import org.com.story.service.AdminService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviewer")
@RequiredArgsConstructor
@Tag(name = "Reviewer Controller", description = "Chức năng kiểm duyệt viên")
public class ReviewerController {

    private final AdminService adminService;

    // ============== STORY ==============

    @GetMapping("/stories/pending")
    @Operation(summary = "Get pending stories", description = "Xem danh sách truyện chờ duyệt",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<StoryResponse> getPendingStories() {
        return adminService.getPendingStories();
    }

    @PostMapping("/stories/{id}/review")
    @Operation(summary = "Review story", description = "Duyệt hoặc từ chối truyện (APPROVE / REJECT)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public StoryResponse reviewStory(
            @PathVariable Long id,
            @Valid @RequestBody ReviewStoryRequest request) {
        return adminService.reviewStory(id, request);
    }

    // ============== CHAPTER ==============

    @GetMapping("/chapters/pending")
    @Operation(summary = "Get pending chapters", description = "Xem danh sách chapter chờ duyệt (PENDING_REVIEW)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<ChapterResponse> getPendingChapters() {
        return adminService.getPendingChaptersForReview();
    }

    @PostMapping("/chapters/{id}/review")
    @Operation(summary = "Review chapter", description = """
            Duyệt hoặc từ chối chapter:
            - APPROVE → chapter chuyển sang PUBLISHED (reader có thể đọc)
            - REJECT  → chapter trả về DRAFT (author sửa lại rồi submit tiếp)
            """,
            security = @SecurityRequirement(name = "bearerAuth"))
    public ChapterResponse reviewChapter(
            @PathVariable Long id,
            @Valid @RequestBody ReviewChapterRequest request) {
        return adminService.reviewChapter(id, request);
    }
}

