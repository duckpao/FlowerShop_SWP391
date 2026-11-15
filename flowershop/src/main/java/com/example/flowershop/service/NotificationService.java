package com.example.flowershop.service;

import com.example.flowershop.entity.Notification;
import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.Refund;
import com.example.flowershop.entity.User;
import com.example.flowershop.entity.enums.PaymentType;
import com.example.flowershop.repository.NotificationRepository;
import com.example.flowershop.repository.ShopStaffRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    public record NotificationResponse(
            String id,
            String title,
            String message,
            String link,
            boolean read,
            LocalDateTime createdDate) {
    }

    private final NotificationRepository notifications;
    private final ShopStaffRepository staff;

    public NotificationService(NotificationRepository notifications, ShopStaffRepository staff) {
        this.notifications = notifications;
        this.staff = staff;
    }

    /** Notify both the customer and the shop owner that a COD order was placed (online orders: see notifyPaymentSuccess). */
    @Transactional
    public void notifyOrderPlaced(Order order) {
        create(
                order.getCustomer(),
                "Đặt hàng thành công",
                "Đơn hàng #" + shortId(order) + " đã được đặt thành công.",
                "/orders/" + order.getId());

        create(
                order.getShop().getOwner(),
                "Có đơn hàng mới",
                "Cửa hàng của bạn vừa nhận đơn hàng #" + shortId(order) + ".",
                "/shop-admin/orders/" + order.getId());
    }

    /** Đơn online vừa tạo, đang giữ hàng và chờ khách thanh toán. */
    @Transactional
    public void notifyPaymentPending(Order order) {
        create(
                order.getCustomer(),
                "Chờ thanh toán",
                "Đơn hàng #" + shortId(order) + " đang chờ thanh toán. Vui lòng thanh toán trong "
                        + SePayGatewayService.PAYMENT_WINDOW_MINUTES + " phút, quá hạn đơn sẽ tự hủy.",
                "/payment/pending?order=" + order.getId());
    }

    /** Tiền đã về (gọi một lần duy nhất khi Payment chuyển sang SUCCESS): báo khách và báo shop. */
    @Transactional
    public void notifyPaymentSuccess(Order order, PaymentType type) {
        String code = shortId(order);
        create(
                order.getCustomer(),
                type == PaymentType.DEPOSIT ? "Đã nhận tiền cọc" : "Thanh toán thành công",
                type == PaymentType.DEPOSIT
                        ? "Đã nhận tiền cọc cho đơn hàng #" + code + ". Phần còn lại thanh toán khi nhận hàng (COD)."
                        : "Đơn hàng #" + code + " đã được thanh toán thành công.",
                "/orders/" + order.getId());
        create(
                order.getShop().getOwner(),
                type == PaymentType.DEPOSIT ? "Khách đã đặt cọc" : type == PaymentType.FINAL ? "Khách đã thanh toán đủ" : "Có đơn hàng mới",
                type == PaymentType.DEPOSIT ? "Khách đã đặt cọc cho đơn hàng #" + code + "."
                        : type == PaymentType.FINAL ? "Khách đã thanh toán phần còn lại của đơn hàng #" + code + "."
                        : "Cửa hàng của bạn vừa nhận đơn hàng #" + code + " (đã thanh toán online).",
                "/shop-admin/orders/" + order.getId());
    }

    /** Đơn chờ thanh toán bị hủy (quá hạn hoặc khách hủy ở cổng): chỉ báo khách, kèm lý do. */
    @Transactional
    public void notifyPaymentFailed(Order order, String reason) {
        create(
                order.getCustomer(),
                "Thanh toán không thành công",
                "Đơn hàng #" + shortId(order) + " đã bị hủy. Lý do: " + reason + ".",
                "/orders/" + order.getId());
    }

    /** Thông báo chuông cho một người dùng (nhắc dịp đặc biệt...). */
    @Transactional
    public void notifyUser(User user, String title, String message, String link) {
        create(user, title, message, link);
    }

    /** Đơn đã trả tiền bị hủy, cần hoàn tiền: báo mọi staff đang làm ở shop để gọi khách. */
    @Transactional
    public void notifyRefundRequested(Refund refund) {
        Order order = refund.getOrder();
        String phone = order.getCustomer().getPhone() != null && !order.getCustomer().getPhone().isBlank()
                ? order.getCustomer().getPhone() : order.getRecipientPhone();
        String message = "Đơn hàng #" + shortId(order) + " đã hủy, cần hoàn " + String.format("%,.0f", refund.getAmount())
                + "đ. Liên hệ khách qua SĐT: " + phone + ".";
        staff.findByShopIdAndActiveTrue(order.getShop().getId())
                .forEach(s -> create(s.getUser(), "Đơn hủy cần hoàn tiền", message, "/shop-admin/refunds"));
    }

    /** Staff đã hoàn tiền hoặc từ chối hoàn: báo khách, link tới chi tiết đơn để xem khoản hoàn. */
    @Transactional
    public void notifyRefundResolved(Refund refund) {
        Order order = refund.getOrder();
        String amount = String.format("%,.0f", refund.getAmount()) + "đ";
        boolean processed = refund.getStatus() == com.example.flowershop.entity.enums.RefundStatus.PROCESSED;
        create(
                order.getCustomer(),
                processed ? "Đã hoàn tiền" : "Yêu cầu hoàn tiền bị từ chối",
                processed
                        ? "Cửa hàng đã hoàn " + amount + " cho đơn hàng #" + shortId(order) + "."
                        : "Khoản hoàn " + amount + " của đơn hàng #" + shortId(order) + " không được chấp nhận. Vui lòng liên hệ cửa hàng.",
                "/orders/" + order.getId());
    }

    private void create(User user, String title, String message, String link) {
        notifications.save(Notification.builder()
                .id(UUID.randomUUID().toString())
                .user(user)
                .title(title)
                .message(message)
                .link(link)
                .build());
    }

    private static String shortId(Order order) {
        return order.getId().replace("-", "").substring(0, 8).toUpperCase();
    }

    public List<NotificationResponse> mine(String userId) {
        return notifications.findByUserIdOrderByCreatedDateDesc(userId)
                .stream()
                .map(NotificationService::toResponse)
                .toList();
    }

    public long unreadCount(String userId) {
        return notifications.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAllAsRead(String userId) {
        notifications.markAllAsRead(userId);
    }

    private static NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getTitle(),
                n.getMessage(),
                n.getLink(),
                n.isRead(),
                n.getCreatedDate());
    }
}
