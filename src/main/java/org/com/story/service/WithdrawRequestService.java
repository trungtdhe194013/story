package org.com.story.service;

import org.com.story.dto.request.WithdrawRequestDto;
import org.com.story.dto.response.WithdrawRequestResponse;

import java.util.List;

public interface WithdrawRequestService {
    WithdrawRequestResponse createWithdrawRequest(WithdrawRequestDto request);
    List<WithdrawRequestResponse> getMyWithdrawRequests();
    List<WithdrawRequestResponse> getAllWithdrawRequests();
    List<WithdrawRequestResponse> getPendingWithdrawRequests();
    WithdrawRequestResponse approveWithdrawRequest(Long id);
    WithdrawRequestResponse rejectWithdrawRequest(Long id, String rejectedReason);
}

