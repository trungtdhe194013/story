package org.com.story.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.story.entity.Chapter;
import org.com.story.repository.ChapterRepository;
import org.com.story.service.NotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cron job tự động publish các chapter đã được hẹn lịch (SCHEDULED).
 * Quét DB mỗi 15 phút, tìm chapter có status = SCHEDULED và publishAt <= now,
 * đổi sang PUBLISHED và gửi notification cho follower.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledChapterPublisher {

    private final ChapterRepository chapterRepository;
    private final NotificationService notificationService;

    /** Chạy mỗi 15 phút */
    @Scheduled(fixedDelay = 15 * 60 * 1000)
    @Transactional
    public void publishScheduledChapters() {
        LocalDateTime now = LocalDateTime.now();
        List<Chapter> due = chapterRepository.findScheduledForPublish(now);

        if (due.isEmpty()) return;

        log.info("⏰ [ScheduledPublisher] Tìm thấy {} chapter cần auto-publish...", due.size());

        for (Chapter chapter : due) {
            try {
                chapter.setStatus("PUBLISHED");
                chapterRepository.save(chapter);

                // Gửi notification cho tất cả follower của truyện
                String storyTitle = chapter.getStory().getTitle();
                notificationService.sendToFollowers(
                        chapter.getStory().getId(),
                        "NEW_CHAPTER",
                        "Chương mới: " + chapter.getTitle(),
                        "Truyện '" + storyTitle + "' vừa ra chương mới: " + chapter.getTitle(),
                        chapter.getId(),
                        "CHAPTER"
                );

                log.info("✅ [ScheduledPublisher] Auto-published chapter ID={} '{}' của truyện '{}'",
                        chapter.getId(), chapter.getTitle(), storyTitle);
            } catch (Exception e) {
                log.error("❌ [ScheduledPublisher] Lỗi publish chapter ID={}: {}", chapter.getId(), e.getMessage());
            }
        }
    }
}

