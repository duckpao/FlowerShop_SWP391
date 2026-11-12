# Quản lý và áp dụng voucher

## Thiết lập

Chạy `sql/024_voucher_management.sql` trên schema ứng dụng trước khi khởi động backend mới. Database local `flower_shop_db` đã được cập nhật. Script chạy lại được, giữ mã và đơn hàng hiện có, không phát hành thêm mã. `database.sql` đã có các cột mới và bốn mã mẫu cho database tạo mới; không chạy lại toàn bộ file này trên database đang dùng.

```sql
USE flower_shop_db;
SOURCE C:/Fall2026/SWP391/tmp/FlowerShop_SWP391/flowershop/sql/024_voucher_management.sql;
```

## Phân quyền và dữ liệu

Dữ liệu mẫu cho shop: chạy `sql/025_seed_shop_vouchers.sql` để bổ sung mỗi shop hiện có lên ba mã ORDER đang dùng được (tính cả mã cũ). Các mã mới giảm 5%/10%/15%, trần tương ứng 20.000/50.000/70.000đ, đơn tối thiểu 0đ, 1.000 lượt, hạn một năm từ lúc nạp. Mỗi mã chỉ áp dụng cho shop sở hữu. Script không ghi đè mã cũ, không đặt lại lượt dùng và chạy lại ngay không thêm trùng. `database.sql` đã nhúng cùng dữ liệu seed cho database tạo mới. Các shop tạo sau khi chạy script có thể tạo mã từ màn quản lý hoặc chạy lại script để bổ sung dữ liệu mẫu.

- ADMIN: màn `/admin/vouchers`, API `/api/admin/vouchers`. CRUD mã sàn (`shop_id IS NULL`) cho tiền hàng, phí giao hàng hoặc một sản phẩm. Không sửa mã riêng của shop qua API sàn.
- SHOP: màn `/shop-admin/vouchers`, API `/api/shop/mine/{shopId}/vouchers`. Chỉ chủ shop quản lý mã ORDER của chính shop, chỉ shop hoạt động mới tạo/cập nhật được. Không tạo mã phí ship/sản phẩm/toàn sàn.
- CUSTOMER: chọn các mã sàn và mã của shop đang mua. Danh sách được lấy từ `Coupons`, không hardcode. Đây là tập mã công khai đủ phạm vi áp dụng; không có bước nhận/lưu voucher cá nhân hay bảng ví voucher riêng.
- CRUD dùng GET danh sách, POST tạo, PUT `/{id}` cập nhật, DELETE `/{id}` xóa mềm. Mã bị xóa không xuất hiện trong lựa chọn mới; đơn cũ vẫn giữ mã và số tiền đã giảm.

`Coupons` thêm `target` (`ORDER`, `SHIPPING`, `PRODUCT`), `product_id`, `deleted`. `Orders` thêm `shipping_coupon_id`, `coupon_code`, `shipping_coupon_code`, `shipping_discount_amount`, `coupons_released`. `coupon_id`/`discount_amount` giữ ý nghĩa mã/số tiền giảm tiền hàng, gồm mã sản phẩm.

## Quy tắc tính

