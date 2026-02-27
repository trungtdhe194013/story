package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.ReportRequest;
import org.com.story.dto.response.ReportResponse;
import org.com.story.service.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Report Controller", description = "Báo cáo vi phạm")
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create report", description = "Báo cáo vi phạm (STORY, CHAPTER, COMMENT)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ReportResponse createReport(@Valid @RequestBody ReportRequest request) {
        return reportService.createReport(request);
    }

    @GetMapping("/my")
    @Operation(summary = "Get my reports", description = "Xem danh sách báo cáo của tôi",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<ReportResponse> getMyReports() {
        return reportService.getMyReports();
    }
}

