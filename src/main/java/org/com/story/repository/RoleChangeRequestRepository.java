package org.com.story.repository;

import org.com.story.entity.RoleChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleChangeRequestRepository extends JpaRepository<RoleChangeRequest, Long> {

    List<RoleChangeRequest> findByStatus(String status);

    List<RoleChangeRequest> findByUserId(Long userId);

    // Kiểm tra user đã có request PENDING chưa
    Optional<RoleChangeRequest> findByUserIdAndStatus(Long userId, String status);
}

