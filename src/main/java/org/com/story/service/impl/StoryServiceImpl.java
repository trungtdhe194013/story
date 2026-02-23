package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.StoryRequest;
import org.com.story.dto.response.StoryResponse;
import org.com.story.entity.Story;
import org.com.story.entity.User;
import org.com.story.exception.BadRequestException;
import org.com.story.exception.NotFoundException;
import org.com.story.exception.UnauthorizedException;
import org.com.story.repository.StoryRepository;
import org.com.story.service.StoryService;
import org.com.story.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StoryServiceImpl implements StoryService {

    private final StoryRepository storyRepository;
    private final UserService userService;

    @Override
    public StoryResponse createStory(StoryRequest request) {
        User currentUser = userService.getCurrentUser();

        Story story = new Story();
        story.setAuthor(currentUser);
        story.setTitle(request.getTitle());
        story.setSummary(request.getSummary());
        story.setCoverUrl(request.getCoverUrl());
        story.setStatus("DRAFT");

        Story savedStory = storyRepository.save(story);
        return mapToResponse(savedStory);
    }

    @Override
    public StoryResponse getStory(Long id) {
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Story not found"));

        // Check if user can access this story
        User currentUser = null;
        try {
            currentUser = userService.getCurrentUser();
        } catch (Exception e) {
            // User not logged in
        }

        // If story is not APPROVED, only author can view
        if (!"APPROVED".equals(story.getStatus())) {
            if (currentUser == null || !story.getAuthor().getId().equals(currentUser.getId())) {
                throw new UnauthorizedException("You don't have permission to view this story");
            }
        }

        return mapToResponse(story);
    }

    @Override
    public List<StoryResponse> getAllPublishedStories() {
        return storyRepository.findAllPublished()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<StoryResponse> getMyStories() {
        User currentUser = userService.getCurrentUser();
        return storyRepository.findByAuthorId(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public StoryResponse updateStory(Long id, StoryRequest request) {
        User currentUser = userService.getCurrentUser();
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Story not found"));

        // Check ownership
        if (!story.getAuthor().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You don't have permission to update this story");
        }

        // Can only update if DRAFT or REJECTED
        if ("PENDING".equals(story.getStatus()) || "APPROVED".equals(story.getStatus())) {
            throw new BadRequestException("Cannot update story in " + story.getStatus() + " status");
        }

        story.setTitle(request.getTitle());
        story.setSummary(request.getSummary());
        story.setCoverUrl(request.getCoverUrl());

        Story updatedStory = storyRepository.save(story);
        return mapToResponse(updatedStory);
    }

    @Override
    public void deleteStory(Long id) {
        User currentUser = userService.getCurrentUser();
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Story not found"));

        // Check ownership
        if (!story.getAuthor().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You don't have permission to delete this story");
        }

        // Can only delete if DRAFT
        if (!"DRAFT".equals(story.getStatus())) {
            throw new BadRequestException("Can only delete stories in DRAFT status");
        }

        storyRepository.delete(story);
    }

    @Override
    public StoryResponse submitForReview(Long id) {
        User currentUser = userService.getCurrentUser();
        Story story = storyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Story not found"));

        // Check ownership
        if (!story.getAuthor().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You don't have permission to submit this story");
        }

        // Can only submit if DRAFT
        if (!"DRAFT".equals(story.getStatus())) {
            throw new BadRequestException("Can only submit stories in DRAFT status");
        }

        story.setStatus("PENDING");
        Story updatedStory = storyRepository.save(story);
        return mapToResponse(updatedStory);
    }

    @Override
    public List<StoryResponse> searchStories(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllPublishedStories();
        }

        return storyRepository.searchPublished(keyword)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private StoryResponse mapToResponse(Story story) {
        return StoryResponse.builder()
                .id(story.getId())
                .title(story.getTitle())
                .summary(story.getSummary())
                .coverUrl(story.getCoverUrl())
                .status(story.getStatus())
                .authorId(story.getAuthor().getId())
                .authorName(story.getAuthor().getFullName())
                .createdAt(story.getCreatedAt())
                .updatedAt(story.getUpdatedAt())
                .build();
    }
}
