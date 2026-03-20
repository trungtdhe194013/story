package org.com.story.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.story.common.CoinPackage;
import org.com.story.config.PayOSProperties;
import org.com.story.dto.request.CreatePaymentLinkRequest;
import org.com.story.dto.response.CoinPackageResponse;
import org.com.story.dto.response.PaymentLinkResponse;
import org.com.story.dto.response.PaymentOrderResponse;
import org.com.story.entity.PaymentOrder;
import org.com.story.entity.User;
import org.com.story.entity.Wallet;
import org.com.story.exception.BadRequestException;
import org.com.story.repository.PaymentOrderRepository;
import org.com.story.repository.WalletRepository;
import org.com.story.service.PaymentService;
import org.com.story.service.UserService;
import org.com.story.service.WalletService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PayOSProperties props;
    private final PaymentOrderRepository paymentOrderRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final UserService userService;
    private final RestTemplate restTemplate;

    private static final String PAYOS_API = "https://api-merchant.payos.vn";

    // ─────────────────────────────────────────────
    // PUBLIC: Danh sách gói coin
    // ─────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<CoinPackageResponse> getAllPackages() {
        return Arrays.stream(CoinPackage.values())
                .map(p -> CoinPackageResponse.builder()
                        .id(p.name())
                        .displayName(p.getDisplayName())
                        .amountVnd(p.getAmountVnd())
                        .coinAmount(p.getCoinAmount())
                        .bonusPercent(p.getBonusPercent())
                        .build())
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // Tạo payment link
    // ─────────────────────────────────────────────
    @Override
    public PaymentLinkResponse createPaymentLink(CreatePaymentLinkRequest request) {
        // 1. Tìm gói coin
        CoinPackage pkg;
        try {
            pkg = CoinPackage.valueOf(request.getPackageId().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Gói coin không hợp lệ: " + request.getPackageId()
                    + ". Các gói: BASIC, SAVING, POPULAR, ADVANCED, VIP");
        }

        // 2. User hiện tại
        User user = userService.getCurrentUser();

        // 3. Tạo orderCode unique
        long orderCode = generateOrderCode();

        // 4. Description tối đa 25 ký tự (giới hạn PayOS)
        String description = "Nap xu " + pkg.name().toLowerCase();

        // 5. Tính chữ ký (bao gồm webhookUrl nếu có)
        String webhookUrl = props.getWebhookUrl();
        String signature = buildSignature(
                pkg.getAmountVnd(), props.getCancelUrl(),
                description, orderCode, props.getReturnUrl(), webhookUrl);

        // 6. Gọi PayOS API
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderCode", orderCode);
        body.put("amount",    pkg.getAmountVnd());
        body.put("description", description);
        body.put("cancelUrl", props.getCancelUrl());
        body.put("returnUrl", props.getReturnUrl());
        // webhookUrl KHÔNG gửi qua API — phải cấu hình trong PayOS merchant dashboard
        body.put("signature", signature);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-client-id", props.getClientId());
        headers.set("x-api-key",   props.getApiKey());

        String checkoutUrl;
        String paymentLinkId = null;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    PAYOS_API + "/v2/payment-requests",
                    new HttpEntity<>(body, headers),
                    Map.class);

            if (response == null || !"00".equals(response.get("code"))) {
                String errMsg = response != null ? String.valueOf(response.get("desc")) : "null response";
                throw new BadRequestException("PayOS lỗi: " + errMsg);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            checkoutUrl   = data != null ? (String) data.get("checkoutUrl")   : null;
            paymentLinkId = data != null ? (String) data.get("paymentLinkId") : null;

            if (checkoutUrl == null) {
                throw new BadRequestException("PayOS không trả về checkoutUrl");
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("PayOS API call failed: {}", e.getMessage(), e);
            throw new BadRequestException("Không thể kết nối cổng thanh toán. Vui lòng thử lại sau.");
        }

        // 7. Lưu order vào DB
        PaymentOrder order = PaymentOrder.builder()
                .orderCode(orderCode)
                .user(user)
                .packageId(pkg.name())
                .amountVnd(pkg.getAmountVnd())
                .coinAmount(pkg.getCoinAmount())
                .status("PENDING")
                .checkoutUrl(checkoutUrl)
                .paymentLinkId(paymentLinkId)
                .build();
        paymentOrderRepository.save(order);

        log.info("Created payment link: orderCode={}, user={}, package={}, amount={}",
                orderCode, user.getId(), pkg.name(), pkg.getAmountVnd());

        return PaymentLinkResponse.builder()
                .orderCode(orderCode)
                .packageId(pkg.name())
                .packageName(pkg.getDisplayName())
                .amountVnd(pkg.getAmountVnd())
                .coinAmount(pkg.getCoinAmount())
                .bonusPercent(pkg.getBonusPercent())
                .checkoutUrl(checkoutUrl)
                .build();
    }

    // ─────────────────────────────────────────────
    // IPN Webhook từ PayOS
    // ─────────────────────────────────────────────
    @Override
    public Map<String, String> handleIpnWebhook(Map<String, Object> payload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            String receivedSignature  = (String) payload.get("signature");

            if (data == null || receivedSignature == null) {
                log.warn("IPN: thiếu data hoặc signature");
                return Map.of("code", "97", "desc", "Invalid payload");
            }

            // 1. Verify chữ ký
            if (!verifyWebhookSignature(data, receivedSignature)) {
                log.warn("IPN: chữ ký không hợp lệ");
                return Map.of("code", "97", "desc", "Invalid signature");
            }

            // 2. Lấy orderCode và trạng thái giao dịch
            Object orderCodeObj = data.get("orderCode");
            String txCode = String.valueOf(data.get("code"));
            long orderCode = orderCodeObj instanceof Number
                    ? ((Number) orderCodeObj).longValue()
                    : Long.parseLong(String.valueOf(orderCodeObj));

            // 3. Chỉ xử lý khi PayOS báo thành công
            if (!"00".equals(txCode)) {
                log.info("IPN: giao dịch không thành công (code={}), orderCode={}", txCode, orderCode);
                return Map.of("code", "00", "desc", "Acknowledged");
            }

            // 4. Tìm order
            Optional<PaymentOrder> orderOpt = paymentOrderRepository.findByOrderCode(orderCode);
            if (orderOpt.isEmpty()) {
                log.warn("IPN: không tìm thấy PaymentOrder, orderCode={}", orderCode);
                return Map.of("code", "01", "desc", "Order not found");
            }

            PaymentOrder order = orderOpt.get();

            // 5. Idempotency — đã xử lý rồi thì bỏ qua
            if ("PAID".equals(order.getStatus())) {
                log.info("IPN: orderCode={} đã xử lý trước đó, bỏ qua", orderCode);
                return Map.of("code", "00", "desc", "Already processed");
            }

            // 6. Credit coins
            creditCoins(order);

            log.info("IPN OK: orderCode={}, +{} coins → userId={}",
                    orderCode, order.getCoinAmount(), order.getUser().getId());

            return Map.of("code", "00", "desc", "success");

        } catch (Exception e) {
            log.error("IPN error: {}", e.getMessage(), e);
            return Map.of("code", "99", "desc", "Internal error");
        }
    }

    // ─────────────────────────────────────────────
    // Lịch sử đơn nạp
    // ─────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<PaymentOrderResponse> getMyPaymentHistory() {
        User user = userService.getCurrentUser();
        return paymentOrderRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // RECOVER — Check và cộng coin cho tất cả đơn PENDING của user
    // ─────────────────────────────────────────────
    @Override
    public List<PaymentOrderResponse> recoverPendingPayments() {
        User user = userService.getCurrentUser();

        List<PaymentOrder> pendingOrders = paymentOrderRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .filter(o -> "PENDING".equals(o.getStatus()))
                .collect(Collectors.toList());

        log.info("recoverPendingPayments: userId={}, pendingCount={}", user.getId(), pendingOrders.size());

        List<PaymentOrderResponse> results = new ArrayList<>();
        for (PaymentOrder order : pendingOrders) {
            try {
                PaymentOrderResponse result = verifyPayment(order.getOrderCode());
                results.add(result);
                log.info("recover orderCode={} → status={}", order.getOrderCode(), result.getStatus());
            } catch (Exception e) {
                log.error("recover failed orderCode={}: {}", order.getOrderCode(), e.getMessage());
                results.add(mapToResponse(order));
            }
        }
        return results;
    }

    // ─────────────────────────────────────────────
    // VERIFY — Frontend gọi sau khi redirect về từ PayOS
    // Query PayOS API → credit coins nếu PAID (idempotent)
    // ─────────────────────────────────────────────
    @Override
    public PaymentOrderResponse verifyPayment(Long orderCode) {
        PaymentOrder order = paymentOrderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new org.com.story.exception.NotFoundException(
                        "Không tìm thấy đơn hàng: " + orderCode));

        // Đã PAID rồi → trả về luôn
        if ("PAID".equals(order.getStatus())) {
            return mapToResponse(order);
        }

        // Query PayOS API để kiểm tra trạng thái thật
        if (order.getPaymentLinkId() == null) {
            return mapToResponse(order);
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-client-id", props.getClientId());
            headers.set("x-api-key",   props.getApiKey());

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.exchange(
                    PAYOS_API + "/v2/payment-requests/" + order.getPaymentLinkId(),
                    org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            ).getBody();

            if (response != null && "00".equals(response.get("code"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null) {
                    String status = (String) data.get("status");
                    log.info("PayOS verify orderCode={}, paymentLinkId={}, status={}",
                            orderCode, order.getPaymentLinkId(), status);

                    if ("PAID".equals(status) && !"PAID".equals(order.getStatus())) {
                        // Credit coins (idempotent)
                        creditCoins(order);
                    } else if ("CANCELLED".equals(status) && "PENDING".equals(order.getStatus())) {
                        order.setStatus("CANCELLED");
                        paymentOrderRepository.save(order);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Verify payment failed orderCode={}: {}", orderCode, e.getMessage());
        }

        return mapToResponse(order);
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    /**
     * Cộng coin vào ví user + ghi transaction + cập nhật order trạng thái PAID.
     * Idempotent: chỉ thực hiện khi order chưa PAID.
     */
    private void creditCoins(PaymentOrder order) {
        Wallet wallet = walletRepository.findByUserId(order.getUser().getId())
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setUser(order.getUser());
                    w.setBalance(0L);
                    w.setLockedBalance(0L);
                    return walletRepository.save(w);
                });
        wallet.setBalance(wallet.getBalance() + order.getCoinAmount());
        walletRepository.save(wallet);

        walletService.createTransaction(
                order.getUser().getId(),
                order.getCoinAmount(),
                "TOPUP",
                order.getId());

        order.setStatus("PAID");
        order.setPaidAt(LocalDateTime.now());
        paymentOrderRepository.save(order);

        log.info("creditCoins: orderCode={}, +{} coins → userId={}",
                order.getOrderCode(), order.getCoinAmount(), order.getUser().getId());
    }

    private long generateOrderCode() {
        long code;
        do {
            long ts     = System.currentTimeMillis() % 9_999_999L;
            long suffix = (long) (Math.random() * 1000);
            code = ts * 1000 + suffix;
        } while (paymentOrderRepository.findByOrderCode(code).isPresent());
        return code;
    }

    /**
     * Chữ ký tạo payment link — PayOS chỉ ký ĐÚNG 5 field theo thứ tự ABC:
     * amount, cancelUrl, description, orderCode, returnUrl
     * webhookUrl được gửi trong body nhưng KHÔNG đưa vào signature.
     */
    private String buildSignature(long amount, String cancelUrl, String description,
                                   long orderCode, String returnUrl, String webhookUrl) {
        // webhookUrl KHÔNG được đưa vào signature (theo PayOS docs)
        String raw = "amount=" + amount
                + "&cancelUrl=" + cancelUrl
                + "&description=" + description
                + "&orderCode=" + orderCode
                + "&returnUrl=" + returnUrl;
        return hmacSHA256(raw, props.getChecksumKey());
    }

    /**
     * Verify IPN: sort data fields ABC → join "k=v&..." → HMAC-SHA256 → so sánh
     */
    private boolean verifyWebhookSignature(Map<String, Object> data, String received) {
        TreeMap<String, Object> sorted = new TreeMap<>(data);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> e : sorted.entrySet()) {
            if (sb.length() > 0) sb.append("&");
            sb.append(e.getKey()).append("=")
              .append(e.getValue() == null ? "" : e.getValue());
        }
        String expected = hmacSHA256(sb.toString(), props.getChecksumKey());
        return expected.equalsIgnoreCase(received);
    }

    private String hmacSHA256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 error", e);
        }
    }

    private PaymentOrderResponse mapToResponse(PaymentOrder o) {
        String pkgName = null;
        try { pkgName = CoinPackage.valueOf(o.getPackageId()).getDisplayName(); }
        catch (Exception ignored) {}
        return PaymentOrderResponse.builder()
                .id(o.getId())
                .orderCode(o.getOrderCode())
                .packageId(o.getPackageId())
                .packageName(pkgName)
                .amountVnd(o.getAmountVnd())
                .coinAmount(o.getCoinAmount())
                .status(o.getStatus())
                .checkoutUrl(o.getCheckoutUrl())
                .createdAt(o.getCreatedAt())
                .paidAt(o.getPaidAt())
                .build();
    }
}









