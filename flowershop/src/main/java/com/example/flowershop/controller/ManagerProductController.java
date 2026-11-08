package com.example.flowershop.controller;
import com.example.flowershop.service.ManagerProductService;
import com.example.flowershop.dto.auth.CurrentUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
@RestController @RequestMapping("/api/shop/mine/{shopId}/products") @PreAuthorize("hasRole('SHOP')")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name="bearerAuth")
public class ManagerProductController {
    private final ManagerProductService service;
    public ManagerProductController(ManagerProductService s) {service=s;}
    @GetMapping public ManagerProductService.Results list(@PathVariable(name = "shopId") String shopId,@AuthenticationPrincipal CurrentUser u,@RequestParam(name = "page", defaultValue="0") int page) {return service.list(shopId,u.id(),page);}
    @GetMapping("/categories") public List<ManagerProductService.CategoryOption> categories(@PathVariable(name = "shopId") String shopId,@AuthenticationPrincipal CurrentUser u) {service.list(shopId,u.id(),0);return service.categories();}
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ManagerProductService.Result create(@PathVariable(name = "shopId") String shopId,@AuthenticationPrincipal CurrentUser u,@Valid @RequestPart("product") ManagerProductService.SaveProductInput body,@RequestParam(name = "images", required=false) List<MultipartFile> images,@RequestParam(name = "videos", required=false) List<MultipartFile> videos) {return service.save(shopId,null,u.id(),body,images,videos);}
    @PutMapping("/{id}") public ManagerProductService.Result update(@PathVariable(name = "shopId") String shopId,@PathVariable(name = "id") String id,@AuthenticationPrincipal CurrentUser u,@Valid @RequestPart("product") ManagerProductService.SaveProductInput body,@RequestParam(name = "images", required=false) List<MultipartFile> images,@RequestParam(name = "videos", required=false) List<MultipartFile> videos) {return service.save(shopId,id,u.id(),body,images,videos);}
    @DeleteMapping("/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void hide(@PathVariable(name = "shopId") String shopId,@PathVariable(name = "id") String id,@AuthenticationPrincipal CurrentUser u) {service.hide(shopId,id,u.id());}
    @PutMapping("/{id}/unhide") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void unhide(@PathVariable(name = "shopId") String shopId,@PathVariable(name = "id") String id,@AuthenticationPrincipal CurrentUser u) {service.unhide(shopId,id,u.id());}
    @PatchMapping("/{id}/unhide") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void unhidePatch(@PathVariable(name = "shopId") String shopId,@PathVariable(name = "id") String id,@AuthenticationPrincipal CurrentUser u) {service.unhide(shopId,id,u.id());}
    @GetMapping("/{id}/images") public List<ManagerProductService.ImageItem> images(@PathVariable(name = "shopId") String shopId,@PathVariable(name = "id") String id,@AuthenticationPrincipal CurrentUser u) {return service.listImages(shopId,id,u.id());}
    @DeleteMapping("/{id}/images/{imageId}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void deleteImage(@PathVariable(name = "shopId") String shopId,@PathVariable(name = "id") String id,@PathVariable(name = "imageId") String imageId,@AuthenticationPrincipal CurrentUser u) {service.deleteImage(shopId,id,imageId,u.id());}
    @GetMapping("/{id}/videos") public List<ManagerProductService.VideoItem> videos(@PathVariable(name = "shopId") String shopId,@PathVariable(name = "id") String id,@AuthenticationPrincipal CurrentUser u) {return service.listVideos(shopId,id,u.id());}
    @PostMapping("/{id}/videos") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public List<ManagerProductService.VideoItem> uploadVideo(@PathVariable(name = "shopId") String shopId,@PathVariable(name = "id") String id,@AuthenticationPrincipal CurrentUser u,@RequestParam("file") MultipartFile file,@RequestParam(name = "title", required=false) String title,@RequestParam(name = "description", required=false) String description) {return service.uploadVideo(shopId,id,u.id(),file,title,description);}
    @DeleteMapping("/{id}/videos/{videoId}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void deleteVideo(@PathVariable(name = "shopId") String shopId,@PathVariable(name = "id") String id,@PathVariable(name = "videoId") String videoId,@AuthenticationPrincipal CurrentUser u) {service.deleteVideo(shopId,id,videoId,u.id());}
}
