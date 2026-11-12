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
    public record Plan(String id,String name,int durationMonths,BigDecimal price,Integer productLimit,int featuredLimit,boolean enabled) {}
    public record PlanInput(@NotBlank @Size(max=100) String name,@Min(1) @Max(120) int durationMonths,
            @NotNull @DecimalMin("1") @Digits(integer=10,fraction=2) BigDecimal price,
            @Min(1) Integer productLimit,@Min(0) @Max(10000) int featuredLimit,boolean enabled) {}
    public record Status(boolean pro,LocalDateTime expiresAt,long productCount,Integer productLimit,int featuredLimit,List<String> featuredProductIds,String planName) {}
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
    public record ManagedProduct(String id,String name,boolean featured,boolean eligible) {}
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
    @org.springframework.beans.factory.annotation.Autowired private ProductQuotaService quota;
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
                .map(this::planView).toList();
    }
    private Plan planView(ShopPlan p) { return new Plan(p.getId(),p.getName(),p.getDurationMonths(),p.getPrice(),p.getProductLimit(),p.getFeaturedLimit(),p.isEnabled()); }
    public Status status(String shopId,String actor) {
        owned(shopId,actor,false);
        return statusForShop(shopId);
    }
    private Status statusForShop(String shopId) {
        var s=subscriptions.forShop(shopId).orElse(null); boolean pro=active(s);
        return new Status(pro,s==null?null:s.getEndsAt(),used(shopId),pro?s.getPlan().getProductLimit():Integer.valueOf(5),pro?s.getPlan().getFeaturedLimit():0,
                pro?featured.forShop(shopId).stream().map(FeaturedProduct::getId).toList():List.of(),pro?s.getPlan().getName():"Free");
    }
    private long used(String shopId) {
        return products.countForShop(shopId);
    }
    /** Caller already holds the shop write lock; repeated create requests cannot exceed the quota. */
    public void assertCanCreate(Shop shop) {
        var s=subscriptions.forShop(shop.getId()).orElse(null);
        Integer limit=active(s)?s.getPlan().getProductLimit():Integer.valueOf(5);
        if(limit!=null && used(shop.getId())>=limit)
            throw invalid("Gói hiện tại giới hạn "+limit+" sản phẩm. Hãy xóa sản phẩm hoặc mua gói có giới hạn cao hơn.");
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
    public List<Plan> adminPlans(String actor) {
        requireAdmin(actor);return plans.findAll(Sort.by("durationMonths")).stream().map(this::planView).toList();
    }
    @Transactional
    public Plan savePlan(String actor,String id,PlanInput input) {
        requireAdmin(actor);
        if("FREE".equals(id)) throw invalid("Gói Free cố định 5 sản phẩm và không có sản phẩm nổi bật.");
        if(input.productLimit()!=null && input.featuredLimit()>input.productLimit())
            throw invalid("Số sản phẩm nổi bật không được vượt giới hạn sản phẩm.");
        var p=id==null?ShopPlan.builder().id("PRO"+UUID.randomUUID().toString().replace("-","").substring(0,17)).build()
                :plans.findLocked(id).orElseThrow(ShopPlanService::missing);
        p.setName(input.name().strip());p.setDurationMonths(input.durationMonths());p.setPrice(input.price());
        p.setProductLimit(input.productLimit());p.setFeaturedLimit(input.featuredLimit());p.setEnabled(input.enabled());
        return planView(plans.saveAndFlush(p));
    }
    /** Retain references from payments/history; existing subscriptions keep their paid term. */
    @Transactional
    public void removePlan(String actor,String id) {
        requireAdmin(actor);
        if("FREE".equals(id)) throw invalid("Không thể xóa gói Free.");
        plans.findLocked(id).orElseThrow(ShopPlanService::missing).setEnabled(false);
    }
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
    public List<ManagedProduct> adminProducts(String shopId,String actor) {
        requireAdmin(actor);adminShop(shopId,false);
        var selected=new HashSet<>(featured.forShop(shopId).stream().map(FeaturedProduct::getId).toList());
        return products.findByShopId(shopId,org.springframework.data.domain.Pageable.unpaged()).getContent().stream()
                .map(p->new ManagedProduct(p.getId(),p.getName(),selected.contains(p.getId()),ProductCardAssembler.visible(p) && quota.allowed(p) && p.getStock()>0 && p.getCategory().getStatus()==CategoryStatus.ACTIVE)).toList();
    }
    @Transactional
    public void adminFeature(String shopId,String actor,String productId,boolean enabled) {
        requireAdmin(actor);var shop=adminShop(shopId,true);
        feature(shopId,shop.getOwner().getId(),productId,enabled);
    }
    @Transactional
    public void feature(String shopId,String actor,String productId,boolean enabled) {
        var shop=owned(shopId,actor,true);
        var product=products.findByIdAndShopId(productId,shopId).orElseThrow(ShopPlanService::missing);
        var old=featured.findById(productId).orElse(null);
        if(!enabled) { if(old!=null)featured.delete(old); return; }
        var subscription=subscriptions.forShop(shopId).orElse(null);
        if(!active(subscription)) throw invalid("Chỉ gói Pro còn hạn được đưa sản phẩm lên nổi bật.");
        quota.requireAllowed(product);
        if(!ProductCardAssembler.visible(product) || product.getStock()<1 || product.getCategory().getStatus()!=CategoryStatus.ACTIVE)
            throw invalid("Chỉ sản phẩm đang bán, còn hàng và không bị ẩn mới được chọn nổi bật.");
        if(old!=null) return;
        int limit=subscription.getPlan().getFeaturedLimit();
        if(featured.countForShop(shopId)>=limit) throw invalid("Gói hiện tại chỉ cho phép "+limit+" sản phẩm nổi bật.");
        featured.save(FeaturedProduct.builder().id(productId).product(product).shop(shop).featuredAt(clock.now()).build());
    }
    public List<ProductCardAssembler.ProductCard> featured() {
        Map<String,Set<String>> allowed=new HashMap<>();
        var visible=featured.visible(clock.now()).stream().filter(f->{
            var ids=allowed.computeIfAbsent(f.getShop().getId(),shopId->{
                var s=subscriptions.forShop(shopId).orElse(null);
                if(!active(s)) return Set.of();
                return new HashSet<>(featured.forShop(shopId).stream().limit(s.getPlan().getFeaturedLimit()).map(FeaturedProduct::getId).toList());
            });
            return ids.contains(f.getId()) && quota.allowed(f.getProduct());
        }).limit(60).map(FeaturedProduct::getProduct).toList();
        return cards.cards(visible);
    }
}
