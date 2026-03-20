package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.RatingRequest;
import org.com.story.dto.response.RatingResponse;
import org.com.story.entity.Rating;
import org.com.story.entity.Story;
import org.com.story.entity.User;
import org.com.story.exception.NotFoundException;
import org.com.story.repository.RatingRepository;
import org.com.story.repository.StoryRepository;
import org.com.story.service.RatingService;
import org.com.story.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final StoryRepository storyRepository;
    private final UserService userService;

    @Override
    public RatingResponse rateStory(RatingRequest request) {
        User currentUser = userService.getCurrentUser();
        Story story = storyRepository.findById(request.getStoryId())
                .orElseThrow(() -> new NotFoundException("Story not found"));

        // Tìm rating cũ — nếu có thì update, không thì tạo mới
        Optional<Rating> existingOpt = ratingRepository.findByUserIdAndStoryId(currentUser.getId(), story.getId());

        Rating rating;
        if (existingOpt.isPresent()) {
            rating = existingOpt.get();
            rating.setScore(request.getScore());
            rating.setReview(request.getReview());
        } else {
            rating = Rating.builder()
                    .user(currentUser)
                    .story(story)
                    .score(request.getScore())
                    .review(request.getReview())
                    .build();
        }
        rating = ratingRepository.save(rating);

        // Update avgRating + ratingCount trên Story
        updateStoryRating(story);

        return mapToResponse(rating);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RatingResponse> getRatingsByStory(Long storyId) {
        return ratingRepository.findByStoryIdOrderByCreatedAtDesc(storyId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RatingResponse getMyRating(Long storyId) {
        User currentUser = userService.getCurrentUser();
        Rating rating = ratingRepository.findByUserIdAndStoryId(currentUser.getId(), storyId)
                .orElseThrow(() -> new NotFoundException("You haven't rated this story yet"));
        return mapToResponse(rating);
    }

    private void updateStoryRating(Story story) {
        Double avg = ratingRepository.getAverageScoreByStoryId(story.getId());
        Integer count = ratingRepository.getCountByStoryId(story.getId());
        story.setAvgRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
        story.setRatingCount(count != null ? count : 0);
        storyRepository.save(story);
    }

    private RatingResponse mapToResponse(Rating rating) {
        return RatingResponse.builder()
                .id(rating.getId())
                .userId(rating.getUser().getId())
                .userName(rating.getUser().getFullName())
                .storyId(rating.getStory().getId())
                .storyTitle(rating.getStory().getTitle())
                .score(rating.getScore())
                .review(rating.getReview())
                .createdAt(rating.getCreatedAt())
                .updatedAt(rating.getUpdatedAt())
                .build();
    }
}

