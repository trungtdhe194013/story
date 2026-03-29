package org.com.story.service;

import org.com.story.dto.request.StoryRequest;
import org.com.story.dto.response.ChapterStatsResponse;
import org.com.story.dto.response.StoryDetailResponse;
import org.com.story.dto.response.StoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StoryService {
    StoryResponse createStory(StoryRequest request);

    StoryResponse getStory(Long id);

    StoryDetailResponse getStoryDetail(Long id);

    Page<StoryResponse> getAllPublishedStories(List<String> categories, String status, String keyword, Integer year, Pageable pageable);

    List<StoryResponse> getMyStories();

    StoryResponse updateStory(Long id, StoryRequest request);

    void deleteStory(Long id);

    StoryResponse submitForReview(Long id);

    List<StoryResponse> searchStories(String keyword);

    /** Trả về số chương published và tổng số chương của một truyện */
    ChapterStatsResponse getChapterStats(Long storyId);

    // New endpoints
    List<StoryResponse> getStoriesByCategory(Long categoryId);

    List<StoryResponse> getTopViewedStories();

    List<StoryResponse> getTopRatedStories();

    List<StoryResponse> getCompletedStories();

    /**
     * Author cập nhật trạng thái bộ truyện: Ongoing ↔ Completed.
     * isCompleted=true → badge "Hoàn thành", isCompleted=false → badge "Đang ra"
     */
    StoryResponse setCompletionStatus(Long id, boolean isCompleted);
}
