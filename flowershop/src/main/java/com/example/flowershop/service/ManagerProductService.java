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
@Service @Transactional(readOnly=true)
public class ManagerProductService {
    public record Input(@NotBlank @Size(max=255) String name,@NotNull @Size(max=5000) String description,
        @NotBlank @Size(max=36) String categoryId,@NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal price,
        @NotNull @Min(0) @Max(1000000) Integer stock,@NotNull ProductStatus status) {}
    public record Result(String id,String shopId,String categoryId,String categoryName,String name,String description,BigDecimal price,Integer stock,ProductStatus status) {}
    public record Results(List<Result> content,int page,int totalPages,long totalElements) {}
    public record CategoryOption(String id,String name) {}
    public record CatalogItem(String id,String shopId,String shopName,String categoryId,String categoryName,String name,String description,BigDecimal price,Integer stock) {}
    public record CatalogResults(List<CatalogItem> content,int page,int totalPages,long totalElements) {}
    public record ImageItem(String id,String url,boolean primary,int displayOrder) {}
    public record VideoItem(String id,String url,String title,String description,int displayOrder) {}
    public record VideoInfo(String url,String title,String description) {}
    public record Detail(String id,String shopId,String shopName,String categoryId,String categoryName,String name,String description,BigDecimal price,Integer stock,List<String> images,List<VideoInfo> videos) {}
    private static final Set<String> ALLOWED_IMAGE_TYPES=Set.of("image/jpeg","image/png","image/webp");
    private static final long MAX_IMAGE_BYTES=5*1024*1024;
    private static final Set<String> ALLOWED_VIDEO_TYPES=Set.of("video/mp4","video/webm","video/quicktime");
    private static final long MAX_VIDEO_BYTES=50*1024*1024;
    private final ShopRepository shops;private final ProductRepository products;private final CategoryRepository categories;
    private final ProductImageRepository images;private final ProductVideoRepository videos;private final CloudinaryService cloudinary;
    public ManagerProductService(ShopRepository s,ProductRepository p,CategoryRepository c,ProductImageRepository images,ProductVideoRepository videos,CloudinaryService cloudinary) {
        shops=s;products=p;categories=c;this.images=images;this.videos=videos;this.cloudinary=cloudinary;
    }
    private static ResponseStatusException missing() {return new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy shop hoặc sản phẩm.");}
    private Shop owned(String id,String actor,boolean write) {
        var shop=(write?shops.findForUpdate(id):shops.findById(id)).orElseThrow(ManagerProductService::missing);
        if(!shop.getOwner().getId().equals(actor)) throw missing();
        if(write && (shop.getStatus()!=ShopStatus.ACTIVE || shop.getOwner().getRole()!=UserRole.SHOP || shop.getOwner().getStatus()!=UserStatus.ACTIVE)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Shop phải được duyệt và đang hoạt động để quản lý mặt hàng.");
        return shop;
    }
    private Result result(Product p) {return new Result(p.getId(),p.getShop().getId(),p.getCategory().getId(),p.getCategory().getName(),p.getName(),p.getDescription(),p.getPrice(),p.getStock(),p.getStatus());}
    private Pageable page(int page) {if(page<0 || page>100000) throw new IllegalArgumentException("Trang không hợp lệ.");return PageRequest.of(page,20,Sort.by(Sort.Order.desc("createdDate"),Sort.Order.asc("id")));}
    private Results response(Page<Product> p) {return new Results(p.getContent().stream().map(this::result).toList(),p.getNumber(),p.getTotalPages(),p.getTotalElements());}
    public Results list(String shop,String actor,int page) {owned(shop,actor,false);return response(products.findByShopId(shop,page(page)));}
    public Results published(String shop,int page) {
        shops.findById(shop).filter(s->s.getStatus()==ShopStatus.ACTIVE).orElseThrow(ManagerProductService::missing);
        return response(products.findByShopIdAndStatus(shop,ProductStatus.ACTIVE,page(page)));
    }
    public List<CategoryOption> categories() {return categories.findByStatusOrderByNameAsc(CategoryStatus.ACTIVE).stream().map(c->new CategoryOption(c.getId(),c.getName())).toList();}
    private CatalogItem catalogItem(Product p) {return new CatalogItem(p.getId(),p.getShop().getId(),p.getShop().getName(),p.getCategory().getId(),p.getCategory().getName(),p.getName(),p.getDescription(),p.getPrice(),p.getStock());}
    public CatalogResults catalog(String categoryId,String q,int page) {
        String query=(q==null || q.isBlank())?null:q.strip();
        if(query!=null && query.length()>100) throw new IllegalArgumentException("Từ khóa tìm kiếm quá dài.");
        var result=products.searchCatalog(ProductStatus.ACTIVE,ShopStatus.ACTIVE,(categoryId==null||categoryId.isBlank())?null:categoryId,query,page(page));
        return new CatalogResults(result.getContent().stream().map(this::catalogItem).toList(),result.getNumber(),result.getTotalPages(),result.getTotalElements());
    }
    @Transactional public Result save(String shopId,String id,String actor,Input input) {
        var shop=owned(shopId,actor,true);
        var product=id==null?new Product():products.findByIdAndShopId(id,shopId).orElseThrow(ManagerProductService::missing);
        var category=categories.findById(input.categoryId()).filter(c->c.getStatus()==CategoryStatus.ACTIVE).orElseThrow(()->new IllegalArgumentException("Danh mục không hợp lệ hoặc đã ngừng hoạt động."));
        if(id==null) {product.setId(UUID.randomUUID().toString());product.setShop(shop);product.setCreatedBy(actor);}
        product.setName(input.name().strip());product.setDescription(input.description().strip());product.setCategory(category);product.setPrice(input.price());product.setStock(input.stock());product.setStatus(input.status());product.setLastModifyBy(actor);
        return result(products.saveAndFlush(product));
    }
    @Transactional public void hide(String shopId,String id,String actor) {
        owned(shopId,actor,true);var p=products.findByIdAndShopId(id,shopId).orElseThrow(ManagerProductService::missing);
        p.setStatus(ProductStatus.INACTIVE);p.setLastModifyBy(actor);
    }
    private ImageItem imageItem(ProductImage i) {return new ImageItem(i.getId(),i.getImageUrl(),Boolean.TRUE.equals(i.getIsPrimary()),i.getDisplayOrder());}
    public List<ImageItem> listImages(String shopId,String productId,String actor) {
        owned(shopId,actor,false);
        products.findByIdAndShopId(productId,shopId).orElseThrow(ManagerProductService::missing);
        return images.findByProductIdOrderByDisplayOrderAsc(productId).stream().map(this::imageItem).toList();
    }
    @Transactional public List<ImageItem> uploadImage(String shopId,String productId,String actor,MultipartFile file) {
        owned(shopId,actor,true);
        var product=products.findByIdAndShopId(productId,shopId).orElseThrow(ManagerProductService::missing);
        if(file==null || file.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn ảnh để tải lên.");
        if(file.getSize()>MAX_IMAGE_BYTES) throw new IllegalArgumentException("Ảnh không được vượt quá 5MB.");
        if(!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) throw new IllegalArgumentException("Chỉ nhận ảnh JPEG, PNG hoặc WebP.");
        String url;
        try {url=cloudinary.upload(file.getBytes(),file.getOriginalFilename(),file.getContentType());}
        catch(java.io.IOException e) {throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Không thể đọc dữ liệu ảnh.",e);}
        long count=images.countByProductId(productId);
        var image=ProductImage.builder().id(UUID.randomUUID().toString()).product(product).imageUrl(url)
                .isPrimary(count==0).displayOrder((int)count).createdBy(actor).build();
        images.saveAndFlush(image);
        return listImages(shopId,productId,actor);
    }
    @Transactional public void deleteImage(String shopId,String productId,String imageId,String actor) {
        owned(shopId,actor,true);
        products.findByIdAndShopId(productId,shopId).orElseThrow(ManagerProductService::missing);
        var image=images.findByIdAndProductId(imageId,productId).orElseThrow(ManagerProductService::missing);
        images.delete(image);
    }
    private VideoItem videoItem(ProductVideo v) {return new VideoItem(v.getId(),v.getVideoUrl(),v.getTitle(),v.getDescription(),v.getDisplayOrder());}
    public List<VideoItem> listVideos(String shopId,String productId,String actor) {
        owned(shopId,actor,false);
        products.findByIdAndShopId(productId,shopId).orElseThrow(ManagerProductService::missing);
        return videos.findByProductIdOrderByDisplayOrderAsc(productId).stream().map(this::videoItem).toList();
    }
    @Transactional public List<VideoItem> uploadVideo(String shopId,String productId,String actor,MultipartFile file,String title,String description) {
        owned(shopId,actor,true);
        var product=products.findByIdAndShopId(productId,shopId).orElseThrow(ManagerProductService::missing);
        if(file==null || file.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn video để tải lên.");
        if(file.getSize()>MAX_VIDEO_BYTES) throw new IllegalArgumentException("Video không được vượt quá 50MB.");
        if(!ALLOWED_VIDEO_TYPES.contains(file.getContentType())) throw new IllegalArgumentException("Chỉ nhận video MP4, WebM hoặc MOV.");
        String url;
        try {url=cloudinary.uploadVideo(file.getBytes(),file.getOriginalFilename(),file.getContentType());}
        catch(java.io.IOException e) {throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Không thể đọc dữ liệu video.",e);}
        long count=videos.countByProductId(productId);
        var video=ProductVideo.builder().id(UUID.randomUUID().toString()).product(product).videoUrl(url)
                .title(title==null||title.isBlank()?null:title.strip()).description(description==null||description.isBlank()?null:description.strip())
                .displayOrder((int)count).createdBy(actor).build();
        videos.saveAndFlush(video);
        return listVideos(shopId,productId,actor);
    }
    @Transactional public void deleteVideo(String shopId,String productId,String videoId,String actor) {
        owned(shopId,actor,true);
        products.findByIdAndShopId(productId,shopId).orElseThrow(ManagerProductService::missing);
        var video=videos.findByIdAndProductId(videoId,productId).orElseThrow(ManagerProductService::missing);
        videos.delete(video);
    }
    public Detail detail(String productId) {
        var product=products.findById(productId).filter(p->p.getStatus()==ProductStatus.ACTIVE && p.getShop().getStatus()==ShopStatus.ACTIVE).orElseThrow(ManagerProductService::missing);
        var urls=images.findByProductIdOrderByDisplayOrderAsc(productId).stream().map(ProductImage::getImageUrl).toList();
        var vids=videos.findByProductIdOrderByDisplayOrderAsc(productId).stream().map(v->new VideoInfo(v.getVideoUrl(),v.getTitle(),v.getDescription())).toList();
        return new Detail(product.getId(),product.getShop().getId(),product.getShop().getName(),product.getCategory().getId(),product.getCategory().getName(),
                product.getName(),product.getDescription(),product.getPrice(),product.getStock(),urls,vids);
    }
}
