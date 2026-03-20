package org.com.story.dto.response;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StreakResponse {
    private Integer currentStreak;
    private Integer longestStreak;
    private LocalDate lastCheckInDate;
    private Boolean hasClaimedToday;
    private Long coinEarned;      // coin nhận từ check-in hôm nay (0 nếu chưa check-in)
    private String message;
}

