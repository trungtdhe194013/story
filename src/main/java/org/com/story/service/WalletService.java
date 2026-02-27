package org.com.story.service;

import org.com.story.dto.request.WalletTopUpRequest;
import org.com.story.dto.response.WalletResponse;
import org.com.story.dto.response.WalletTransactionResponse;

import java.util.List;

public interface WalletService {
    WalletResponse getMyWallet();
    WalletResponse topUp(WalletTopUpRequest request);
    List<WalletTransactionResponse> getMyTransactions();
    void createTransaction(Long userId, Long amount, String type, Long refId);
}

