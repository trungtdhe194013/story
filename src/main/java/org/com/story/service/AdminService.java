package org.com.story.service;

import org.com.story.dto.request.ReviewChapterRequest;
import org.com.story.dto.request.ReviewStoryRequest;
import org.com.story.dto.request.UpdateUserRoleRequest;
import org.com.story.dto.response.ChapterResponse;
import org.com.story.dto.response.DashboardStatsResponse;
import org.com.story.dto.response.StoryResponse;
import org.com.story.dto.response.UserResponse;

import java.util.List;

public interface AdminService {
    List<StoryResponse> getPendingStories();
    StoryResponse reviewStory(Long storyId, ReviewStoryRequest request);

    // Chapter review
    List<ChapterResponse> getPendingChaptersForReview();
    ChapterResponse reviewChapter(Long chapterId, ReviewChapterRequest request);

    UserResponse updateUserRoles(UpdateUserRoleRequest request);
    List<UserResponse> getAllUsers();

    DashboardStatsResponse getDashboardStats();
}
