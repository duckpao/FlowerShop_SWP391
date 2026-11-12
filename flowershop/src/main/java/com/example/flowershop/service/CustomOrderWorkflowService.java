package com.example.flowershop.service;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class CustomOrderWorkflowService {
    public record CreateInput(@NotBlank String addressId, @NotNull @Valid CustomFlowerOrderService.RequestInput design) {}
    public record Quote(@NotNull @DecimalMin("1") @Digits(integer=10, fraction=2) BigDecimal amount,
                        @Size(max=2000) String note) {}
    public record Accept(@NotNull PaymentMethod paymentMethod, @NotNull @DecimalMin("1") BigDecimal expectedTotal,
                         @Size(max=50) String couponCode, @Size(max=50) String shippingCouponCode) {
        public Accept(PaymentMethod paymentMethod,BigDecimal expectedTotal) { this(paymentMethod,expectedTotal,null,null); }
    }
    public record VoucherInput(@Size(max=50) String couponCode,@Size(max=50) String shippingCouponCode) {}
    public record DeliveryInput(@NotBlank String addressId) {}
    public record Design(BigDecimal budget, String description, CustomFlowerOrderService.Design preferences, boolean legacy) {
        public Design(BigDecimal budget,String description,CustomFlowerOrderService.Design preferences) { this(budget,description,preferences,false); }
    }
    public record Receipt(String orderId, String status) {}
    private final OrderRepository orders;
    private final OrderDetailRepository details;
    private final PaymentRepository payments;
    private final AddressRepository addresses;
    private final ShopRepository shops;
    private final UserRepository users;
    private final CustomFlowerOrderService designs;
    private final GhnService ghn;
    private final CouponService vouchers;
    private final NotificationService notifications;
    private final ObjectMapper json = new ObjectMapper();
    @org.springframework.beans.factory.annotation.Autowired private ShopStaffRepository staff;

    public CustomOrderWorkflowService(OrderRepository orders, OrderDetailRepository details, PaymentRepository payments,
            AddressRepository addresses, ShopRepository shops, UserRepository users, CustomFlowerOrderService designs, GhnService ghn, CouponService vouchers, NotificationService notifications) {
        this.orders=orders; this.details=details; this.payments=payments; this.addresses=addresses;
        this.shops=shops; this.users=users; this.designs=designs;
        this.ghn=ghn;
        this.vouchers=vouchers;
        this.notifications=notifications;
    }
    private static ResponseStatusException invalid(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
    private static ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng thuộc quyền truy cập của bạn."); }
    private Order locked(String id) { return orders.findByIdForUpdate(id).orElseThrow(CustomOrderWorkflowService::missing); }
    private void requireCustom(Order order) {
        if (order.getOrderType() != OrderType.CUSTOM) throw invalid("Chức năng này chỉ dành cho đơn hoa theo yêu cầu.");
    }
    private void requireShop(Order order, String shopId, String actor) {
        boolean member = order.getShop().getOwner().getId().equals(actor)
                || staff.findByShopIdAndUserId(shopId, actor).map(ShopStaff::isActive).orElse(false);
        if (!order.getShop().getId().equals(shopId) || !member) throw missing();
        if (order.getShop().getStatus() != ShopStatus.ACTIVE || order.getShop().getOwner().getStatus() != UserStatus.ACTIVE)
            throw invalid("Cửa hàng phải đang hoạt động.");
    }
    @Transactional
    public Receipt create(String actor, CreateInput input) {
        var request = input.design();
        var shop = shops.findByIdAndStatus(request.shopId(), ShopStatus.ACTIVE).orElseThrow(CustomOrderWorkflowService::missing);
        var customer = users.findByIdAndRoleAndStatus(actor, UserRole.CUSTOMER, UserStatus.ACTIVE).orElseThrow(CustomOrderWorkflowService::missing);
        var address = addresses.findByIdAndUserId(input.addressId(), actor).orElseThrow(CustomOrderWorkflowService::missing);
        // A design request must reach the shop even if its saved address still needs
        // carrier codes. Those codes are required when quoting shipping, not submitting.
        var design = new Design(null, request.description(), designs.design(request));
        var order = Order.builder().id(UUID.randomUUID().toString()).customer(customer).shop(shop)
                .deliveryAddress(address).recipientName(address.getRecipientName()).recipientPhone(address.getRecipientPhone())
                .orderType(OrderType.CUSTOM).subTotal(BigDecimal.ZERO).totalAmount(BigDecimal.ZERO)
                .customerNote(request.description()).status(OrderStatus.AWAITING_QUOTE).createdBy(actor).lastModifyBy(actor).build();
        order=orders.save(order);
        String encoded;
        try { encoded=json.writeValueAsString(design); } catch (Exception e) { throw new IllegalStateException("Không thể lưu thiết kế.", e); }
        details.save(OrderDetail.builder().id(UUID.randomUUID().toString()).order(order).customMaterials(encoded)
                .price(BigDecimal.ZERO).quantity(1).createdBy(actor).build());
        notifications.notifyUser(shop.getOwner(), "Yêu cầu đặt hoa mới",
                "Khách hàng đã gửi thiết kế hoa. Vui lòng xem và báo giá trong quản lý đơn hàng.",
                "/shop-admin/orders?open="+order.getId());
        return new Receipt(order.getId(), order.getStatus().name());
    }
    public Design design(Order order) {
        if (order.getOrderType() != OrderType.CUSTOM) return null;
        var detail = details.findCustomDesignByOrderId(order.getId()).orElse(null);
        if (detail==null) return null;
        try {
            var tree=json.readTree(detail.getCustomMaterials());
            // Read snapshots written as JSON strings by older VARCHAR bindings too.
            if(tree.isTextual()) tree=json.readTree(tree.textValue());
            if(!tree.isObject()) throw new IllegalArgumentException("Thiết kế phải là một đối tượng.");
            if(!tree.has("preferences")) {
                BigDecimal budget=order.getSubTotal();
                if(order.getCustomRequestId()!=null) {
                    try { budget=designs.detail(order.getCustomer().getId(),order.getCustomRequestId()).budget(); }
                    catch(ResponseStatusException missingHistory) { /* Keep old paid orders readable even without their request row. */ }
                }
                return new Design(budget,order.getCustomerNote(),designs.decode(tree.toString()),true);
            }
            return new Design(tree.path("budget").isNumber()?tree.path("budget").decimalValue():null, tree.path("description").isNull() ? null : tree.path("description").asText(),
                    designs.decode(tree.path("preferences").toString()));
        }
        catch (Exception e) { throw new IllegalStateException("Không đọc được thiết kế hoa riêng.", e); }
    }
    @Transactional
    public void quote(String shopId, String actor, String id, Quote quote) {
        var order=locked(id); requireCustom(order); requireShop(order, shopId, actor);
        if (order.getStatus()!=OrderStatus.AWAITING_QUOTE && order.getStatus()!=OrderStatus.QUOTED) throw invalid("Đơn không còn chờ báo giá.");
        var design=design(order);
        if (design==null) throw invalid("Không tìm thấy thiết kế để báo giá.");
        var pickup=addresses.findPickupByShopId(shopId).orElse(null);
        var destination=order.getDeliveryAddress();
        if (!hasCarrierAddress(destination))
            throw invalid("Khách cần bổ sung địa chỉ nhận hoa trước khi shop chốt báo giá.");
        if (!hasCarrierAddress(pickup))
            throw invalid("Cửa hàng cần cập nhật địa chỉ lấy hàng GHN trước khi báo giá.");
        var shippingFee=ghn.calculateFee(pickup.getGhnDistrictId(), pickup.getGhnWardCode(),
                destination.getGhnDistrictId(), destination.getGhnWardCode(), GhnService.DEFAULT_WEIGHT_GRAM);
        order.setSubTotal(quote.amount()); order.setShippingFee(shippingFee); order.setTotalAmount(quote.amount().add(shippingFee));
        order.setShopNote(quote.note()==null ? "" : quote.note().strip()); order.setStatus(OrderStatus.QUOTED); order.setLastModifyBy(actor);
        var line=details.findCustomDesignByOrderId(id).orElseThrow(CustomOrderWorkflowService::missing);
        line.setPrice(quote.amount());
        notifications.notifyUser(order.getCustomer(), "Shop đã báo giá hoa theo yêu cầu",
                "Vui lòng xem báo giá, đồng ý thiết kế và chọn phương thức thanh toán.", "/orders/"+id);
    }
    @Transactional
    public Receipt accept(String actor, String id, Accept input) {
        var order=locked(id); requireCustom(order);
        if (!order.getCustomer().getId().equals(actor)) throw missing();
        if (order.getStatus()!=OrderStatus.QUOTED) throw invalid("Đơn chưa được báo giá hoặc đã được xử lý.");
        vouchers.apply(order,context(order),input.couponCode(),input.shippingCouponCode());
        if (input.expectedTotal()==null || order.getTotalAmount().compareTo(input.expectedTotal())!=0)
            throw invalid("Báo giá đã thay đổi. Vui lòng tải lại đơn hàng và kiểm tra trước khi đồng ý.");
        if (order.getDeliveryAddress()==null) throw invalid("Vui lòng bổ sung địa chỉ nhận hàng.");
        if (input.paymentMethod()!=PaymentMethod.COD && input.paymentMethod()!=PaymentMethod.ONLINE) throw invalid("Chọn COD hoặc thanh toán trực tuyến.");
        order.setPaymentStartedAt(LocalDateTime.now()); order.setLastModifyBy(actor);
        order.setStatus(input.paymentMethod()==PaymentMethod.COD ? OrderStatus.PROCESSING : OrderStatus.PENDING);
        if (input.paymentMethod()==PaymentMethod.COD) {
            payments.save(Payment.builder().id(UUID.randomUUID().toString()).order(order).paymentType(PaymentType.FULL)
                    .paymentMethod(PaymentMethod.COD).amount(order.getTotalAmount()).status(PaymentStatus.PENDING).createdBy(actor).build());
        }
        return new Receipt(id, order.getStatus().name());
    }
    @Transactional
    public void deliveryAddress(String actor, String id, DeliveryInput input) {
        var order=locked(id); requireCustom(order);
        if (!order.getCustomer().getId().equals(actor)) throw missing();
        if (order.getStatus()!=OrderStatus.AWAITING_QUOTE && order.getStatus()!=OrderStatus.QUOTED)
            throw invalid("Không thể thay đổi địa chỉ sau khi đã đồng ý báo giá.");
        var address=addresses.findByIdAndUserId(input.addressId(), actor).orElseThrow(CustomOrderWorkflowService::missing);
        if (!hasCarrierAddress(address))
            throw invalid("Vui lòng cập nhật địa chỉ theo danh mục GHN.");
        order.setDeliveryAddress(address); order.setRecipientName(address.getRecipientName()); order.setRecipientPhone(address.getRecipientPhone());
        order.setStatus(OrderStatus.AWAITING_QUOTE); order.setLastModifyBy(actor);
    }

    private CouponService.Context context(Order order) {
        // A custom design is a quoted batch, not a purchase of its reference catalog product.
        return new CouponService.Context(order.getShop().getId(),order.getSubTotal(),order.getShippingFee(),Map.of());
    }
    public static boolean hasCarrierAddress(Address address) {
        return address!=null && address.getGhnDistrictId()!=null && address.getGhnDistrictId()>0
                && address.getGhnWardCode()!=null && !address.getGhnWardCode().isBlank();
    }
    public CouponService.Preview previewVouchers(String actor,String id,VoucherInput input) {
        var order=orders.findByIdAndCustomerId(id,actor).orElseThrow(CustomOrderWorkflowService::missing);
        requireCustom(order);
        if(order.getStatus()!=OrderStatus.QUOTED) throw invalid("Đơn chưa được báo giá hoặc đã được xử lý.");
        return vouchers.preview(context(order),input.couponCode(),input.shippingCouponCode());
    }
}
