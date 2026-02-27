package org.com.story.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private Set<String> roles;
    private String provider;
    private Boolean enabled;

    // Wallet info
    private Long walletBalance;

    // Stats
    private Integer totalFollowedStories;
    private Integer totalPurchasedChapters;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
