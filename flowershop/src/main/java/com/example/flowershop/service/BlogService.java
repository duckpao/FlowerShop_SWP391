package com.example.flowershop.service;

import com.example.flowershop.entity.Blog;
import com.example.flowershop.entity.Shop;
import com.example.flowershop.entity.enums.BlogStatus;
import com.example.flowershop.entity.enums.ShopStatus;
import com.example.flowershop.entity.enums.UserRole;
import com.example.flowershop.entity.enums.UserStatus;
import com.example.flowershop.repository.BlogRepository;
import com.example.flowershop.repository.ShopRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class BlogService {
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    public record Input(
            @NotBlank @Size(max = 255) String title,
            @NotBlank @Size(max = 50000) String content,
            @NotNull BlogStatus status,
            @Size(max = 10) List<@NotBlank @Size(max = 1000) String> retainedImages) {
    }

    public record ShopSummary(String id, String name, String logoUrl) {
    }

    public record Result(String id, ShopSummary shop, String title, String content, List<String> images,
            BlogStatus status, LocalDateTime createdDate, LocalDateTime lastModifyDate) {
    }

    public record Results(List<Result> content, int page, int totalPages, long totalElements) {
    }

    private final BlogRepository blogs;
    private final ShopRepository shops;
    private final CloudinaryService cloudinary;
    private final ObjectMapper mapper = new ObjectMapper();

    public BlogService(BlogRepository blogs, ShopRepository shops, CloudinaryService cloudinary) {
        this.blogs = blogs;
        this.shops = shops;
        this.cloudinary = cloudinary;
    }

    public Results listManaged(String shopId, String actor, int page) {
        owned(shopId, actor, false);
        return results(blogs.findByShopId(shopId, pageable(page)));
    }

    public Results listPublic(String shopId, String q, int page) {
        String term = q == null ? "" : q.strip();
        if (term.length() > 100) {
            throw new IllegalArgumentException("Từ khóa tối đa 100 ký tự.");
        }
        String selectedShop = shopId == null || shopId.isBlank() ? null : shopId;
        return results(blogs.searchPublic(BlogStatus.PUBLISHED, ShopStatus.ACTIVE, selectedShop, term, pageable(page)));
    }

    public Result publicDetail(String id) {
        return result(blogs.findByIdAndStatusAndShop_Status(id, BlogStatus.PUBLISHED, ShopStatus.ACTIVE)
                .orElseThrow(BlogService::missing));
    }

    @Transactional
    public Result save(String shopId, String id, String actor, Input input) {
        return save(shopId, id, actor, input, List.of());
    }

    @Transactional
    public Result save(String shopId, String id, String actor, Input input, List<MultipartFile> images) {
        Shop shop = owned(shopId, actor, true);
        if (input.status() == BlogStatus.HIDDEN) {
            throw new IllegalArgumentException("Hãy dùng thao tác ẩn bài viết.");
        }
        Blog blog;
        if (id == null) {
            blog = Blog.builder().id(UUID.randomUUID().toString()).shop(shop).createdBy(actor).build();
        } else {
            blog = blogs.findByIdAndShopId(id, shopId).orElseThrow(BlogService::missing);
        }
        List<String> currentImages = readImages(blog.getImages());
        List<String> retained = input.retainedImages() == null ? List.of()
                : input.retainedImages().stream().map(String::strip).distinct().toList();
        if (retained.stream().anyMatch(image -> !currentImages.contains(image))) {
            throw new IllegalArgumentException("Danh sách ảnh giữ lại không hợp lệ.");
        }
        List<MultipartFile> imageFiles = images == null ? List.of() : images;
        if (retained.size() + imageFiles.size() > 10) {
            throw new IllegalArgumentException("Mỗi bài viết chỉ được dùng tối đa 10 ảnh.");
        }
        imageFiles.forEach(this::validateImageFile);
        List<String> storedImages = new ArrayList<>(retained);
        for (MultipartFile file : imageFiles) {
            try {
                storedImages.add(cloudinary.uploadBlogImage(file.getBytes(), file.getOriginalFilename(), file.getContentType()));
            } catch (IOException exception) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Không thể đọc dữ liệu ảnh. Vui lòng thử lại.", exception);
            }
        }
        blog.setTitle(input.title().strip());
        blog.setContent(input.content().strip());
        blog.setStatus(input.status());
        blog.setImages(writeImages(storedImages));
        blog.setLastModifyBy(actor);
        return result(blogs.saveAndFlush(blog));
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ảnh để tải lên.");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Ảnh không được vượt quá 5MB.");
        }
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Chỉ nhận ảnh JPEG, PNG hoặc WebP.");
        }
    }

    @Transactional
    public void hide(String shopId, String id, String actor) {
        owned(shopId, actor, true);
        Blog blog = blogs.findByIdAndShopId(id, shopId).orElseThrow(BlogService::missing);
        blog.setStatus(BlogStatus.HIDDEN);
        blog.setLastModifyBy(actor);
    }

    private Shop owned(String shopId, String actor, boolean write) {
        Shop shop = shops.findById(shopId).orElseThrow(BlogService::missing);
        if (!shop.getOwner().getId().equals(actor)) {
            throw missing();
        }
        if (write && (shop.getStatus() != ShopStatus.ACTIVE
                || shop.getOwner().getRole() != UserRole.SHOP
                || shop.getOwner().getStatus() != UserStatus.ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Shop phải được duyệt và đang hoạt động để đăng bài.");
        }
        return shop;
    }

    private PageRequest pageable(int page) {
        if (page < 0 || page > 100000) {
            throw new IllegalArgumentException("Trang không hợp lệ.");
        }
        return PageRequest.of(page, 12, Sort.by(Sort.Order.desc("createdDate"), Sort.Order.asc("id")));
    }

    private Results results(org.springframework.data.domain.Page<Blog> page) {
        return new Results(page.getContent().stream().map(this::result).toList(), page.getNumber(),
                page.getTotalPages(), page.getTotalElements());
    }

    private Result result(Blog blog) {
        Shop shop = blog.getShop();
        return new Result(blog.getId(), new ShopSummary(shop.getId(), shop.getName(), shop.getLogoUrl()),
                blog.getTitle(), blog.getContent(), readImages(blog.getImages()), blog.getStatus(),
                blog.getCreatedDate(), blog.getLastModifyDate());
    }

    private String writeImages(List<String> images) {
        List<String> normalized = images == null ? List.of() : images.stream().map(String::strip).distinct().toList();
        try {
            return mapper.writeValueAsString(normalized);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Danh sách ảnh không hợp lệ.", exception);
        }
    }

    private List<String> readImages(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return mapper.readValue(json, TypeFactory.defaultInstance().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private static ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài viết.");
    }
}
