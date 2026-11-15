package com.example.flowershop.service;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import com.example.flowershop.exception.ApiException;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class CouponService {
    public record Input(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{3,50}") String code,
            @NotNull CouponTarget target, @NotNull DiscountType discountType,
            @NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal discountValue,
            @NotNull @DecimalMin("0") @Digits(integer=10,fraction=2) BigDecimal minOrderValue,
            @NotNull @DecimalMin(value="0",inclusive=false) @DecimalMax("100000") @Digits(integer=6,fraction=0) BigDecimal maxDiscountValue,
            @NotNull LocalDateTime startDate, @NotNull LocalDateTime endDate,
            @NotNull @Min(1) Integer usageLimit, @NotNull CouponStatus status, String productId) {}
    public record View(String id, String code, String shopId, CouponTarget target, DiscountType discountType,
            BigDecimal discountValue, BigDecimal minOrderValue, BigDecimal maxDiscountValue,
            LocalDateTime startDate, LocalDateTime endDate, int usageLimit, int usedCount,
            CouponStatus status, String productId) {}
    public record Context(String shopId, BigDecimal subTotal, BigDecimal shippingFee, Map<String,BigDecimal> productTotals) {}
    public record Choice(String id, String code, String scope, CouponTarget target, DiscountType discountType,
            BigDecimal discountValue, BigDecimal maxDiscountValue, BigDecimal minOrderValue,
            BigDecimal saving, boolean eligible, String reason, LocalDateTime endDate) {}
    public record Preview(List<Choice> orderVouchers, List<Choice> shippingVouchers,
            String recommendedOrderCode, String recommendedShippingCode, BigDecimal bestSaving,
            BigDecimal discountAmount, BigDecimal shippingDiscountAmount, BigDecimal totalAmount) {}
    private final CouponRepository coupons;
    private final ShopRepository shops;
    private final ProductRepository products;
    private final OrderRepository orders;
    public CouponService(CouponRepository coupons, ShopRepository shops, ProductRepository products, OrderRepository orders) {
        this.coupons=coupons; this.shops=shops; this.products=products; this.orders=orders;
    }
    private LocalDateTime now() { return LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")); }
    private Shop owned(String shopId, String actor) {
        return shops.findById(shopId).filter(s -> s.getOwner().getId().equals(actor))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy cửa hàng thuộc quyền quản lý."));
    }
    private View view(Coupon c) {
        return new View(c.getId(),c.getCode(),c.getShop()==null?null:c.getShop().getId(),c.getTarget(),c.getDiscountType(),
                c.getDiscountValue(),c.getMinOrderValue(),c.getMaxDiscountValue(),c.getStartDate(),c.getEndDate(),
                c.getUsageLimit(),c.getUsedCount(),c.getStatus(),c.getProduct()==null?null:c.getProduct().getId());
    }
    public List<View> list(String actor, String shopId) {
        if(shopId!=null) owned(shopId,actor);
        return coupons.managed(shopId).stream().map(this::view).toList();
    }
    private Coupon managed(String actor,String shopId,String id) {
        if(shopId!=null) owned(shopId,actor);
        return coupons.findLocked(id).filter(c -> !c.isDeleted() && (shopId==null ? c.getShop()==null
                : c.getShop()!=null && shopId.equals(c.getShop().getId())))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy mã giảm giá thuộc quyền quản lý."));
    }
    @Transactional
    public View save(String actor,String shopId,String id,Input input) {
        Shop shop=shopId==null?null:owned(shopId,actor);
        if(shop!=null && (shop.getStatus()!=ShopStatus.ACTIVE || input.target()!=CouponTarget.ORDER))
            throw ApiException.badRequest("Shop đang hoạt động chỉ được tạo mã giảm tiền đơn hàng của chính shop.");
        if(!input.endDate().isAfter(input.startDate())) throw ApiException.badRequest("Ngày kết thúc phải sau ngày bắt đầu.");
        if(input.discountType()==DiscountType.PERCENTAGE && input.discountValue().compareTo(new BigDecimal("100"))>0)
            throw ApiException.badRequest("Phần trăm giảm phải lớn hơn 0 và không quá 100%.");
        if(input.maxDiscountValue().signum()<=0 || input.maxDiscountValue().compareTo(new BigDecimal("100000"))>0)
            throw ApiException.badRequest("Trần giảm phải lớn hơn 0 và không quá 100.000 đồng.");
        String code=input.code().strip().toUpperCase(Locale.ROOT);
        coupons.findByCodeIgnoreCase(code).filter(c -> !c.getId().equals(id)).ifPresent(c -> {
            throw ApiException.badRequest("Mã giảm giá đã tồn tại.");
        });
        Coupon c=id==null?Coupon.builder().id(UUID.randomUUID().toString()).createdBy(actor).build():managed(actor,shopId,id);
        if(input.usageLimit()<c.getUsedCount()) throw ApiException.badRequest("Số lượt tối đa không được nhỏ hơn số lượt đã dùng.");
        Product product=null;
        if(input.target()==CouponTarget.PRODUCT) {
            product=products.findById(input.productId()==null?"":input.productId())
                    .filter(p -> p.getStatus()==ProductStatus.ACTIVE && p.getShop().getStatus()==ShopStatus.ACTIVE)
                    .orElseThrow(() -> ApiException.badRequest("Chọn sản phẩm đang bán cho voucher sản phẩm."));
        } else if(input.productId()!=null && !input.productId().isBlank()) throw ApiException.badRequest("Chỉ mã sản phẩm mới có sản phẩm áp dụng.");
        c.setShop(shop); c.setProduct(product); c.setCode(code); c.setTarget(input.target());
        c.setDiscountType(input.discountType()); c.setDiscountValue(input.discountValue()); c.setMinOrderValue(input.minOrderValue());
        c.setMaxDiscountValue(input.maxDiscountValue()); c.setStartDate(input.startDate()); c.setEndDate(input.endDate());
        c.setUsageLimit(input.usageLimit()); c.setStatus(input.status()); c.setLastModifyBy(actor);
        return view(coupons.saveAndFlush(c));
    }
    @Transactional
    public void delete(String actor,String shopId,String id) {
        Coupon c=managed(actor,shopId,id); c.setDeleted(true); c.setStatus(CouponStatus.DISABLED); c.setLastModifyBy(actor);
    }
    private String reason(Coupon c, Context ctx, Set<String> usedByCustomer) {
        if(c.isDeleted() || c.getStatus()!=CouponStatus.ACTIVE) return "Mã đã ngừng áp dụng";
        var time=now();
        if(time.isBefore(c.getStartDate())) return "Mã chưa đến ngày bắt đầu";
        if(!time.isBefore(c.getEndDate())) return "Mã đã hết hạn";
        if(c.getUsedCount()>=c.getUsageLimit()) return "Mã đã hết lượt sử dụng";
        if(!usedByCustomer.isEmpty()) return "Bạn đã dùng mã giảm giá rồi, mỗi người chỉ được dùng 1 mã";
        if(c.getShop()!=null && (!c.getShop().getId().equals(ctx.shopId()) || c.getTarget()!=CouponTarget.ORDER)) return "Mã không áp dụng cho shop này";
        if(c.getDiscountValue()==null || c.getDiscountValue().signum()<=0 || c.getMaxDiscountValue()==null
                || c.getMaxDiscountValue().signum()<=0 || c.getMaxDiscountValue().compareTo(new BigDecimal("100000"))>0
                || (c.getDiscountType()==DiscountType.PERCENTAGE && c.getDiscountValue().compareTo(new BigDecimal("100"))>0)) return "Mã chưa được cấu hình hợp lệ";
        if(ctx.subTotal().compareTo(c.getMinOrderValue())<0) return "Đơn chưa đạt giá trị tối thiểu";
        if(c.getTarget()==CouponTarget.PRODUCT && (c.getProduct()==null || !ctx.productTotals().containsKey(c.getProduct().getId()))) return "Đơn không có sản phẩm áp dụng";
        if(saving(c,ctx).signum()<=0) return "Mã không giảm thêm cho đơn này";
        return null;
    }
    private BigDecimal saving(Coupon c,Context ctx) {
        BigDecimal base=switch(c.getTarget()) {
            case SHIPPING -> ctx.shippingFee();
            case PRODUCT -> c.getProduct()==null?BigDecimal.ZERO:ctx.productTotals().getOrDefault(c.getProduct().getId(),BigDecimal.ZERO);
            case ORDER -> ctx.subTotal();
        };
        BigDecimal amount=c.getDiscountType()==DiscountType.PERCENTAGE
                ? base.multiply(c.getDiscountValue()).divide(new BigDecimal("100"),0,RoundingMode.DOWN):c.getDiscountValue();
        BigDecimal limit=c.getTarget()==CouponTarget.SHIPPING?base:base.min(ctx.subTotal().subtract(BigDecimal.ONE).max(BigDecimal.ZERO));
        return amount.min(c.getMaxDiscountValue()).min(limit).max(BigDecimal.ZERO).setScale(0,RoundingMode.DOWN);
    }
    private Comparator<Coupon> bySaving(Context ctx) { return Comparator.comparing((Coupon c) -> saving(c,ctx)).thenComparing(Coupon::getCode); }
    private Coupon selected(List<Coupon> all,String code,boolean shipping,Context ctx,Set<String> used) {
        if(code==null || code.isBlank()) return null;
        var c=all.stream().filter(v -> v.getCode().equalsIgnoreCase(code.strip())).findFirst()
                .orElseThrow(() -> ApiException.badRequest("Mã giảm giá không tồn tại hoặc không áp dụng cho shop này."));
        String error=reason(c,ctx,used);
        if(error!=null) throw ApiException.badRequest(c.getCode()+": "+error);
        if((c.getTarget()==CouponTarget.SHIPPING)!=shipping) throw ApiException.badRequest("Mã không đúng loại giảm giá đã chọn.");
        return c;
    }
    /** Không biết khách là ai: bỏ qua luật mỗi người chỉ dùng 1 mã giảm giá. */
    public Preview preview(Context ctx,String orderCode,String shippingCode) {
        return evaluate(ctx,orderCode,shippingCode,coupons.available(ctx.shopId()),Set.of());
    }
    /** Xem trước cho một khách: nếu khách đã dùng một mã cho đơn chưa hủy thì mọi mã đều bị đánh dấu không dùng được. */
    public Preview preview(Context ctx,String customerId,String orderCode,String shippingCode) {
        return preview(ctx,customerId,null,orderCode,shippingCode);
    }
    /** Như trên, bỏ qua mã đang gắn trên chính đơn excludeOrderId (đơn báo giá đang xem). */
    public Preview preview(Context ctx,String customerId,String excludeOrderId,String orderCode,String shippingCode) {
        return evaluate(ctx,orderCode,shippingCode,coupons.available(ctx.shopId()),orders.couponIdsUsedBy(customerId,excludeOrderId));
    }
    private static final String ONE_VOUCHER_ONLY = "Mỗi người chỉ được dùng 1 mã giảm giá";
    private Preview evaluate(Context ctx,String orderCode,String shippingCode,List<Coupon> all,Set<String> used) {
        boolean hasOrder=orderCode!=null && !orderCode.isBlank(), hasShipping=shippingCode!=null && !shippingCode.isBlank();
        if(hasOrder && hasShipping) throw ApiException.badRequest(ONE_VOUCHER_ONLY+": chỉ chọn mã giảm đơn hàng hoặc mã giảm phí vận chuyển.");
        Coupon selectedMoney=selected(all,orderCode,false,ctx,used), selectedShip=selected(all,shippingCode,true,ctx,used);
        Coupon best=all.stream().filter(c -> reason(c,ctx,used)==null).max(bySaving(ctx)).orElse(null);
        List<Choice> moneyChoices=new ArrayList<>(),shipChoices=new ArrayList<>();
        for(Coupon c:all.stream().sorted(Comparator.comparing(Coupon::getCode)).toList()) {
            String error=reason(c,ctx,used);
            BigDecimal amount=error==null?saving(c,ctx):BigDecimal.ZERO;
            Choice choice=new Choice(c.getId(),c.getCode(),c.getShop()==null?"PLATFORM":"SHOP",c.getTarget(),c.getDiscountType(),
                    c.getDiscountValue(),c.getMaxDiscountValue(),c.getMinOrderValue(),amount,error==null,error,c.getEndDate());
            (c.getTarget()==CouponTarget.SHIPPING?shipChoices:moneyChoices).add(choice);
        }
        BigDecimal discount=selectedMoney==null?BigDecimal.ZERO:saving(selectedMoney,ctx);
        BigDecimal shippingDiscount=selectedShip==null?BigDecimal.ZERO:saving(selectedShip,ctx);
        boolean bestIsShipping=best!=null && best.getTarget()==CouponTarget.SHIPPING;
        return new Preview(moneyChoices,shipChoices,best==null||bestIsShipping?null:best.getCode(),bestIsShipping?best.getCode():null,
                best==null?BigDecimal.ZERO:saving(best,ctx),discount,shippingDiscount,
                ctx.subTotal().add(ctx.shippingFee()).subtract(discount).subtract(shippingDiscount));
    }
    /** Call only within the order's write transaction. Both slots lock in deterministic code order. */
    @Transactional
    public Preview apply(Order order, Context ctx, String orderCode, String shippingCode) {
        var codes=new TreeSet<String>();
        if(orderCode!=null&&!orderCode.isBlank()) codes.add(orderCode.strip().toUpperCase(Locale.ROOT));
        if(shippingCode!=null&&!shippingCode.isBlank()) codes.add(shippingCode.strip().toUpperCase(Locale.ROOT));
        Map<String,Coupon> locked=new HashMap<>();
        for(String code:codes) locked.put(code,coupons.findByCodeForUpdate(code).orElseThrow(() -> ApiException.badRequest("Mã giảm giá không tồn tại.")));
        var all=coupons.available(ctx.shopId());
        Preview result=evaluate(ctx,orderCode,shippingCode,all,order.getCustomer()==null?Set.of():orders.couponIdsUsedByForUpdate(order.getCustomer().getId(),order.getId()));
        for(Coupon c:locked.values()) c.setUsedCount(c.getUsedCount()+1);
        Coupon money=orderCode==null?null:locked.get(orderCode.strip().toUpperCase(Locale.ROOT));
        Coupon ship=shippingCode==null?null:locked.get(shippingCode.strip().toUpperCase(Locale.ROOT));
        order.setCouponId(money==null?null:money.getId()); order.setCouponCode(money==null?null:money.getCode());
        order.setShippingCouponId(ship==null?null:ship.getId()); order.setShippingCouponCode(ship==null?null:ship.getCode());
        order.setDiscountAmount(result.discountAmount()); order.setShippingDiscountAmount(result.shippingDiscountAmount());
        order.setTotalAmount(result.totalAmount()); order.setCouponsReleased(false);
        return result;
    }
    @Transactional
    public void release(Order order) {
        if(order.isCouponsReleased()) return;
        var ids=new TreeSet<String>();
        if(order.getCouponId()!=null) ids.add(order.getCouponId());
        if(order.getShippingCouponId()!=null) ids.add(order.getShippingCouponId());
        // Same code order as apply, preventing opposite-slot deadlocks.
        var ordered=ids.stream().map(coupons::findById).flatMap(Optional::stream).sorted(Comparator.comparing(Coupon::getCode)).toList();
        for(Coupon value:ordered) coupons.findLocked(value.getId()).ifPresent(c -> c.setUsedCount(Math.max(0,c.getUsedCount()-1)));
        order.setCouponsReleased(true);
    }
}
