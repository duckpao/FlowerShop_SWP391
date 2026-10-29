package com.example.flowershop.service;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
@Transactional(readOnly=true)
public class CustomFlowerOrderService {
    public record Preferences(@Size(max=200) String flowers, @Size(max=100) String colors,
            @Size(max=100) String occasion, @Size(max=200) String wrapping,
            @Size(max=10) String deliveryDate, @Size(max=36) String referenceProductId) {}
    public record RequestInput(@NotBlank @Size(max=36) String shopId,
            @NotNull @DecimalMin("1.00") @Digits(integer=10,fraction=2) BigDecimal budget,
            @NotNull @Min(1) @Max(100) Integer quantity,
            @Size(max=3000) String description,
            @NotNull @Valid Preferences preferences) {}
    public record Decision(@NotNull CustomOrderStatus status,
            @Size(max=2000) String note,
            @DecimalMin("1.00") @Digits(integer=10,fraction=2) BigDecimal quotedPrice) {}
    public record Design(int quantity, String flowers, String colors, String occasion,
            String wrapping, String deliveryDate, String referenceProductId,
            String referenceProductName, BigDecimal estimatedTotal, String shopNote, BigDecimal quotedPrice) {}
    public record Result(String id,String shopId,String shopName,String customerName,
            BigDecimal budget,String description,CustomOrderStatus status,Design design,
            LocalDateTime createdAt,LocalDateTime updatedAt) {}
    public record Results(List<Result> content,int page,int totalPages,long totalElements) {}
    public record Suggestion(ProductCardAssembler.ProductCard product, BigDecimal estimatedTotal,BigDecimal remainingBudget) {}
    public record Suggestions(List<Suggestion> content,int page,int totalPages,long totalElements,BigDecimal budget,int quantity) {}

