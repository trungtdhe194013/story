package org.com.story.repository;

import org.com.story.entity.WithdrawRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WithdrawRequestRepository extends JpaRepository<WithdrawRequest, Long> {
    List<WithdrawRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<WithdrawRequest> findByStatus(String status);
    List<WithdrawRequest> findAllByOrderByCreatedAtDesc();

    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM WithdrawRequest w WHERE w.status = :status")
    Long sumAmountByStatus(@Param("status") String status);

    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM WithdrawRequest w WHERE w.status = 'APPROVED' AND w.processedAt BETWEEN :from AND :to")
    Long sumApprovedAmountByDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}

