package com.example.flowershop.service;

import com.example.flowershop.entity.CartItem;
import com.example.flowershop.entity.Coupon;
import com.example.flowershop.entity.Shop;
import com.example.flowershop.entity.enums.CouponStatus;
import com.example.flowershop.entity.enums.DiscountType;
import com.example.flowershop.entity.enums.OrderStatus;
import com.example.flowershop.entity.enums.ShopStatus;
import com.example.flowershop.exception.ApiException;
import com.example.flowershop.repository.CartItemRepository;
import com.example.flowershop.repository.CouponRepository;
import com.example.flowershop.repository.OrderRepository;
import com.example.flowershop.repository.ShopRepository;
import com.example.flowershop.repository.UserRepository;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Voucher giảm tiền đơn hàng: quản lý (admin cho voucher sàn, chủ shop cho voucher của shop),
 * xem trước khi thanh toán và áp mã khi đặt đơn. Mỗi khách chỉ được dùng 1 mã (đơn chưa hủy đang giữ mã).
 */
@Service
@Transactional(readOnly = true)
public class CouponService {
    public static final String ONE_VOUCHER_ONLY = "Bạn đã dùng mã giảm giá rồi, mỗi người chỉ được dùng 1 mã.";
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    public record Input(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{3,50}", message = "Mã gồm 3 đến 50 ký tự chữ, số, gạch ngang hoặc gạch dưới") String code,
            @NotNull DiscountType discountType,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal discountValue,
            @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal minOrderValue,
            @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal maxDiscountValue,
            @NotNull LocalDateTime startDate, @NotNull LocalDateTime endDate,
            @NotNull @Min(1) Integer usageLimit, @NotNull CouponStatus status) {}

    public record View(String id, String code, String shopId, DiscountType discountType, BigDecimal discountValue,
            BigDecimal minOrderValue, BigDecimal maxDiscountValue, LocalDateTime startDate, LocalDateTime endDate,
            int usageLimit, int usedCount, CouponStatus status) {}

    public record Choice(String id, String code, String scope, DiscountType discountType, BigDecimal discountValue,
            BigDecimal maxDiscountValue, BigDecimal minOrderValue, BigDecimal saving, boolean eligible,
            String reason, LocalDateTime endDate) {}

    public record Preview(List<Choice> vouchers, String recommendedCode, BigDecimal bestSaving, String selectedCode,
            BigDecimal discountAmount, BigDecimal subTotal, BigDecimal totalAfterDiscount) {}

    /** Kết quả áp mã: id mã đã giữ lượt và số tiền giảm. */
    public record Applied(String couponId, BigDecimal discountAmount) {}

    private final CouponRepository coupons;
    private final ShopRepository shops;
    private final OrderRepository orders;
    private final CartItemRepository cartItems;
    private final UserRepository users;

    public CouponService(CouponRepository coupons, ShopRepository shops, OrderRepository orders,
            CartItemRepository cartItems, UserRepository users) {
        this.coupons = coupons; this.shops = shops; this.orders = orders; this.cartItems = cartItems; this.users = users;
    }

