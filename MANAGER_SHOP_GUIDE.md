# Hồ sơ cửa hàng và nhân viên

Đã có đăng ký shop qua OTP và lời mời nhân viên qua email: xem [SHOP_ONBOARDING_GUIDE.md](SHOP_ONBOARDING_GUIDE.md).
Mục thêm Staff có sẵn bên dưới vẫn dùng cho liên kết trực tiếp; người chưa có Staff có thể đi qua lời mời.

Mỗi Manager chỉ sở hữu tối đa một shop, ràng buộc UNIQUE(owner_id) tại DB và OneToOne trong entity.
Manager local shop1@gmail.com chỉ quản lý FPTU Smart Floral. Giao diện mở thẳng shop này;
không có bộ chọn shop cho Manager. API vẫn kiểm tra owner, không cho sửa shop khác qua ID.
Migration local: `007_one_shop_per_manager.sql`. Chạy một lần trên DB cũ; nếu có owner trùng,
migration dừng để xử lý dữ liệu, không tự xóa hoặc chuyển shop. Staff không đổi quy tắc phân công.

## Test local không cần đăng nhập

Trong flowershop chạy:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=dev-admin,dev-manager"
```

Chạy frontend `npm.cmd run dev`, mở http://localhost:5173/dev-manager.
Profile dev-manager dùng email `shop1@gmail.com` trong application-dev-manager.properties.
Tài khoản đó phải tồn tại, role SHOP và ACTIVE; chỉ chế độ local này bỏ mật khẩu/xác thực email.
Không sửa emailVerified hoặc role trong DB. Thiếu tài khoản trả 503 với hướng dẫn cấu hình.
CSRF, quyền sở hữu và trạng thái khóa shop vẫn áp dụng. Staff thêm vào shop vẫn cần đủ điều kiện.
Chỉ lắng nghe 127.0.0.1, không public Vite/backend qua mạng hoặc tunnel.
Thao tác ghi vào DB hiện tại. Tắt bằng restart backend không kèm các profile dev.
Trang thử nghiệm không bật trong frontend production. Không tạo phiên JWT thử nghiệm.

Manager đăng nhập role SHOP: trang tài khoản có **Cửa hàng của tôi**.
Chọn shop để xem/sửa tên, mô tả, URL logo HTTPS và địa chỉ Hà Nội.
PENDING được sửa hồ sơ/địa chỉ; ACTIVE được sửa và quản lý nhân viên;
BANNED/INACTIVE chỉ xem. Không tự đổi chủ shop hoặc trạng thái phê duyệt.

## Nhân viên

Manager thêm bằng email tài khoản SHOP_STAFF, ACTIVE, đã xác thực.
Mục thêm trực tiếp liên kết tài khoản hiện có. Mục **Mời nhân viên qua email** hỗ trợ
người chưa có Staff: tự đăng ký OTP, đăng nhập và xác nhận tham gia theo SHOP_ONBOARDING_GUIDE.md.
Không tự đổi role Customer/Admin/Manager. Một Staff có thể thuộc nhiều shop.
Ngừng quyền chỉ vô hiệu hóa liên kết tại shop đó, không khóa tài khoản cá nhân.
Staff đăng nhập thấy các shop được phân công và trạng thái shop, không được sửa hồ sơ.

## API (JWT + CSRF cho thao tác ghi)

| Method | URL | Mục đích |
| --- | --- | --- |
| GET | `/api/shop/mine` | Danh sách shop sở hữu |
| PUT | `/api/shop/mine/{id}` | Body name, description, logoUrl |
| GET/PUT | `/api/shop/mine/{id}/address` | Xem danh sách / cập nhật địa chỉ chính |
| GET/POST | `/api/shop/mine/{id}/staff` | Danh sách / thêm bằng body email |
| PUT | `/api/shop/mine/{id}/staff/{userId}` | Body `{"active":false}` hoặc true |
| GET | `/api/staff/shops` | Shop Staff được phân công |

PUT address dùng addressLine, city, district, ward; địa chỉ chính luôn isDefault=true.
Các ID shop của Manager khác trả 404. URL logo chỉ lưu, không tải file lên server.
Các trường nhạy cảm/role/status/ownerId không nằm trong DTO cập nhật.

Migration `flowershop/sql/006_shop_staff.sql` đã chạy trên DB local, tạo bảng Shop_Staff.
Máy khác cần chạy migration này trước khi dùng; không chạy lại database.sql.
Không có tài khoản thật nào được tự gán role hoặc được thêm vào shop.
Dev-admin chỉ hỗ trợ API Admin; dev-manager là profile riêng cho API Manager, không giả làm Staff.

## Test

1. Restart backend, chạy frontend, đăng nhập Manager đã xác thực có shop.
2. Sửa tên/mô tả/logo và địa chỉ, reload kiểm tra lưu dữ liệu.
3. Dùng email Staff đã xác thực để thêm vào shop ACTIVE.
4. Đăng nhập Staff ở cửa sổ khác: thấy shop được phân công.
5. Manager ngừng quyền, Staff tải lại: shop không còn trong danh sách.
6. Admin khóa shop: Manager vẫn xem được nhưng không sửa hồ sơ/nhân viên.
7. Thử ID shop thuộc Manager khác: trả 404.

`ShopOperationGuard.requireAccess(shopId,actorId)` kiểm tra shop ACTIVE và quyền owner/staff
trong transaction nghiệp vụ. Các API catalog/đơn hàng chưa triển khai phải gọi guard này
khi được bổ sung; việc phân công staff không tự tạo các nghiệp vụ đó.

```powershell
.\gradlew.bat test --tests com.example.flowershop.ManagerShopTests
```

Frontend MVC: controllers/useManagerShopController.js, views/ManagerShopsView.jsx,
services/managerShopService.js; nhãn trạng thái dùng models/shopModel.js.
