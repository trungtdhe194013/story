package org.com.story.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "role_change_requests")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RoleChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "requested_role_name", nullable = false)
    private String requestedRole; // AUTHOR, EDITOR, REVIEWER, etc.

    @Column(name = "current_role_name", nullable = false)
    private String currentRole;   // role hiện tại của user

    // PENDING, APPROVED, REJECTED
    @Column(name = "request_status", nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "request_reason", columnDefinition = "TEXT")
    private String reason; // lý do user muốn đổi role

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote; // ghi chú của admin khi duyệt/từ chối

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}




