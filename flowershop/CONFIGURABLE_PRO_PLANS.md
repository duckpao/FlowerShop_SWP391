# Gói Pro do admin cấu hình

Thay thế quy tắc cũ 5 lượt/tháng và Pro không giới hạn ở mọi gói.

| Gói mặc định | Giá | Tổng sản phẩm hiện có | Sản phẩm nổi bật |
|---|---:|---:|---:|
| Free | 0 | 5 | 0 |
| Pro 1 tháng | 59.000 | 15 | 3 |
| Pro 3 tháng | 149.000 | 25 | 5 |
| Pro 6 tháng | 298.000 | 40 | 8 |
| Pro 12 tháng | 596.000 | Không giới hạn | 15 |

## Admin

Menu **Quản lý gói Pro** (`/admin/shop-plans`) cho phép tạo gói, sửa tên, giá,
thời lượng, giới hạn sản phẩm, giới hạn nổi bật và ngừng bán/bật lại gói.
Giới hạn sản phẩm để trống nghĩa là không giới hạn. Free cố định 5/0.
Xóa gói là ngừng bán, giữ lịch sử và quyền đến hết hạn của shop đã mua.
Không thể mua hoặc cấp mới gói đã ngừng bán.

Giá và số tháng trên invoice đã tạo được giữ nguyên sau khi admin sửa gói.
Quyền lợi lấy từ cấu hình gói hiện tại, áp dụng cả cho shop đang sử dụng.
Khi giảm giới hạn hoặc hết hạn, không xóa sản phẩm cũ; chỉ chặn tạo thêm nếu đạt/vượt mức.
Các sản phẩm vượt giới hạn cũng bị khóa bán ngay: không hiện công khai, không mở được
chi tiết mua hàng, không thêm giỏ/đặt hàng/áp voucher và không thể bật nổi bật.
Hệ thống ưu tiên sản phẩm theo thời điểm tạo tăng dần, cùng thời điểm thì theo ID.
Trong trang quản lý, sản phẩm bị khóa vẫn hiển thị để shop xóa hoặc nâng cấp gói.
Quyền được kiểm tra trực tiếp từ database ở mỗi thao tác, áp dụng cả dữ liệu import,
gia hạn/hết hạn và thay đổi giới hạn của admin, không chờ job hoặc đặt lại theo tháng.
Xóa bớt sản phẩm cũ hoặc nâng cấp sẽ tự mở khóa các sản phẩm đủ điều kiện.
Danh sách nổi bật công khai chỉ hiển thị số vị trí được phép; shop/admin có thể bỏ bớt lựa chọn cũ.

Trong chi tiết shop, admin có thể cấp gói/xác nhận hóa đơn và chọn/bỏ chọn
sản phẩm nổi bật. Cả admin lẫn shop đều bị kiểm tra giới hạn ở backend.

## Sản phẩm và quota

Quota là tổng sản phẩm chưa xóa của shop, gồm cả ACTIVE, OUT_OF_STOCK, INACTIVE
và sản phẩm bị admin ẩn. Không đặt lại theo tháng và không tính bằng lượt tạo.
Nút **Ẩn** không giải phóng quota. Nút **Xóa** giải phóng quota và không cho phục hồi.
Xóa mềm bằng `Products.deleted`, giữ khóa ngoại đơn hàng/review; đồng thời đặt INACTIVE,
gỡ khỏi danh sách nổi bật và loại khỏi danh sách quản lý của shop.
Lịch sử `Shop_Product_Postings` vẫn được giữ nhưng không còn quyết định quota.

## Database

Database đang dùng: chạy `sql/033_configurable_shop_plans.sql` sau khi sao lưu.
Đổi `Shop_Plans.monthly_product_limit` thành `product_limit`; thêm `Products.deleted`.
Chỉ cập nhật quyền lợi seed cũ; không reset giá/invoice/subscription và không ghi đè
cấu hình admin khi chạy lại trên các gói đã thay đổi giới hạn.
Database tạo mới: dùng `database.sql`, không import lại lên database đang có dữ liệu.

Thanh toán vẫn dùng SePay Sandbox. Nhắc hạn 09:00 và 19:00 trong 3 ngày cuối giữ nguyên,
nội dung không còn mặc định mọi gói được đăng không giới hạn/10 vị trí.
