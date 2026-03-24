package org.com.story.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.story.service.MissionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cron job reset tiến độ tất cả DAILY missions về 0 vào lúc 00:00 mỗi ngày.
 * Người dùng cần thực hiện lại các hành động hàng ngày (login, comment, follow, v.v.)
 * để nhận thưởng coin ngày hôm sau.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyMissionResetScheduler {

    private final MissionService missionService;

    /**
     * Reset lúc 00:00 mỗi ngày.
     * cron = "giây phút giờ ngày tháng thứ"
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void resetDailyMissions() {
        log.info("⏰ [DailyMissionReset] Đang reset tiến độ các nhiệm vụ hàng ngày...");
        try {
            missionService.resetDailyMissions();
            log.info("✅ [DailyMissionReset] Reset hoàn thành.");
        } catch (Exception e) {
            log.error("❌ [DailyMissionReset] Lỗi khi reset daily missions: {}", e.getMessage(), e);
        }
    }
}

