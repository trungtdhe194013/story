package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.WithdrawRequestDto;
import org.com.story.dto.response.WithdrawRequestResponse;
import org.com.story.service.WithdrawRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/withdraw-requests")
@RequiredArgsConstructor
@Tag(name = "Withdraw Request Controller", description = "Yêu cầu rút tiền")
public class WithdrawRequestController {

    private final WithdrawRequestService withdrawRequestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create withdraw request", description = "Tạo yêu cầu rút tiền",
            security = @SecurityRequirement(name = "bearerAuth"))
    public WithdrawRequestResponse createWithdrawRequest(@Valid @RequestBody WithdrawRequestDto request) {
        return withdrawRequestService.createWithdrawRequest(request);
    }

    @GetMapping("/my")
    @Operation(summary = "Get my withdraw requests", description = "Xem danh sách yêu cầu rút tiền của tôi",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<WithdrawRequestResponse> getMyWithdrawRequests() {
        return withdrawRequestService.getMyWithdrawRequests();
    }
}

