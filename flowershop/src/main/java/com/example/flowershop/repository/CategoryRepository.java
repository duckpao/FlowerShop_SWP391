package com.example.flowershop.repository;
import com.example.flowershop.entity.Category;
import com.example.flowershop.entity.enums.CategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
// Truy vấn Categories cho ô lọc công khai, form Shop và màn hình Admin.
public interface CategoryRepository extends JpaRepository<Category,String> {
    // WHERE status = tham số, ORDER BY name ASC; catalog/form Shop truyền ACTIVE.
    List<Category> findByStatusOrderByNameAsc(CategoryStatus status);
}