    private final CustomOrderRequestRepository requests;
    private final ShopRepository shops;
    private final UserRepository users;
    private final ProductRepository products;
    private final ProductCardAssembler cards;
    private final ObjectMapper json=new ObjectMapper();
    public CustomFlowerOrderService(CustomOrderRequestRepository requests,ShopRepository shops,
            UserRepository users,ProductRepository products,ProductCardAssembler cards) {
        this.requests=requests;this.shops=shops;this.users=users;this.products=products;this.cards=cards;
    }
    private static ResponseStatusException missing(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy dữ liệu thuộc quyền truy cập của bạn.");}
    private static ResponseStatusException conflict(String message){return new ResponseStatusException(HttpStatus.CONFLICT,message);}
    private static String clean(String value){return value==null?null:value.strip();}
    private Pageable page(int value){if(value<0||value>100000)throw new IllegalArgumentException("Trang không hợp lệ.");return PageRequest.of(value,10,Sort.by(Sort.Order.desc("createdDate"),Sort.Order.asc("id")));}
    private Shop activeShop(String id){return shops.findById(id).filter(s->s.getStatus()==ShopStatus.ACTIVE).orElseThrow(CustomFlowerOrderService::missing);}
    private Shop ownedShop(String id,String actor){var s=shops.findById(id).filter(x->x.getOwner().getId().equals(actor)).orElseThrow(CustomFlowerOrderService::missing);return s;}
    private void validateMoney(BigDecimal budget,int quantity){
        if(budget==null||budget.compareTo(BigDecimal.ONE)<0||budget.compareTo(new BigDecimal("9999999999.99"))>0||budget.stripTrailingZeros().scale()>2)
            throw new IllegalArgumentException("Ngân sách phải từ 1 đến 9.999.999.999,99 đồng, tối đa 2 số thập phân.");
        if(quantity<1||quantity>100)throw new IllegalArgumentException("Số lượng từ 1 đến 100.");
    }
    private String encode(Design value){try{return json.writeValueAsString(value);}catch(JsonProcessingException e){throw new IllegalStateException("Không thể lưu thiết kế.",e);}}
    private Design decode(String value){
        if(value==null||value.isBlank())return new Design(1,null,null,null,null,null,null,null,null,null,null);
        // Earlier fixtures may contain arbitrary JSON; tolerate unknown keys.
        try{
            var tree=json.readTree(value);
            if(!tree.isObject())throw new IllegalArgumentException("Expected a design object");
            var object=(com.fasterxml.jackson.databind.node.ObjectNode)tree;
            if(object.path("flowers").isArray()){
                var names=new ArrayList<String>();object.path("flowers").forEach(n->names.add(n.asText()));
                object.put("flowers",String.join(", ",names));
            }
            if(!object.has("wrapping")&&object.has("wrapper"))object.set("wrapping",object.get("wrapper"));
            if(object.path("quantity").asInt()<1)object.put("quantity",1);
            return json.readerFor(Design.class).without(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(object.toString());
        }
        catch(JsonProcessingException e){throw new IllegalStateException("Dữ liệu thiết kế không hợp lệ.",e);}
    }
    private Result result(CustomOrderRequest r){return new Result(r.getId(),r.getShop().getId(),r.getShop().getName(),r.getCustomer().getFullName(),r.getBudget(),r.getDescription(),r.getStatus(),decode(r.getDesiredComponents()),r.getCreatedDate(),r.getLastModifyDate());}
    private Results results(Page<CustomOrderRequest> p){return new Results(p.getContent().stream().map(this::result).toList(),p.getNumber(),p.getTotalPages(),p.getTotalElements());}
    private Design design(RequestInput input){
        validateMoney(input.budget(),input.quantity());var p=input.preferences();
        if(p.deliveryDate()!=null&&!p.deliveryDate().isBlank()){
            LocalDate date;try{date=LocalDate.parse(p.deliveryDate());}catch(java.time.format.DateTimeParseException e){throw new IllegalArgumentException("Ngày nhận hoa không hợp lệ.");}
            if(date.isBefore(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))))throw new IllegalArgumentException("Ngày nhận hoa không được ở quá khứ.");
        }
        String productId=null,name=null;BigDecimal total=null;
        if(p.referenceProductId()!=null&&!p.referenceProductId().isBlank()){
            var product=products.findById(p.referenceProductId()).filter(ProductCardAssembler::visible)
                    .filter(x->x.getShop().getId().equals(input.shopId())).orElseThrow(CustomFlowerOrderService::missing);
            if(product.getStock()==null||product.getStock()<input.quantity())throw conflict("Mẫu hoa tham khảo không đủ số lượng.");
            total=product.getPrice().multiply(BigDecimal.valueOf(input.quantity()));
            if(total.compareTo(input.budget())>0)throw conflict("Mẫu hoa đã thay đổi giá hoặc vượt ngân sách. Hãy chọn lại.");
            productId=product.getId();name=product.getName();
        }
        return new Design(input.quantity(),clean(p.flowers()),clean(p.colors()),clean(p.occasion()),clean(p.wrapping()),clean(p.deliveryDate()),productId,name,total,null,null);
    }
    @Transactional
    public Result create(String actor,RequestInput input){
        var shop=activeShop(input.shopId());
        var user=users.findById(actor).filter(u->u.getRole()==UserRole.CUSTOMER&&u.getStatus()==UserStatus.ACTIVE).orElseThrow(CustomFlowerOrderService::missing);
        var request=CustomOrderRequest.builder().id(UUID.randomUUID().toString()).customer(user).shop(shop)
                .budget(input.budget()).description(input.description() == null ? "" : input.description().strip()).desiredComponents(encode(design(input)))
                .status(CustomOrderStatus.REQUESTED).createdBy(actor).lastModifyBy(actor).build();
        return result(requests.saveAndFlush(request));
    }
    public Results mine(String actor,int page){return results(requests.findByCustomerId(actor,page(page)));}
    public Result detail(String actor,String id){return result(requests.findByIdAndCustomerId(id,actor).orElseThrow(CustomFlowerOrderService::missing));}
    public Results shopRequests(String shopId,String actor,CustomOrderStatus status,int page){
        ownedShop(shopId,actor);return results(status==null?requests.findByShopId(shopId,page(page)):requests.findByShopIdAndStatus(shopId,status,page(page)));
    }
    @Transactional
    public Result decide(String shopId,String actor,String id,Decision input){
        var shop=ownedShop(shopId,actor);
        if(shop.getStatus()!=ShopStatus.ACTIVE||shop.getOwner().getStatus()!=UserStatus.ACTIVE)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Shop phải đang hoạt động để xử lý yêu cầu.");
        var request=requests.findLocked(id).filter(r->r.getShop().getId().equals(shopId)).orElseThrow(CustomFlowerOrderService::missing);
        if(request.getStatus()==CustomOrderStatus.ACCEPTED||request.getStatus()==CustomOrderStatus.REJECTED)throw conflict("Yêu cầu đã kết thúc, không thể đổi trạng thái.");
        if(input.status()==CustomOrderStatus.REQUESTED)throw new IllegalArgumentException("Shop không thể chuyển về trạng thái mới gửi.");
        if(input.note()==null||input.note().isBlank())throw new IllegalArgumentException("Vui lòng nhập nội dung phản hồi cho khách.");
        if(input.status()==CustomOrderStatus.ACCEPTED&&input.quotedPrice()==null)throw new IllegalArgumentException("Cần báo giá tổng khi chấp nhận.");
        if(input.quotedPrice()!=null){validateMoney(input.quotedPrice(),1);if(input.quotedPrice().compareTo(request.getBudget())>0)throw new IllegalArgumentException("Báo giá tổng không được vượt ngân sách của khách.");}
        var d=decode(request.getDesiredComponents());
        request.setDesiredComponents(encode(new Design(Math.max(1,d.quantity()),d.flowers(),d.colors(),d.occasion(),d.wrapping(),d.deliveryDate(),d.referenceProductId(),d.referenceProductName(),d.estimatedTotal(),clean(input.note()),input.quotedPrice())));
        request.setStatus(input.status());request.setLastModifyBy(actor);
        return result(requests.saveAndFlush(request));
    }
    public Suggestions suggestions(BigDecimal budget,int quantity,String shopId,String q,int page){
        validateMoney(budget,quantity);page(page);
        if(q!=null&&q.length()>100)throw new IllegalArgumentException("Từ khóa tối đa 100 ký tự.");
        if(shopId!=null&&!shopId.isBlank())activeShop(shopId);
        String term=(q==null?"":q.strip().toLowerCase(Locale.ROOT)).replace("!","!!").replace("%","!%").replace("_","!_");
        Specification<Product> spec=(root,query,cb)->{
            var filters=new ArrayList<Predicate>();
            filters.add(cb.equal(root.get("status"),ProductStatus.ACTIVE));filters.add(cb.isFalse(root.get("adminHidden")));
            filters.add(cb.equal(root.join("shop").get("status"),ShopStatus.ACTIVE));
            filters.add(cb.greaterThan(root.get("price"),BigDecimal.ZERO));
            filters.add(cb.le(cb.prod(root.<BigDecimal>get("price"),BigDecimal.valueOf(quantity)),budget));
            filters.add(cb.greaterThanOrEqualTo(root.get("stock"),quantity));
            if(shopId!=null&&!shopId.isBlank())filters.add(cb.equal(root.get("shop").get("id"),shopId));
            if(!term.isEmpty())filters.add(cb.like(cb.lower(root.get("name")),"%"+term+"%",'!'));
            return cb.and(filters.toArray(Predicate[]::new));
        };
        var found=products.findAll(spec,PageRequest.of(page,12,Sort.by(Sort.Order.desc("price"),Sort.Order.asc("id"))));
        var content=cards.cards(found.getContent()).stream().map(p->{var total=p.price().multiply(BigDecimal.valueOf(quantity));return new Suggestion(p,total,budget.subtract(total));}).toList();
        return new Suggestions(content,page,found.getTotalPages(),found.getTotalElements(),budget,quantity);
    }
}
