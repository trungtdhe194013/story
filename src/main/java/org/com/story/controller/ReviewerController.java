package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.ReviewChapterRequest;
import org.com.story.dto.request.ReviewStoryRequest;
import org.com.story.dto.response.ChapterResponse;
import org.com.story.dto.response.ReviewHistoryResponse;
import org.com.story.dto.response.StoryDetailResponse;
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
    @Operation(summary = "Danh sách story chờ duyệt",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<StoryResponse> getPendingStories() {
        return adminService.getPendingStories();
    }

    @GetMapping("/stories/{id}/detail")
    @Operation(summary = "Đọc chi tiết story (kèm toàn bộ chapter) để duyệt",
            description = "Reviewer đọc thông tin story + danh sách TẤT CẢ chapter (kể cả PENDING_REVIEW) trước khi quyết định.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public StoryDetailResponse getStoryDetailForReview(@PathVariable Long id) {
        return adminService.getStoryDetailForReview(id);
    }

    @PostMapping("/stories/{id}/review")
    @Operation(summary = "Duyệt / từ chối story (APPROVE | REJECT)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public StoryResponse reviewStory(
            @PathVariable Long id,
            @Valid @RequestBody ReviewStoryRequest request) {
        return adminService.reviewStory(id, request);
    }

    // ============== CHAPTER ==============

    @GetMapping("/chapters/pending")
    @Operation(summary = "Danh sách chapter chờ duyệt (PENDING_REVIEW)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<ChapterResponse> getPendingChapters() {
        return adminService.getPendingChaptersForReview();
    }

    @GetMapping("/chapters/{id}")
    @Operation(summary = "Đọc full nội dung chapter để duyệt",
            description = "Reviewer đọc toàn bộ content của chapter trước khi APPROVE hoặc REJECT.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ChapterResponse getChapterForReview(@PathVariable Long id) {
        return adminService.getChapterForReview(id);
    }

    @PostMapping("/chapters/{id}/review")
    @Operation(summary = "Duyệt / từ chối chapter",
            description = """
                    - APPROVE → chapter chuyển sang **APPROVED** (Author tự publish sau)
                    - REJECT  → chapter trả về **DRAFT** kèm `note` là lý do từ chối
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    public ChapterResponse reviewChapter(
            @PathVariable Long id,
            @Valid @RequestBody ReviewChapterRequest request) {
        return adminService.reviewChapter(id, request);
    }

    // ============== REVIEW HISTORY ==============

    @GetMapping("/history")
    @Operation(summary = "Lịch sử duyệt của tôi (tất cả)",
            description = """
                    Trả về toàn bộ lịch sử duyệt (STORY + CHAPTER) của reviewer đang đăng nhập,
                    sắp xếp mới nhất lên đầu.

                    Mỗi record gồm: targetType, targetTitle, storyTitle (nếu là chapter),
                    action (APPROVE/REJECT), note (lý do từ chối), currentStatus, thời gian duyệt.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<ReviewHistoryResponse> getMyHistory() {
        return adminService.getMyReviewHistory();
    }

    @GetMapping("/history/stories")
    @Operation(summary = "Lịch sử duyệt story của tôi",
            description = "Chỉ lấy lịch sử duyệt **STORY** của reviewer đang đăng nhập.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<ReviewHistoryResponse> getMyStoryHistory() {
        return adminService.getMyReviewHistoryByType("STORY");
    }

    @GetMapping("/history/chapters")
    @Operation(summary = "Lịch sử duyệt chapter của tôi",
            description = "Chỉ lấy lịch sử duyệt **CHAPTER** của reviewer đang đăng nhập.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<ReviewHistoryResponse> getMyChapterHistory() {
        return adminService.getMyReviewHistoryByType("CHAPTER");
    }

    @GetMapping("/history/story/{storyId}")
    @Operation(summary = "Lịch sử duyệt của 1 story cụ thể",
            description = """
                    Xem ai đã duyệt story này, vào thời điểm nào, kết quả gì.
                    Hữu ích để tra cứu lịch sử review của 1 truyện.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<ReviewHistoryResponse> getStoryHistory(@PathVariable Long storyId) {
        return adminService.getStoryReviewHistory(storyId);
    }

    @GetMapping("/history/chapter/{chapterId}")
    @Operation(summary = "Lịch sử duyệt của 1 chapter cụ thể",
            description = "Xem ai đã duyệt chapter này, vào thời điểm nào, kết quả gì.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<ReviewHistoryResponse> getChapterHistory(@PathVariable Long chapterId) {
        return adminService.getChapterReviewHistory(chapterId);
    }
}
