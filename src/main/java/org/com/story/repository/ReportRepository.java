package org.com.story.repository;

import org.com.story.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByStatus(String status);
    List<Report> findByReporterIdOrderByCreatedAtDesc(Long reporterId);
    List<Report> findAllByOrderByCreatedAtDesc();
}

