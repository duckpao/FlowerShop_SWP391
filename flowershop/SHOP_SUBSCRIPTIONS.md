# Gói đăng ký Free / Pro của shop

Shop mở **Gói đăng ký Pro** tại `/shop-admin/subscription` để mua, gia hạn và chọn sản phẩm nổi bật.

| Gói | Giá VND | Lượt tạo sản phẩm | Sản phẩm nổi bật đồng thời |
| --- | ---: | --- | ---: |
| Free | 0 | 5/tháng dương lịch | 0 |
| Pro 1 tháng | 59.000 | Không giới hạn | 10 |
| Pro 3 tháng | 149.000 | Không giới hạn | 10 |
| Pro 6 tháng | 298.000 | Không giới hạn | 10 |
| Pro 1 năm | 596.000 | Không giới hạn | 10 |

Giá 6/12 tháng áp dụng cùng mức giảm của gói 3 tháng: `1 - 149000/(59000*3)` (~15,82%).
Giá và thời hạn được lưu trong `Shop_Plans`; giao dịch chụp lại số tiền và số tháng tại lúc mua.

## Quyền lợi và hạn mức

- Mọi shop chưa có Pro còn hạn mặc định dùng Free. Tạo sản phẩm mới tiêu thụ lượt, kể cả sản phẩm chưa bật bán; sửa sản phẩm không tiêu thụ lượt.
- `Shop_Product_Postings` giữ lịch sử lượt tạo; xóa/ẩn sản phẩm không hoàn lại lượt. Hạn mức tính theo tháng dương lịch, múi giờ Việt Nam, kể cả sản phẩm tạo khi đang dùng Pro trong tháng đó.
- Khóa dòng shop trước khi kiểm tra/lưu lượt để các request đồng thời không vượt hạn mức.
- Pro được chọn tối đa 10 sản phẩm đang bán, còn hàng, danh mục hoạt động và không bị admin ẩn. Mục nổi bật trên danh sách hoa lấy từ `/api/public/featured-products`.
- Hết hạn: trở về Free và tự ngừng xuất hiện nổi bật ngay theo truy vấn; giữ nguyên sản phẩm và đơn hàng. Mua lại sau khi hết hạn cần chọn lại sản phẩm nổi bật.

## Thanh toán và gia hạn

### Admin xác nhận và kích hoạt Pro

Vào **Admin → Quản lý shop → Chi tiết shop → Gói Pro của cửa hàng**:

- Xác nhận giao dịch `PENDING`/`REVIEW` để kích hoạt đúng gói và số tháng đã lưu khi shop mua. Bắt buộc ghi chú đối chiếu; không khẳng định SePay đã xác nhận thay cho admin.
- Hoặc cấp Pro trực tiếp theo gói 1/3/6/12 tháng, bắt buộc ghi chú lý do. Việc cấp trực tiếp là cấp quyền, không tạo khoản thu hoặc giao dịch thanh toán giả.
- Gia hạn sớm cộng vào hạn còn lại; shop/chủ shop phải đang hoạt động. Chủ shop nhận thông báo chuông.
- `Shop_Pro_Activations` lưu nguồn (`SEPAY`, `ADMIN_CONFIRM`, `ADMIN_GRANT`), admin thực hiện, ghi chú, số tháng và thời gian cộng.
- Xác nhận lại cùng invoice hoặc SePay đối soát sau khi admin đã xác nhận không cộng hạn thêm. Cấp trực tiếp dùng `requestId` UUID chống gửi lặp khi retry.
- API admin được bảo vệ bởi role ADMIN, kiểm tra admin đang hoạt động ở service và CSRF cho POST. Shop/customer không được gọi các API này.

Database đang dùng cần thêm `sql/031_admin_pro_activation.sql` sau `030`; đã đồng bộ vào `database.sql` và áp dụng local sau khi sao lưu `C:/Fall2026/SWP391/tmp/flower_shop_before_admin_pro_20261107.sql`.

