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

    // Authenticated — hoàn thành mission và nhận coin
    MissionResponse completeMission(Long missionId);
}
