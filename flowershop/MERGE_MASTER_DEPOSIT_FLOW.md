# Luồng đặt hoa riêng sau khi gộp master

## Yêu cầu và đặt cọc

1. Khách gửi thiết kế qua `/api/customer/custom-orders`; lưu `Custom_Order_Requests`, chưa tạo Orders.
2. Shop vào **Yêu cầu đặt hoa riêng**, trao đổi, báo tổng giá hoa và mức cọc 10–100%.
3. Khách vào **Yêu cầu đặt hoa riêng** trong menu tài khoản, chọn địa chỉ GHN và voucher.
4. Preview và tạo đơn dùng cùng giá báo, phí GHN và quy tắc voucher. Server kiểm tra lại tổng tiền khi tạo.
5. Tạo `Orders` CUSTOM/AWAITING_DEPOSIT và `Order_Details`, liên kết `custom_request_id`.
6. Thanh toán DEPOSIT qua SePay; chỉ giao dịch đã xác minh mới chuyển DEPOSIT_PAID.
   Đơn thường và đơn cọc chưa thanh toán hết hạn sau 1 phút; job quét mỗi 5 giây và đối soát SePay trước khi hủy. Đơn COD và đơn đã thanh toán không bị hủy theo thời hạn này.
7. Đơn xuất hiện trong quản lý đơn hàng để shop chuẩn bị và gửi GHN. COD còn lại bằng tổng sau ưu đãi trừ tiền đã thanh toán.

Request được khóa khi tạo đơn để tránh tạo nhiều đơn đang hoạt động từ cùng một yêu cầu.
Nếu đã tạo đơn, trang yêu cầu dẫn tới đơn đó để tiếp tục thanh toán.

## Voucher

- Áp tối đa một mã tiền hàng và một mã ship, của sàn hoặc đúng shop sở hữu đơn.
- Khi chọn mã ship, chỉ mã tiền hàng có mức giảm thực tế thấp nhất trong các mã đủ điều kiện được dùng cùng.
- Hoa thiết kế không phải sản phẩm tham khảo trong catalog: voucher PRODUCT không áp cho thiết kế.
- Tiền cọc mới = tỷ lệ shop báo × tiền hoa sau giảm giá. Voucher ship giảm phí giao hàng, không giảm thêm vào giá hoa.
- Báo giá cũ chưa có tỷ lệ cọc giữ cách tính 50% tổng tiền sau ưu đãi.
- Chốt mã và tăng lượt dùng cùng transaction tạo đơn. Hủy đơn chưa giao trả lượt dùng qua CouponService.

## Pro và tương thích

- Giữ menu Pro/voucher, quyền admin kích hoạt Pro và toàn bộ giới hạn đăng/nổi bật.
- Tạo yêu cầu hay đơn thiết kế không tạo sản phẩm catalog, không tiêu lượt đăng của gói Free.
- Các đơn AWAITING_QUOTE/QUOTED đã có trước merge vẫn xem, báo giá và xử lý tiếp qua luồng tương thích cũ.
- Repository Payment tiếp tục dùng Hibernate tự viết, bổ sung truy vấn lịch sử giao dịch từ master.
- Database mới dùng `database.sql`. Database cũ chạy `sql/032_pending_shop_address_sync.sql` sau khi sao lưu; không import lại toàn bộ database.

Không tự commit/push merge. Sau khi kiểm thử, xem `git status` rồi commit merge khi muốn lưu kết quả gộp.
