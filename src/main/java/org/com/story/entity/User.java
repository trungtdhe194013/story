package org.com.story.entity;

import jakarta.persistence.*;
import lombok.*;
import org.com.story.common.AuthProvider;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;
@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // dùng email làm username
    @Column(nullable = false, unique = true)
    private String email;

    @Column(unique = true)
    private String username;

    // null nếu login Google
    @Column
    private String password;

    private String fullName;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

    @Enumerated(EnumType.STRING)
    private AuthProvider provider; // LOCAL, GOOGLE

    private String providerId; // google sub

    private Boolean enabled = true;

    // Extended profile fields
    private String avatarUrl;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 20)
    private String phone;

    private java.time.LocalDate dateOfBirth;

    @Column(length = 10)
    private String gender; // MALE, FEMALE, OTHER

    @Column(length = 100)
    private String location;

    // null = không bị ban, set giá trị = bị ban đến thời điểm đó
    private LocalDateTime banUntil;

    @Column(columnDefinition = "TEXT")
    private String banReason;

    /** null = không bị hạn chế bình luận; có giá trị = bị hạn chế đến thời điểm đó */
    private LocalDateTime commentBanUntil;

    /** Tổng coin tác giả đã kiếm được từ bán chương + nhận quà */
    private Long totalEarnedCoin = 0L;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToMany
    @JoinTable(
            name = "follows",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "story_id")
    )
    private Set<Story> followedStories;
}
