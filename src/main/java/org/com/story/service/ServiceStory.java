package org.com.story.service;

import org.com.story.dto.request.StoryRequest;
import org.com.story.dto.response.StoryDetailResponse;
import org.com.story.dto.response.StoryResponse;

import java.util.List;

public interface ServiceStory {
    StoryResponse createStory(StoryRequest request);

    StoryResponse getStory(Long id);

    StoryDetailResponse getStoryDetail(Long id);

    List<StoryResponse> getAllPublishedStories();

    List<StoryResponse> getMyStories();

    StoryResponse updateStory(Long id, StoryRequest request);

    void deleteStory(Long id);

    StoryResponse submitForReview(Long id);

    List<StoryResponse> searchStories(String keyword);
}
