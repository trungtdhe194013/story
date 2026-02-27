package org.com.story.repository;

import org.com.story.entity.WithdrawRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WithdrawRequestRepository extends JpaRepository<WithdrawRequest, Long> {
    List<WithdrawRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<WithdrawRequest> findByStatus(String status);
    List<WithdrawRequest> findAllByOrderByCreatedAtDesc();
}