    private static LocalDateTime now() { return LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")); }

    private Shop owned(String shopId, String actor) {
        return shops.findById(shopId).filter(s -> s.getOwner().getId().equals(actor))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy cửa hàng thuộc quyền quản lý."));
    }

    private static View view(Coupon c) {
        return new View(c.getId(), c.getCode(), c.getShop() == null ? null : c.getShop().getId(), c.getDiscountType(),
                c.getDiscountValue(), c.getMinOrderValue(), c.getMaxDiscountValue(), c.getStartDate(), c.getEndDate(),
                c.getUsageLimit(), c.getUsedCount(), c.getStatus());
    }

    /** shopId null: voucher sàn (admin); khác null: voucher của shop do actor làm chủ. */
    public List<View> list(String actor, String shopId) {
        if (shopId != null) owned(shopId, actor);
        return (shopId == null ? coupons.managedPlatform() : coupons.managedByShop(shopId)).stream().map(CouponService::view).toList();
    }

    private Coupon managed(String actor, String shopId, String id) {
        if (shopId != null) owned(shopId, actor);
        return coupons.findLocked(id).filter(c -> !c.isDeleted() && (shopId == null ? c.getShop() == null
                        : c.getShop() != null && shopId.equals(c.getShop().getId())))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy mã giảm giá thuộc quyền quản lý."));
    }

    @Transactional
    public View save(String actor, String shopId, String id, Input in) {
        Shop shop = shopId == null ? null : owned(shopId, actor);
        if (shop != null && shop.getStatus() != ShopStatus.ACTIVE)
            throw ApiException.badRequest("Chỉ cửa hàng đang hoạt động mới được tạo hoặc sửa mã giảm giá.");
        if (!in.endDate().isAfter(in.startDate())) throw ApiException.badRequest("Ngày kết thúc phải sau ngày bắt đầu.");
        if (in.discountType() == DiscountType.PERCENTAGE) {
            if (in.discountValue().compareTo(HUNDRED) > 0)
                throw ApiException.badRequest("Phần trăm giảm phải lớn hơn 0 và không quá 100%.");
            if (in.maxDiscountValue() == null || in.maxDiscountValue().signum() <= 0)
                throw ApiException.badRequest("Mã giảm theo phần trăm phải có mức giảm tối đa lớn hơn 0.");
        } else if (in.maxDiscountValue() != null && in.maxDiscountValue().signum() <= 0) {
            throw ApiException.badRequest("Mức giảm tối đa phải lớn hơn 0.");
        }
        String code = in.code().strip().toUpperCase(Locale.ROOT);
        coupons.findByCodeIgnoreCase(code).filter(c -> !c.getId().equals(id)).ifPresent(c -> {
            throw ApiException.badRequest("Mã giảm giá đã tồn tại.");
        });
        Coupon c = id == null ? Coupon.builder().id(UUID.randomUUID().toString()).createdBy(actor).build() : managed(actor, shopId, id);
        if (in.usageLimit() < c.getUsedCount()) throw ApiException.badRequest("Số lượt tối đa không được nhỏ hơn số lượt đã dùng.");
        c.setShop(shop); c.setCode(code); c.setDiscountType(in.discountType()); c.setDiscountValue(in.discountValue());
        c.setMinOrderValue(in.minOrderValue()); c.setMaxDiscountValue(in.maxDiscountValue());
        c.setStartDate(in.startDate()); c.setEndDate(in.endDate()); c.setUsageLimit(in.usageLimit());
        c.setStatus(in.status()); c.setLastModifyBy(actor);
        return view(coupons.saveAndFlush(c));
    }

    /** Xóa mềm: đơn cũ vẫn tham chiếu được mã, nhưng mã không còn dùng được. */
    @Transactional
    public void delete(String actor, String shopId, String id) {
        Coupon c = managed(actor, shopId, id);
        c.setDeleted(true); c.setStatus(CouponStatus.DISABLED); c.setLastModifyBy(actor);
    }

    /** Lý do mã không dùng được cho đơn (null nếu dùng được). customerUsedOne: khách đang giữ một mã ở đơn chưa hủy. */
    private String reason(Coupon c, String shopId, BigDecimal subTotal, boolean customerUsedOne) {
        if (c.isDeleted() || c.getStatus() != CouponStatus.ACTIVE) return "Mã đã ngừng áp dụng";
        LocalDateTime time = now();
        if (time.isBefore(c.getStartDate())) return "Mã chưa đến ngày bắt đầu";
        if (time.isAfter(c.getEndDate())) return "Mã đã hết hạn";
        if (c.getUsedCount() >= c.getUsageLimit()) return "Mã đã hết lượt sử dụng";
        if (customerUsedOne) return "Bạn đã dùng mã giảm giá rồi, mỗi người chỉ được dùng 1 mã";
        if (c.getShop() != null && !c.getShop().getId().equals(shopId)) return "Mã không áp dụng cho shop này";
        if (c.getDiscountValue() == null || c.getDiscountValue().signum() <= 0
                || (c.getDiscountType() == DiscountType.PERCENTAGE && c.getDiscountValue().compareTo(HUNDRED) > 0))
            return "Mã chưa được cấu hình hợp lệ";
        if (subTotal.compareTo(c.getMinOrderValue() == null ? BigDecimal.ZERO : c.getMinOrderValue()) < 0)
            return "Đơn chưa đạt giá trị tối thiểu";
        BigDecimal saving = saving(c, subTotal);
        if (saving.signum() <= 0) return "Mã không giảm thêm cho đơn này";
        if (saving.compareTo(subTotal) >= 0) return "Mã giảm hết giá trị đơn, tổng thanh toán phải lớn hơn 0";
        return null;
    }

    private static BigDecimal saving(Coupon c, BigDecimal subTotal) {
        BigDecimal amount = c.getDiscountType() == DiscountType.PERCENTAGE
                ? subTotal.multiply(c.getDiscountValue()).divide(HUNDRED, 2, RoundingMode.HALF_UP)
                : c.getDiscountValue();
        if (c.getMaxDiscountValue() != null) amount = amount.min(c.getMaxDiscountValue());
        return amount.min(subTotal).max(BigDecimal.ZERO);
    }

    private BigDecimal subTotalOf(String userId, String shopId, List<String> cartItemIds) {
        BigDecimal sum = BigDecimal.ZERO;
        for (String id : cartItemIds) {
            CartItem item = cartItems.findById(id).orElseThrow(() -> ApiException.notFound("Cart item không tồn tại: " + id));
            if (!item.getUser().getId().equals(userId)) throw ApiException.forbidden("Cart item không thuộc về bạn: " + id);
            if (!item.getProduct().getShop().getId().equals(shopId))
                throw ApiException.badRequest("Sản phẩm '" + item.getProduct().getName() + "' không thuộc shop đã chọn.");
            sum = sum.add(item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return sum;
    }

    /**
     * Chỉ đọc: các mã xét cho đơn của shop (kèm lý do không dùng được), mã tiết kiệm nhất và,
     * nếu khách đã chọn selectedCode, số tiền giảm và tổng sau giảm (chưa tính phí giao hàng).
     */
    /** Đơn đã thanh toán (hoặc COD đã được shop xác nhận) giữ mã là lúc khách bị khóa không dùng thêm mã. */
    public static final java.util.List<OrderStatus> VOUCHER_USED_STATUSES = java.util.List.of(
            OrderStatus.DEPOSIT_PAID, OrderStatus.PROCESSING, OrderStatus.DELIVERING, OrderStatus.COMPLETED, OrderStatus.REFUNDED);

    public Preview preview(String userId, String shopId, List<String> cartItemIds, String selectedCode) {
        BigDecimal subTotal = subTotalOf(userId, shopId, cartItemIds);
        boolean usedOne = orders.existsByCustomerIdAndCouponIdIsNotNullAndStatusIn(userId, VOUCHER_USED_STATUSES);
        List<Choice> choices = new ArrayList<>();
        Coupon best = null;
        BigDecimal bestSaving = BigDecimal.ZERO;
        Coupon chosen = null;
        String code = selectedCode == null ? "" : selectedCode.strip();
        for (Coupon c : coupons.available(shopId)) {
            String error = reason(c, shopId, subTotal, usedOne);
            BigDecimal amount = error == null ? saving(c, subTotal) : BigDecimal.ZERO;
            choices.add(new Choice(c.getId(), c.getCode(), c.getShop() == null ? "PLATFORM" : "SHOP", c.getDiscountType(),
                    c.getDiscountValue(), c.getMaxDiscountValue(), c.getMinOrderValue(), amount, error == null, error, c.getEndDate()));
            if (error == null && (best == null || amount.compareTo(bestSaving) > 0)) { best = c; bestSaving = amount; }
            if (!code.isEmpty() && c.getCode().equalsIgnoreCase(code)) {
                if (error != null) throw ApiException.badRequest(c.getCode() + ": " + error + ".");
                chosen = c;
            }
        }
        if (!code.isEmpty() && chosen == null) throw ApiException.badRequest("Mã giảm giá không tồn tại hoặc không áp dụng cho shop này.");
        BigDecimal discount = chosen == null ? BigDecimal.ZERO : saving(chosen, subTotal);
        return new Preview(choices, best == null ? null : best.getCode(), bestSaving, chosen == null ? null : chosen.getCode(),
                discount, subTotal, subTotal.subtract(discount));
    }

    /**
     * Gọi trong transaction đặt đơn. Khóa dòng khách để hai yêu cầu song song không cùng dùng hai mã,
     * khóa mã, kiểm tra lại toàn bộ điều kiện rồi giữ một lượt dùng.
     */
    @Transactional
    public Applied apply(String userId, String shopId, BigDecimal subTotal, String code) {
        users.findByIdForUpdate(userId);
        if (orders.existsByCustomerIdAndCouponIdIsNotNullAndStatusIn(userId, VOUCHER_USED_STATUSES))
            throw ApiException.badRequest(ONE_VOUCHER_ONLY);
        Coupon c = coupons.findByCodeForUpdate(code.trim()).orElseThrow(() -> ApiException.badRequest("Mã giảm giá không hợp lệ."));
        String error = reason(c, shopId, subTotal, false);
        if (error != null) throw ApiException.badRequest("Mã giảm giá không áp dụng được: " + error + ".");
        BigDecimal discount = saving(c, subTotal);
        c.setUsedCount(c.getUsedCount() + 1);
        coupons.save(c);
        return new Applied(c.getId(), discount);
    }

    /** Trả lại một lượt dùng khi đơn giữ mã bị hủy. */
    @Transactional
    public void release(String couponId) {
        coupons.findLocked(couponId).ifPresent(c -> c.setUsedCount(Math.max(0, c.getUsedCount() - 1)));
    }
}
