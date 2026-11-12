package com.example.flowershop.service;

import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.Payment;
import com.example.flowershop.entity.Refund;
import com.example.flowershop.entity.enums.OrderStatus;
import com.example.flowershop.entity.enums.PaymentStatus;
import com.example.flowershop.entity.enums.RefundStatus;
import com.example.flowershop.exception.ApiException;
import com.example.flowershop.repository.OrderRepository;
import com.example.flowershop.repository.PaymentRepository;
import com.example.flowershop.repository.RefundRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * SRS 5.2: khách tự gửi yêu cầu hoàn tiền cho đơn đã thanh toán (đã hoàn thành, đã hủy hoặc bị hoàn hàng),
 * kèm lý do, mô tả và ảnh bằng chứng; được hủy khi yêu cầu còn chờ. Shop xử lý ở StaffRefundService.
 */
@Service
public class CustomerRefundService {
    public static final List<String> REASONS = List.of("Đổi ý", "Sai sản phẩm", "Hoa bị dập, héo", "Giao hàng trễ", "Khác");
    private static final Set<String> NEEDS_EVIDENCE = Set.of("Sai sản phẩm", "Hoa bị dập, héo");
    private static final Set<OrderStatus> REFUNDABLE = EnumSet.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED, OrderStatus.REFUNDED);
    private static final Set<RefundStatus> ACTIVE = EnumSet.of(RefundStatus.REQUESTED, RefundStatus.APPROVED);
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final ObjectMapper JSON = new ObjectMapper();

    public record RefundInput(@NotBlank @Size(max = 50) String reason, @Size(max = 1000) String description,
                              @Size(max = 5) List<@Size(max = 500) String> images) {}

    public record RefundResult(String id, BigDecimal amount, String status) {}

    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final RefundRepository refunds;
    private final CloudinaryService cloudinary;
    private final NotificationService notifications;

    public CustomerRefundService(OrderRepository orders, PaymentRepository payments, RefundRepository refunds,
                                 CloudinaryService cloudinary, NotificationService notifications) {
        this.orders = orders;
        this.payments = payments;
        this.refunds = refunds;
        this.cloudinary = cloudinary;
        this.notifications = notifications;
    }

    /** Số tiền hoàn = tổng đã trả thành công − các khoản đã hoàn; không vượt số đã trả. */
    @Transactional
    public RefundResult request(String customerId, String orderId, RefundInput input) {
        Order order = owned(customerId, orderId);
        if (!REFUNDABLE.contains(order.getStatus()))
            throw ApiException.badRequest("Chỉ gửi yêu cầu hoàn tiền cho đơn đã hoàn thành, đã hủy hoặc bị hoàn hàng.");
        List<Refund> existing = refunds.findByOrderIdOrderByCreatedDateAsc(orderId);
        if (existing.stream().anyMatch(r -> ACTIVE.contains(r.getStatus())))
            throw ApiException.badRequest("Đơn hàng đang có yêu cầu hoàn tiền chờ xử lý.");

        String reason = input.reason() == null ? "" : input.reason().strip();
        if (!REASONS.contains(reason)) throw ApiException.badRequest("Lý do hoàn tiền không hợp lệ.");
        String description = input.description() == null ? "" : input.description().strip();
        if (reason.equals("Khác") && description.isEmpty()) throw ApiException.badRequest("Vui lòng mô tả lý do hoàn tiền.");
        List<String> images = input.images() == null ? List.of()
                : input.images().stream().filter(u -> u != null && !u.isBlank()).toList();
        if (images.size() > 5 || images.stream().anyMatch(u -> !u.startsWith("https://")))
            throw ApiException.badRequest("Ảnh bằng chứng không hợp lệ (tối đa 5 ảnh).");
        if (NEEDS_EVIDENCE.contains(reason) && images.isEmpty())
            throw ApiException.badRequest("Vui lòng đính kèm ảnh bằng chứng cho lý do này.");

        BigDecimal paid = payments.findByOrderId(orderId).stream().filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refunded = existing.stream().filter(r -> r.getStatus() == RefundStatus.PROCESSED)
                .map(Refund::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal amount = paid.subtract(refunded);
        if (amount.signum() <= 0) throw ApiException.badRequest("Đơn hàng không còn khoản đã thanh toán để hoàn.");

        Refund refund = refunds.save(Refund.builder().id(UUID.randomUUID().toString()).order(order).amount(amount)
                .reason(reason).description(description.isEmpty() ? null : description)
                .evidenceImages(images.isEmpty() ? null : write(images))
                .createdBy(customerId).lastModifyBy(customerId).build());
        notifications.notifyRefundRequested(refund);
        return new RefundResult(refund.getId(), refund.getAmount(), refund.getStatus().name());
    }

    /** Khách hủy yêu cầu khi shop chưa xử lý (REQUESTED). */
    @Transactional
    public RefundResult cancel(String customerId, String orderId, String refundId) {
        owned(customerId, orderId);
        Refund refund = refunds.findById(refundId).filter(r -> r.getOrder().getId().equals(orderId))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy yêu cầu hoàn tiền."));
        if (refund.getStatus() != RefundStatus.REQUESTED)
            throw ApiException.badRequest("Chỉ hủy được yêu cầu đang chờ xử lý.");
        refund.setStatus(RefundStatus.CANCELLED);
        refund.setLastModifyBy(customerId);
        return new RefundResult(refund.getId(), refund.getAmount(), refund.getStatus().name());
    }

    /** Ảnh được giải mã và mã hóa lại thành PNG trước khi lên Cloudinary để loại tệp không phải ảnh. */
    @Transactional(readOnly = true)
    public String uploadEvidence(String customerId, String orderId, MultipartFile file) {
        owned(customerId, orderId);
        if (file.isEmpty() || file.getSize() > MAX_IMAGE_BYTES)
            throw ApiException.badRequest("Ảnh phải có dung lượng từ 1 byte đến 5 MB.");
        try {
            var image = javax.imageio.ImageIO.read(file.getInputStream());
            if (image == null) throw ApiException.badRequest("Tệp tải lên không phải ảnh hợp lệ.");
            var out = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", out);
            return cloudinary.upload(out.toByteArray(), "refund-" + UUID.randomUUID() + ".png", "image/png");
        } catch (IOException e) {
            throw ApiException.badRequest("Không thể đọc ảnh. Vui lòng chọn ảnh hợp lệ.");
        }
    }

    public static List<String> images(Refund refund) {
        if (refund.getEvidenceImages() == null || refund.getEvidenceImages().isBlank()) return List.of();
        try {
            return JSON.readValue(refund.getEvidenceImages(), new TypeReference<List<String>>() { });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private Order owned(String customerId, String orderId) {
        return orders.findById(orderId).filter(o -> o.getCustomer().getId().equals(customerId))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng."));
    }

    private static String write(List<String> images) {
        try {
            return JSON.writeValueAsString(images);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không thể lưu ảnh bằng chứng.", e);
        }
    }
}
