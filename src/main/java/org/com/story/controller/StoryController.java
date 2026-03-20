package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.StoryRequest;
import org.com.story.dto.response.ChapterStatsResponse;
import org.com.story.dto.response.StoryDetailResponse;
import org.com.story.dto.response.StoryResponse;
import org.com.story.service.StoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
@Tag(name = "Story Controller", description = "Quản lý truyện")
public class StoryController {

    private final StoryService storyService;

    // Public - Get all published stories
    @GetMapping
    @Operation(summary = "Get all published stories",
            description = "Danh sách truyện đã được duyệt, chưa bị xóa mềm, và có ít nhất 1 chương đã xuất bản")
    public List<StoryResponse> getAllStories() {
        return storyService.getAllPublishedStories();
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
}
