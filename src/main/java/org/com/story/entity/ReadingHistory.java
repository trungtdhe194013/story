package org.com.story.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reading_histories", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "story_id"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ReadingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    /** Chapter gần nhất user đọc */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_chapter_id")
    private Chapter lastChapter;

    @UpdateTimestamp
    private LocalDateTime lastReadAt;
}

