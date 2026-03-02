package org.com.story.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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

    // Extended profile
    private String avatarUrl;
    private String bio;
    private String phone;
    private LocalDate dateOfBirth;
    private String gender;
    private String location;

    // Wallet info
    private Long walletBalance;

    // Stats
    private Integer totalFollowedStories;
    private Integer totalPurchasedChapters;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
