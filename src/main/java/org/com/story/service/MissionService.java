package org.com.story.service;

import org.com.story.dto.request.MissionRequest;
import org.com.story.dto.response.MissionResponse;

import java.util.List;

public interface MissionService {
    // Admin
    MissionResponse createMission(MissionRequest request);
    MissionResponse updateMission(Long id, MissionRequest request);
    void deleteMission(Long id);

    // User
    List<MissionResponse> getAllMissions();
    MissionResponse completeMission(Long missionId);
}

