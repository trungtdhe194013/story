package org.com.story.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.ReviewStoryRequest;
import org.com.story.dto.response.StoryResponse;
import org.com.story.service.AdminService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviewer")
@RequiredArgsConstructor
public class ReviewerController {

    private final AdminService adminService;

    // Get all pending stories for review
    @GetMapping("/stories/pending")
    public List<StoryResponse> getPendingStories() {

        return adminService.getPendingStories();
    }

    // Review story (approve/reject) - Same as admin but limited to story review only
    @PostMapping("/stories/{id}/review")
    public StoryResponse reviewStory(
            @PathVariable Long id,
            @Valid @RequestBody ReviewStoryRequest request) {
        return adminService.reviewStory(id, request);
    }
}
