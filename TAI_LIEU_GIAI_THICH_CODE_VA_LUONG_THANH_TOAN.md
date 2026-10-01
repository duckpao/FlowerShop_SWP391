# TÀI LIỆU TOÀN DIỆN: GIẢI THÍCH KIẾN TRÚC, LUỒNG HOẠT ĐỘNG & CODE
## DỰ ÁN: FLOWERSHOP MARKETPLACE (SWP391 - ĐẠI HỌC FPT)
**Chuyên đề:** Hệ thống Giỏ hàng, Quản lý Đơn hàng & Tích hợp Cổng thanh toán SePay  
**Dành cho:** Sinh viên chuẩn bị nội dung và bảo vệ đồ án trước Hội đồng / Giảng viên

---

## MỤC LỤC
1. [Tổng quan kiến trúc hệ thống](#1-tổng-quan-kiến-trúc-hệ-thống)
2. [Sơ đồ luồng dữ liệu tuần tự (Sequence Diagram)](#2-sơ-đồ-luồng-dữ-liệu-tuần-tự)
3. [Phân tích chi tiết từng file code (Chức năng & Từng dòng lệnh quan trọng)](#3-phân-tích-chi-tiết-từng-file-code)
   - [A. Backend: Spring Boot (Java 17)](#a-backend-spring-boot-java-17)
   - [B. Frontend: React 19 (Vite)](#b-frontend-react-19-vite)
   - [C. Cơ sở dữ liệu: MySQL 8 (Docker)](#c-cơ-sở-dữ-liệu-mysql-8-docker)
4. [Các vấn đề kỹ thuật thực tế đã xử lý & Giải pháp](#4-các-vấn-đề-kỹ-thuật-thực-tế-đã-xử-lý)
5. [Bộ câu hỏi vấn đáp bảo vệ đồ án (Kèm câu trả lời chuẩn điểm tối đa)](#5-bộ-câu-hỏi-vấn-đáp-bảo-vệ-đồ-án)

---

## 1. TỔNG QUAN KIẾN TRÚC HỆ THỐNG

Dự án FlowerShop được xây dựng theo kiến trúc hiện đại phân tách hoàn toàn giữa Frontend và Backend:
- **Frontend:** React 19 (giao diện người dùng, Single Page Application), Vite (công cụ build), Axios (kết nối HTTP API), React Router DOM (điều hướng trang).
- **Backend:** Spring Boot 4.x / Java 17, Spring Data JPA (truy vấn cơ sở dữ liệu), Lombok, Jakarta Validation.
- **Database:** MySQL 8.0 chạy trong Docker container (`flowershop_db`) tại cổng `3307`.
- **Hệ thống thanh toán:** Tích hợp trực tiếp với **Cổng thanh toán SePay (pay.sepay.vn)** và ngân hàng **MBBank** (Virtual Account Prefix `TKPNXT`).

---

## 2. SƠ ĐỒ LUỒNG DỮ LIỆU TUẦN TỰ

Quy trình từ lúc khách hàng duyệt hoa đến khi hoàn tất đơn hàng và thanh toán:

```
[Khách chọn hoa tại HomePage]
              │
              ▼ (1) Thêm sản phẩm
[Backend: CartService.addToCart] ──► Lưu bảng `Cart_Items` trong MySQL
              │
              ▼ (2) Vào trang Giỏ hàng (/cart)
[Frontend: CartPage.jsx] ◄── Lấy danh sách từ `GET /api/cart/{userId}`
              │
              ▼ (3) Bấm "Thanh toán SePay"
[Backend: OrderService.makeOrder]
   ├── Tạo bản ghi trong bảng `Orders` (Status: PENDING)
   ├── Trừ tồn kho (`stock`) trong bảng `Products`
   └── XÓA các sản phẩm tương ứng trong bảng `Cart_Items`
              │
              ▼ (4) Khởi tạo thanh toán bảo mật
[Backend: SePayGatewayService.createPayment]
   ├── Tự truy vấn Database lấy `totalAmount` (Zero Trust Client)
   ├── Sinh mã hóa đơn duy nhất: `INV-07282527-FULL-196466`
   ├── Ký chữ ký số điện tử bằng thuật toán HMAC-SHA256 (Secret Key bí mật)
   └── Lưu bản ghi `Payments` (Status: PENDING)
              │
              ▼ (5) Chuyển hướng khách hàng
[Frontend: submitSePayCheckoutForm] ──► Tự động POST sang Cổng SePay (pay.sepay.vn)
              │
              ▼ (6) Khách hàng quét mã VietQR / Chuyển khoản
[Hệ thống SePay tiếp nhận & Khớp lệnh thành công]
   └── SePay ghi nhận đơn hàng ở trạng thái: "order_status": "CAPTURED"
              │
              ▼ (7) Chuyển hướng quay về Web
[SePay redirect về]: http://localhost:5173/payment/success?invoice_number=INV-...
              │
              ▼ (8) Cơ chế đối soát chủ động thời gian thực (Active Reconciliation)
[Frontend PaymentSuccessPage] ──► Polling: `GET /api/payments/status-by-invoice/{invoiceNumber}`
              │
[Backend SePayGatewayService.getStatusByInvoice]
   ├── Nhận thấy trạng thái trong DB đang là PENDING
   ├── Chủ động gửi GET https://pgapi.sepay.vn/v1/order/detail/{invoiceNumber} (Basic Auth)
   ├── SePay phản hồi: `CAPTURED` (Mã giao dịch: `PAY15996AAB9EC727D49`)
   ├── Backend tự động cập nhật:
   │     • `Payment.status` = SUCCESS
   │     • `Order.status` = PROCESSING
   │     • `depositAmount` = 350.000 đ
   └── Trả về cho Frontend: `{ isPaid: true, status: "SUCCESS" }`
              │
              ▼ (9) Kết thúc
[Giao diện React hiển thị dấu tích xanh: "Thanh toán thành công!"]
```

---

## 3. PHÂN TÍCH CHI TIẾT TỪNG FILE CODE

### A. BACKEND: SPRING BOOT (JAVA 17)

#### 1. `SePayGatewayService.java`
*Đường dẫn:* `flowershop/src/main/java/com/example/flowershop/service/SePayGatewayService.java`  
*Mục đích:* Là bộ não điều khiển toàn bộ nghiệp vụ tích hợp Cổng thanh toán SePay.

**Các đoạn code trọng tâm:**
1. **Khởi tạo phiên thanh toán (`createPayment`):**
   ```java
   // 1. Kiểm tra đơn hàng & quyền sở hữu
   Order order = orderRepository.findById(orderId)
           .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng"));
   
   // 2. Bảo mật Zero Trust: Tự tính số tiền trên Server, KHÔNG tin số tiền từ Client gửi lên
   BigDecimal payableAmount = order.getTotalAmount();
   
   // 3. Tạo mã invoice duy nhất (Unique Invoice Number) có gắn Timestamp
   String invoiceNumber = "INV-" + getShortOrderCode(orderId) + "-" + paymentType + "-" + System.currentTimeMillis() % 1000000;
   
   // 4. Ký số HMAC-SHA256 bảo vệ dữ liệu trên đường truyền
   String signature = SePaySignatureUtil.generateSignature(fields, secretKey);
   fields.put("signature", signature);
   ```
2. **Cơ chế Đối soát chủ động thời gian thực (`reconcileWithSePayGateway`):**
   ```java
   // Dùng HttpClient chuẩn của Java gửi yêu cầu sang SePay REST API
   String pgApiUrl = "https://pgapi.sepay.vn/v1/order/detail/" + invoiceNumber;
   String auth = Base64.getEncoder().encodeToString((merchantId + ":" + secretKey).getBytes(StandardCharsets.UTF_8));
   
   HttpRequest req = HttpRequest.newBuilder()
           .uri(URI.create(pgApiUrl))
           .header("Authorization", "Basic " + auth)
           .GET().build();
   
   HttpResponse<String> resp = HttpClient.newHttpClient().send(req, BodyHandlers.ofString());
   
   // Bóc tách JSON: Nếu SePay xác nhận CAPTURED -> Cập nhật Database ngay
   if (resp.statusCode() == 200 && resp.body().contains("\"order_status\":\"CAPTURED\"")) {
       payment.setStatus(PaymentStatus.SUCCESS);
       payment.setGatewayTransactionNo(transactionNo); // Ví dụ: PAY15996...
       paymentRepository.save(payment);
       
       Order order = payment.getOrder();
       order.setStatus(OrderStatus.PROCESSING);
       orderRepository.save(order);
   }
   ```
3. **Truy vấn trạng thái kèm đối soát (`getStatusByInvoice`):**
   ```java
   // Khi Frontend Polling hỏi kết quả, nếu chưa SUCCESS thì tự động đối soát với SePay ngay
   if (payment.getStatus() != PaymentStatus.SUCCESS) {
       reconcileWithSePayGateway(payment);
   }
   boolean isPaid = payment.getStatus() == PaymentStatus.SUCCESS;
   ```

---

#### 2. `PaymentController.java`
*Đường dẫn:* `flowershop/src/main/java/com/example/flowershop/controller/PaymentController.java`  
*Mục đích:* Khai báo các RESTful API endpoints tiếp nhận yêu cầu từ Frontend hoặc Webhook bên ngoài.

- `POST /api/payments/sepay/create`: Tiếp nhận yêu cầu từ React để tạo phiên thanh toán SePay.
- `GET /api/payments/status-by-invoice/{invoiceNumber}`: Cung cấp endpoint cho React Polling đối soát kết quả.
- `POST /api/payments/sepay/ipn` & `POST /api/sepay/ipn`: Tiếp nhận Webhook ngầm từ SePay Gateway khi triển khai máy chủ thực tế (có kiểm tra tính hợp lệ của chữ ký và chống trùng lặp Idempotency).
- `POST /api/payments/create-qr/{orderId}`: Sinh mã VietQR cho modal quét mã trực tiếp.

---

#### 3. `CartService.java`
*Đường dẫn:* `flowershop/src/main/java/com/example/flowershop/service/CartService.java`  
*Mục đích:* Quản lý giỏ hàng lưu bền vững trong Database (bảng `Cart_Items`).

- `addToCart(userId, request)`: Thêm hoa vào giỏ. Nếu hoa đã có sẵn, tự động cộng dồn số lượng. Luôn kiểm tra tồn kho (`stock`) trong database, nếu số lượng vượt quá tồn kho thì ném ra lỗi `ApiException.badRequest`.
- `updateCartItem(userId, itemId, request)`: Cập nhật số lượng khi bấm nút `+` hoặc `−` trên giao diện.
- `getCart(userId)`: Lấy toàn bộ sản phẩm trong giỏ, tự động tính tổng tiền từng món (`itemTotal = price * quantity`) và tổng tiền toàn bộ giỏ hàng (`grandTotal`).

---

#### 4. `OrderService.java`
*Đường dẫn:* `flowershop/src/main/java/com/example/flowershop/service/OrderService.java`  
*Mục đích:* Xử lý nghiệp vụ chốt đơn hàng từ giỏ hàng.

- `makeOrder(userId, request)`:
  1. Kiểm tra các sản phẩm được chọn có thuộc cùng 1 cửa hàng (`shopId`) hay không.
  2. Tạo bản ghi đơn hàng trong bảng `Orders` với trạng thái ban đầu là `PENDING`.
  3. Tạo chi tiết từng sản phẩm trong bảng `Order_Details`.
  4. Trừ số lượng tồn kho sản phẩm trong bảng `Products`.
  5. **Dòng 152:** `cartItemRepository.deleteAll(selectedCartItems);` — Xóa các sản phẩm đã được đưa vào đơn hàng ra khỏi giỏ để tránh việc đặt trùng lặp.

---

### B. FRONTEND: REACT 19 (VITE)

#### 1. `useCart.js`
*Đường dẫn:* `flower-shop-client/src/hooks/useCart.js`  
*Mục đích:* Custom Hook quản lý giỏ hàng toàn cục cho toàn bộ trang web.

- **Cơ chế chống sản phẩm ảo (Anti-stale State):** Đặt `await fetchCart()` vào khối `finally` của `updateItem` và `removeItem`. Dù gọi API thành công hay thất bại (như đơn hàng đã được tạo trước đó), giao diện luôn đồng bộ lại dữ liệu mới nhất từ MySQL.
- **Xử lý triệt để lỗi bfcache (Back-Forward Cache):**
  ```javascript
  useEffect(() => {
    fetchCart();
    
    // Khi người dùng bấm nút Back của trình duyệt quay lại từ SePay, tự động load lại giỏ hàng
    const handlePageShow = () => { fetchCart(); };
    window.addEventListener('pageshow', handlePageShow);
    window.addEventListener('focus', handlePageShow);
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'visible') fetchCart();
    });
  }, [fetchCart]);
  ```

---

#### 2. `CartPage.jsx`
*Đường dẫn:* `flower-shop-client/src/pages/CartPage.jsx`  
*Mục đích:* Trang giỏ hàng chính của khách hàng.

- Cho phép chọn/bỏ chọn từng sản phẩm hoặc chọn tất cả.
- Khi bấm nút **"Thanh toán SePay"**:
  1. Gọi `orderApi.makeOrder(...)` tạo đơn hàng.
  2. Gọi `paymentApi.createPayment(...)` lấy thông tin thanh toán từ Spring Boot.
  3. Gọi `await fetchCart()` để làm mới giỏ hàng về 0 trước khi rời đi.
  4. Gọi `paymentApi.submitSePayCheckoutForm(checkoutUrl, fields)` để chuyển hướng sang Cổng SePay.

---

#### 3. `PaymentSuccessPage.jsx`
*Đường dẫn:* `flower-shop-client/src/pages/PaymentSuccessPage.jsx`  
*Mục đích:* Màn hình thông báo kết quả giao dịch sau khi khách thanh toán xong.

- Lấy mã `invoice_number` từ URL parameters.
- Thực hiện cơ chế **Polling (hỏi thăm trạng thái)** mỗi 2.5 giây một lần:
  ```javascript
  const res = await paymentApi.getPaymentStatusByInvoice(invoiceNumber);
  if (res.isPaid || res.paymentStatus === 'SUCCESS') {
      setLoading(false); // Dừng xoay vòng
      // Hiển thị màn hình thành công: Mã hóa đơn, Số tiền, Mã giao dịch SePay
  }
  ```

---

## 4. CÁC VẤN ĐỀ KỸ THUẬT THỰC TẾ ĐÃ XỬ LÝ

### Vấn đề 1: Lỗi "Không tìm thấy cart item với id: b03d4c4b-..." khi bấm tăng/giảm số lượng
- **Nguyên nhân:** Khi bấm "Thanh toán SePay", `OrderService.makeOrder` đã xóa sản phẩm trong MySQL để tạo đơn hàng. Khi người dùng bấm nút Back quay lại trang giỏ hàng, trình duyệt dùng bộ nhớ RAM đệm (bfcache) nên vẫn hiển thị sản phẩm cũ. Khi bấm `+`/`−`, client gửi ID cũ đã bị xóa lên server, server trả về lỗi `404 Not Found`. Trong code cũ, hàm `updateItem` gặp lỗi thì chỉ báo lỗi mà không gọi `fetchCart()`, dẫn đến giao diện bị "kẹt cứng" với sản phẩm ảo.
- **Cách xử lý:** Đưa `fetchCart()` vào khối `finally` của `useCart.js` và bổ sung các event listener `pageshow`, `focus` để trình duyệt luôn làm mới dữ liệu từ Database khi quay lại trang.

### Vấn đề 2: Chuyển khoản thành công nhưng Web không tự đối soát được trên Localhost
- **Nguyên nhân:** Cổng SePay nằm trên mạng Internet toàn cầu (`pay.sepay.vn`), còn dự án đang chạy trên máy cá nhân (`http://localhost:8080`). Mạng Internet không thể gửi Webhook/IPN vào máy nội bộ Localhost qua tường lửa. Trước đây hệ thống chỉ thụ động ngồi chờ Webhook gửi tới nên trạng thái trong MySQL bị kẹt ở `PENDING`.
- **Cách xử lý:** Lập trình cơ chế **Active Real-Time Reconciliation (Đối soát chủ động)** trong `SePayGatewayService.java`. Khi trang kết quả hỏi thăm trạng thái, Spring Boot chủ động gọi thẳng REST API của SePay (`https://pgapi.sepay.vn/v1/order/detail/{invoiceNumber}`) bằng Basic Auth để kiểm tra. Khi SePay báo `CAPTURED`, hệ thống cập nhật đơn hàng thành công trong MySQL ngay lập tức mà không cần `ngrok`.

---

## 5. BỘ CÂU HỎI VẤN ĐÁP BẢO VỆ ĐỒ ÁN (DÀNH CHO GIẢNG VIÊN)

### ❓ Câu 1: Tại sao không để React gửi thẳng số tiền sang SePay mà phải gọi qua Spring Boot?
> **Trả lời:**  
> *"Dạ thưa thầy/cô, trong bảo mật hệ thống thanh toán luôn tuân thủ nguyên tắc **Zero Trust Client** (Không bao giờ tin tưởng dữ liệu từ phía trình duyệt). Nếu để React tính và gửi tiền, người dùng am hiểu kỹ thuật có thể mở F12 Developer Tools để sửa giá trị đơn hàng 1.000.000đ thành 1.000đ trước khi gửi đi.  
> Do đó, React chỉ được phép gửi `orderId`. Phía Spring Boot sẽ tự truy vấn Database để tính toán số tiền chính xác, sau đó ký số bảo mật bằng thuật toán **HMAC-SHA256** với Secret Key được lưu an toàn trên máy chủ."*

---

### ❓ Câu 2: Chữ ký số (HMAC-SHA256 signature) có tác dụng gì?
> **Trả lời:**  
> *"Dạ chữ ký HMAC-SHA256 đảm bảo 2 yếu tố cốt lõi:  
> 1. **Tính toàn vẹn dữ liệu (Data Integrity):** Ngăn chặn việc dữ liệu bị chỉnh sửa trên đường truyền mạng giữa khách hàng và Cổng SePay.  
> 2. **Xác thực nguồn gốc (Authentication):** SePay sẽ dùng Secret Key bí mật dùng chung để băm lại các tham số nhận được. Nếu chữ ký trùng khớp, SePay mới công nhận yêu cầu thanh toán này xuất phát chính xác từ website FlowerShop của nhóm em."*

---

### ❓ Câu 3: Làm thế nào hệ thống biết khách hàng đã chuyển tiền thành công khi đang chạy trên máy Localhost?
> **Trả lời:**  
> *"Dạ thưa thầy/cô, nhóm em đã thiết kế kiến trúc **Dual Reconcile Architecture (Bảo vệ kép)**:  
> - Do môi trường `localhost` không thể nhận Webhook trực tiếp từ Internet, hệ thống áp dụng cơ chế **Đối soát chủ động (Active Real-Time Reconciliation)**: Khi khách hàng chuyển tiền xong và quay về trang `PaymentSuccessPage`, Backend Spring Boot sẽ chủ động gọi REST API của SePay (`https://pgapi.sepay.vn/v1/order/detail/{invoiceNumber}`) với Basic Auth để kiểm tra. Ngay khi SePay xác nhận `CAPTURED`, hệ thống cập nhật đơn hàng thành công trong MySQL.  
> - Đồng thời, nhóm vẫn giữ nguyên endpoint `POST /api/payments/sepay/ipn` để khi dự án được triển khai lên máy chủ thật có tên miền công khai, SePay có thể gửi Webhook ngầm song song."*

---

### ❓ Câu 4: Hệ thống phòng chống thanh toán trùng lặp (Idempotency) như thế nào?
> **Trả lời:**  
> *"Dạ hệ thống phòng chống trùng lặp qua 2 lớp bảo vệ:  
> 1. Mỗi lần tạo phiên thanh toán, hệ thống sinh ra một mã hóa đơn duy nhất (`invoice_number`), có gắn kèm mã đơn hàng và Timestamp (`System.currentTimeMillis()`), không bao giờ bị trùng.  
> 2. Trong logic cập nhật thanh toán (cả Webhook lẫn đối soát chủ động), Backend luôn kiểm tra: nếu bản ghi Payment trong database đã ở trạng thái `SUCCESS`, hệ thống lập tức bỏ qua và không cộng dồn tiền hay trừ kho thêm lần nào nữa."*

