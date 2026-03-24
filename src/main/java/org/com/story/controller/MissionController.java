package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.response.MissionResponse;
import org.com.story.service.MissionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
@Tag(name = "Mission Controller", description = "Nhiệm vụ / Quest hàng ngày")
public class MissionController {

    private final MissionService missionService;

    @GetMapping
    @Operation(summary = "Danh sách tất cả mission (public)",
            description = """
                    Trả về tất cả mission đang active.
                    Nếu đã đăng nhập: kèm trạng thái `completed` của user.
                    Dùng cho trang giới thiệu nhiệm vụ (không cần login).
                    """)
    public List<MissionResponse> getAllMissions() {
        return missionService.getAllMissions();
    }

    @GetMapping("/my")
    @Operation(summary = "Nhiệm vụ của tôi (kèm tiến độ đầy đủ)",
            description = """
                    Trả về danh sách mission với đầy đủ trạng thái của user:
                    - `progress`: tiến độ hiện tại (ví dụ: đọc 3/5 chương)
                    - `completed`: đã hoàn thành và nhận thưởng chưa
                    - `completedAt`: thời điểm hoàn thành
                    - `targetCount`: cần làm bao nhiêu lần để hoàn thành
                    
                    Dùng để hiển thị trang Nhiệm Vụ khi user đã đăng nhập.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<MissionResponse> getMyMissions() {
        return missionService.getMyMissions();
    }

    @PostMapping("/{missionId}/claim")
    @Operation(summary = "Nhận thưởng nhiệm vụ",
            description = """
                    User bấm "Nhận thưởng" khi nhiệm vụ đã đủ điều kiện (status = CLAIMABLE).
                    - Coin được cộng vào ví ngay lập tức.
                    - Status chuyển sang COMPLETED.
                    
                    Lỗi nếu:
                    - Chưa đủ tiến độ (400 "Chưa đủ tiến độ")
                    - Đã nhận rồi (400 "Đã nhận thưởng rồi")
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    public MissionResponse claimMission(@PathVariable Long missionId) {
        return missionService.claimMissionReward(missionId);
    }

    @PostMapping("/{missionId}/complete")
    @Operation(summary = "[Admin/Test] Force complete mission",
            description = "Admin/test — force hoàn thành và nhận coin bất kể progress. Dùng khi debug.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public MissionResponse completeMission(@PathVariable Long missionId) {
        return missionService.completeMission(missionId);
    }
}
