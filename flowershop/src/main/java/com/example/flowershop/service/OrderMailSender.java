package com.example.flowershop.service;

import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.OrderDetail;
import com.example.flowershop.entity.Payment;
import com.example.flowershop.entity.User;
import com.example.flowershop.entity.enums.PaymentMethod;
import com.example.flowershop.entity.enums.PaymentStatus;
import com.example.flowershop.event.OrderPlacedEvent;
import com.example.flowershop.event.OrderStatusChangedEvent;
import com.example.flowershop.repository.DeliveryRepository;
import com.example.flowershop.repository.OrderDetailRepository;
import com.example.flowershop.repository.OrderRepository;
import com.example.flowershop.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.example.flowershop.service.HtmlMailSender.esc;
import static com.example.flowershop.service.HtmlMailSender.money;

/**
 * Gửi mail cho khách về đơn hàng: biên lai khi đặt hàng và thông báo mỗi lần đổi trạng thái.
 * Chạy bất đồng bộ sau khi transaction nghiệp vụ commit; lỗi gửi mail chỉ ghi log, không ảnh hưởng đơn.
 */
@Component
public class OrderMailSender {

    private static final Logger log = LoggerFactory.getLogger(OrderMailSender.class);

    private record Mail(String to, String subject, String html) {}

    private final OrderRepository orders;
    private final OrderDetailRepository details;
    private final PaymentRepository payments;
    private final DeliveryRepository deliveries;
    private final HtmlMailSender mail;
    private final TransactionTemplate readOnlyTx;

    public OrderMailSender(OrderRepository orders, OrderDetailRepository details, PaymentRepository payments,
                           DeliveryRepository deliveries, HtmlMailSender mail, PlatformTransactionManager tm) {
        this.orders = orders;
        this.details = details;
        this.payments = payments;
        this.deliveries = deliveries;
        this.mail = mail;
        this.readOnlyTx = new TransactionTemplate(tm);
        this.readOnlyTx.setReadOnly(true);
    }

    /** Biên lai khi vừa đặt hàng; nhiều đơn cùng lần checkout được gộp trong một thư. */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPlaced(OrderPlacedEvent event) {
        deliver(() -> buildReceipt(event.orderIds()));
    }

    /** Thông báo đổi trạng thái; trạng thái không cần báo (chờ xác nhận, chờ đặt cọc...) bị bỏ qua. */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onStatusChanged(OrderStatusChangedEvent event) {
        deliver(() -> buildStatusMail(event));
    }

    private void deliver(java.util.function.Supplier<Optional<Mail>> builder) {
        try {
            Optional<Mail> built = readOnlyTx.execute(status -> builder.get());
            if (built != null && built.isPresent()) mail.send(built.get().to(), built.get().subject(), built.get().html());
        } catch (Exception e) {
            log.warn("Order email skipped: {}", e.toString());
        }
    }

    private static boolean canMail(User customer) {
        return customer != null && customer.getEmail() != null && !customer.getEmail().isBlank()
                && Boolean.TRUE.equals(customer.getIsEmailVerified());
    }

    static String shortId(Order order) {
        return order.getId().replace("-", "").substring(0, 8).toUpperCase();
    }

    // ---- Biên lai ----

    private Optional<Mail> buildReceipt(List<String> orderIds) {
        List<Order> list = new ArrayList<>();
        for (String id : orderIds) orders.findById(id).ifPresent(list::add);
        if (list.isEmpty() || !canMail(list.get(0).getCustomer())) return Optional.empty();
        User customer = list.get(0).getCustomer();

        StringBuilder body = new StringBuilder();
        body.append("<p>Xin chào ").append(esc(customer.getFullName())).append(", cảm ơn bạn đã đặt hoa tại FlowerShop. ")
                .append("Dưới đây là biên lai đơn hàng của bạn.</p>");
        BigDecimal grandTotal = BigDecimal.ZERO;
        for (Order order : list) {
            body.append(receiptSection(order));
            grandTotal = grandTotal.add(order.getTotalAmount());
        }
        if (list.size() > 1) {
            body.append("<p style=\"font-size:16px\"><strong>Tổng cộng ").append(list.size()).append(" đơn: ")
                    .append(money(grandTotal)).append("</strong></p>");
        }
        body.append(deliverySection(list.get(0)));

        String subject = list.size() == 1
                ? "FlowerShop - Biên lai đơn hàng #" + shortId(list.get(0))
                : "FlowerShop - Biên lai " + list.size() + " đơn hàng";
        String link = list.size() == 1 ? "/orders/" + list.get(0).getId() : "/orders";
        return Optional.of(new Mail(customer.getEmail(), subject,
                HtmlMailSender.layout("Biên lai đặt hàng", body.toString(), "Xem đơn hàng", mail.url(link))));
    }

