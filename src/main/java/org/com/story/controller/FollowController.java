package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.response.FollowResponse;
import org.com.story.dto.response.StoryResponse;
import org.com.story.service.FollowService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
@Tag(name = "Follow Controller", description = "Theo dõi truyện")
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{storyId}")
    @Operation(summary = "Toggle follow", description = "Follow/Unfollow truyện (nếu đang follow → unfollow, và ngược lại)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public FollowResponse toggleFollow(@PathVariable Long storyId) {
        return followService.toggleFollow(storyId);
    }

    @GetMapping
    @Operation(summary = "Get followed stories", description = "Xem danh sách truyện đang theo dõi",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<StoryResponse> getFollowedStories() {
        return followService.getFollowedStories();
    }

    @GetMapping("/{storyId}/status")
    @Operation(summary = "Check follow status", description = "Kiểm tra đã follow truyện chưa",
            security = @SecurityRequirement(name = "bearerAuth"))
    public boolean isFollowing(@PathVariable Long storyId) {
        return followService.isFollowing(storyId);
    }

    @GetMapping("/story/{storyId}/count")
    @Operation(summary = "Get follow count", description = "Đếm số người đang follow một truyện (public, không cần đăng nhập)")
    public long getFollowCount(@PathVariable Long storyId) {
        return followService.getFollowCount(storyId);
    }
}

