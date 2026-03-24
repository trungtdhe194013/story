package org.com.story.service;

import org.com.story.dto.request.ChapterRequest;
import org.com.story.dto.request.ScheduleChapterRequest;
import org.com.story.dto.response.ChapterResponse;

import java.util.List;

public interface ChapterService {
    ChapterResponse createChapter(Long storyId, ChapterRequest request);
    ChapterResponse getChapter(Long id);
    List<ChapterResponse> getChaptersByStory(Long storyId);
    ChapterResponse updateChapter(Long id, ChapterRequest request);
    void deleteChapter(Long id);

    /** Author nộp chapter lên Reviewer (DRAFT/EDITED → PENDING_REVIEW) */
    ChapterResponse submitForReview(Long id);

    /** Author tự publish ngay sau khi Reviewer đã APPROVE (APPROVED → PUBLISHED) */
    ChapterResponse publishApprovedChapter(Long id);

    /**
     * Author hẹn lịch publish vào thời điểm cụ thể trong tương lai.
     * Status: APPROVED → SCHEDULED. Cron job sẽ tự đổi sang PUBLISHED đúng giờ.
     */
    ChapterResponse schedulePublish(Long id, ScheduleChapterRequest request);

    ChapterResponse purchaseChapter(Long id);
}
