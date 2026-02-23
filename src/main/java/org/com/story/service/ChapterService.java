package org.com.story.service;

import org.com.story.dto.request.ChapterRequest;
import org.com.story.dto.response.ChapterResponse;

import java.util.List;

public interface ChapterService {
    ChapterResponse createChapter(Long storyId, ChapterRequest request);
    ChapterResponse getChapter(Long id);
    List<ChapterResponse> getChaptersByStory(Long storyId);
    ChapterResponse updateChapter(Long id, ChapterRequest request);
    void deleteChapter(Long id);
    ChapterResponse publishChapter(Long id);
    ChapterResponse purchaseChapter(Long id);
}
