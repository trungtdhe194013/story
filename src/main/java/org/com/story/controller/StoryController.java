package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.StoryRequest;
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
    @Operation(summary = "Get all published stories", description = "Danh sách tất cả truyện đã được duyệt")
    public List<StoryResponse> getAllStories() {
        return storyService.getAllPublishedStories();
    }

    // Public - Search stories
    @GetMapping("/search")
    @Operation(summary = "Search stories", description = "Tìm kiếm truyện theo tên")
    public List<StoryResponse> searchStories(@RequestParam String keyword) {
        return storyService.searchStories(keyword);
    }

    // Public - Get my stories
    @GetMapping("/my")
    @Operation(summary = "Get my stories", description = "Xem truyện của tôi (AUTHOR)")
    public List<StoryResponse> getMyStories() {
        return storyService.getMyStories();
    }

    // Public/Protected - Get story basic info
    @GetMapping("/{id}")
    @Operation(summary = "Get story", description = "Thông tin cơ bản của truyện")
    public StoryResponse getStory(@PathVariable Long id) {
        return storyService.getStory(id);
    }

    // Public - Get story full detail (gồm chapters + comments của story)
    @GetMapping("/{id}/detail")
    @Operation(summary = "Get story detail",
            description = "Chi tiết đầy đủ của truyện: thông tin truyện + danh sách chương + comment đánh giá truyện. Dùng cho trang chi tiết truyện.")
    public StoryDetailResponse getStoryDetail(@PathVariable Long id) {
        return storyService.getStoryDetail(id);
    }

    // Protected - Create new story
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoryResponse createStory(@Valid @RequestBody StoryRequest request) {
        return storyService.createStory(request);
    }

    // Protected - Update story
    @PutMapping("/{id}")
    public StoryResponse updateStory(
            @PathVariable Long id,
            @Valid @RequestBody StoryRequest request) {
        return storyService.updateStory(id, request);
    }

    // Protected - Delete story
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStory(@PathVariable Long id) {

        storyService.deleteStory(id);
    }

    // Protected - Submit story for review
    @PostMapping("/{id}/submit")
    public StoryResponse submitForReview(@PathVariable Long id) {
        return storyService.submitForReview(id);
    }
}
