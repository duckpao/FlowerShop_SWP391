# Customer đăng ký mở shop và Manager quản lý mặt hàng

## Luồng chính

1. Đăng nhập Customer tại `/login`, quay về `/`.
2. Chọn **Đăng ký làm Manager shop**. Nhập họ tên, điện thoại, tên shop, mô tả, địa chỉ Hà Nội và gửi đơn.
3. Khi chờ Admin duyệt, tài khoản vẫn là Customer và chưa tạo shop. Không gửi được đơn trùng đang chờ.
4. Admin đăng nhập, mở **Hồ sơ cá nhân** (`/account`) → **Đơn đăng ký Manager / mở shop**. Có lọc chờ duyệt/đã duyệt/đã từ chối, phân trang, ghi chú phản hồi. `/dev-admin` cũng có mục này nếu backend bật profile dev-admin để test local.
5. Admin **Duyệt và tạo shop**: trong cùng giao dịch, backend tạo shop ACTIVE, tạo địa chỉ shop, chuyển Customer sang SHOP và thu hồi các phiên cũ. Customer phải đăng nhập lại. Mỗi người sở hữu tối đa một shop.
6. Admin **Từ chối**: giữ nguyên role và không tạo shop; Customer xem ghi chú rồi có thể gửi lại.
7. Người được duyệt đăng nhập lại → **Vào trang quản trị shop** → **Quản lý mặt hàng của shop**.
8. Chọn danh mục có sẵn, nhập tên/mô tả/giá/tồn kho/trạng thái, lưu. Sản phẩm ACTIVE xuất hiện trong trang shop công khai. Có thể sửa hoặc ẩn sản phẩm; ẩn giữ lại dữ liệu để không làm hỏng tham chiếu đơn hàng.

## Kiểm tra cách ly shop

- Dùng Manager A và Manager B sở hữu hai shop khác nhau.
- Manager B gọi GET/POST products của shop A phải bị từ chối (404); không có dữ liệu riêng của shop A trong danh sách quản trị của B.
- PUT/DELETE với shop B nhưng productId của A cũng phải bị từ chối (404).
- Shop PENDING/BANNED không thêm, sửa, ẩn sản phẩm được (403).
- Customer/Staff không được gọi API quản lý mặt hàng Manager (403).
- Quyền sở hữu lấy từ tài khoản JWT và DB; form không cho thay owner hay chuyển sản phẩm sang shop khác.
- Người dùng đã chuyển Staff hoặc bị khóa trong thời gian chờ không được Admin duyệt thành Manager qua đơn này.

## API

| Method | Đường dẫn | Mục đích |
|---|---|---|
| POST | `/api/customer/manager-applications` | Gửi đơn mở shop |
| GET | `/api/account/manager-applications` | Xem đơn của mình |
| GET | `/api/admin/shops/applications?status=PENDING&page=0` | Admin xem đơn |
| PUT | `/api/admin/shops/applications/{id}` | `{"approve":true,"note":"Đủ thông tin"}`; false để từ chối |
| GET/POST | `/api/shop/mine/{shopId}/products` | Xem danh sách / tạo sản phẩm |
| GET | `/api/shop/mine/{shopId}/products/categories` | Các danh mục được sử dụng |
| PUT/DELETE | `/api/shop/mine/{shopId}/products/{id}` | Sửa / ẩn sản phẩm |
| GET | `/api/public/shops/{shopId}/products?page=0` | Sản phẩm ACTIVE của shop ACTIVE |

API ghi cần JWT và CSRF; frontend tự gửi. Ví dụ body sản phẩm:

```json
{"name":"Bó hoa hồng","description":"Hoa hồng tươi","categoryId":"cat-01","price":250000,"stock":10,"status":"ACTIVE"}
```

## Cài đặt và phạm vi

- Chạy `flowershop/sql/010_manager_applications.sql` trên database hiện có rồi khởi động lại backend. Không chạy lại file database.sql có lệnh xóa DB.
- Đã áp dụng migration 010 trên DB local trong phiên triển khai.
- Trạng thái đơn được xem trong ứng dụng qua nút cập nhật; không có email thông báo cho đơn Manager ở phiên bản này.
- Form sản phẩm hiện hỗ trợ dữ liệu cơ bản, chưa có tải file ảnh/video. Danh mục là dữ liệu chung chỉ được chọn, Manager không sửa danh mục hệ thống.
- API đăng ký Vendor qua OTP cũ vẫn giữ tương thích; shop tạo qua luồng cũ vẫn phải được Admin duyệt ACTIVE trước khi đăng sản phẩm. Luồng giao diện mới sử dụng Customer đã đăng nhập và đơn mở shop riêng.
