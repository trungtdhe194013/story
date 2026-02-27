package org.com.story.service;

import org.com.story.dto.request.ReportRequest;
import org.com.story.dto.response.ReportResponse;

import java.util.List;

public interface ReportService {
    ReportResponse createReport(ReportRequest request);
    List<ReportResponse> getMyReports();
    List<ReportResponse> getAllReports();
    List<ReportResponse> getPendingReports();
    ReportResponse resolveReport(Long id);
}

