# Admin quản lý cửa hàng

Đã có danh sách, tìm kiếm tên shop/tên hoặc email chủ shop, lọc trạng thái, phân trang,
chi tiết và duyệt/khóa/mở khóa. Không tự tạo hoặc thay đổi dữ liệu shop mẫu.

## Chạy và test

Trong flowershop:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=dev-admin"
```

Trong flower-shop-client: `npm.cmd run dev`. Mở http://localhost:5173/dev-admin,
kéo xuống **Quản lý cửa hàng**. Chế độ thường: đăng nhập Admin đã xác thực email.
Thao tác ghi vào DB đang cấu hình; không public dev-admin qua proxy/tunnel.

1. Tìm theo tên shop/email chủ shop, lọc PENDING/ACTIVE/BANNED/INACTIVE.
2. Xem chi tiết shop và tài khoản chủ shop.
3. Shop PENDING có chủ đủ điều kiện: Duyệt → ACTIVE.
4. Shop ACTIVE: Khóa → BANNED; Manager vẫn đăng nhập được.
5. Shop BANNED có chủ đủ điều kiện: Mở khóa → ACTIVE.
6. Duyệt lần hai hoặc mở khóa shop PENDING qua Swagger: trả 409.

Chủ đủ điều kiện: role SHOP, status ACTIVE, is_email_verified=true.
Tài khoản seed shop1@gmail.com có thể chưa xác thực. Nếu trả 409, cần hoàn tất
thiết lập tài khoản Manager trước khi duyệt/mở khóa; không bỏ kiểm tra OTP.

## API

| Method | URL | Chức năng |
| --- | --- | --- |
| GET | `/api/admin/shops?q=&status=&page=0&size=10` | Danh sách, size 1–100 |
| GET | `/api/admin/shops/{id}` | Chi tiết shop/chủ shop, không trả hash/token |
| PUT | `/api/admin/shops/{id}/approve` | PENDING → ACTIVE |
| PUT | `/api/admin/shops/{id}/block` | ACTIVE → BANNED |
| PUT | `/api/admin/shops/{id}/unblock` | BANNED → ACTIVE |

PUT không cần body, cần CSRF mới từ `/api/auth/csrf` trong cùng phiên.
Chế độ thường cần JWT Admin; dev-admin chỉ bỏ JWT ở localhost cho các route trên.
Sai role trả 403, ID không tồn tại 404, chuyển trạng thái sai hoặc chủ chưa đủ điều kiện 409.
INACTIVE không tự chuyển ACTIVE bằng approve/unblock. Chưa có luồng từ chối hồ sơ PENDING.
Các chuyển trạng thái khóa bản ghi shop trong transaction, ghi last_modify_by/date.
Không đổi trạng thái/role/mật khẩu hoặc thu hồi phiên cá nhân Manager.
Chưa có bảng lịch sử duyệt/khóa đầy đủ, chỉ có người/thời gian cập nhật cuối.

## Kiểm tra hoạt động kinh doanh

`ShopOperationGuard.requireActive(shopId)` khóa bản ghi shop và trả 403 nếu không ACTIVE.
Phải gọi trong transaction ghi hiện có (MANDATORY), sau khi kiểm tra quyền sở hữu hoặc staff–shop.
Project chưa có API catalog/đơn hàng của Manager/Staff, cũng chưa có quan hệ nhân viên–shop.
Guard vì vậy chưa gắn vào các API chưa tồn tại: khi triển khai chúng phải gọi guard trong cùng
transaction với thao tác ghi; kiểm tra role riêng không đủ. Hiện hoàn tất quản lý trạng thái shop,
chưa triển khai các nghiệp vụ kinh doanh hoặc quyền staff theo cửa hàng.

## Kiểm thử và cấu trúc

```powershell
.\gradlew.bat test --tests com.example.flowershop.AdminShopTests --tests com.example.flowershop.DevAdminTests
```

Test H2/mail giả lập: trạng thái, chủ shop, quyền, CSRF, bộ lọc, guard và duyệt đồng thời.
Frontend: models/shopModel.js, controllers/useAdminShopsController.js,
views/AdminShopsView.jsx, services/adminShopService.js.
Backend: AdminShopController, AdminShopService, ShopRepository, ShopResponse, ShopOperationGuard.
Không cần migration; không chạy lại database.sql.

Cập nhật: đã có liên kết staff–shop và API hồ sơ/nhân viên; xem MANAGER_SHOP_GUIDE.md. Các API kinh doanh mới phải dùng ShopOperationGuard.requireAccess để kiểm tra cả trạng thái và quyền.