    private String receiptSection(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"border:1px solid #e5e7eb;border-radius:8px;padding:14px;margin:14px 0\">")
                .append("<p style=\"margin:0 0 8px\"><strong>Đơn #").append(shortId(order)).append("</strong> · ")
                .append(esc(order.getShop().getName())).append("</p>")
                .append("<table style=\"width:100%;border-collapse:collapse;font-size:14px\">");
        for (OrderDetail d : details.findByOrderId(order.getId())) {
            String name = d.getProduct() != null ? d.getProduct().getName() : "Hoa đặt theo yêu cầu";
            sb.append("<tr><td style=\"padding:4px 0\">").append(esc(name)).append(" × ").append(d.getQuantity())
                    .append("</td><td style=\"text-align:right\">")
                    .append(money(d.getPrice().multiply(BigDecimal.valueOf(d.getQuantity())))).append("</td></tr>");
        }
        sb.append("</table><hr style=\"border:none;border-top:1px solid #e5e7eb;margin:10px 0\">")
                .append(row("Tạm tính", money(order.getSubTotal())));
        if (order.getDiscountAmount() != null && order.getDiscountAmount().signum() > 0)
            sb.append(row("Giảm giá", "-" + money(order.getDiscountAmount())));
        sb.append(row("Phí vận chuyển (GHN)", money(order.getShippingFee())))
                .append("<p style=\"margin:8px 0 0;text-align:right;font-size:16px\"><strong>Tổng: ")
                .append(money(order.getTotalAmount())).append("</strong></p>")
                .append("<p style=\"margin:6px 0 0;font-size:13px;color:#6b7280\">").append(paymentNote(order)).append("</p>")
                .append("</div>");
        return sb.toString();
    }

    private static String row(String label, String value) {
        return "<p style=\"margin:2px 0;font-size:14px;display:flex;justify-content:space-between\"><span>"
                + esc(label) + "</span><span>" + esc(value) + "</span></p>";
    }

    private String paymentNote(Order order) {
        List<Payment> list = payments.findByOrderId(order.getId());
        if (list.stream().anyMatch(p -> p.getPaymentMethod() == PaymentMethod.COD))
            return "Thanh toán khi nhận hàng (COD): chuẩn bị " + esc(money(order.getTotalAmount())) + " khi nhận hoa.";
        BigDecimal paid = list.stream().filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (paid.compareTo(order.getTotalAmount()) >= 0) return "Đã thanh toán online.";
        if (paid.signum() > 0) return "Đã thanh toán " + esc(money(paid)) + ", còn lại thanh toán sau.";
        return "Chờ thanh toán online. Đơn sẽ tự hủy nếu quá hạn thanh toán.";
    }

    private static String deliverySection(Order order) {
        String address = String.join(", ", java.util.stream.Stream.of(
                        order.getDeliveryAddress().getAddressLine(), order.getDeliveryAddress().getWard(),
                        order.getDeliveryAddress().getDistrict(), order.getDeliveryAddress().getCity())
                .filter(s -> s != null && !s.isBlank()).toList());
        String name = order.getRecipientName() != null ? order.getRecipientName() : order.getCustomer().getFullName();
        String phone = order.getRecipientPhone() != null ? order.getRecipientPhone() : order.getCustomer().getPhone();
        return "<p style=\"font-size:14px\"><strong>Giao đến:</strong> " + esc(name)
                + (phone == null ? "" : " · " + esc(phone)) + "<br>" + esc(address) + "</p>";
    }

    // ---- Đổi trạng thái ----

    private Optional<Mail> buildStatusMail(OrderStatusChangedEvent event) {
        String headline;
        String detail;
        switch (event.to()) {
            case PROCESSING -> { headline = "Đơn hàng đã được xác nhận"; detail = "Cửa hàng đang chuẩn bị hoa cho bạn."; }
            case DEPOSIT_PAID -> { headline = "Đã nhận tiền cọc"; detail = "Cửa hàng sẽ bắt đầu thực hiện đơn của bạn."; }
            case DELIVERING -> { headline = "Đơn hàng đang được giao"; detail = "Hoa của bạn đã được bàn giao cho đơn vị vận chuyển."; }
            case COMPLETED -> { headline = "Giao hàng thành công"; detail = "Cảm ơn bạn đã mua hoa tại FlowerShop. Chúc bạn một ngày vui vẻ!"; }
            case CANCELLED -> { headline = "Đơn hàng đã bị hủy"; detail = "Đơn hàng của bạn đã được hủy."; }
            case REFUNDED -> { headline = "Đơn hàng được hoàn trả"; detail = "Đơn hàng đã được hoàn về cửa hàng."; }
            default -> { return Optional.empty(); }
        }
        Order order = orders.findById(event.orderId()).orElse(null);
        if (order == null || !canMail(order.getCustomer())) return Optional.empty();

        StringBuilder body = new StringBuilder();
        body.append("<p>Xin chào ").append(esc(order.getCustomer().getFullName())).append(",</p>")
                .append("<p>Đơn hàng <strong>#").append(shortId(order)).append("</strong> tại ")
                .append(esc(order.getShop().getName())).append(": ").append(esc(detail)).append("</p>");
        if (event.to() == com.example.flowershop.entity.enums.OrderStatus.DELIVERING) {
            deliveries.findByOrderId(order.getId()).ifPresent(d -> {
                if (d.getTrackingCode() != null)
                    body.append("<p>Mã vận đơn GHN: <strong>").append(esc(d.getTrackingCode())).append("</strong></p>");
            });
        }
        if (event.to() == com.example.flowershop.entity.enums.OrderStatus.CANCELLED
                && order.getCancelReason() != null && !order.getCancelReason().isBlank())
            body.append("<p>Lý do: ").append(esc(order.getCancelReason())).append("</p>");
        body.append("<p style=\"font-size:14px;color:#6b7280\">Tổng giá trị đơn: ").append(esc(money(order.getTotalAmount())))
                .append("</p>");

        return Optional.of(new Mail(order.getCustomer().getEmail(),
                "FlowerShop - " + headline + " #" + shortId(order),
                HtmlMailSender.layout(headline, body.toString(), "Xem đơn hàng", mail.url("/orders/" + order.getId()))));
    }
}
