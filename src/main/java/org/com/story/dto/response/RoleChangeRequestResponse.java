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
public class RoleChangeRequestResponse {
    private Long id;
    private Long userId;
    private String userEmail;
    private String userFullName;
    private String currentRole;
    private String requestedRole;
    private String status;       // PENDING, APPROVED, REJECTED
    private String reason;
    private String adminNote;
    private String reviewedBy;   // email của admin đã duyệt
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

