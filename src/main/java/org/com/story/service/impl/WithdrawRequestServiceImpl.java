package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.WithdrawRequestDto;
import org.com.story.dto.response.WithdrawRequestResponse;
import org.com.story.entity.User;
import org.com.story.entity.Wallet;
import org.com.story.entity.WithdrawRequest;
import org.com.story.exception.BadRequestException;
import org.com.story.exception.NotFoundException;
import org.com.story.repository.WalletRepository;
import org.com.story.repository.WithdrawRequestRepository;
import org.com.story.service.UserService;
import org.com.story.service.WalletService;
import org.com.story.service.WithdrawRequestService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WithdrawRequestServiceImpl implements WithdrawRequestService {

    private final WithdrawRequestRepository withdrawRequestRepository;
    private final WalletRepository walletRepository;
    private final UserService userService;
    private final WalletService walletService;

    @Override
    public WithdrawRequestResponse createWithdrawRequest(WithdrawRequestDto request) {
        User currentUser = userService.getCurrentUser();

        Wallet wallet = walletRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Wallet not found"));

        if (wallet.getBalance() < request.getAmount()) {
            throw new BadRequestException("Insufficient balance. Current balance: " + wallet.getBalance());
        }

        WithdrawRequest wr = new WithdrawRequest();
        wr.setUser(currentUser);
        wr.setAmount(request.getAmount());
        wr.setStatus("PENDING");

        WithdrawRequest saved = withdrawRequestRepository.save(wr);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WithdrawRequestResponse> getMyWithdrawRequests() {
        User currentUser = userService.getCurrentUser();
        return withdrawRequestRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WithdrawRequestResponse> getAllWithdrawRequests() {
        return withdrawRequestRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WithdrawRequestResponse> getPendingWithdrawRequests() {
        return withdrawRequestRepository.findByStatus("PENDING")
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public WithdrawRequestResponse approveWithdrawRequest(Long id) {
        WithdrawRequest wr = withdrawRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Withdraw request not found"));

        if (!"PENDING".equals(wr.getStatus())) {
            throw new BadRequestException("Withdraw request is not in PENDING status");
        }

        // Trừ tiền trong ví
        Wallet wallet = walletRepository.findByUserId(wr.getUser().getId())
                .orElseThrow(() -> new NotFoundException("Wallet not found"));

        if (wallet.getBalance() < wr.getAmount()) {
            throw new BadRequestException("User has insufficient balance");
        }

        wallet.setBalance(wallet.getBalance() - wr.getAmount());
        walletRepository.save(wallet);

        wr.setStatus("APPROVED");
        WithdrawRequest updated = withdrawRequestRepository.save(wr);

        // Ghi giao dịch
        walletService.createTransaction(wr.getUser().getId(), -wr.getAmount(), "WITHDRAW", wr.getId());

        return mapToResponse(updated);
    }

    @Override
    public WithdrawRequestResponse rejectWithdrawRequest(Long id) {
        WithdrawRequest wr = withdrawRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Withdraw request not found"));

        if (!"PENDING".equals(wr.getStatus())) {
            throw new BadRequestException("Withdraw request is not in PENDING status");
        }

        wr.setStatus("REJECTED");
        WithdrawRequest updated = withdrawRequestRepository.save(wr);
        return mapToResponse(updated);
    }

    private WithdrawRequestResponse mapToResponse(WithdrawRequest wr) {
        return WithdrawRequestResponse.builder()
                .id(wr.getId())
                .userId(wr.getUser().getId())
                .userName(wr.getUser().getFullName())
                .amount(wr.getAmount())
                .status(wr.getStatus())
                .createdAt(wr.getCreatedAt())
                .build();
    }
}

