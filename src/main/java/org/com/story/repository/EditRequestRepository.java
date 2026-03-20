package org.com.story.repository;

import org.com.story.entity.EditRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EditRequestRepository extends JpaRepository<EditRequest, Long> {

    List<EditRequest> findByStatus(String status);

    List<EditRequest> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    List<EditRequest> findByEditorIdOrderByCreatedAtDesc(Long editorId);

    /** Kiểm tra chapter đã có request đang active chưa (OPEN / IN_PROGRESS / SUBMITTED) */
    Optional<EditRequest> findByChapterIdAndStatusIn(Long chapterId, List<String> statuses);
}

