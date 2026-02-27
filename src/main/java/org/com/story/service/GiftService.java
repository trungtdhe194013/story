package org.com.story.service;

import org.com.story.dto.request.GiftRequest;
import org.com.story.dto.response.GiftResponse;

import java.util.List;

public interface GiftService {
    GiftResponse sendGift(GiftRequest request);
    List<GiftResponse> getMySentGifts();
    List<GiftResponse> getMyReceivedGifts();
    List<GiftResponse> getGiftsByStory(Long storyId);
}

