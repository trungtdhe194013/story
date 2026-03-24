package org.com.story.repository;

import org.com.story.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    Optional<UserBlock> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    /** Danh sách người bị chặn bởi một tác giả */
    List<UserBlock> findByBlockerId(Long blockerId);

    /** Danh sách tác giả đã chặn một user */
    List<UserBlock> findByBlockedId(Long blockedId);
}

