package com.example.flowershop.repository;
import com.example.flowershop.entity.Category;
import com.example.flowershop.entity.enums.CategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CategoryRepository extends JpaRepository<Category,String> {
    List<Category> findByStatusOrderByNameAsc(CategoryStatus status);
}
