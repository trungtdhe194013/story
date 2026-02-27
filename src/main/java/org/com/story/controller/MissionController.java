package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.MissionRequest;
import org.com.story.dto.response.MissionResponse;
import org.com.story.service.MissionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
@Tag(name = "Mission Controller", description = "Nhiệm vụ / Quest hàng ngày")
public class MissionController {

    private final MissionService missionService;

    @GetMapping
    @Operation(summary = "Get all missions", description = "Xem tất cả nhiệm vụ (kèm trạng thái hoàn thành của user hiện tại)")
    public List<MissionResponse> getAllMissions() {
        return missionService.getAllMissions();
    }

    @PostMapping("/{missionId}/complete")
    @Operation(summary = "Complete mission", description = "Hoàn thành nhiệm vụ và nhận thưởng coin",
            security = @SecurityRequirement(name = "bearerAuth"))
    public MissionResponse completeMission(@PathVariable Long missionId) {
        return missionService.completeMission(missionId);
    }
}

