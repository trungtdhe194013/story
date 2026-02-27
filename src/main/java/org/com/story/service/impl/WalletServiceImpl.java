package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.WalletTopUpRequest;
import org.com.story.dto.response.WalletResponse;
import org.com.story.dto.response.WalletTransactionResponse;
import org.com.story.entity.User;
import org.com.story.entity.Wallet;
import org.com.story.entity.WalletTransaction;
import org.com.story.exception.NotFoundException;
import org.com.story.repository.UserRepository;
import org.com.story.repository.WalletRepository;
import org.com.story.repository.WalletTransactionRepository;
import org.com.story.service.UserService;
import org.com.story.service.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getMyWallet() {
        User currentUser = userService.getCurrentUser();
        Wallet wallet = getOrCreateWallet(currentUser);

        return WalletResponse.builder()
                .userId(currentUser.getId())
                .userName(currentUser.getFullName())
                .balance(wallet.getBalance())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    @Override
    public WalletResponse topUp(WalletTopUpRequest request) {
        User currentUser = userService.getCurrentUser();
        Wallet wallet = getOrCreateWallet(currentUser);

        wallet.setBalance(wallet.getBalance() + request.getAmount());
        walletRepository.save(wallet);

        // Ghi lại giao dịch
        createTransaction(currentUser.getId(), request.getAmount(), "TOPUP", null);

        return WalletResponse.builder()
                .userId(currentUser.getId())
                .userName(currentUser.getFullName())
                .balance(wallet.getBalance())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> getMyTransactions() {
        User currentUser = userService.getCurrentUser();

        return walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::mapTransactionToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void createTransaction(Long userId, Long amount, String type, Long refId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        WalletTransaction transaction = new WalletTransaction();
        transaction.setUser(user);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setRefId(refId);

        walletTransactionRepository.save(transaction);
    }

    private Wallet getOrCreateWallet(User user) {
        return walletRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Wallet newWallet = new Wallet();
                    newWallet.setUser(user);
                    newWallet.setBalance(0L);
                    return walletRepository.save(newWallet);
                });
    }

    private WalletTransactionResponse mapTransactionToResponse(WalletTransaction tx) {
        return WalletTransactionResponse.builder()
                .id(tx.getId())
                .userId(tx.getUser().getId())
                .amount(tx.getAmount())
                .type(tx.getType())
                .refId(tx.getRefId())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}

