package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.response.StreakResponse;
import org.com.story.service.StreakService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/streak")
@RequiredArgsConstructor
@Tag(name = "Streak Controller", description = "Check-in hàng ngày & nhận thưởng streak")
public class StreakController {

    private final StreakService streakService;

    @PostMapping("/check-in")
    @Operation(
            summary = "Check-in hàng ngày",
            description = """
                    User check-in một lần mỗi ngày để nhận coin thưởng.
                    
                    **Coin nhận được mỗi lần check-in:**
                    - Cơ bản: **5 coin** mỗi ngày
                    - Mốc streak đặc biệt (cộng thêm vào 5 coin cơ bản):
                      - Ngày 1  → +0 coin (chỉ 5 coin cơ bản)
                      - Ngày 3  → +10 coin
                      - Ngày 7  → +45 coin
                      - Ngày 14 → +95 coin
                      - Ngày 30 → +295 coin
                      - Ngày 100 → +995 coin
                    
                    **Lỗi:**
                    - `400` nếu đã check-in hôm nay rồi (`"Bạn đã check-in hôm nay rồi!"`)
                    
                    **Vỡ streak:** Nếu bỏ 1 ngày không check-in, streak về 1 từ ngày hôm sau.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public StreakResponse checkIn() {
        return streakService.checkIn();
    }

    @GetMapping("/status")
    @Operation(
            summary = "Xem trạng thái streak hiện tại",
            description = """
                    Lấy thông tin streak của user mà không check-in.
                    Dùng để hiển thị trên UI: streak hiện tại, đã check-in hôm nay chưa, v.v.
                    
                    **Response:**
                    - `currentStreak`: streak ngày hiện tại
                    - `longestStreak`: streak dài nhất từng đạt
                    - `lastCheckInDate`: ngày check-in gần nhất (`yyyy-MM-dd`)
                    - `hasClaimedToday`: `true` nếu đã check-in hôm nay
                    - `coinEarned`: luôn là `0` (không phát sinh coin ở endpoint này)
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public StreakResponse getStatus() {
        return streakService.getMyStreak();
    }
}

