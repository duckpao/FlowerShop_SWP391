package com.example.flowershop.service;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.*;

// Nghiệp vụ tạo/sửa/ẩn sản phẩm của Shop. Input/Result/Results là các record DTO thực sự dùng bởi controller hiện tại.
// @Transactional ở method ghi ghi đè readOnly của class; lỗi runtime làm rollback các thay đổi DB trong transaction.
@Service
@Transactional(readOnly = true)
public class ManagerProductService {
    public record ImageInput(@NotBlank @Size(max=500)
        @Pattern(regexp="https://[^\\s]+",message="Ảnh phải là URL HTTPS") String url, boolean primary) {}
    /** images = null giữ nguyên ảnh cũ; khác null thì thay toàn bộ danh sách. */
    public record Input(@NotBlank @Size(max=255) String name,@NotNull @Size(max=5000) String description,
        @NotBlank @Size(max=36) String categoryId,@NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal price,
        @NotNull @Min(0) @Max(1000000) Integer stock,@NotNull ProductStatus status, ProductType type,
        @jakarta.validation.Valid @Size(max=10) List<ImageInput> images) {}
    public record Result(String id,String shopId,String categoryId,String categoryName,String name,String description,BigDecimal price,Integer stock,ProductStatus status,ProductType type,boolean adminHidden,List<String> images) {}
    public record Results(List<Result> content,int page,int totalPages,long totalElements) {}
    public record CategoryOption(String id,String name) {}
    public record ImageItem(String id,String imageUrl,boolean primary,int displayOrder) {}
    public record VideoItem(String id,String videoUrl,String title,String description,int displayOrder) {}
    private static final long MAX_IMAGE_BYTES = 5L*1024*1024;
    private static final long MAX_VIDEO_BYTES = 50L*1024*1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg","image/png","image/webp");
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of("video/mp4","video/webm","video/quicktime");
    private final ShopRepository shops;private final ProductRepository products;private final CategoryRepository categories;
    private final ProductImageRepository images;private final ProductVideoRepository videos;private final CloudinaryService cloudinary;
    public ManagerProductService(ShopRepository s,ProductRepository p,CategoryRepository c,ProductImageRepository i,ProductVideoRepository v,CloudinaryService cl) {shops=s;products=p;categories=c;images=i;videos=v;cloudinary=cl;}
    private static ResponseStatusException missing() {return new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy shop hoặc sản phẩm.");}
    // Cổng kiểm tra sở hữu: actor phải là owner; ghi còn yêu cầu shop/user ACTIVE, role SHOP và khóa shop bằng findForUpdate.
    private Shop owned(String id,String actor,boolean write) {
        var shop=(write?shops.findForUpdate(id):shops.findById(id)).orElseThrow(ManagerProductService::missing);
        if(!shop.getOwner().getId().equals(actor)) throw missing();
        if(write && (shop.getStatus()!=ShopStatus.ACTIVE || shop.getOwner().getRole()!=UserRole.SHOP || shop.getOwner().getStatus()!=UserStatus.ACTIVE)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Shop phải được duyệt và đang hoạt động để quản lý mặt hàng.");
        return shop;
    }
    // Ảnh chính đứng đầu: giao diện dựng lại cờ primary theo vị trí, nên thứ tự này giữ đúng ảnh chính khi sửa sản phẩm.
    // Chuyển Product thành Result cho form quản lý, kèm URL ảnh chính trước; không trả trực tiếp entity LAZY.
    private Result result(Product p) {return new Result(p.getId(),p.getShop().getId(),p.getCategory().getId(),p.getCategory().getName(),p.getName(),p.getDescription(),p.getPrice(),p.getStock(),p.getStatus(),p.getType(),p.isAdminHidden(),
        images.findByProductIdOrderByDisplayOrderAscIdAsc(p.getId()).stream()
            .sorted(java.util.Comparator.comparingInt(i->Boolean.TRUE.equals(i.getIsPrimary())?0:1))
            .map(ProductImage::getImageUrl).toList());}
    // Kiểm tra chỉ số trang rồi tạo Pageable 20 mục, mới nhất trước và id để ổn định thứ tự.
    private Pageable page(int page) {if(page<0 || page>100000) throw new IllegalArgumentException("Trang không hợp lệ.");return PageRequest.of(page,20,Sort.by(Sort.Order.desc("createdDate"),Sort.Order.asc("id")));}
    // Chuyển Page<Product> thành Results có content + page + totalPages + totalElements.
    private Results response(Page<Product> p) {return new Results(p.getContent().stream().map(this::result).toList(),p.getNumber(),p.getTotalPages(),p.getTotalElements());}
    // Kiểm tra chủ shop -> ProductRepository.findByShopId -> response; không áp điều kiện hiển thị công khai.
    public Results list(String shop,String actor,int page) {owned(shop,actor,false);return response(products.findByShopId(shop,page(page)));}
    // Chỉ trả id/name các danh mục ACTIVE để Shop chọn trong form.
    public List<CategoryOption> categories() {return categories.findByStatusOrderByNameAsc(CategoryStatus.ACTIVE).stream().map(c->new CategoryOption(c.getId(),c.getName())).toList();}
    // POST và PUT dùng chung: id=null tạo mới; có id tìm theo cả id và shopId, rồi kiểm tra danh mục và lưu.
    @Transactional public Result save(String shopId,String id,String actor,Input input) {
        var shop=owned(shopId,actor,true);
        var product=id==null?new Product():products.findByIdAndShopId(id,shopId).orElseThrow(ManagerProductService::missing);
        var category=categories.findById(input.categoryId()).filter(c->c.getStatus()==CategoryStatus.ACTIVE).orElseThrow(()->new IllegalArgumentException("Danh mục không hợp lệ hoặc đã ngừng hoạt động."));
        if(id==null) {product.setId(UUID.randomUUID().toString());product.setShop(shop);product.setCreatedBy(actor);}
        // adminHidden không nằm trong Input nên cờ kiểm duyệt của Admin luôn được giữ nguyên.
        product.setName(input.name().strip());product.setDescription(input.description().strip());product.setCategory(category);product.setPrice(input.price());product.setStock(input.stock());product.setStatus(input.status());product.setType(input.type()==null?ProductType.READY_MADE:input.type());product.setLastModifyBy(actor);
        // saveAndFlush yêu cầu JPA đồng bộ INSERT/UPDATE xuống DB ngay; transaction vẫn chưa commit tại dòng này.
        // Các thay đổi sản phẩm và ảnh bên dưới cùng nằm trong transaction của save.
        var stored=products.saveAndFlush(product);
        // images=null giữ ảnh cũ; [] xóa hết; có danh sách thì xóa ảnh cũ và tạo lại theo URL gửi lên.
        // Chọn cờ primary đầu tiên, nếu không có thì ảnh đầu; đây là lưu URL, không upload file.
        if(input.images()!=null) {
            images.deleteByProductId(stored.getId());images.flush();
            var list=input.images();
            int primary=java.util.stream.IntStream.range(0,list.size()).filter(i->list.get(i).primary()).findFirst().orElse(0);
            for(int i=0;i<list.size();i++)
                images.save(ProductImage.builder().id(UUID.randomUUID().toString()).product(stored)
                    .imageUrl(list.get(i).url().strip()).isPrimary(i==primary).displayOrder(i)
                    .createdBy(actor).lastModifyBy(actor).build());
            images.flush();
        }
        return result(stored);
    }

    @Transactional
    // Đổi entity đang được JPA quản lý sang INACTIVE. Khi commit, dirty checking tự UPDATE dù không gọi save.
    public void hide(String shopId, String id, String actor) {
        owned(shopId, actor, true);
        var p = products.findByIdAndShopId(id, shopId).orElseThrow(ManagerProductService::missing);
        p.setStatus(ProductStatus.INACTIVE);
        p.setLastModifyBy(actor);
    }

    // Chuyển ProductImage entity sang record trả API media, gồm id/URL/ảnh chính/thứ tự.
    private ImageItem imageItem(ProductImage i) {
        return new ImageItem(i.getId(), i.getImageUrl(), Boolean.TRUE.equals(i.getIsPrimary()), i.getDisplayOrder());
    }

    // Kiểm tra sở hữu shop + sản phẩm thuộc shop -> lấy ảnh của productId.
    public List<ImageItem> listImages(String shopId, String productId, String actor) {
        owned(shopId, actor, false);
        products.findByIdAndShopId(productId, shopId).orElseThrow(ManagerProductService::missing);
        return images.findByProductIdOrderByDisplayOrderAsc(productId).stream().map(this::imageItem).toList();
    }

    @Transactional
    // Kiểm tra sở hữu và file (5MB, JPEG/PNG/WebP) -> Cloudinary.upload -> lưu URL vào Product_Images.
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
        var image = ProductImage.builder().id(UUID.randomUUID().toString()).product(product).imageUrl(url)
                .isPrimary(count == 0).displayOrder((int) count).createdBy(actor).build();
        images.saveAndFlush(image);
        return listImages(shopId, productId, actor);
    }

    @Transactional
    // Kiểm tra imageId thuộc productId -> repository.delete; không gửi lệnh xóa file trên Cloudinary.
    public void deleteImage(String shopId, String productId, String imageId, String actor) {
        owned(shopId, actor, true);
        products.findByIdAndShopId(productId, shopId).orElseThrow(ManagerProductService::missing);
        var image = images.findByIdAndProductId(imageId, productId).orElseThrow(ManagerProductService::missing);
        images.delete(image);
    }

    // Chuyển ProductVideo entity sang record gồm URL, tiêu đề, mô tả, thứ tự.
    private VideoItem videoItem(ProductVideo v) {
        return new VideoItem(v.getId(), v.getVideoUrl(), v.getTitle(), v.getDescription(), v.getDisplayOrder());
    }

    // Kiểm tra sở hữu shop + sản phẩm -> đọc video theo thứ tự hiển thị.
    public List<VideoItem> listVideos(String shopId, String productId, String actor) {
        owned(shopId, actor, false);
        products.findByIdAndShopId(productId, shopId).orElseThrow(ManagerProductService::missing);
        return videos.findByProductIdOrderByDisplayOrderAsc(productId).stream().map(this::videoItem).toList();
    }

    @Transactional
    // Kiểm tra sở hữu và file (50MB, MP4/WebM/MOV) -> Cloudinary.uploadVideo -> lưu Product_Videos.
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
        var video = ProductVideo.builder().id(UUID.randomUUID().toString()).product(product).videoUrl(url)
                .title(title == null || title.isBlank() ? null : title.strip()).description(description == null || description.isBlank() ? null : description.strip())
                .displayOrder((int) count).createdBy(actor).build();
        videos.saveAndFlush(video);
        return listVideos(shopId, productId, actor);
    }

    @Transactional
    // Kiểm tra videoId thuộc productId -> repository.delete; không xóa file Cloudinary.
    public void deleteVideo(String shopId, String productId, String videoId, String actor) {
        owned(shopId, actor, true);
        products.findByIdAndShopId(productId, shopId).orElseThrow(ManagerProductService::missing);
        var video = videos.findByIdAndProductId(videoId, productId).orElseThrow(ManagerProductService::missing);
        videos.delete(video);
    }
}
