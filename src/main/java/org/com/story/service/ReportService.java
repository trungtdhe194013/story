package org.com.story.service;

import org.com.story.dto.request.ReportRequest;
import org.com.story.dto.request.ResolveReportRequest;
import org.com.story.dto.response.ReportResponse;

import java.util.List;

public interface ReportService {
    ReportResponse createReport(ReportRequest request);
    List<ReportResponse> getMyReports();

    // ── Admin views ──────────────────────────────────────────────
    List<ReportResponse> getAllReports();
    List<ReportResponse> getPendingReports();
    List<ReportResponse> getReportsByTargetType(String targetType);
    List<ReportResponse> getReportsForTarget(String targetType, Long targetId);
    ReportResponse getReportById(Long id);

    // ── Admin actions ────────────────────────────────────────────
    ReportResponse resolveReport(Long id, ResolveReportRequest request);
}

