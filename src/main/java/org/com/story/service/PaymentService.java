package org.com.story.service;

import org.com.story.dto.request.CreatePaymentLinkRequest;
import org.com.story.dto.response.CoinPackageResponse;
import org.com.story.dto.response.PaymentLinkResponse;
import org.com.story.dto.response.PaymentOrderResponse;

import java.util.List;
import java.util.Map;

public interface PaymentService {

    /** Danh sách tất cả gói coin (public) */
    List<CoinPackageResponse> getAllPackages();

    /**
     * Tạo payment link PayOS cho user đang đăng nhập.
     * Trả về checkoutUrl để frontend redirect.
     */
    PaymentLinkResponse createPaymentLink(CreatePaymentLinkRequest request);

    /**
     * Nhận IPN webhook từ PayOS.
     * Verify signature → cộng coin nếu hợp lệ.
     * Trả về Map { "code": "00" } khi thành công.
     */
    Map<String, String> handleIpnWebhook(Map<String, Object> payload);

    /** Lịch sử đơn nạp của user hiện tại */
    List<PaymentOrderResponse> getMyPaymentHistory();

    /**
     * Frontend gọi sau khi redirect về từ PayOS (returnUrl).
     * Query PayOS API kiểm tra trạng thái thật → credit coins nếu PAID.
     * Idempotent: gọi nhiều lần cũng an toàn.
     */
    PaymentOrderResponse verifyPayment(Long orderCode);

    /**
     * Kiểm tra và recover TẤT CẢ đơn PENDING của user hiện tại.
     * Query PayOS từng đơn → cộng coin nếu đã PAID.
     * Dùng khi user đã thanh toán nhưng coin chưa được cộng.
     */
    List<PaymentOrderResponse> recoverPendingPayments();
}



