package com.example.flowershop.service;

import com.example.flowershop.entity.FavoriteProduct;
import com.example.flowershop.entity.FavoriteProductId;
import com.example.flowershop.entity.Product;
import com.example.flowershop.repository.FavoriteProductRepository;
import com.example.flowershop.repository.ProductRepository;
import com.example.flowershop.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class FavoriteService {
    private final FavoriteProductRepository favorites;
    private final ProductRepository products;
    private final UserRepository users;
    private final ProductCardAssembler assembler;

    public FavoriteService(FavoriteProductRepository favorites, ProductRepository products,
                           UserRepository users, ProductCardAssembler assembler) {
        this.favorites = favorites;
        this.products = products;
        this.users = users;
        this.assembler = assembler;
    }

    private static ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm.");
    }

    @Transactional
    public void add(String userId, String productId) {
        Product product = products.findById(productId).filter(ProductCardAssembler::visible)
                .orElseThrow(FavoriteService::missing);
        FavoriteProductId key = new FavoriteProductId(userId, productId);
        if (favorites.existsById(key)) return; // Lưu lại thứ đã lưu vẫn thành công.
        FavoriteProduct favorite = new FavoriteProduct();
        favorite.setId(key);
        favorite.setUser(users.findById(userId).orElseThrow(FavoriteService::missing));
        favorite.setProduct(product);
        favorite.setCreatedBy(userId);
        favorites.saveAndFlush(favorite);
    }

    /** Bỏ thứ chưa lưu vẫn thành công: deleteById không ném lỗi khi bản ghi không tồn tại. */
    @Transactional
    public void remove(String userId, String productId) {
        favorites.deleteById(new FavoriteProductId(userId, productId));
    }

    public CatalogService.Results mine(String userId, int page) {
        if (page < 0 || page > 100000) throw new IllegalArgumentException("Trang không hợp lệ.");
        Page<FavoriteProduct> result = favorites.findOwned(userId, PageRequest.of(page, 12,
                Sort.by(Sort.Order.desc("createdDate"), Sort.Order.asc("id.productId"))));
        List<Product> items = result.getContent().stream().map(FavoriteProduct::getProduct).toList();
        // Sản phẩm đã bị ẩn vẫn nằm trong danh sách, chỉ mang cờ available = false.
        return new CatalogService.Results(assembler.cards(items), page, 12,
                result.getTotalElements(), result.getTotalPages());
    }
}
