package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.WalletTopUpRequest;
import org.com.story.dto.response.WalletResponse;
import org.com.story.dto.response.WalletTransactionResponse;
import org.com.story.entity.User;
import org.com.story.entity.Wallet;
import org.com.story.entity.WalletTransaction;
import org.com.story.exception.NotFoundException;
import org.com.story.exception.BadRequestException;
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
                .lockedBalance(wallet.getLockedBalance())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    @Override
    public WalletResponse topUp(WalletTopUpRequest request) {
        User currentUser = userService.getCurrentUser();
        Wallet wallet = getOrCreateWallet(currentUser);

        wallet.setBalance(wallet.getBalance() + request.getAmount());
        walletRepository.save(wallet);

        createTransaction(currentUser.getId(), request.getAmount(), "TOPUP", null);

        return WalletResponse.builder()
                .userId(currentUser.getId())
                .userName(currentUser.getFullName())
                .balance(wallet.getBalance())
                .lockedBalance(wallet.getLockedBalance())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    @Override
    public void lockCoins(Long userId, Long amount, Long editRequestId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Wallet wallet = getOrCreateWallet(user);

        long available = wallet.getBalance() - wallet.getLockedBalance();
        if (available < amount) {
            throw new org.com.story.exception.BadRequestException(
                    "Không đủ coin. Khả dụng: " + available + ", cần: " + amount);
        }

        wallet.setLockedBalance(wallet.getLockedBalance() + amount);
        walletRepository.save(wallet);

        createTransaction(userId, -amount, "LOCKED", editRequestId);
    }

    @Override
    public void releaseCoinsToEditor(Long authorId, Long editorId, Long amount, Long editRequestId) {
        // Giảm lockedBalance + balance của Author
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new NotFoundException("Author not found"));
        Wallet authorWallet = getOrCreateWallet(author);
        authorWallet.setLockedBalance(authorWallet.getLockedBalance() - amount);
        authorWallet.setBalance(authorWallet.getBalance() - amount);
        walletRepository.save(authorWallet);

        // Tăng balance của Editor
        User editor = userRepository.findById(editorId)
                .orElseThrow(() -> new NotFoundException("Editor not found"));
        Wallet editorWallet = getOrCreateWallet(editor);
        editorWallet.setBalance(editorWallet.getBalance() + amount);
        walletRepository.save(editorWallet);

        createTransaction(authorId, -amount, "EDIT_REWARD_PAID", editRequestId);
        createTransaction(editorId, amount, "EDIT_REWARD_RECEIVED", editRequestId);
    }

    @Override
    public void refundCoinsToAuthor(Long authorId, Long amount, Long editRequestId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new NotFoundException("Author not found"));
        Wallet wallet = getOrCreateWallet(author);
        wallet.setLockedBalance(wallet.getLockedBalance() - amount);
        // balance không đổi vì không trừ khi lock (chỉ trừ khi approve)
        walletRepository.save(wallet);

        createTransaction(authorId, amount, "EDIT_REFUND", editRequestId);
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

