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
    public record Accept(@NotNull PaymentMethod paymentMethod, @NotNull @DecimalMin("1") BigDecimal expectedTotal) {}
    public record DeliveryInput(@NotBlank String addressId) {}
    public record Design(BigDecimal budget, String description, CustomFlowerOrderService.Design preferences) {}
    public record Receipt(String orderId, String status) {}
    private final OrderRepository orders;
    private final OrderDetailRepository details;
    private final PaymentRepository payments;
    private final AddressRepository addresses;
    private final ShopRepository shops;
    private final UserRepository users;
    private final CustomFlowerOrderService designs;
    private final GhnService ghn;
    private final ObjectMapper json = new ObjectMapper();

    public CustomOrderWorkflowService(OrderRepository orders, OrderDetailRepository details, PaymentRepository payments,
            AddressRepository addresses, ShopRepository shops, UserRepository users, CustomFlowerOrderService designs, GhnService ghn) {
        this.orders=orders; this.details=details; this.payments=payments; this.addresses=addresses;
        this.shops=shops; this.users=users; this.designs=designs;
        this.ghn=ghn;
    }
    private static ResponseStatusException invalid(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
    private static ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng thuộc quyền truy cập của bạn."); }
    private Order locked(String id) { return orders.findByIdForUpdate(id).orElseThrow(CustomOrderWorkflowService::missing); }
    private void requireCustom(Order order) {
        if (order.getOrderType() != OrderType.CUSTOM) throw invalid("Chức năng này chỉ dành cho đơn hoa theo yêu cầu.");
    }
    private void requireShop(Order order, String shopId, String actor) {
        if (!order.getShop().getId().equals(shopId) || !order.getShop().getOwner().getId().equals(actor)) throw missing();
        if (order.getShop().getStatus() != ShopStatus.ACTIVE || order.getShop().getOwner().getStatus() != UserStatus.ACTIVE)
            throw invalid("Cửa hàng phải đang hoạt động.");
    }
    @Transactional
    public Receipt create(String actor, CreateInput input) {
        var request = input.design();
        var shop = shops.findById(request.shopId()).filter(s -> s.getStatus() == ShopStatus.ACTIVE).orElseThrow(CustomOrderWorkflowService::missing);
        var customer = users.findById(actor).filter(u -> u.getRole() == UserRole.CUSTOMER && u.getStatus() == UserStatus.ACTIVE).orElseThrow(CustomOrderWorkflowService::missing);
        var address = addresses.findById(input.addressId()).filter(a -> a.getUser()!=null && a.getUser().getId().equals(actor)).orElseThrow(CustomOrderWorkflowService::missing);
        if (address.getGhnDistrictId()==null || address.getGhnWardCode()==null || address.getGhnWardCode().isBlank())
            throw invalid("Vui lòng cập nhật địa chỉ nhận hàng theo danh mục GHN.");
        var design = new Design(request.budget(), request.description(), designs.design(request));
        var order = Order.builder().id(UUID.randomUUID().toString()).customer(customer).shop(shop)
                .deliveryAddress(address).recipientName(address.getRecipientName()).recipientPhone(address.getRecipientPhone())
                .orderType(OrderType.CUSTOM).subTotal(BigDecimal.ZERO).totalAmount(BigDecimal.ZERO)
                .customerNote(request.description()).status(OrderStatus.AWAITING_QUOTE).createdBy(actor).lastModifyBy(actor).build();
        orders.save(order);
        String encoded;
        try { encoded=json.writeValueAsString(design); } catch (Exception e) { throw new IllegalStateException("Không thể lưu thiết kế.", e); }
        details.save(OrderDetail.builder().id(UUID.randomUUID().toString()).order(order).customMaterials(encoded)
                .price(BigDecimal.ZERO).quantity(1).createdBy(actor).build());
        return new Receipt(order.getId(), order.getStatus().name());
    }
    public Design design(Order order) {
        if (order.getOrderType() != OrderType.CUSTOM) return null;
        var detail = details.findByOrderId(order.getId()).stream().filter(d -> d.getCustomMaterials()!=null).findFirst().orElse(null);
        if (detail==null) return null;
        try {
            var tree=json.readTree(detail.getCustomMaterials());
            return new Design(tree.path("budget").decimalValue(), tree.path("description").isNull() ? null : tree.path("description").asText(),
                    designs.decode(tree.path("preferences").toString()));
        }
        catch (Exception e) { throw new IllegalStateException("Không đọc được thiết kế hoa riêng.", e); }
    }
    @Transactional
    public void quote(String shopId, String actor, String id, Quote quote) {
        var order=locked(id); requireCustom(order); requireShop(order, shopId, actor);
        if (order.getStatus()!=OrderStatus.AWAITING_QUOTE && order.getStatus()!=OrderStatus.QUOTED) throw invalid("Đơn không còn chờ báo giá.");
        var design=design(order);
        if (design==null || quote.amount().compareTo(design.budget())>0) throw invalid("Giá hoa không được vượt ngân sách khách hàng.");
        var pickupList=addresses.findByShopIdAndUserIsNullOrderByCreatedDateAscIdAsc(shopId);
        var pickup=pickupList.stream().filter(a -> Boolean.TRUE.equals(a.getIsDefault())).findFirst()
                .orElse(pickupList.isEmpty() ? null : pickupList.get(0));
        var destination=order.getDeliveryAddress();
        if (destination==null || destination.getGhnDistrictId()==null || destination.getGhnWardCode()==null)
            throw invalid("Khách cần bổ sung địa chỉ nhận hoa trước khi shop chốt báo giá.");
        if (pickup==null || pickup.getGhnDistrictId()==null || pickup.getGhnWardCode()==null)
            throw invalid("Cửa hàng cần cập nhật địa chỉ lấy hàng GHN trước khi báo giá.");
        var shippingFee=ghn.calculateFee(pickup.getGhnDistrictId(), pickup.getGhnWardCode(),
                destination.getGhnDistrictId(), destination.getGhnWardCode(), GhnService.DEFAULT_WEIGHT_GRAM);
        order.setSubTotal(quote.amount()); order.setShippingFee(shippingFee); order.setTotalAmount(quote.amount().add(shippingFee));
        order.setShopNote(quote.note()==null ? "" : quote.note().strip()); order.setStatus(OrderStatus.QUOTED); order.setLastModifyBy(actor);
        var line=details.findByOrderId(id).get(0);
        line.setPrice(quote.amount());
    }
    @Transactional
    public Receipt accept(String actor, String id, Accept input) {
        var order=locked(id); requireCustom(order);
        if (!order.getCustomer().getId().equals(actor)) throw missing();
        if (order.getStatus()!=OrderStatus.QUOTED) throw invalid("Đơn chưa được báo giá hoặc đã được xử lý.");
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
        var address=addresses.findById(input.addressId()).filter(a -> a.getUser()!=null && a.getUser().getId().equals(actor)).orElseThrow(CustomOrderWorkflowService::missing);
        if (address.getGhnDistrictId()==null || address.getGhnWardCode()==null || address.getGhnWardCode().isBlank())
            throw invalid("Vui lòng cập nhật địa chỉ theo danh mục GHN.");
        order.setDeliveryAddress(address); order.setRecipientName(address.getRecipientName()); order.setRecipientPhone(address.getRecipientPhone());
        order.setStatus(OrderStatus.AWAITING_QUOTE); order.setLastModifyBy(actor);
    }
}
