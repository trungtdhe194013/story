package org.com.story.repository;

import org.com.story.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<WalletTransaction> findByUserIdAndType(Long userId, String type);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransaction t WHERE t.type = :type AND t.createdAt BETWEEN :from AND :to")
    Long sumAmountByTypeAndDateRange(@Param("type") String type,
                                    @Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransaction t WHERE t.type IN :types AND t.createdAt BETWEEN :from AND :to")
    Long sumAmountByTypesAndDateRange(@Param("types") List<String> types,
                                     @Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to);

    /** Tổng coin trong tất cả ví (balance + lockedBalance) — coin đang lưu hành */
    @Query("SELECT COALESCE(SUM(w.balance + COALESCE(w.lockedBalance, 0)), 0) FROM Wallet w")
    Long sumTotalCoinInCirculation();
}

