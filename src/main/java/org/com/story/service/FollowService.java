package org.com.story.service;

import org.com.story.dto.response.FollowResponse;
import org.com.story.dto.response.StoryResponse;

import java.util.List;

public interface FollowService {
    FollowResponse toggleFollow(Long storyId);
    List<StoryResponse> getFollowedStories();
    boolean isFollowing(Long storyId);
    /** Đếm số người đang follow một truyện */
    long getFollowCount(Long storyId);
}

