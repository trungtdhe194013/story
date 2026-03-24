package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.ChapterRequest;
import org.com.story.dto.request.ScheduleChapterRequest;
import org.com.story.dto.response.ChapterResponse;
import org.com.story.service.ChapterService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chapters")
@RequiredArgsConstructor
@Tag(name = "Chapter Controller", description = "Quản lý chương truyện")
public class ChapterController {

    private final ChapterService chapterService;

    @GetMapping("/{id}")
    @Operation(summary = "Get chapter detail")
    public ChapterResponse getChapter(@PathVariable Long id) {
        return chapterService.getChapter(id);
    }

    @GetMapping("/story/{storyId}")
    @Operation(summary = "Get chapters by story")
    public List<ChapterResponse> getChaptersByStory(@PathVariable Long storyId) {
        return chapterService.getChaptersByStory(storyId);
    }

    @PostMapping("/story/{storyId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create chapter (AUTHOR only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ChapterResponse createChapter(
            @PathVariable Long storyId,
            @Valid @RequestBody ChapterRequest request) {
        return chapterService.createChapter(storyId, request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update chapter (AUTHOR only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ChapterResponse updateChapter(
            @PathVariable Long id,
            @Valid @RequestBody ChapterRequest request) {
        return chapterService.updateChapter(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete chapter (AUTHOR only)", security = @SecurityRequirement(name = "bearerAuth"))
    public void deleteChapter(@PathVariable Long id) {
        chapterService.deleteChapter(id);
    }

    /**
     * Author nộp chapter lên Reviewer duyệt.
     * Status: DRAFT hoặc EDITED hoặc REJECTED → PENDING
     */
    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit chapter for review (AUTHOR only)",
            description = "Nộp chapter lên Reviewer duyệt. Chapter phải đang ở DRAFT hoặc EDITED.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ChapterResponse submitForReview(@PathVariable Long id) {
        return chapterService.submitForReview(id);
    }

    /**
     * Author tự publish chapter sau khi Reviewer đã APPROVE.
     * Status: APPROVED → PUBLISHED
     */
    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish approved chapter (AUTHOR only)",
            description = "Publish chapter sau khi đã được Reviewer APPROVE. Author quyết định thời điểm lên sóng.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ChapterResponse publishApprovedChapter(@PathVariable Long id) {
        return chapterService.publishApprovedChapter(id);
    }

    /**
     * Author hẹn lịch publish vào ngày giờ cụ thể trong tương lai.
     * Status: APPROVED → SCHEDULED. Cron job mỗi 15 phút sẽ tự đổi sang PUBLISHED đúng giờ.
     */
    @PostMapping("/{id}/schedule")
    @Operation(summary = "Schedule chapter publish (AUTHOR only)",
            description = "Hẹn lịch xuất bản chương. Chapter phải đã APPROVED. publishAt phải là thời điểm trong tương lai.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ChapterResponse schedulePublish(
            @PathVariable Long id,
            @Valid @RequestBody ScheduleChapterRequest request) {
        return chapterService.schedulePublish(id, request);
    }

    @PostMapping("/{id}/purchase")
    @Operation(summary = "Purchase chapter (READER)", security = @SecurityRequirement(name = "bearerAuth"))
    public ChapterResponse purchaseChapter(@PathVariable Long id) {

        return chapterService.purchaseChapter(id);
    }
}


