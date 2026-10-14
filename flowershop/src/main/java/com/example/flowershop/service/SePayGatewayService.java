package com.example.flowershop.service;

import com.example.flowershop.dto.payment.CreateGroupPaymentRequest;
import com.example.flowershop.dto.payment.CreatePaymentRequest;
import com.example.flowershop.dto.payment.CreatePaymentResponse;
import com.example.flowershop.dto.payment.PaymentStatusResponse;
import com.example.flowershop.dto.payment.SePayCheckoutFormResponse;
import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.Payment;
import com.example.flowershop.entity.Refund;
import com.example.flowershop.entity.enums.OrderStatus;
import com.example.flowershop.entity.enums.OrderType;
import com.example.flowershop.entity.enums.PaymentMethod;
import com.example.flowershop.entity.enums.PaymentStatus;
import com.example.flowershop.entity.enums.PaymentType;
import com.example.flowershop.exception.ApiException;
import com.example.flowershop.repository.OrderRepository;
import com.example.flowershop.repository.PaymentRepository;
import com.example.flowershop.repository.RefundRepository;
import com.example.flowershop.util.SePaySignatureUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SePayGatewayService {

    /** Giá trị payment_method SePay chấp nhận (null = để khách chọn trên cổng). */
    private static final Set<String> SEPAY_PAYMENT_METHODS = Set.of("CARD", "BANK_TRANSFER", "NAPAS_BANK_TRANSFER");
    private static final String EXPIRED_REASON = "Quá hạn 10 phút không quét mã thanh toán";
    private static final String DEPOSIT_EXPIRED_REASON = "Quá hạn 24 giờ không thanh toán tiền cọc";
    /** Thời hạn thanh toán tiền cọc kể từ lúc tạo đơn AWAITING_DEPOSIT. */
    public static final long DEPOSIT_DEADLINE_HOURS = 24;
    /** Thời hạn thanh toán đơn thường kể từ lúc tạo đơn (kiểu Shopee: hủy giao dịch vẫn giữ đơn tới hết hạn). */
    public static final long PAYMENT_WINDOW_MINUTES = 10;

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final OrderService orderService;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${sepay.merchant-id:SP-TEST-001}")
    private String merchantId;

    @Value("${sepay.secret-key:}")
    private String secretKey;

    @Value("${sepay.checkout-url:https://pay-sandbox.sepay.vn/v1/checkout/init}")
    private String checkoutUrl;

    @Value("${sepay.api-base-url:https://pgapi.sepay.vn}")
    private String apiBaseUrl;

    @Value("${sepay.success-url:http://localhost:8080/payment/success}")
    private String successUrl;

    @Value("${sepay.error-url:http://localhost:8080/payment/error}")
    private String errorUrl;

    @Value("${sepay.cancel-url:http://localhost:8080/payment/cancel}")
    private String cancelUrl;

    private String getShortOrderCode(String orderId) {
        if (orderId == null) return "DH000000";
        String clean = orderId.replace("-", "").toUpperCase();
        return clean.length() >= 8 ? clean.substring(0, 8) : clean;
    }

    private static boolean isExpired(Order order) {
        if (order.getCreatedDate() == null) return false;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (order.getStatus() == OrderStatus.AWAITING_DEPOSIT) {
            return order.getCreatedDate().plusHours(DEPOSIT_DEADLINE_HOURS).isBefore(now);
        }
        return order.getStatus() == OrderStatus.PENDING && order.getCreatedDate().plusMinutes(PAYMENT_WINDOW_MINUTES).isBefore(now);
    }

    /** Thời hạn hiển thị cho người dùng: đơn custom (đặt cọc) 24 giờ, đơn thường 10 phút. */
    static String expiryText(Order order) {
        return order.getOrderType() == OrderType.CUSTOM ? DEPOSIT_DEADLINE_HOURS + " giờ" : "10 phút";
    }

    /** Tiền cọc yêu cầu: depositAmount nếu > 0, ngược lại 50% tổng. Dùng chung cho createPayment và status. */
    static BigDecimal requiredDeposit(Order order) {
        return order.getDepositAmount() != null && order.getDepositAmount().compareTo(BigDecimal.ZERO) > 0
                ? order.getDepositAmount()
                : order.getTotalAmount().multiply(new BigDecimal("0.5"));
    }

    /** Điền các field tiến độ thanh toán (cọc/còn lại/bước tiếp theo) vào response trạng thái. */
    static void addOrderProgress(PaymentStatusResponse.PaymentStatusResponseBuilder status, Order order,
                                 List<Payment> orderPayments) {
        BigDecimal totalPaid = orderPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = order.getTotalAmount();
        String next = null;
        if (order.getStatus() == OrderStatus.AWAITING_DEPOSIT
                || (order.getOrderType() == OrderType.CUSTOM && order.getStatus() == OrderStatus.PENDING)) {
            next = "DEPOSIT";
        } else if (order.getStatus() == OrderStatus.DEPOSIT_PAID) {
            next = "FINAL";
        }
        status.orderType(order.getOrderType() != null ? order.getOrderType().name() : null)
                .totalAmount(total)
                .depositAmount(total != null ? requiredDeposit(order) : null)
                .totalPaid(totalPaid)
                .remainingAmount(total != null ? total.subtract(totalPaid).max(BigDecimal.ZERO) : null)
                .nextPaymentType(next);
    }

    /**
     * PHASE 5: Khởi tạo thanh toán SePay với tính toán số tiền bảo mật từ Server
     */
    @Transactional
    public CreatePaymentResponse createPayment(CreatePaymentRequest request, String authenticatedUserId) {
        if (secretKey == null || secretKey.isBlank() || merchantId == null || merchantId.isBlank()) {
            throw ApiException.badRequest("Cổng thanh toán chưa được cấu hình.");
        }
        String paymentMethod = request.getPaymentMethod() == null || request.getPaymentMethod().isBlank()
                ? null : request.getPaymentMethod().trim().toUpperCase();
        if (paymentMethod != null && !SEPAY_PAYMENT_METHODS.contains(paymentMethod)) {
            throw ApiException.badRequest("Phương thức thanh toán không hợp lệ.");
        }
        String orderId = request.getOrderId();
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng với id: " + orderId));

        // PHASE 12: Xác thực quyền sở hữu đơn hàng
        if (authenticatedUserId == null || order.getCustomer() == null
                || !order.getCustomer().getId().equals(authenticatedUserId)) {
            throw ApiException.forbidden("Bạn không có quyền thanh toán cho đơn hàng này.");
        }

        // Kiểm tra trạng thái đơn hàng
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw ApiException.badRequest("Đơn hàng này đã hoàn thành, không thể thanh toán thêm.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw ApiException.badRequest("Đơn hàng này đã bị hủy, không thể thanh toán.");
        }
        // Không hủy tại đây: scheduler sẽ hủy phía SePay trước rồi mới nhả hàng.
        if (isExpired(order)) {
            throw ApiException.badRequest(order.getStatus() == OrderStatus.AWAITING_DEPOSIT
                    ? "Đơn hàng đã hết hạn đặt cọc (quá " + DEPOSIT_DEADLINE_HOURS + " giờ)."
                    : "Đơn hàng đã hết hạn thanh toán (quá 10 phút).");
        }
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.DEPOSIT_PAID
                && order.getStatus() != OrderStatus.AWAITING_DEPOSIT) {
            throw ApiException.badRequest("Đơn hàng không còn chờ thanh toán.");
        }
        List<Payment> orderPayments = paymentRepository.findByOrderId(orderId);
        if (orderPayments.stream().anyMatch(p -> p.getPaymentMethod() == PaymentMethod.COD)) {
            throw ApiException.badRequest("Đơn COD không thể thanh toán trực tuyến.");
        }

        // PHASE 2 & 3: Xác định PaymentType (FULL, DEPOSIT, FINAL)
        PaymentType paymentType = request.getPaymentType() != null ? request.getPaymentType() : PaymentType.FULL;
        if (order.getOrderType() == OrderType.STANDARD
                && paymentType != PaymentType.FULL) {
            throw ApiException.badRequest("Đơn hàng thường chỉ hỗ trợ thanh toán toàn bộ.");
        }
        if (order.getStatus() == OrderStatus.DEPOSIT_PAID && paymentType != PaymentType.FINAL) {
            throw ApiException.badRequest("Đơn đã trả cọc chỉ được thanh toán phần còn lại.");
        }
        if (paymentType == PaymentType.FINAL && order.getStatus() != OrderStatus.DEPOSIT_PAID) {
            throw ApiException.badRequest("Đơn hàng chưa thanh toán tiền cọc.");
        }
        if (paymentType == PaymentType.DEPOSIT && order.getStatus() == OrderStatus.DEPOSIT_PAID) {
            throw ApiException.badRequest("Tiền cọc đã được thanh toán.");
        }

        // PHASE 13: Server-side tính toán số tiền thanh toán (TUYỆT ĐỐI không tin amount từ React)
        BigDecimal payableAmount;
        if (paymentType == PaymentType.DEPOSIT) {
            payableAmount = requiredDeposit(order);
        } else if (paymentType == PaymentType.FINAL) {
            BigDecimal totalPaid = orderPayments.stream()
                    .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            payableAmount = order.getTotalAmount().subtract(totalPaid);
            if (payableAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw ApiException.badRequest("Đơn hàng này đã được thanh toán đầy đủ.");
            }
        } else {
            // FULL payment
            payableAmount = order.getTotalAmount();
        }

        // Mỗi lần thử tạo invoice mới (SePay có thể từ chối invoice trùng). Invoice cũ vẫn có thể được trả:
        // đối soát vẫn xác nhận được đơn, và scheduler sẽ hủy nó phía SePay khi đơn hết hạn.
        for (Payment old : orderPayments) {
            if (old.getPaymentMethod() == PaymentMethod.ONLINE && old.getPaymentType() == paymentType
                    && old.getStatus() == PaymentStatus.PENDING) {
                old.setStatus(PaymentStatus.FAILED);
                old.setLastModifyBy(authenticatedUserId);
                paymentRepository.save(old);
            }
        }
        Payment payment = Payment.builder()
                .id(UUID.randomUUID().toString())
                .order(order)
                .paymentType(paymentType)
                .paymentMethod(PaymentMethod.ONLINE)
                .amount(payableAmount)
                .invoiceNumber("INV-" + getShortOrderCode(orderId) + "-" + paymentType + "-" + UUID.randomUUID().toString().substring(0, 8))
                .status(PaymentStatus.PENDING)
                .createdBy(order.getCustomer().getId())
                .build();
        paymentRepository.save(payment);
        String invoiceNumber = payment.getInvoiceNumber();

        long amountLong = payableAmount.longValue();
        Map<String, String> fields = signedCheckoutFields(invoiceNumber, amountLong,
                "Thanh toan don hoa #" + getShortOrderCode(orderId) + " (" + paymentType + ")",
                order.getCustomer() != null ? order.getCustomer().getId() : null, paymentMethod);

        // PHASE 18: Ghi log an toàn (KHÔNG log secretKey)
        log.info("Tạo yêu cầu thanh toán SePay thành công: invoiceNumber={}, orderId={}, amount={}đ, type={}",
                invoiceNumber, orderId, amountLong, paymentType);

        return CreatePaymentResponse.builder()
                .checkoutUrl(checkoutUrl)
                .paymentId(payment.getId())
                .invoiceNumber(invoiceNumber)
                .orderId(orderId)
                .amount(payableAmount)
                .fields(fields)
                .build();
    }

    /** PHASE 6: Thứ tự field gửi đi phải trùng thứ tự ký (SDK SePay ký theo thứ tự field của form). */
    private Map<String, String> signedCheckoutFields(String invoiceNumber, long amount, String description,
                                                     String customerId, String paymentMethod) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("merchant", merchantId);
        fields.put("operation", "PURCHASE");
        if (paymentMethod != null) {
            fields.put("payment_method", paymentMethod);
        }
        fields.put("order_amount", String.valueOf(amount));
        fields.put("currency", "VND");
        fields.put("order_invoice_number", invoiceNumber);
        fields.put("order_description", description);
        if (customerId != null) {
            fields.put("customer_id", customerId);
        }
        fields.put("success_url", successUrl + "?invoice_number=" + invoiceNumber);
        fields.put("error_url", errorUrl + "?invoice_number=" + invoiceNumber);
        fields.put("cancel_url", cancelUrl + "?invoice_number=" + invoiceNumber);
        fields.put("signature", SePaySignatureUtil.generateSignature(fields, secretKey));
        return fields;
    }

    /**
     * Thanh toán gộp nhiều đơn thường (mỗi shop một đơn) trong MỘT giao dịch SePay.
     * Mỗi đơn có Payment riêng (số tiền = tổng đơn) cùng groupInvoice; SePay chỉ biết groupInvoice với tổng tiền.
     */
    @Transactional
    public CreatePaymentResponse createGroupPayment(CreateGroupPaymentRequest request, String userId) {
        if (secretKey == null || secretKey.isBlank() || merchantId == null || merchantId.isBlank()) {
            throw ApiException.badRequest("Cổng thanh toán chưa được cấu hình.");
        }
        String paymentMethod = request.getPaymentMethod() == null || request.getPaymentMethod().isBlank()
                ? null : request.getPaymentMethod().trim().toUpperCase();
        if (paymentMethod != null && !SEPAY_PAYMENT_METHODS.contains(paymentMethod)) {
            throw ApiException.badRequest("Phương thức thanh toán không hợp lệ.");
        }
        // Khóa theo thứ tự id để hai request đồng thời không khóa chéo nhau.
        List<String> ids = request.getOrderIds().stream().distinct().sorted().toList();
        List<Order> orders = new ArrayList<>();
        for (String id : ids) {
            Order order = orderRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng với id: " + id));
            if (userId == null || order.getCustomer() == null || !order.getCustomer().getId().equals(userId)) {
                throw ApiException.forbidden("Bạn không có quyền thanh toán cho đơn hàng này.");
            }
            if (order.getOrderType() != OrderType.STANDARD || order.getStatus() != OrderStatus.PENDING) {
                throw ApiException.badRequest("Đơn " + getShortOrderCode(id) + " không còn chờ thanh toán.");
            }
            if (isExpired(order)) {
                throw ApiException.badRequest("Đơn " + getShortOrderCode(id) + " đã hết hạn thanh toán (quá 10 phút).");
            }
            if (paymentRepository.findByOrderId(id).stream().anyMatch(p -> p.getPaymentMethod() == PaymentMethod.COD)) {
                throw ApiException.badRequest("Đơn COD không thể thanh toán trực tuyến.");
            }
            orders.add(order);
        }

        String groupInvoice = "GRP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        BigDecimal total = BigDecimal.ZERO;
        for (Order order : orders) {
            // Như createPayment: invoice cũ chưa trả được đánh FAILED, scheduler vẫn hủy phía SePay khi đơn hết hạn.
            for (Payment old : paymentRepository.findByOrderId(order.getId())) {
                if (old.getPaymentMethod() == PaymentMethod.ONLINE && old.getStatus() == PaymentStatus.PENDING) {
                    old.setStatus(PaymentStatus.FAILED);
                    old.setLastModifyBy(userId);
                    paymentRepository.save(old);
                }
            }
            paymentRepository.save(Payment.builder()
                    .id(UUID.randomUUID().toString())
                    .order(order)
                    .paymentType(PaymentType.FULL)
                    .paymentMethod(PaymentMethod.ONLINE)
                    .amount(order.getTotalAmount())
                    .invoiceNumber("INV-" + getShortOrderCode(order.getId()) + "-FULL-" + UUID.randomUUID().toString().substring(0, 8))
                    .groupInvoice(groupInvoice)
                    .status(PaymentStatus.PENDING)
                    .createdBy(userId)
                    .build());
            total = total.add(order.getTotalAmount());
        }

        Map<String, String> fields = signedCheckoutFields(groupInvoice, total.longValue(),
                "Thanh toan " + orders.size() + " don hoa", userId, paymentMethod);
        log.info("Tạo thanh toán gộp SePay: invoice={}, {} đơn, amount={}đ", groupInvoice, orders.size(), total.longValue());
        return CreatePaymentResponse.builder()
                .checkoutUrl(checkoutUrl)
                .invoiceNumber(groupInvoice)
                .orderId(orders.get(0).getId())
                .amount(total)
                .fields(fields)
                .build();
    }

    /**
     * Backward-compatible: Khởi tạo cho controller cũ
     */
    @Transactional
    public SePayCheckoutFormResponse initiateCheckout(String orderId, String authenticatedUserId) {
        CreatePaymentResponse res = createPayment(
                CreatePaymentRequest.builder().orderId(orderId).paymentType(PaymentType.FULL).build(),
                authenticatedUserId
        );
        return SePayCheckoutFormResponse.builder()
                .checkoutUrl(res.getCheckoutUrl())
                .fields(res.getFields())
                .build();
    }

    /**
     * Hủy đơn đang chờ khi khách quay về từ nút hủy của SePay.
     * Không @Transactional: gọi HTTP sang SePay không được giữ khóa DB.
     */
    public boolean cancelCheckout(String invoiceNumber, String authenticatedUserId) {
        List<Payment> members = paymentRepository.findByGroupInvoice(invoiceNumber);
        if (!members.isEmpty()) {
            return cancelGroupCheckout(invoiceNumber, members, authenticatedUserId);
        }
        Payment payment = paymentRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy giao dịch cần hủy."));
        Order order = payment.getOrder();
        if (authenticatedUserId == null || order == null || order.getCustomer() == null
                || !authenticatedUserId.equals(order.getCustomer().getId())) {
            throw ApiException.forbidden("Bạn không có quyền hủy giao dịch này.");
        }
        if (payment.getPaymentMethod() != PaymentMethod.ONLINE) {
            throw ApiException.badRequest("Giao dịch này không thuộc cổng SePay.");
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS || order.getStatus() == OrderStatus.PROCESSING) {
            return true;
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            orderService.restoreCancelledOrderItems(order);
            return false;
        }
        if (order.getStatus() != OrderStatus.PENDING) return false;

        // Đóng giao dịch phía SePay trước, rồi kiểm tra lại phòng trường hợp khách vừa trả xong.
        // Kiểu Shopee: hủy giao dịch KHÔNG hủy đơn; đơn được giữ tới hết hạn, khách thanh toán lại được, scheduler tự hủy khi quá hạn.
        cancelAtSePay(invoiceNumber);
        return reconcileWithSePayGateway(invoiceNumber);
    }

    /** Khách bấm hủy ở cổng SePay cho giao dịch gộp: đóng giao dịch và đối soát; các đơn vẫn được giữ tới hết hạn. */
    private boolean cancelGroupCheckout(String groupInvoice, List<Payment> members, String userId) {
        for (Payment member : members) {
            Order order = member.getOrder();
            if (userId == null || order == null || order.getCustomer() == null
                    || !userId.equals(order.getCustomer().getId())) {
                throw ApiException.forbidden("Bạn không có quyền hủy giao dịch này.");
            }
        }
        cancelAtSePay(groupInvoice);
        return reconcileWithSePayGateway(groupInvoice);
    }

    /**
     * Hủy đơn PENDING quá hạn (gọi từ scheduler): hủy phía SePay → đối soát → mới nhả hàng.
     * Không @Transactional vì có gọi HTTP.
     */
    public void expireUnpaidOrder(String orderId) {
        // Đơn COD (kể cả đơn "mua ngay" của CustomerOrderService) không có bước thanh toán online → không hết hạn sau 10 phút.
        if (paymentRepository.findByOrderId(orderId).stream().anyMatch(p -> p.getPaymentMethod() == PaymentMethod.COD)) {
            return;
        }
        List<String> invoices = paymentRepository.findByOrderId(orderId).stream()
                .filter(p -> p.getPaymentMethod() == PaymentMethod.ONLINE && p.getInvoiceNumber() != null
                        && p.getStatus() != PaymentStatus.SUCCESS)
                // Thanh toán gộp: SePay chỉ biết groupInvoice.
                .map(p -> p.getGroupInvoice() != null ? p.getGroupInvoice() : p.getInvoiceNumber())
                .distinct()
                .toList();
        for (String invoice : invoices) {
            cancelAtSePay(invoice);
        }
        for (String invoice : invoices) {
            if (reconcileWithSePayGateway(invoice)) {
                log.info("Đơn {} đã được thanh toán qua invoice {} trước khi hết hạn, không hủy.", orderId, invoice);
                return;
            }
        }
        boolean awaitingDeposit = orderRepository.findById(orderId)
                .map(o -> o.getStatus() == OrderStatus.AWAITING_DEPOSIT).orElse(false);
        orderService.cancelExpiredOrder(orderId, awaitingDeposit ? DEPOSIT_EXPIRED_REASON : EXPIRED_REASON);
    }

    private enum PaidOutcome { PAID, ALREADY_PAID, REFUND_REQUIRED, NOT_FOUND, AMOUNT_MISMATCH }

    /**
     * Ghi nhận tiền đã về cho 1 invoice (từ kết quả đối soát). Phải chạy trong transaction.
     * Khóa Order trước rồi Payment (cùng thứ tự với cancelPendingOrder) và đọc lại từ DB
     * để không quyết định dựa trên dữ liệu cũ.
     */
    private PaidOutcome applyPaid(String invoiceNumber, BigDecimal paidAmount, String transactionNo,
                                  String gatewayResponse, String actor) {
        Payment payment = paymentRepository.findByInvoiceNumber(invoiceNumber).orElse(null);
        if (payment == null) {
            log.warn("Không tìm thấy Payment tương ứng với invoiceNumber: {}", invoiceNumber);
            return PaidOutcome.NOT_FOUND;
        }
        Order order = payment.getOrder();
        entityManager.refresh(order, LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(payment, LockModeType.PESSIMISTIC_WRITE);

        // PHASE 9: Chống trùng lặp (Idempotency)
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return PaidOutcome.ALREADY_PAID;
        }
        // PHASE 13: Kiểm tra khớp số tiền (Amount Validation)
        if (payment.getAmount().compareTo(paidAmount) != 0) {
            log.error("PHÁT HIỆN LỆCH TIỀN: invoice={}, Yêu cầu={}đ, SePay báo={}đ",
                    invoiceNumber, payment.getAmount(), paidAmount);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setGatewayResponse(String.format(
                    "{\"error\":\"AMOUNT_MISMATCH\",\"expected\":%s,\"received\":%s}",
                    payment.getAmount(), paidAmount));
            paymentRepository.save(payment);
            return PaidOutcome.AMOUNT_MISMATCH;
        }

        // Tiền đã về thật: luôn ghi nhận, kể cả khi đơn không còn chờ khoản này.
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setGatewayTransactionNo(transactionNo);
        payment.setGatewayResponse(gatewayResponse);
        payment.setLastModifyBy(actor);
        paymentRepository.save(payment);

        boolean expected = payment.getPaymentType() == PaymentType.FINAL
                ? order.getStatus() == OrderStatus.DEPOSIT_PAID
                : order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.AWAITING_DEPOSIT;
        if (!expected) {
            // Đơn đã hủy (hết hạn) hoặc đã được trả bằng invoice khác → tạo yêu cầu hoàn tiền cho admin xử lý.
            log.error("Nhận tiền cho đơn {} ở trạng thái {} (invoice {}, {}đ) → tạo yêu cầu hoàn tiền.",
                    order.getId(), order.getStatus(), invoiceNumber, paidAmount);
            refundRepository.save(Refund.builder()
                    .id(UUID.randomUUID().toString())
                    .order(order)
                    .amount(paidAmount)
                    .reason("Khách thanh toán invoice " + invoiceNumber + " khi đơn đang ở trạng thái "
                            + order.getStatus() + ". Cần hoàn tiền.")
                    .gatewayResponse(gatewayResponse)
                    .createdBy(actor)
                    .lastModifyBy(actor)
                    .build());
            return PaidOutcome.REFUND_REQUIRED;
        }

        if (payment.getPaymentType() == PaymentType.DEPOSIT) {
            order.setStatus(OrderStatus.DEPOSIT_PAID);
        } else {
            // FINAL hoặc FULL: đủ tiền, chuyển sang xử lý. depositAmount chỉ là "tiền cọc yêu cầu", không ghi đè.
            order.setStatus(OrderStatus.PROCESSING);
        }
        order.setLastModifyBy(actor);
        orderRepository.save(order);
        orderService.clearPaidOrderItems(order);

        log.info("Xác nhận thanh toán thành công cho đơn hàng: {}, invoice: {}, số tiền: {}đ",
                order.getId(), invoiceNumber, paidAmount);
        return PaidOutcome.PAID;
    }

    /**
     * Chủ động đối soát với Cổng SePay qua REST API (nguồn xác nhận thanh toán duy nhất).
     * Gọi HTTP bên ngoài transaction, chỉ mở transaction ngắn để ghi kết quả.
     */
    public boolean reconcileWithSePayGateway(String invoiceNumber) {
        if (invoiceNumber == null) return false;
        String body = callSePayApi("GET", "/v1/order/detail/" + invoiceNumber, null);
        if (body == null) return false;
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode details = root.has("data") && root.get("data").isObject() ? root.get("data") : root;
            if (!"CAPTURED".equalsIgnoreCase(details.path("order_status").asText())) return false;
            String transactionNo = details.path("order_id").asText(null);
            String amountText = details.path("order_amount").asText(null);
            String returnedInvoice = details.path("order_invoice_number").asText(null);
            if (amountText == null || (returnedInvoice != null && !invoiceNumber.equals(returnedInvoice))) return false;
            BigDecimal paidAmount = new BigDecimal(amountText);
            List<Payment> members = paymentRepository.findByGroupInvoice(invoiceNumber);
            if (!members.isEmpty()) {
                return applyGroupPaid(invoiceNumber, members, paidAmount,
                        transactionNo != null ? transactionNo : "SEPAY-CAPTURED", body);
            }
            PaidOutcome outcome = transactionTemplate.execute(status -> applyPaid(invoiceNumber, paidAmount,
                    transactionNo != null ? transactionNo : "SEPAY-CAPTURED", body, "SEPAY_AUTO_RECONCILE"));
            // Chỉ coi là "đã thanh toán" khi đơn thực sự được xác nhận (không tính trường hợp phải hoàn tiền).
            return outcome == PaidOutcome.PAID || (outcome == PaidOutcome.ALREADY_PAID && isOrderPaidBy(invoiceNumber));
        } catch (Exception e) {
            log.warn("Không thể đối soát invoice {} với SePay: {}", invoiceNumber, e.getMessage());
            return false;
        }
    }

    /**
     * Giao dịch gộp đã CAPTURED: tổng tiền phải khớp tổng các Payment, rồi ghi nhận từng đơn bằng applyPaid
     * (đơn nào đã hủy/hết hạn sẽ tự sinh yêu cầu hoàn tiền riêng cho đơn đó).
     * @return true nếu có ít nhất một đơn thực sự được xác nhận thanh toán.
     */
    private boolean applyGroupPaid(String groupInvoice, List<Payment> members, BigDecimal paidAmount,
                                   String transactionNo, String gatewayResponse) {
        BigDecimal expected = members.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (expected.compareTo(paidAmount) != 0) {
            log.error("PHÁT HIỆN LỆCH TIỀN (thanh toán gộp): invoice={}, Yêu cầu={}đ, SePay báo={}đ",
                    groupInvoice, expected, paidAmount);
            return false;
        }
        List<PaidOutcome> outcomes = transactionTemplate.execute(status -> members.stream()
                .map(m -> applyPaid(m.getInvoiceNumber(), m.getAmount(), transactionNo, gatewayResponse,
                        "SEPAY_AUTO_RECONCILE"))
                .toList());
        return outcomes != null && outcomes.stream()
                .anyMatch(o -> o == PaidOutcome.PAID || o == PaidOutcome.ALREADY_PAID);
    }

    private boolean isOrderPaidBy(String invoiceNumber) {
        return paymentRepository.findByInvoiceNumber(invoiceNumber)
                .map(p -> p.getOrder().getStatus() != OrderStatus.CANCELLED)
                .orElse(false);
    }

    /** Đóng đơn phía SePay để khách không thể trả tiền sau khi mình đã hủy. Lỗi chỉ ghi log. */
    private void cancelAtSePay(String invoiceNumber) {
        try {
            String json = objectMapper.writeValueAsString(Map.of("order_invoice_number", invoiceNumber));
            if (callSePayApi("POST", "/v1/order/cancel", json) != null) {
                log.info("Đã hủy invoice {} phía SePay.", invoiceNumber);
            }
        } catch (Exception e) {
            log.warn("Không thể hủy invoice {} phía SePay: {}", invoiceNumber, e.getMessage());
        }
    }

    /** @return body khi HTTP 2xx, ngược lại null. */
    private String callSePayApi(String method, String path, String jsonBody) {
        if (secretKey == null || secretKey.isBlank()) return null;
        try {
            String auth = Base64.getEncoder().encodeToString(
                    (merchantId + ":" + secretKey).getBytes(StandardCharsets.UTF_8));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl.replaceAll("/$", "") + path))
                    .header("Authorization", "Basic " + auth)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(6))
                    .method(method, jsonBody == null ? HttpRequest.BodyPublishers.noBody()
                            : HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 == 2) return resp.body();
            log.warn("SePay API {} {} trả về HTTP {}: {}", method, path, resp.statusCode(), resp.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("Không thể gọi SePay API {} {}: {}", method, path, e.getMessage());
        }
        return null;
    }

    /**
     * PHASE 11: Truy vấn trạng thái thanh toán theo invoiceNumber (kèm đối soát chủ động thời gian thực)
     */
    public PaymentStatusResponse getStatusByInvoice(String invoiceNumber, String authenticatedUserId) {
        List<Payment> members = paymentRepository.findByGroupInvoice(invoiceNumber);
        if (!members.isEmpty()) {
            return buildGroupStatus(invoiceNumber, members, authenticatedUserId);
        }
        Payment payment = paymentRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy thông tin thanh toán với mã hóa đơn: " + invoiceNumber));
        return buildStatus(payment, authenticatedUserId);
    }

    /** Trạng thái tổng hợp của giao dịch gộp: isPaid khi mọi đơn đã nhận tiền và không đơn nào bị hủy. */
    private PaymentStatusResponse buildGroupStatus(String groupInvoice, List<Payment> initial, String userId) {
        for (Payment member : initial) {
            Order order = member.getOrder();
            if (userId == null || order == null || order.getCustomer() == null
                    || !order.getCustomer().getId().equals(userId)) {
                throw ApiException.forbidden("Bạn không có quyền xem thông tin giao dịch này.");
            }
        }
        boolean open = initial.stream().anyMatch(m -> m.getStatus() != PaymentStatus.SUCCESS
                && m.getOrder().getStatus() != OrderStatus.CANCELLED);
        if (open) reconcileWithSePayGateway(groupInvoice);
        List<Payment> members = open ? paymentRepository.findByGroupInvoice(groupInvoice) : initial;
        boolean allPaid = members.stream().allMatch(m -> m.getStatus() == PaymentStatus.SUCCESS
                && m.getOrder().getStatus() != OrderStatus.CANCELLED);
        boolean allCancelled = members.stream().allMatch(m -> m.getOrder().getStatus() == OrderStatus.CANCELLED);
        BigDecimal total = members.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paid = members.stream()
                .filter(m -> m.getStatus() == PaymentStatus.SUCCESS && m.getOrder().getStatus() != OrderStatus.CANCELLED)
                .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        String message = allPaid ? "Thanh toán thành công qua SePay Gateway!"
                : allCancelled ? "Các đơn hàng đã bị hủy do quá thời gian thanh toán (10 phút)."
                : "Đang chờ thanh toán...";
        Payment first = members.get(0);
        return PaymentStatusResponse.builder()
                .paymentId(first.getId())
                .orderId(first.getOrder().getId())
                .orderIds(members.stream().map(m -> m.getOrder().getId()).toList())
                .invoiceNumber(groupInvoice)
                .orderStatus(allPaid ? OrderStatus.PROCESSING.name()
                        : allCancelled ? OrderStatus.CANCELLED.name() : OrderStatus.PENDING.name())
                .paymentStatus(allPaid ? PaymentStatus.SUCCESS.name() : PaymentStatus.PENDING.name())
                .isPaid(allPaid)
                .amount(total)
                .paidAmount(paid)
                .totalAmount(total)
                .paymentType(PaymentType.FULL.name())
                .message(message)
                .paidAt(first.getLastModifyDate())
                .build();
    }

    /**
     * Truy vấn trạng thái theo paymentId (kèm đối soát chủ động thời gian thực)
     */
    public PaymentStatusResponse getStatusById(String paymentId, String authenticatedUserId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy thông tin thanh toán với id: " + paymentId));
        return buildStatus(payment, authenticatedUserId);
    }

    private PaymentStatusResponse buildStatus(Payment payment, String authenticatedUserId) {
        if (authenticatedUserId == null || payment.getOrder() == null
                || payment.getOrder().getCustomer() == null
                || !payment.getOrder().getCustomer().getId().equals(authenticatedUserId)) {
            throw ApiException.forbidden("Bạn không có quyền xem thông tin giao dịch này.");
        }

        // Đối soát trực tiếp với SePay nếu chưa SUCCESS và đơn chưa bị hủy (việc hủy do scheduler đảm nhiệm)
        if (payment.getPaymentMethod() == PaymentMethod.ONLINE && payment.getStatus() != PaymentStatus.SUCCESS
                && payment.getOrder().getStatus() != OrderStatus.CANCELLED) {
            reconcileWithSePayGateway(payment.getInvoiceNumber());
            payment = paymentRepository.findById(payment.getId()).orElse(payment);
        }

        boolean cancelled = payment.getOrder().getStatus() == OrderStatus.CANCELLED;
        boolean isPaid = payment.getStatus() == PaymentStatus.SUCCESS && !cancelled;
        String message;
        if (isPaid) {
            message = payment.getPaymentType() == PaymentType.DEPOSIT
                    ? String.format("Đã nhận tiền cọc %,.0f đ. Còn lại %,.0f đ thanh toán trước khi giao.",
                            payment.getAmount(),
                            payment.getOrder().getTotalAmount().subtract(payment.getAmount()).max(BigDecimal.ZERO))
                    : "Thanh toán thành công qua SePay Gateway!";
        } else if (cancelled && payment.getStatus() == PaymentStatus.SUCCESS) {
            message = "Đơn hàng đã bị hủy trước khi nhận được tiền. Cửa hàng sẽ hoàn tiền cho bạn.";
        } else if (cancelled) {
            message = "Đơn hàng đã bị hủy do quá thời gian thanh toán (" + expiryText(payment.getOrder()) + ").";
        } else {
            message = "Đang chờ thanh toán...";
        }

        PaymentStatusResponse.PaymentStatusResponseBuilder status = PaymentStatusResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrder().getId())
                .invoiceNumber(payment.getInvoiceNumber())
                .orderStatus(payment.getOrder().getStatus().name())
                .paymentStatus(payment.getStatus().name())
                .isPaid(isPaid)
                .amount(payment.getAmount())
                .paidAmount(isPaid ? payment.getAmount() : BigDecimal.ZERO)
                .paymentType(payment.getPaymentType() != null ? payment.getPaymentType().name() : "FULL")
                .transactionNo(payment.getGatewayTransactionNo())
                .message(message)
                .paidAt(payment.getLastModifyDate());
        addOrderProgress(status, payment.getOrder(), paymentRepository.findByOrderId(payment.getOrder().getId()));
        return status.build();
    }
}
