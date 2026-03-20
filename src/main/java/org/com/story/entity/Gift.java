package org.com.story.entity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.*;
import lombok.*;
import org.com.story.common.AuthProvider;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "gifts")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Gift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User fromUser;

    @ManyToOne
    private User toUser;

    @ManyToOne
    private Story story;

    private Long amount;

    /** Lời nhắn kèm quà */
    @Column(columnDefinition = "TEXT")
    private String message;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
