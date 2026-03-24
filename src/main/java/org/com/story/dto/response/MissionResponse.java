package org.com.story.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionResponse {
    private Long id;
    private String name;
    private String description;
    private Long rewardCoin;
    /** DAILY, READ */
    private String type;
    /** Hành động kích hoạt: LOGIN, READ_CHAPTER, COMMENT, FOLLOW_STORY, BUY_CHAPTER, SEND_GIFT */
    private String action;
    /** Số lần phải thực hiện để hoàn thành (ví dụ: đọc 5 chương) */
    private Integer targetCount;
    private String icon;
    private Integer displayOrder;
    private Boolean isActive;

    // ── Trạng thái của user hiện tại ──────────────────────────────────────────
    /** Tiến độ hiện tại (0 → targetCount) */
    private Integer progress;
    /** Đã hoàn thành VÀ đã nhận thưởng chưa */
    private Boolean completed;
    /** Đã đủ điều kiện nhận thưởng (progress >= targetCount) nhưng chưa claim */
    private Boolean canClaim;
    /**
     * Trạng thái hiển thị:
     * NOT_STARTED | IN_PROGRESS | CLAIMABLE | COMPLETED
     */
    private String status;
    /** Thời điểm hoàn thành/nhận thưởng (null nếu chưa hoàn thành) */
    private LocalDateTime completedAt;
}


