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

import java.time.LocalDateTime;
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

    // ─────────────────────────────────────────────
    // Tạo yêu cầu rút tiền — FREEZE coin ngay lập tức
    // ─────────────────────────────────────────────
    @Override
    public WithdrawRequestResponse createWithdrawRequest(WithdrawRequestDto request) {
        User currentUser = userService.getCurrentUser();

        Wallet wallet = walletRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Wallet not found"));

        // Kiểm tra số dư khả dụng (trừ phần đang bị lock)
        long available = wallet.getBalance() - wallet.getLockedBalance();
        if (available < request.getAmount()) {
            throw new BadRequestException(
                    "Số coin khả dụng không đủ. Hiện có: " + available + " coin, cần: " + request.getAmount());
        }

        // Kiểm tra không có request PENDING nào đang chờ
        long pendingCount = withdrawRequestRepository.findByStatus("PENDING")
                .stream().filter(w -> w.getUser().getId().equals(currentUser.getId())).count();
        if (pendingCount > 0) {
            throw new BadRequestException("Bạn đang có yêu cầu rút tiền chờ xử lý. Vui lòng chờ admin duyệt.");
        }

        // Freeze coin ngay lập tức (lock để không tiêu được)
        wallet.setLockedBalance(wallet.getLockedBalance() + request.getAmount());
        walletRepository.save(wallet);

        WithdrawRequest wr = new WithdrawRequest();
        wr.setUser(currentUser);
        wr.setAmount(request.getAmount());
        wr.setBankName(request.getBankName());
        wr.setBankAccount(request.getBankAccount());
        wr.setBankOwner(request.getBankOwner());
        wr.setStatus("PENDING");

        WithdrawRequest saved = withdrawRequestRepository.save(wr);

        // Ghi giao dịch LOCK
        walletService.createTransaction(currentUser.getId(), -request.getAmount(), "WITHDRAW_LOCK", saved.getId());

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WithdrawRequestResponse> getMyWithdrawRequests() {
        User currentUser = userService.getCurrentUser();
        return withdrawRequestRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WithdrawRequestResponse> getAllWithdrawRequests() {
        return withdrawRequestRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WithdrawRequestResponse> getPendingWithdrawRequests() {
        return withdrawRequestRepository.findByStatus("PENDING")
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // Admin APPROVE — coin đã bị freeze, chỉ cần trừ thật và mở lock
    // ─────────────────────────────────────────────
    @Override
    public WithdrawRequestResponse approveWithdrawRequest(Long id) {
        WithdrawRequest wr = withdrawRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Withdraw request not found"));

        if (!"PENDING".equals(wr.getStatus())) {
            throw new BadRequestException("Yêu cầu không ở trạng thái PENDING");
        }

        User admin = userService.getCurrentUser();
        Wallet wallet = walletRepository.findByUserId(wr.getUser().getId())
                .orElseThrow(() -> new NotFoundException("Wallet not found"));

        // Trừ balance thật + giải phóng lock
        wallet.setBalance(wallet.getBalance() - wr.getAmount());
        wallet.setLockedBalance(wallet.getLockedBalance() - wr.getAmount());
        walletRepository.save(wallet);

        wr.setStatus("APPROVED");
        wr.setProcessedBy(admin);
        wr.setProcessedAt(LocalDateTime.now());
        WithdrawRequest updated = withdrawRequestRepository.save(wr);

        // Ghi giao dịch WITHDRAW thật
        walletService.createTransaction(wr.getUser().getId(), -wr.getAmount(), "WITHDRAW", wr.getId());

        return mapToResponse(updated);
    }

    // ─────────────────────────────────────────────
    // Admin REJECT — hoàn trả coin đã freeze
    // ─────────────────────────────────────────────
    @Override
    public WithdrawRequestResponse rejectWithdrawRequest(Long id, String rejectedReason) {
        WithdrawRequest wr = withdrawRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Withdraw request not found"));

        if (!"PENDING".equals(wr.getStatus())) {
            throw new BadRequestException("Yêu cầu không ở trạng thái PENDING");
        }

        User admin = userService.getCurrentUser();
        Wallet wallet = walletRepository.findByUserId(wr.getUser().getId())
                .orElseThrow(() -> new NotFoundException("Wallet not found"));

        // Hoàn trả locked coin về available
        wallet.setLockedBalance(wallet.getLockedBalance() - wr.getAmount());
        walletRepository.save(wallet);

        wr.setStatus("REJECTED");
        wr.setRejectedReason(rejectedReason);
        wr.setProcessedBy(admin);
        wr.setProcessedAt(LocalDateTime.now());
        WithdrawRequest updated = withdrawRequestRepository.save(wr);

        // Ghi giao dịch hoàn trả
        walletService.createTransaction(wr.getUser().getId(), wr.getAmount(), "WITHDRAW_REFUND", wr.getId());

        return mapToResponse(updated);
    }

    private WithdrawRequestResponse mapToResponse(WithdrawRequest wr) {
        return WithdrawRequestResponse.builder()
                .id(wr.getId())
                .userId(wr.getUser().getId())
                .userName(wr.getUser().getFullName())
                .amount(wr.getAmount())
                .status(wr.getStatus())
                .bankName(wr.getBankName())
                .bankAccount(wr.getBankAccount())
                .bankOwner(wr.getBankOwner())
                .processedById(wr.getProcessedBy() != null ? wr.getProcessedBy().getId() : null)
                .processedByName(wr.getProcessedBy() != null ? wr.getProcessedBy().getFullName() : null)
                .processedAt(wr.getProcessedAt())
                .rejectedReason(wr.getRejectedReason())
                .createdAt(wr.getCreatedAt())
                .build();
    }
}