- Giao dịch gói lưu trong `Shop_Plan_Payments`, tách khỏi `Orders`/`Payments` của khách mua hoa.
- Checkout dùng cấu hình SePay hiện có; số tiền được lấy từ database ở server, form được ký HMAC.
- URL kết quả chỉ đưa shop về màn hình gói. Luồng tự động chỉ kích hoạt khi phản hồi server-to-server SePay có `CAPTURED`, đúng invoice, tiền và VND; admin có thể xác nhận/cấp trực tiếp như mô tả trên.
- Cơ chế đối soát dùng [API SePay](https://developer.sepay.vn/vi/cong-thanh-toan/API/don-hang/chi-tiet-don-hang), giống luồng thanh toán hiện tại: trang kết quả poll và job chạy nền tiếp tục đối soát ngay cả khi shop đóng tab.
- Mỗi invoice thành công chỉ cộng hạn một lần. Gia hạn sớm cộng vào thời điểm hết hạn hiện tại; mua lại sau hạn tính từ lúc xác nhận thanh toán. Ngày cuối tháng dùng quy tắc `plusMonths` (31/01 + 1 tháng = 28/02 hoặc 29/02).
- Số tiền/invoice lệch chuyển `REVIEW`, không cấp Pro; thông báo chủ shop liên hệ hỗ trợ.

## Job, email và thông báo

`ShopSubscriptionJob` dùng cron Spring Boot `0 0 9,19 * * *`, múi giờ `Asia/Ho_Chi_Minh`.
Nhắc trên ba ngày lịch cuối còn hiệu lực, theo hạn của từng gói, không cố định ngày 28/29/30 hàng tháng.
Không gửi nhắc gia hạn cho gói đã hết hạn. Nếu hạn kết thúc trước 19h ngày cuối thì không gửi khung 19h sau hạn.

`Subscription_Reminders` có khóa duy nhất theo gói, hạn hết, ngày và khung giờ. Chuông thông báo chỉ tạo một lần;
email lỗi được thử lại riêng, tăng khoảng cách tối đa 60 phút. Gia hạn làm thay đổi hạn hết: email nhắc cũ chưa gửi sẽ bị hủy.
Job nền kiểm tra mỗi phút và khôi phục khung gần nhất trong ngày sau khi ứng dụng khởi động lại.
Ứng dụng cần chạy nền liên tục; email cần cấu hình SMTP hiện có. SMTP là tác vụ ngoài database: nếu tiến trình chết ngay sau khi SMTP đã nhận thư nhưng trước commit, lần thử lại có thể gửi lại email đó.

Cấu hình tùy chọn:

```properties
app.shop-subscription.enabled=true
app.shop-subscription.reminder-cron=0 0 9,19 * * *
app.shop-subscription.retry-ms=60000
app.shop-subscription.initial-delay-ms=60000
```

`FRONTEND_BASE_URL` phải trỏ đến frontend đang dùng để SePay đưa shop về đúng `/shop-admin/subscription`.

## Database và triển khai

- Database đang dùng: sao lưu rồi chạy `sql/030_shop_subscriptions.sql`. Migration chỉ thêm bảng/index/seed, chạy lại không reset gói hoặc số tiền đã cấu hình.
- Database mới: import `database.sql` đã chứa phần này. Không import lại toàn bộ file lên database có dữ liệu.
- Database local đã được sao lưu tại `C:/Fall2026/SWP391/tmp/flower_shop_before_subscriptions_20261107.sql` và áp dụng migration.
- Repository mới đều là repository Hibernate tự viết, dùng Session theo transaction của service.

Kiểm thử: `ShopSubscriptionTests` bao gồm quota, request đồng thời, gia hạn, snapshot giá, hết hạn,
giới hạn nổi bật, nhắc đúng ngày/khung giờ, lỗi SMTP, CSRF/phân quyền. `SubscriptionGatewayTests` dùng HTTP server local giả lập phản hồi SePay và kiểm tra chữ ký; không thu tiền thật.
