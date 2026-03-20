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

    /** Khoá coin vào escrow khi Author tạo EditRequest */
    void lockCoins(Long userId, Long amount, Long editRequestId);

    /** Giải phóng coin escrow, chuyển sang ví Editor khi Author approve */
    void releaseCoinsToEditor(Long authorId, Long editorId, Long amount, Long editRequestId);

    /** Hoàn trả coin escrow về ví Author khi Author cancel */
    void refundCoinsToAuthor(Long authorId, Long amount, Long editRequestId);
}

