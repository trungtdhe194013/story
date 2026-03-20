package org.com.story.service;

import org.com.story.dto.request.RatingRequest;
import org.com.story.dto.response.RatingResponse;

import java.util.List;

public interface RatingService {
    RatingResponse rateStory(RatingRequest request);
    List<RatingResponse> getRatingsByStory(Long storyId);
    RatingResponse getMyRating(Long storyId);
}

