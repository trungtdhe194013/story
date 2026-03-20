package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.response.FollowResponse;
import org.com.story.dto.response.StoryResponse;
import org.com.story.entity.Story;
import org.com.story.entity.User;
import org.com.story.exception.NotFoundException;
import org.com.story.repository.StoryRepository;
import org.com.story.repository.UserRepository;
import org.com.story.service.FollowService;
import org.com.story.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FollowServiceImpl implements FollowService {

    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    public FollowResponse toggleFollow(Long storyId) {
        User currentUser = userService.getCurrentUser();
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new NotFoundException("Story not found"));

        boolean isFollowing = currentUser.getFollowedStories().contains(story);

        if (isFollowing) {
            currentUser.getFollowedStories().remove(story);
            userRepository.save(currentUser);
            long followCount = userRepository.countFollowersByStoryId(storyId);
            return FollowResponse.builder()
                    .storyId(storyId)
                    .storyTitle(story.getTitle())
                    .status("UNFOLLOWED")
                    .message("Đã bỏ theo dõi truyện: " + story.getTitle())
                    .followCount(followCount)
                    .build();
        } else {
            currentUser.getFollowedStories().add(story);
            userRepository.save(currentUser);
            long followCount = userRepository.countFollowersByStoryId(storyId);
            return FollowResponse.builder()
                    .storyId(storyId)
                    .storyTitle(story.getTitle())
                    .status("FOLLOWED")
                    .message("Đã theo dõi truyện: " + story.getTitle())
                    .followCount(followCount)
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoryResponse> getFollowedStories() {
        User currentUser = userService.getCurrentUser();
        return currentUser.getFollowedStories().stream()
                .map(this::mapStoryToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFollowing(Long storyId) {
        User currentUser = userService.getCurrentUser();
        return currentUser.getFollowedStories().stream()
                .anyMatch(s -> s.getId().equals(storyId));
    }

    @Override
    @Transactional(readOnly = true)
    public long getFollowCount(Long storyId) {
        return userRepository.countFollowersByStoryId(storyId);
    }

    private StoryResponse mapStoryToResponse(Story story) {
        long followCount = userRepository.countFollowersByStoryId(story.getId());
        return StoryResponse.builder()
                .id(story.getId())
                .title(story.getTitle())
                .summary(story.getSummary())
                .coverUrl(story.getCoverUrl())
                .status(story.getStatus())
                .authorId(story.getAuthor().getId())
                .authorName(story.getAuthor().getFullName())
                .followCount(followCount)
                .isFollowing(true) // danh sách followed của user hiện tại → luôn đang follow
                .createdAt(story.getCreatedAt())
                .updatedAt(story.getUpdatedAt())
                .build();
    }
}

