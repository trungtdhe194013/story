package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.GiftRequest;
import org.com.story.dto.response.GiftResponse;
import org.com.story.service.GiftService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gifts")
@RequiredArgsConstructor
@Tag(name = "Gift Controller", description = "Tặng quà cho tác giả")
public class GiftController {

    private final GiftService giftService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Send gift", description = "Tặng coin cho tác giả của truyện (trừ tiền người gửi, cộng tiền tác giả)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public GiftResponse sendGift(@Valid @RequestBody GiftRequest request) {
        return giftService.sendGift(request);
    }

    @GetMapping("/sent")
    @Operation(summary = "Get sent gifts", description = "Xem danh sách quà đã tặng",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<GiftResponse> getMySentGifts() {
        return giftService.getMySentGifts();
    }

    @GetMapping("/received")
    @Operation(summary = "Get received gifts", description = "Xem danh sách quà đã nhận",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<GiftResponse> getMyReceivedGifts() {
        return giftService.getMyReceivedGifts();
    }

    @GetMapping("/story/{storyId}")
    @Operation(summary = "Get gifts by story", description = "Xem danh sách quà của một truyện")
    public List<GiftResponse> getGiftsByStory(@PathVariable Long storyId) {
        return giftService.getGiftsByStory(storyId);
    }
}

