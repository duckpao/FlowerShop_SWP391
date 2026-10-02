package com.example.flowershop.controller;
import com.example.flowershop.service.ManagerProductService;
import com.example.flowershop.dto.auth.CurrentUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
// API Product Management của SHOP: role kiểm tra tại đây; quyền sở hữu shop kiểm tra tiếp trong ManagerProductService.
// @AuthenticationPrincipal là người đã được xác thực; @Valid kiểm tra Input JSON trước khi chạy service.
@RestController @RequestMapping("/api/shop/mine/{shopId}/products") @PreAuthorize("hasRole('SHOP')")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name="bearerAuth")
public class ManagerProductController {
    private final ManagerProductService service;
    public ManagerProductController(ManagerProductService s) {service=s;}
    // GET -> service.list: tất cả sản phẩm của shop mình, kể cả đang ẩn.
    @GetMapping public ManagerProductService.Results list(@PathVariable String shopId,@AuthenticationPrincipal CurrentUser u,@RequestParam(defaultValue="0") int page) {return service.list(shopId,u.id(),page);}
    // GET /categories: gọi list trước để kiểm tra chủ shop rồi trả danh mục ACTIVE cho form.
    @GetMapping("/categories") public List<ManagerProductService.CategoryOption> categories(@PathVariable String shopId,@AuthenticationPrincipal CurrentUser u) {service.list(shopId,u.id(),0);return service.categories();}
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    // POST + JSON Input -> save với id=null để tạo mới; trả HTTP 201.
    public ManagerProductService.Result create(@PathVariable String shopId,@AuthenticationPrincipal CurrentUser u,@Valid @RequestBody ManagerProductService.Input body) {return service.save(shopId,null,u.id(),body);}
    // PUT /{id} + JSON Input -> save với id có sẵn để sửa đúng sản phẩm của shop.
    @PutMapping("/{id}") public ManagerProductService.Result update(@PathVariable String shopId,@PathVariable String id,@AuthenticationPrincipal CurrentUser u,@Valid @RequestBody ManagerProductService.Input body) {return service.save(shopId,id,u.id(),body);}
    @DeleteMapping("/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    // DELETE /{id} -> hide chỉ đổi status=INACTIVE; HTTP 204 không có JSON kết quả.
    public void hide(@PathVariable String shopId,@PathVariable String id,@AuthenticationPrincipal CurrentUser u) {service.hide(shopId,id,u.id());}
    // GET /{id}/images -> đọc thông tin ảnh từ Product_Images.
    @GetMapping("/{id}/images") public List<ManagerProductService.ImageItem> images(@PathVariable String shopId,@PathVariable String id,@AuthenticationPrincipal CurrentUser u) {return service.listImages(shopId,id,u.id());}
    @PostMapping("/{id}/images") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    // POST multipart có field file -> service kiểm tra file, upload Cloudinary rồi lưu URL.
    public List<ManagerProductService.ImageItem> uploadImage(@PathVariable String shopId,@PathVariable String id,@AuthenticationPrincipal CurrentUser u,@RequestParam("file") MultipartFile file) {return service.uploadImage(shopId,id,u.id(),file);}
    @DeleteMapping("/{id}/images/{imageId}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    // DELETE ảnh theo imageId thuộc productId; chỉ xóa bản ghi ảnh trong DB.
    public void deleteImage(@PathVariable String shopId,@PathVariable String id,@PathVariable String imageId,@AuthenticationPrincipal CurrentUser u) {service.deleteImage(shopId,id,imageId,u.id());}
    // GET /{id}/videos -> đọc Product_Videos; UI sản phẩm hiện chưa gọi nhánh này.
    @GetMapping("/{id}/videos") public List<ManagerProductService.VideoItem> videos(@PathVariable String shopId,@PathVariable String id,@AuthenticationPrincipal CurrentUser u) {return service.listVideos(shopId,id,u.id());}
    @PostMapping("/{id}/videos") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    // POST multipart gồm file và title/description tùy chọn -> upload Cloudinary + lưu URL.
    public List<ManagerProductService.VideoItem> uploadVideo(@PathVariable String shopId,@PathVariable String id,@AuthenticationPrincipal CurrentUser u,@RequestParam("file") MultipartFile file,@RequestParam(required=false) String title,@RequestParam(required=false) String description) {return service.uploadVideo(shopId,id,u.id(),file,title,description);}
    @DeleteMapping("/{id}/videos/{videoId}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    // DELETE video theo videoId thuộc productId; chỉ xóa bản ghi video trong DB.
    public void deleteVideo(@PathVariable String shopId,@PathVariable String id,@PathVariable String videoId,@AuthenticationPrincipal CurrentUser u) {service.deleteVideo(shopId,id,videoId,u.id());}
}
