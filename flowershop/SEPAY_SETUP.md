# SePay Sandbox cho demo

Ứng dụng dùng Sandbox cho thanh toán đơn hoa, tiền cọc và gói Pro.
Giao dịch giả lập không chuyển tiền thật, không cần liên kết ngân hàng thật.

## Cấu hình

Điền thông tin merchant Sandbox vào `flowershop/application-sepay-local.properties`
(file này đã được Git bỏ qua):

```properties
sepay.merchant-id=MERCHANT_ID_SANDBOX_CUA_BAN
sepay.secret-key=SECRET_KEY_SANDBOX_CUA_BAN
sepay.checkout-url=https://pay-sandbox.sepay.vn/v1/checkout/init
sepay.api-base-url=https://pgapi-sandbox.sepay.vn
```

Thay giá trị mẫu bằng thông tin Sandbox thực tế; không dùng khóa Production.
Hoặc giữ placeholder trong file local và đặt biến môi trường
`SEPAY_SANDBOX_MERCHANT_ID`, `SEPAY_SANDBOX_SECRET_KEY`.
Không commit Secret Key. Khởi động lại backend sau khi cập nhật.

## URL quay về

`app.frontend-base-url` mặc định là `http://localhost:5173` cho Vite.
SePay quay về `/shop-admin/subscription` khi thanh toán Pro thành công, lỗi hoặc hủy, kèm `invoice_number` và `result`.
Nếu dùng tên miền khác, đặt `FRONTEND_BASE_URL` là URL frontend mà trình duyệt truy cập.
Thanh toán đơn hàng và đặt cọc dùng `sepay.success-url`, `sepay.error-url`, `sepay.cancel-url`; mặc định là `${app.frontend-base-url}/payment/success|error|cancel`, kèm `?invoice_number=...`. Backend còn có `GET /payment/{success|error|cancel}` chuyển tiếp về frontend (`PaymentReturnController`).
Chỉ đặt cổng 8080 khi backend thực sự phục vụ cả bản build frontend.

## Chạy demo

1. Tạo đơn hoa, đơn đặt cọc hoặc chọn gói Pro như bình thường.
2. Kiểm tra host trang thanh toán là `pay-sandbox.sepay.vn`.
3. Dùng chức năng giả lập thanh toán của SePay Sandbox để hoàn tất giao dịch.
4. Backend đối soát qua `pgapi-sandbox.sepay.vn` và cập nhật trạng thái.

Không dùng ứng dụng ngân hàng thật để chuyển tiền demo.
Redirect về frontend không tự xác nhận đã thanh toán; backend vẫn xác minh từ SePay.

Tài liệu chính thức: https://developer.sepay.vn/vi/cong-thanh-toan/sandbox
