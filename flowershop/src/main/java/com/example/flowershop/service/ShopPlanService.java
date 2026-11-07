package com.example.flowershop.service;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service @Transactional(readOnly=true)
public class ShopPlanService {
    public record Plan(String id,String name,int durationMonths,BigDecimal price,Integer monthlyProductLimit,int featuredLimit) {}
    public record Status(boolean pro,LocalDateTime expiresAt,long createdThisMonth,Integer monthlyLimit,int featuredLimit,List<String> featuredProductIds) {}
    public record Purchase(@NotBlank @Size(max=20) String planId) {}
    public record Checkout(String invoiceNumber,BigDecimal amount,String checkoutUrl,Map<String,String> fields) {}
    public record PaymentStatus(String invoiceNumber,String status,BigDecimal amount,LocalDateTime confirmedAt) {}
    public record Feature(@NotNull Boolean featured) {}
    public record AdminGrant(@NotBlank @Size(max=20) String planId,
            @NotBlank @Size(max=1000) String note,
            @NotBlank @Pattern(regexp="[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}") String requestId) {}
    public record AdminConfirmation(@NotBlank @Size(max=1000) String note) {}
    public record PendingPayment(String invoiceNumber,String planName,int durationMonths,BigDecimal amount,String status,LocalDateTime createdAt) {}
    public record Activation(String id,String planName,int durationMonths,String source,String actorName,String note,LocalDateTime startsAt,LocalDateTime endsAt,LocalDateTime createdAt) {}
    public record AdminStatus(Status subscription,List<PendingPayment> payments,List<Activation> history) {}
    private final ShopPlanRepository plans;
    private final ShopSubscriptionRepository subscriptions;
    private final ShopPlanPaymentRepository payments;
    private final ShopProductPostingRepository postings;
    private final FeaturedProductRepository featured;
    private final ShopRepository shops;
    private final ProductRepository products;
    private final ProductCardAssembler cards;
    private final NotificationService notifications;
    private final ShopPlanClock clock;
    private final SePayGatewayService gateway;
    private final TransactionTemplate tx;
    private final String frontend;
    private final UserRepository users;
    private final ShopProActivationRepository activations;
    public ShopPlanService(ShopPlanRepository plans,ShopSubscriptionRepository subscriptions,ShopPlanPaymentRepository payments,
            ShopProductPostingRepository postings,FeaturedProductRepository featured,ShopRepository shops,ProductRepository products,
            ProductCardAssembler cards,NotificationService notifications,ShopPlanClock clock,SePayGatewayService gateway,
            PlatformTransactionManager manager,@Value("${app.frontend-base-url:http://localhost:5173}") String frontend,
            UserRepository users,ShopProActivationRepository activations) {
        this.plans=plans;this.subscriptions=subscriptions;this.payments=payments;this.postings=postings;this.featured=featured;
        this.shops=shops;this.products=products;this.cards=cards;this.notifications=notifications;this.clock=clock;
        this.gateway=gateway;this.tx=new TransactionTemplate(manager);this.frontend=frontend.replaceAll("/+$","");
        this.users=users;this.activations=activations;
    }
    private static ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy gói, cửa hàng hoặc giao dịch thuộc quyền truy cập."); }
    private static ResponseStatusException invalid(String text) { return new ResponseStatusException(HttpStatus.CONFLICT,text); }
    private Shop owned(String shopId,String actor,boolean lock) {
        var shop=(lock?shops.findForUpdate(shopId):shops.findById(shopId)).orElseThrow(ShopPlanService::missing);
        if(!shop.getOwner().getId().equals(actor)) throw missing();
        if(shop.getStatus()!=ShopStatus.ACTIVE || shop.getOwner().getStatus()!=UserStatus.ACTIVE)
            throw invalid("Cửa hàng phải đang hoạt động.");
        return shop;
    }
    private boolean active(ShopSubscription s) { return s!=null && s.getEndsAt()!=null && s.getEndsAt().isAfter(clock.now()); }
    public List<Plan> plans() {
        return plans.findAll(Sort.by("durationMonths")).stream().filter(ShopPlan::isEnabled)
                .map(p->new Plan(p.getId(),p.getName(),p.getDurationMonths(),p.getPrice(),p.getMonthlyProductLimit(),p.getFeaturedLimit())).toList();
    }
    public Status status(String shopId,String actor) {
        owned(shopId,actor,false);
        return statusForShop(shopId);
    }
    private Status statusForShop(String shopId) {
        var s=subscriptions.forShop(shopId).orElse(null); boolean pro=active(s);
        return new Status(pro,s==null?null:s.getEndsAt(),used(shopId),pro?null:5,pro?10:0,
                pro?featured.forShop(shopId).stream().map(FeaturedProduct::getId).toList():List.of());
    }
    private long used(String shopId) {
        var start=clock.now().toLocalDate().withDayOfMonth(1).atStartOfDay();
        return postings.inMonth(shopId,start,start.plusMonths(1));
    }
    /** Caller already holds the shop write lock; repeated create requests cannot exceed the quota. */
    public void assertCanCreate(Shop shop) {
        if(!active(subscriptions.forShop(shop.getId()).orElse(null)) && used(shop.getId())>=5)
            throw invalid("Gói Free chỉ được tạo tối đa 5 sản phẩm/tháng. Vui lòng đăng ký Pro để tiếp tục.");
    }
    @Transactional
    public void recordCreation(Product product) {
        postings.save(ShopProductPosting.builder().id(product.getId()).shopId(product.getShop().getId()).postedAt(clock.now()).build());
    }
    @Transactional
    public Checkout checkout(String shopId,String actor,String planId) {
        var shop=owned(shopId,actor,true);
        var plan=plans.findById(planId).filter(p->p.isEnabled() && p.getDurationMonths()>0 && p.getPrice().signum()>0).orElseThrow(ShopPlanService::missing);
        String invoice="SUB-"+UUID.randomUUID().toString().replace("-","");
        var checkout=gateway.subscriptionCheckout(invoice,plan.getPrice(),actor,frontend+"/shop-admin/subscription");
        payments.save(ShopPlanPayment.builder().id(UUID.randomUUID().toString()).shop(shop).plan(plan)
                .durationMonths(plan.getDurationMonths()).amount(plan.getPrice()).invoiceNumber(invoice)
                .status("PENDING").createdAt(clock.now()).build());
        return new Checkout(invoice,plan.getPrice(),checkout.checkoutUrl(),checkout.fields());
    }
    /** Network I/O happens outside transactions; entitlement activation is a short locked transaction. */
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public PaymentStatus payment(String shopId,String actor,String invoice) {
        tx.executeWithoutResult(t->{ owned(shopId,actor,false); requirePayment(shopId,invoice); });
        reconcile(invoice);
        return tx.execute(t->{ var p=requirePayment(shopId,invoice); return new PaymentStatus(invoice,p.getStatus(),p.getAmount(),p.getConfirmedAt()); });
    }
    private ShopPlanPayment requirePayment(String shopId,String invoice) {
        return payments.invoice(invoice).filter(p->p.getShop().getId().equals(shopId)).orElseThrow(ShopPlanService::missing);
    }
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public void reconcile(String invoice) {
        boolean pending=Boolean.TRUE.equals(tx.execute(t->payments.invoice(invoice).map(p->"PENDING".equals(p.getStatus())).orElse(false)));
        if(!pending) return;
        var captured=gateway.capturedSubscriptionInvoice(invoice);
        tx.executeWithoutResult(t->{
            var initial=payments.invoice(invoice).orElseThrow(ShopPlanService::missing);
            // Lock the shop before rereading payment: all renewal and product operations use this same lock.
            var shop=shops.findForUpdate(initial.getShop().getId()).orElseThrow(ShopPlanService::missing);
            payments.refresh(initial);
            if(!"PENDING".equals(initial.getStatus())) return;
            initial.setLastCheckedAt(clock.now());
            if(captured.isEmpty()) return;
            var paid=captured.get();
            if(!invoice.equals(paid.invoiceNumber()) || initial.getAmount().compareTo(paid.amount())!=0) {
                initial.setStatus("REVIEW");
                notifications.notifyUser(shop.getOwner(),"Cần kiểm tra thanh toán gói","Số tiền SePay khác số tiền mua gói. Vui lòng liên hệ hỗ trợ.","/shop-admin/subscription");
                return;
            }
            activate(shop,initial.getPlan(),initial.getDurationMonths(),"SEPAY",null,"SePay confirmed",initial.getId(),UUID.randomUUID().toString());
            initial.setStatus("SUCCESS");initial.setConfirmedAt(clock.now());initial.setTransactionNo(paid.transactionNo());
        });
    }
    private void requireAdmin(String actor) {
        users.findByIdAndRoleAndStatus(actor,UserRole.ADMIN,UserStatus.ACTIVE)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN,"Chỉ admin đang hoạt động được kích hoạt Pro."));
    }
    private Shop adminShop(String shopId,boolean lock) {
        var shop=(lock?shops.findForUpdate(shopId):shops.findById(shopId)).orElseThrow(ShopPlanService::missing);
        if(lock && (shop.getStatus()!=ShopStatus.ACTIVE || shop.getOwner().getStatus()!=UserStatus.ACTIVE || shop.getOwner().getRole()!=UserRole.SHOP))
            throw invalid("Cửa hàng và chủ shop phải đang hoạt động để kích hoạt Pro.");
        return shop;
    }
    public List<Plan> adminPlans(String actor) { requireAdmin(actor);return plans(); }
    public AdminStatus adminStatus(String shopId,String actor) {
        requireAdmin(actor);adminShop(shopId,false);
        var pending=payments.pendingForShop(shopId).stream().map(p->new PendingPayment(p.getInvoiceNumber(),p.getPlan().getName(),p.getDurationMonths(),p.getAmount(),p.getStatus(),p.getCreatedAt())).toList();
        var history=activations.history(shopId).stream().map(a->new Activation(a.getId(),a.getPlan().getName(),a.getDurationMonths(),a.getSource(),
                a.getActorId()==null?"SePay":users.findById(a.getActorId()).map(User::getFullName).orElse(a.getActorId()),a.getNote(),a.getStartsAt(),a.getEndsAt(),a.getCreatedAt())).toList();
        return new AdminStatus(statusForShop(shopId),pending,history);
    }
    @Transactional
    public AdminStatus adminGrant(String shopId,String actor,AdminGrant input) {
        requireAdmin(actor);var shop=adminShop(shopId,true);
        var existing=activations.findById(input.requestId()).orElse(null);
        if(existing!=null) {
            if(!"ADMIN_GRANT".equals(existing.getSource()) || !shopId.equals(existing.getShop().getId())
                    || !actor.equals(existing.getActorId()) || !input.planId().equals(existing.getPlan().getId()) || !input.note().strip().equals(existing.getNote()))
                throw invalid("Mã yêu cầu đã được dùng cho một lần kích hoạt khác.");
            return adminStatus(shopId,actor);
        }
        var plan=plans.findById(input.planId()).filter(p->p.isEnabled() && p.getDurationMonths()>0 && p.getPrice().signum()>0).orElseThrow(ShopPlanService::missing);
        activate(shop,plan,plan.getDurationMonths(),"ADMIN_GRANT",actor,input.note().strip(),null,input.requestId());
        return adminStatus(shopId,actor);
    }
    @Transactional
    public AdminStatus adminConfirm(String shopId,String actor,String invoice,AdminConfirmation input) {
        requireAdmin(actor);var shop=adminShop(shopId,true);
        var payment=requirePayment(shopId,invoice);
        if("SUCCESS".equals(payment.getStatus())) return adminStatus(shopId,actor);
        if(!List.of("PENDING","REVIEW").contains(payment.getStatus())) throw invalid("Giao dịch không còn chờ xác nhận.");
        activate(shop,payment.getPlan(),payment.getDurationMonths(),"ADMIN_CONFIRM",actor,input.note().strip(),payment.getId(),UUID.randomUUID().toString());
        payment.setStatus("SUCCESS");payment.setConfirmedAt(clock.now());payments.flush();
        return adminStatus(shopId,actor);
    }
    /** The shop lock serializes admin activation, quota operations and SePay confirmation. */
    private void activate(Shop shop,ShopPlan plan,int months,String source,String actor,String note,String paymentId,String requestId) {
        var s=subscriptions.forShop(shop.getId()).orElse(null);
        if(s==null) s=ShopSubscription.builder().id(UUID.randomUUID().toString()).shop(shop).build();
        boolean wasActive=active(s);var start=wasActive?s.getEndsAt():clock.now();
        if(!wasActive) {s.setStartedAt(start);featured.deleteAll(featured.forShop(shop.getId()));}
        s.setPlan(plan);s.setEndsAt(start.plusMonths(months));subscriptions.save(s);
        activations.save(ShopProActivation.builder().id(requestId).shop(shop).plan(plan).durationMonths(months).source(source)
                .paymentId(paymentId).actorId(actor).note(note).startsAt(start).endsAt(s.getEndsAt()).createdAt(clock.now()).build());
        notifications.notifyUser(shop.getOwner(),source.startsWith("ADMIN")?"Admin đã kích hoạt/gia hạn Pro":"Gia hạn Pro thành công",
                "Gói Pro có hiệu lực đến "+s.getEndsAt().toLocalDate()+".","/shop-admin/subscription");
    }
    public List<String> pendingInvoices() { return payments.pendingInvoices(clock.now()); }
    @Transactional
    public void feature(String shopId,String actor,String productId,boolean enabled) {
        var shop=owned(shopId,actor,true);
        var product=products.findByIdAndShopId(productId,shopId).orElseThrow(ShopPlanService::missing);
        var old=featured.findById(productId).orElse(null);
        if(!enabled) { if(old!=null)featured.delete(old); return; }
        if(!active(subscriptions.forShop(shopId).orElse(null))) throw invalid("Chỉ gói Pro còn hạn được đưa sản phẩm lên nổi bật.");
        if(!ProductCardAssembler.visible(product) || product.getStock()<1 || product.getCategory().getStatus()!=CategoryStatus.ACTIVE)
            throw invalid("Chỉ sản phẩm đang bán, còn hàng và không bị ẩn mới được chọn nổi bật.");
        if(old!=null) return;
        if(featured.countForShop(shopId)>=10) throw invalid("Mỗi shop Pro được chọn tối đa 10 sản phẩm nổi bật.");
        featured.save(FeaturedProduct.builder().id(productId).product(product).shop(shop).featuredAt(clock.now()).build());
    }
    public List<ProductCardAssembler.ProductCard> featured() { return cards.cards(featured.visible(clock.now()).stream().map(FeaturedProduct::getProduct).toList()); }
}
