package com.example.flowershop.repository;

import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.List;

/**
 * Repository thao tác dữ liệu bảng Đơn hàng (Orders).
 * Cung cấp các phương thức truy vấn danh sách, phân trang, khóa bi quan (pessimistic lock)
 * phục vụ quản lý đơn hàng của khách hàng và chủ cửa hàng (Shop Manager).
 */
public interface OrderRepository extends JpaRepository<Order, String> {
    
    /**
     * Tìm đơn hàng theo ID và áp dụng khóa bi quan (Pessimistic Write Lock).
     * Ngăn chặn race condition khi nhiều luồng cùng thao tác cập nhật trạng thái đơn (xác nhận, giao hàng, hủy...).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") String id);

    /**
     * Lấy toàn bộ danh sách đơn hàng của một khách hàng, sắp xếp theo ngày tạo mới nhất trước.
     */
    List<Order> findByCustomerIdOrderByCreatedDateDesc(String customerId);

    /**
     * Lấy danh sách đơn hàng theo trạng thái được tạo trước mốc thời gian threshold.
     * Thường dùng cho các cron job tự động hủy đơn quá hạn thanh toán (expire orders).
     */
    List<Order> findByStatusAndCreatedDateBefore(OrderStatus status, LocalDateTime threshold);

    /**
     * Kiểm tra xem địa chỉ nhận hàng có đang được liên kết với bất kỳ đơn hàng nào không.
     */
    boolean existsByDeliveryAddressId(String addressId);

    /**
     * Lấy danh sách đơn hàng của shop theo tập hợp trạng thái (ví dụ: hàng đợi chờ xử lý PENDING, PROCESSING), có phân trang.
     */
    Page<Order> findByShopIdAndStatusIn(String shopId, Collection<OrderStatus> statuses, Pageable pageable);

    /**
     * Đếm tổng số đơn hàng của shop thuộc các trạng thái được chỉ định (dùng hiển thị badge số lượng đơn chờ).
     */
    long countByShopIdAndStatusIn(String shopId, Collection<OrderStatus> statuses);

    /**
     * Lấy tất cả đơn hàng thuộc quyền quản lý của shop (hỗ trợ phân trang và sắp xếp).
     */
    Page<Order> findByShopId(String shopId, Pageable pageable);

    /**
     * Lấy danh sách đơn hàng của shop lọc theo một trạng thái cụ thể (PENDING, DELIVERING, COMPLETED...), có phân trang.
     */
    Page<Order> findByShopIdAndStatus(String shopId, OrderStatus status, Pageable pageable);

    /**
     * Tìm đơn hàng theo ID và Shop ID (đảm bảo shop chỉ xem/thao tác đúng đơn hàng của mình, tránh lộ dữ liệu chéo shop).
     */
    Optional<Order> findByIdAndShopId(String id, String shopId);

    /**
     * Lấy danh sách đơn hàng của khách hàng theo phân trang, sắp xếp theo thời gian tạo giảm dần.
     */
    Page<Order> findByCustomerIdOrderByCreatedDateDesc(String customerId, Pageable pageable);
}