- Hỗ trợ phần trăm (>0 đến 100%) hoặc số tiền cố định. Mỗi mã có ngày bắt đầu/kết thúc (giờ Việt Nam), đơn tối thiểu, lượt dùng tối đa, trạng thái và trần giảm nguyên đồng lớn hơn 0, tối đa 100.000đ. Trần là giới hạn giảm, không phải mức giảm tối thiểu; ví dụ mã 1% của đơn 100.000đ giảm 1.000đ.
- Mã ORDER giảm trên tổng tiền hàng. Mã PRODUCT chỉ giảm trên các dòng của sản phẩm chỉ định, chiếm cùng ô mã tiền hàng. Mã SHIPPING giảm trên phí GHN; không giảm quá phí thực tế. Đơn hoa riêng được báo giá theo thiết kế, không coi mẫu tham khảo là sản phẩm đã mua để áp mã PRODUCT.
- Mỗi đơn tối đa 1 mã tiền hàng/sản phẩm + 1 mã ship. Khi dùng mã ship, mã tiền hàng chỉ hợp lệ nếu số tiền giảm thực tế bằng mức thấp nhất trong các mã tiền hàng đang đủ điều kiện của đơn đó. Các mã đồng hạng thấp nhất đều dùng được. Loại trừ mã hết hạn, chưa bắt đầu, hết lượt, sai shop/sản phẩm, chưa đạt đơn tối thiểu, bị ngừng/xóa khỏi phép tìm mức thấp nhất.
- Các mã tiền hàng khác vẫn xuất hiện, bị Disable và hiển thị chính xác `mã voucher không phù hợp`. Backend cũng từ chối tổ hợp này, kể cả gọi API trực tiếp.
- Gợi ý so sánh mã tiền hàng tốt nhất khi dùng riêng với tổ hợp mã ship tốt nhất + mã tiền hàng có mức giảm thực tế thấp nhất. Chọn phương án có tổng tiết kiệm cao hơn. Bằng nhau ưu tiên một mã tiền hàng để tránh tốn thêm mã ship.
- Làm tròn tiền giảm xuống đồng nguyên; tiền hàng sau giảm còn tối thiểu 1đ. Tổng = tiền hàng + phí ship gốc − giảm tiền hàng − giảm ship.

## Tích hợp đặt hàng

Các API preview xác định tiền hàng từ database và phí ship từ GHN, không tin tổng tiền client:

- `POST /api/orders/{userId}/voucher-preview`: giỏ hàng, dữ liệu như tạo đơn, `couponId` là chuỗi mã tiền hàng để tương thích API cũ; thêm `shippingCouponCode`.
- `POST /api/customer/orders/voucher-preview`: mua ngay, `productId`, `quantity`, `addressId`, `couponCode`, `shippingCouponCode`.
- `POST /api/customer/orders/{id}/voucher-preview`: đơn CUSTOM đã QUOTED của khách, `couponCode`, `shippingCouponCode`.

Preview trả hai danh sách với `eligible`, `reason`, `saving`, mã gợi ý và các tổng tiền. Preview không giữ lượt dùng. Khi tạo đơn hoặc chấp nhận báo giá, backend khóa các mã theo cùng thứ tự, kiểm tra lại điều kiện và tăng lượt dùng trong cùng transaction với đơn hàng. Có thể mã hết lượt/thay đổi giữa xem và đặt; API sẽ báo để khách xem lại. Frontend gửi `expectedTotal` khi đặt/chấp nhận để tránh chốt tổng khác với tổng khách vừa xem.

Mua ngay, giỏ hàng và COD/ONLINE dùng số tiền sau giảm làm số tiền thanh toán. Đơn CUSTOM chỉ áp/giữ lượt dùng khi khách chấp nhận báo giá, không áp khi mới gửi thiết kế hoặc shop báo giá. Tên mã và tiền giảm chụp lại trên đơn, nên sửa/xóa voucher không thay đổi tổng đơn cũ. Khi shop hủy đơn trước giao, đơn hết hạn thanh toán hoặc GHN báo hủy, hoàn lượt dùng của cả hai mã tối đa một lần. Hoàn hàng sau giao không tự cấp lại lượt voucher.

## Kiểm thử

`CouponTests`: quyền CRUD, phạm vi shop, trần giảm, thời gian/đơn tối thiểu/hết lượt, mã sản phẩm, quy tắc Disable theo số tiền thực tế và đồng hạng, gợi ý tối ưu, tổng đơn/thanh toán của ba luồng, hoàn lượt một lần, chặn tổ hợp giả mạo và hai transaction tranh lượt cuối. Các kiểm thử chạy bằng H2, không tạo đơn hoặc voucher trên database local.
