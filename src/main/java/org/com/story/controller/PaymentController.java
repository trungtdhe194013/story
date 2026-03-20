package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.CreatePaymentLinkRequest;
import org.com.story.dto.response.CoinPackageResponse;
import org.com.story.dto.response.PaymentLinkResponse;
import org.com.story.dto.response.PaymentOrderResponse;
import org.com.story.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Tag(name = "Payment Controller", description = "Nạp coin qua PayOS")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * GET /api/payment/packages — danh sách gói coin (public)
     */
    @GetMapping("/packages")
    @Operation(summary = "Danh sách gói nạp coin",
            description = "Trả về 5 gói coin với giá VND và số coin nhận, không cần đăng nhập")
    public List<CoinPackageResponse> getPackages() {
        return paymentService.getAllPackages();
    }

    /**
     * POST /api/payment/create-link — tạo payment link PayOS
     * Body: { "packageId": "POPULAR" }
     * Response: { checkoutUrl, orderCode, coinAmount, ... }
     */
    @PostMapping("/create-link")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo link thanh toán PayOS",
            description = "Tạo đơn nạp coin qua PayOS. Frontend nhận checkoutUrl và redirect user sang trang PayOS.",
            security = @SecurityRequirement(name = "bearerAuth"))
    public PaymentLinkResponse createPaymentLink(
            @Valid @RequestBody CreatePaymentLinkRequest request) {
        return paymentService.createPaymentLink(request);
    }

    /**
     * POST /api/payment/ipn — webhook nhận từ PayOS sau khi thanh toán
     * PUBLIC — không cần JWT (PayOS server gọi)
     */
    @PostMapping("/ipn")
    @Operation(summary = "PayOS IPN webhook (internal)",
            description = "Endpoint cho PayOS server gọi sau khi giao dịch hoàn tất. Verify chữ ký → cộng coin.")
    public Map<String, String> handleIpn(@RequestBody Map<String, Object> payload) {
        return paymentService.handleIpnWebhook(payload);
    }

    /**
     * POST /api/payment/recover 🔒
     * Kiểm tra và cộng coin cho tất cả đơn PENDING của user hiện tại.
     * Dùng khi đã thanh toán nhưng coin chưa được cộng.
     */
    @PostMapping("/recover")
    @Operation(summary = "Recover coin cho đơn PENDING",
            description = """
                    Kiểm tra tất cả đơn nạp PENDING của bạn với PayOS.
                    Nếu đã thanh toán thành công → coin được cộng ngay.
                    Dùng khi đã chuyển tiền nhưng coin chưa vào tài khoản.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<PaymentOrderResponse> recoverPendingPayments() {
        return paymentService.recoverPendingPayments();
    }

    /**
     * GET /api/payment/verify/{orderCode} 🔒
     * Frontend gọi sau khi PayOS redirect về returnUrl.
     * Backend query PayOS → cộng coin nếu PAID → trả về trạng thái mới nhất.
     */
    @GetMapping("/verify/{orderCode}")
    @Operation(summary = "Xác minh kết quả thanh toán",
            description = """
                    Frontend gọi endpoint này ngay sau khi PayOS redirect về returnUrl.
                    Backend tự query PayOS API để kiểm tra trạng thái thật.
                    Nếu PAID → coin được cộng ngay (dù IPN chưa tới).
                    Idempotent — gọi nhiều lần cũng an toàn.
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    public PaymentOrderResponse verifyPayment(@PathVariable Long orderCode) {
        return paymentService.verifyPayment(orderCode);
    }

    /**
     * GET /api/payment/history — lịch sử đơn nạp của user hiện tại
     */
    @GetMapping("/history")
    @Operation(summary = "Lịch sử đơn nạp coin",
            description = "Xem toàn bộ lịch sử nạp coin của tài khoản hiện tại",
            security = @SecurityRequirement(name = "bearerAuth"))
    public List<PaymentOrderResponse> getMyPaymentHistory() {
        return paymentService.getMyPaymentHistory();
    }
}



