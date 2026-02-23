package org.com.story.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.StoryRequest;
import org.com.story.dto.response.StoryResponse;
import org.com.story.service.StoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;

    // Public - Get all published stories
    @GetMapping
    public List<StoryResponse> getAllStories() {

        return storyService.getAllPublishedStories();
    }

    // Public - Search stories
    @GetMapping("/search")
    public List<StoryResponse> searchStories(@RequestParam String keyword) {
        return storyService.searchStories(keyword);
    }

    // Public/Protected - Get story detail
    @GetMapping("/{id}")
    public StoryResponse getStory(@PathVariable Long id) {

        return storyService.getStory(id);
    }

    // Protected - Get my stories
    @GetMapping("/my")
    public List<StoryResponse> getMyStories() {

        return storyService.getMyStories();
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

