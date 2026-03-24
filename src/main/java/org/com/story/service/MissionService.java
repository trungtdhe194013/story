package org.com.story.service;

import org.com.story.dto.request.MissionRequest;
import org.com.story.dto.response.MissionResponse;

import java.util.List;

public interface MissionService {
    // Admin
    MissionResponse createMission(MissionRequest request);
    MissionResponse updateMission(Long id, MissionRequest request);
    void deleteMission(Long id);

    // Public — danh sách tất cả mission (kèm trạng thái completed nếu đã login)
    List<MissionResponse> getAllMissions();

    // Authenticated — danh sách mission với tiến độ đầy đủ (progress, completedAt)
    List<MissionResponse> getMyMissions();

    // Authenticated — hoàn thành mission và nhận coin (thủ công — dành cho admin/test)
    MissionResponse completeMission(Long missionId);

    /**
     * Tự động theo dõi tiến độ nhiệm vụ khi user thực hiện một hành động.
     * Gọi sau khi user: đăng nhập, đọc chương, bình luận, follow, mua chương, tặng quà.
     *
     * @param action  Một trong: LOGIN | READ_CHAPTER | COMMENT | FOLLOW_STORY | BUY_CHAPTER | SEND_GIFT
     */
    void trackMissionAction(String action);

    /** Gọi bởi Cron Job — reset progress của tất cả DAILY missions vào nửa đêm */
    void resetDailyMissions();
}
