package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.WalletTopUpRequest;
import org.com.story.dto.response.WalletResponse;
import org.com.story.dto.response.WalletTransactionResponse;
import org.com.story.service.WalletService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet Controller", description = "Quản lý ví tiền")
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    @Operation(summary = "Get my wallet", description = "Xem thông tin ví của tôi",
            security = @SecurityRequirement(name = "bearerAuth"))
    public WalletResponse getMyWallet() {
        return walletService.getMyWallet();
    }

    @PostMapping("/topup")
    @Operation(summary = "Top up wallet", description = "Nạp tiền vào ví",
            security = @SecurityRequirement(name = "bearerAuth"))
    public WalletResponse topUp(@Valid @RequestBody WalletTopUpRequest request) {
        return walletService.topUp(request);
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get my transactions", description = "Xem lịch sử giao dịch của tôi",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<WalletTransactionResponse> getMyTransactions() {
        return walletService.getMyTransactions();
    }
}

