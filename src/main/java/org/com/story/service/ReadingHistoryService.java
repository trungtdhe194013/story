package org.com.story.service;

import org.com.story.dto.response.ReadingHistoryResponse;

import java.util.List;

public interface ReadingHistoryService {
    void recordReading(Long userId, Long storyId, Long chapterId);
    List<ReadingHistoryResponse> getMyReadingHistory();
}

