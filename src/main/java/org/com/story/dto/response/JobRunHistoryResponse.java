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
public class JobRunHistoryResponse {
    private String jobName;         // stats-aggregator | monthly-settlement
    private String triggeredBy;     // ADMIN_MANUAL | SCHEDULER
    private String status;          // SUCCESS | FAILED | RUNNING
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long durationMs;
    private String note;            // Kết quả hoặc lỗi nếu FAILED
}

