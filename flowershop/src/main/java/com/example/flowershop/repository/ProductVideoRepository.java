package com.example.flowershop.repository;
import com.example.flowershop.entity.ProductVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ProductVideoRepository extends JpaRepository<ProductVideo,String> {
    List<ProductVideo> findByProductIdOrderByDisplayOrderAsc(String productId);
    long countByProductId(String productId);
    Optional<ProductVideo> findByIdAndProductId(String id,String productId);
}
