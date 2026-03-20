package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.RatingRequest;
import org.com.story.dto.response.RatingResponse;
import org.com.story.service.RatingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
@Tag(name = "Rating Controller", description = "Quản lý đánh giá truyện")
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Rate a story", description = """
            Đánh giá truyện (1-5 sao). Nếu đã đánh giá trước đó, sẽ cập nhật lại điểm.
            - `storyId`: ID truyện muốn đánh giá (bắt buộc)
            - `score`: Điểm từ 1 đến 5 (bắt buộc)
            - `review`: Nhận xét (tùy chọn)
            """,
            security = @SecurityRequirement(name = "bearerAuth"))
    public RatingResponse rateStory(@Valid @RequestBody RatingRequest request) {
        return ratingService.rateStory(request);
    }

    @GetMapping("/story/{storyId}")
    @Operation(summary = "Get ratings by story", description = "Lấy tất cả đánh giá của 1 truyện, sắp xếp theo thời gian mới nhất")
    public List<RatingResponse> getRatingsByStory(@PathVariable Long storyId) {
        return ratingService.getRatingsByStory(storyId);
    }

    @GetMapping("/my/{storyId}")
    @Operation(summary = "Get my rating for a story", description = "Lấy đánh giá của người dùng hiện tại cho 1 truyện",
            security = @SecurityRequirement(name = "bearerAuth"))
    public RatingResponse getMyRating(@PathVariable Long storyId) {
        return ratingService.getMyRating(storyId);
    }
}

