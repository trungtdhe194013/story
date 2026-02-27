package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.ReportRequest;
import org.com.story.dto.response.ReportResponse;
import org.com.story.entity.Report;
import org.com.story.entity.User;
import org.com.story.exception.BadRequestException;
import org.com.story.exception.NotFoundException;
import org.com.story.repository.ReportRepository;
import org.com.story.service.ReportService;
import org.com.story.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final UserService userService;

    private static final Set<String> VALID_TARGET_TYPES = Set.of("STORY", "CHAPTER", "COMMENT");

    @Override
    public ReportResponse createReport(ReportRequest request) {
        User currentUser = userService.getCurrentUser();

        if (!VALID_TARGET_TYPES.contains(request.getTargetType())) {
            throw new BadRequestException("Invalid target type. Must be STORY, CHAPTER, or COMMENT");
        }

        Report report = new Report();
        report.setReporter(currentUser);
        report.setTargetType(request.getTargetType());
        report.setTargetId(request.getTargetId());
        report.setReason(request.getReason());
        report.setStatus("PENDING");

        Report savedReport = reportRepository.save(report);
        return mapToResponse(savedReport);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> getMyReports() {
        User currentUser = userService.getCurrentUser();
        return reportRepository.findByReporterIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> getAllReports() {
        return reportRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> getPendingReports() {
        return reportRepository.findByStatus("PENDING")
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ReportResponse resolveReport(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Report not found"));

        if ("RESOLVED".equals(report.getStatus())) {
            throw new BadRequestException("Report is already resolved");
        }

        report.setStatus("RESOLVED");
        Report updatedReport = reportRepository.save(report);
        return mapToResponse(updatedReport);
    }

    private ReportResponse mapToResponse(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .reporterId(report.getReporter().getId())
                .reporterName(report.getReporter().getFullName())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reason(report.getReason())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .build();
    }
}

