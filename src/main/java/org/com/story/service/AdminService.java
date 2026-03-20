package org.com.story.service;

import org.com.story.dto.request.ReviewChapterRequest;
import org.com.story.dto.request.ReviewStoryRequest;
import org.com.story.dto.request.UpdateUserRoleRequest;
import org.com.story.dto.response.ChapterResponse;
import org.com.story.dto.response.DashboardStatsResponse;
import org.com.story.dto.response.StoryDetailResponse;
import org.com.story.dto.response.StoryResponse;
import org.com.story.dto.response.UserResponse;

import java.util.List;

public interface AdminService {
    List<StoryResponse> getPendingStories();
    StoryResponse reviewStory(Long storyId, ReviewStoryRequest request);

    // Chapter review
    List<ChapterResponse> getPendingChaptersForReview();
    ChapterResponse reviewChapter(Long chapterId, ReviewChapterRequest request);

    // Reviewer đọc nội dung đầy đủ trước khi duyệt
    StoryDetailResponse getStoryDetailForReview(Long storyId);
    ChapterResponse getChapterForReview(Long chapterId);

    UserResponse updateUserRoles(UpdateUserRoleRequest request);
    List<UserResponse> getAllUsers();

    DashboardStatsResponse getDashboardStats();

    /** Ban tài khoản. banDays = -1 là ban vĩnh viễn, banDays > 0 là số ngày */
    UserResponse banUser(Long userId, int banDays);

    /** Gỡ ban tài khoản */
    UserResponse unbanUser(Long userId);
}
