package com.example.flowershop.service;

import com.example.flowershop.entity.Category;
import com.example.flowershop.entity.Product;
import com.example.flowershop.entity.enums.CategoryStatus;
import com.example.flowershop.entity.enums.ProductStatus;
import com.example.flowershop.repository.CategoryRepository;
import com.example.flowershop.repository.ProductRepository;
import com.example.flowershop.service.ProductCardAssembler.ProductCard;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class AdminCatalogService {

    /** groupName: nhóm hiển thị trên menu (Chủ đề, Hoa tươi...); để trống nghĩa là chưa phân nhóm. */
    public record CategoryInput(@NotBlank @Size(max = 100) String name,
            @NotNull @Size(max = 1000) String description, @NotNull CategoryStatus status,
            @Size(max = 50) String groupName) {}

    public record CategoryResult(String id, String name, String description, String groupName,
            CategoryStatus status, long productCount) {}

    public record GroupRenameInput(@NotNull @Size(max = 50) String from, @NotNull @Size(max = 50) String to) {}

    public record HiddenInput(@NotNull Boolean hidden) {}

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final ProductCardAssembler assembler;

    public AdminCatalogService(CategoryRepository categories, ProductRepository products,
                               ProductCardAssembler assembler) {
        this.categories = categories;
        this.products = products;
        this.assembler = assembler;
    }

    private static ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu.");
    }

    private Map<String, Long> counts() {
        Map<String, Long> result = new HashMap<>();
        for (Object[] row : products.countVisibleByCategory())
            result.put((String) row[0], ((Number) row[1]).longValue());
        return result;
    }

    public List<CategoryResult> categories() {
        Map<String, Long> counts = counts();
        return categories.findAll(Sort.by(Sort.Order.asc("name"))).stream()
                .map(c -> new CategoryResult(c.getId(), c.getName(), c.getDescription(), c.getGroupName(),
                        c.getStatus(), counts.getOrDefault(c.getId(), 0L))).toList();
    }

    private static String cleanGroup(String group) {
        String value = group == null ? "" : group.strip();
        return value.isEmpty() ? null : value;
    }

    @Transactional
    public CategoryResult createCategory(String actor, CategoryInput input) {
        Category saved = categories.saveAndFlush(Category.builder().id(UUID.randomUUID().toString())
                .name(input.name().strip()).description(input.description().strip())
                .groupName(cleanGroup(input.groupName()))
                .status(input.status()).createdBy(actor).build());
        return new CategoryResult(saved.getId(), saved.getName(), saved.getDescription(), saved.getGroupName(),
                saved.getStatus(), 0L);
    }

    @Transactional
    public CategoryResult updateCategory(String id, String actor, CategoryInput input) {
        Category category = categories.findById(id).orElseThrow(AdminCatalogService::missing);
        category.setName(input.name().strip());
        category.setDescription(input.description().strip());
        category.setGroupName(cleanGroup(input.groupName()));
        category.setStatus(input.status());
        category.setLastModifyBy(actor);
        Category saved = categories.saveAndFlush(category);
        return new CategoryResult(saved.getId(), saved.getName(), saved.getDescription(), saved.getGroupName(),
                saved.getStatus(), counts().getOrDefault(saved.getId(), 0L));
    }

    /** Chỉ xóa được danh mục chưa có sản phẩm nào (kể cả đã ẩn); nếu đã có sản phẩm thì chuyển sang Ngừng dùng. */
    @Transactional
    public void deleteCategory(String id) {
        Category category = categories.findById(id).orElseThrow(AdminCatalogService::missing);
        if (products.existsByCategoryId(id))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Danh mục đang có sản phẩm nên không thể xóa. Hãy chuyển sang \"Ngừng dùng\" để ẩn khỏi menu.");
        categories.delete(category);
        categories.flush();
    }

    /** Đổi tên một nhóm menu cho mọi danh mục thuộc nhóm đó; tên mới để trống nghĩa là bỏ phân nhóm. */
    @Transactional
    public int renameGroup(String actor, GroupRenameInput input) {
        String from = cleanGroup(input.from());
        if (from == null) throw new IllegalArgumentException("Chưa chọn nhóm cần đổi tên.");
        String to = cleanGroup(input.to());
        List<Category> affected = categories.findByGroupName(from);
        for (Category c : affected) { c.setGroupName(to); c.setLastModifyBy(actor); }
        categories.saveAllAndFlush(affected);
        return affected.size();
    }

    public CatalogService.Results products(String q, String shopId, String status, int page) {
        if (page < 0 || page > 100000) throw new IllegalArgumentException("Trang không hợp lệ.");
        String raw = q == null ? "" : q.strip();
        if (raw.length() > 100) throw new IllegalArgumentException("Từ khóa tối đa 100 ký tự.");
        String term = raw.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_");
        ProductStatus selected = null;
        if (status != null && !status.isBlank()) {
            try { selected = ProductStatus.valueOf(status); }
            catch (IllegalArgumentException e) { throw new IllegalArgumentException("Trạng thái sản phẩm không hợp lệ."); }
        }
        ProductStatus filter = selected;
        Specification<Product> spec = (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (filter != null) predicates.add(cb.equal(root.get("status"), filter));
            if (shopId != null && !shopId.isBlank())
                predicates.add(cb.equal(root.get("shop").get("id"), shopId));
            if (!term.isEmpty())
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + term + "%", '!'));
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<Product> result = products.findAll(spec, PageRequest.of(page, 20,
                Sort.by(Sort.Order.desc("createdDate"), Sort.Order.asc("id"))));
        return new CatalogService.Results(assembler.cards(result.getContent()), page, 20,
                result.getTotalElements(), result.getTotalPages());
    }

    /** Cờ này chỉ Admin ghi được; nó không nằm trong DTO cập nhật của Manager. */
    @Transactional
    public ProductCard setHidden(String productId, boolean hidden, String actor) {
        Product product = products.findById(productId).orElseThrow(AdminCatalogService::missing);
        product.setAdminHidden(hidden);
        product.setLastModifyBy(actor);
        return assembler.cards(List.of(products.saveAndFlush(product))).get(0);
    }
}
