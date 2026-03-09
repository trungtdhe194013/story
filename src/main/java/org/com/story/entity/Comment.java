package org.com.story.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;


    @ManyToOne(fetch = FetchType.LAZY)
    private Comment parent;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Boolean hidden = false; // true khi bị ẩn do vi phạm

    @CreationTimestamp
    private LocalDateTime createdAt;
}
