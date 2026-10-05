package com.example.flowershop.service;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ManagerProductService {
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_VIDEO_BYTES = 50L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of("video/mp4", "video/webm", "video/quicktime");

    public record ImageInput(
            @NotBlank @Size(max = 500) @Pattern(regexp = "https://[^\\s]+", message = "Ảnh phải là URL HTTPS") String url,
            boolean primary
    ) {}

    public record Input(
            @NotBlank @Size(max = 255) String name,
            @NotNull @Size(max = 5000) String description,
            @NotBlank @Size(max = 36) String categoryId,
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
            @NotNull @Min(0) @Max(1000000) Integer stock,
            @NotNull ProductStatus status,
            ProductType type,
            @Valid @Size(max = 10) List<ImageInput> images
    ) {}

    public record Result(
            String id,
            String shopId,
            String categoryId,
            String categoryName,
            String name,
            String description,
            BigDecimal price,
            Integer stock,
            ProductStatus status,
            ProductType type,
            boolean adminHidden,
            List<String> images
    ) {}

    public record CategoryOption(String id, String name) {}

    public record ImageItem(String id, String imageUrl, boolean isPrimary, Integer displayOrder) {}

    public record VideoInfo(String videoUrl, String title, String description) {}

    public record Detail(
            String id,
            String shopId,
            String shopName,
            String categoryId,
            String categoryName,
            String name,
            String description,
            BigDecimal price,
            Integer stock,
            List<String> images,
            List<VideoInfo> videos
    ) {}

    public record Results(List<Result> content, int page, int totalPages, long totalElements) {}

    private final ShopRepository shops;
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final ProductImageRepository images;
    private final ProductVideoRepository videos;
    private final CloudinaryService cloudinary;

    public ManagerProductService(
            ShopRepository shops,
            ProductRepository products,
            CategoryRepository categories,
            ProductImageRepository images,
            ProductVideoRepository videos,
            CloudinaryService cloudinary
    ) {
        this.shops = shops;
        this.products = products;
        this.categories = categories;
        this.images = images;
        this.videos = videos;
        this.cloudinary = cloudinary;
    }

    private static ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy shop hoặc sản phẩm.");
    }

    private Shop owned(String shopId, String actor, boolean write) {
        var shop = (write ? shops.findForUpdate(shopId) : shops.findById(shopId)).orElseThrow(ManagerProductService::missing);
        if (!shop.getOwner().getId().equals(actor)) {
            throw missing();
        }
        if (write && (shop.getStatus() != ShopStatus.ACTIVE || shop.getOwner().getRole() != UserRole.SHOP
                || shop.getOwner().getStatus() != UserStatus.ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Shop phải được duyệt và đang hoạt động để quản lý mặt hàng.");
        }
        return shop;
    }

    private Pageable page(int page) {
        if (page < 0 || page > 100000) {
            throw new IllegalArgumentException("Trang không hợp lệ.");
        }
        return PageRequest.of(page, 20, Sort.by(Sort.Order.desc("createdDate"), Sort.Order.asc("id")));
    }

    private Result result(Product product) {
        var imageUrls = images.findByProductIdOrderByDisplayOrderAscIdAsc(product.getId()).stream()
                .sorted((a, b) -> Boolean.TRUE.equals(a.getIsPrimary()) == Boolean.TRUE.equals(b.getIsPrimary())
                        ? Integer.compare(a.getDisplayOrder(), b.getDisplayOrder())
                        : Boolean.TRUE.equals(a.getIsPrimary()) ? -1 : 1)
                .map(ProductImage::getImageUrl)
                .toList();

        return new Result(
                product.getId(),
                product.getShop().getId(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getStatus(),
                product.getType(),
                product.isAdminHidden(),
                imageUrls
        );
    }

    private Results response(Page<Product> page) {
        return new Results(page.getContent().stream().map(this::result).toList(), page.getNumber(), page.getTotalPages(), page.getTotalElements());
    }

    public Results list(String shopId, String actor, int page) {
        owned(shopId, actor, false);
        return response(products.findByShopId(shopId, page(page)));
    }

    public List<CategoryOption> categories() {
        return categories.findByStatusOrderByNameAsc(CategoryStatus.ACTIVE).stream()
                .map(c -> new CategoryOption(c.getId(), c.getName()))
                .toList();
    }

    @Transactional
    public Result save(String shopId, String id, String actor, Input input) {
        var shop = owned(shopId, actor, true);
        var product = id == null ? new Product() : products.findByIdAndShopId(id, shopId).orElseThrow(ManagerProductService::missing);
        var category = categories.findById(input.categoryId())
                .filter(c -> c.getStatus() == CategoryStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Danh mục không hợp lệ hoặc đã ngừng hoạt động."));

        if (id == null) {
            product.setId(UUID.randomUUID().toString());
            product.setShop(shop);
            product.setCreatedBy(actor);
        }

        product.setName(input.name().strip());
        product.setDescription(input.description().strip());
        product.setCategory(category);
        product.setPrice(input.price());
        product.setStock(input.stock());
        product.setStatus(input.status());
        product.setType(input.type() == null ? ProductType.READY_MADE : input.type());
        product.setLastModifyBy(actor);

        var stored = products.saveAndFlush(product);

        if (input.images() != null) {
            images.deleteByProductId(stored.getId());
            var list = input.images();
            int primaryIndex = java.util.stream.IntStream.range(0, list.size())
                    .filter(i -> list.get(i).primary())
                    .findFirst()
                    .orElse(0);

            for (int i = 0; i < list.size(); i++) {
                var imageInput = list.get(i);
                images.save(ProductImage.builder()
                        .id(UUID.randomUUID().toString())
                        .product(stored)
                        .imageUrl(imageInput.url().strip())
                        .isPrimary(i == primaryIndex)
                        .displayOrder(i)
                        .createdBy(actor)
                        .lastModifyBy(actor)
                        .build());
            }
            images.flush();
        }

        return result(stored);
    }

    @Transactional
    public void hide(String shopId, String id, String actor) {
        owned(shopId, actor, true);
        var product = products.findByIdAndShopId(id, shopId).orElseThrow(ManagerProductService::missing);
        product.setStatus(ProductStatus.INACTIVE);
        product.setLastModifyBy(actor);
    }

    public List<ImageItem> listImages(String shopId, String productId, String actor) {
        owned(shopId, actor, false);
        products.findByIdAndShopId(productId, shopId).orElseThrow(ManagerProductService::missing);
        return images.findByProductIdOrderByDisplayOrderAsc(productId).stream()
                .map(this::imageItem)
                .toList();
    }

    private ImageItem imageItem(ProductImage image) {
        return new ImageItem(image.getId(), image.getImageUrl(), Boolean.TRUE.equals(image.getIsPrimary()), image.getDisplayOrder());
    }

    @Transactional
    public List<ImageItem> uploadImage(String shopId, String productId, String actor, MultipartFile file) {
        owned(shopId, actor, true);
        var product = products.findByIdAndShopId(productId, shopId).orElseThrow(ManagerProductService::missing);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ảnh để tải lên.");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Ảnh không được vượt quá 5MB.");
        }
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Chỉ nhận ảnh JPEG, PNG hoặc WebP.");
        }

        String url;
        try {
            url = cloudinary.upload(file.getBytes(), file.getOriginalFilename(), file.getContentType());
        } catch (java.io.IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể đọc dữ liệu ảnh.", e);
        }

        long count = images.countByProductId(productId);
        var image = ProductImage.builder()
                .id(UUID.randomUUID().toString())
                .product(product)
                .imageUrl(url)
                .isPrimary(count == 0)
                .displayOrder((int) count)
                .createdBy(actor)
                .lastModifyBy(actor)
                .build();
        images.saveAndFlush(image);
        return listImages(shopId, productId, actor);
    }

    @Transactional
    public void deleteImage(String shopId, String productId, String imageId, String actor) {
        owned(shopId, actor, true);
        products.findByIdAndShopId(productId, shopId).orElseThrow(ManagerProductService::missing);
        var image = images.findByIdAndProductId(imageId, productId).orElseThrow(ManagerProductService::missing);
        images.delete(image);
    }

    public record VideoItem(String id, String videoUrl, String title, String description, Integer displayOrder) {}

    public List<VideoItem> listVideos(String shopId, String productId, String actor) {
        owned(shopId, actor, false);
        products.findByIdAndShopId(productId, shopId).orElseThrow(ManagerProductService::missing);
        return videos.findByProductIdOrderByDisplayOrderAsc(productId).stream().map(this::videoItem).toList();
    }

    private VideoItem videoItem(ProductVideo video) {
        return new VideoItem(video.getId(), video.getVideoUrl(), video.getTitle(), video.getDescription(), video.getDisplayOrder());
    }

    @Transactional
    public List<VideoItem> uploadVideo(String shopId, String productId, String actor, MultipartFile file, String title, String description) {
        owned(shopId, actor, true);
        var product = products.findByIdAndShopId(productId, shopId).orElseThrow(ManagerProductService::missing);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn video để tải lên.");
        }
        if (file.getSize() > MAX_VIDEO_BYTES) {
            throw new IllegalArgumentException("Video không được vượt quá 50MB.");
        }
        if (!ALLOWED_VIDEO_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Chỉ nhận video MP4, WebM hoặc MOV.");
        }

        String url;
        try {
            url = cloudinary.uploadVideo(file.getBytes(), file.getOriginalFilename(), file.getContentType());
        } catch (java.io.IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể đọc dữ liệu video.", e);
        }

        long count = videos.countByProductId(productId);
        var video = ProductVideo.builder()
                .id(UUID.randomUUID().toString())
                .product(product)
                .videoUrl(url)
                .title(title == null || title.isBlank() ? null : title.strip())
                .description(description == null || description.isBlank() ? null : description.strip())
                .displayOrder((int) count)
                .createdBy(actor)
                .lastModifyBy(actor)
                .build();

        videos.saveAndFlush(video);
        return listVideos(shopId, productId, actor);
    }

    @Transactional
    public void deleteVideo(String shopId, String productId, String videoId, String actor) {
        owned(shopId, actor, true);
        products.findByIdAndShopId(productId, shopId).orElseThrow(ManagerProductService::missing);
        var video = videos.findByIdAndProductId(videoId, productId).orElseThrow(ManagerProductService::missing);
        videos.delete(video);
    }
}
