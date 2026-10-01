# 🌻 Dự án Chuỗi Cửa Hàng Hoa (Flower Shop Marketplace)

Hệ thống quản lý chuỗi cửa hàng bán hoa đa chi nhánh (**Marketplace**), bao gồm các chức năng:

* 🛍️ Mua sắm hoa
* 🌷 Đặt hoa theo yêu cầu (**Custom Order**)
* 💳 Tích hợp thanh toán
* 🚚 Quản lý giao hàng

Dự án được chia thành hai phần độc lập:

* **Backend:** Java Spring Boot
* **Frontend:** ReactJS + Vite

---

## Database cho các chức năng trên nhánh Quangdv

Sau khi khởi tạo database mới, cần chạy các migration trong `flowershop/sql/` theo thứ tự `001` đến `012` trên database `flower_shop_db`. Migration `011_online_cod_payment_methods.sql` quy chuẩn dữ liệu cũ về hai phương thức `ONLINE` và `COD`.

Với database đã chạy các migration này, không chạy lại toàn bộ: một số migration thêm cột hoặc ràng buộc chỉ được chạy một lần. Không chạy lại script khởi tạo trên database có dữ liệu cần giữ. Cập nhật code không tự động chạy migration.

## 🛠️ Yêu cầu hệ thống (Prerequisites)

Để chạy được dự án trên máy cá nhân, team cần cài đặt sẵn các công cụ sau:

* **Java 17:** Dùng cho môi trường Backend.
* **Node.js:** Khuyến nghị sử dụng phiên bản LTS để chạy Frontend.
* **Docker Desktop:** Bắt buộc phải cài đặt và bật trước khi chạy project. Docker được sử dụng để tự động khởi tạo cơ sở dữ liệu MySQL thông qua file `docker-compose.yml`.

---

## 🐳 Hướng dẫn làm việc với Docker (Database)

Dự án sử dụng **MySQL 8.0** chạy bằng Docker để đồng bộ môi trường Database cho toàn bộ team.

Mọi thao tác cấu hình Database, tạo bảng và chèn dữ liệu mẫu đã được tự động hóa thông qua file `database.sql`.

### 1. Khởi động Database

Mở terminal tại thư mục gốc của project, nơi chứa file `docker-compose.yml` và `database.sql`, sau đó chạy:

```bash
docker-compose up -d
```

> **Lưu ý:** Lần chạy đầu tiên có thể mất khoảng **15–20 giây** để Docker tự động tạo các bảng và chèn dữ liệu mẫu (Mock Data).

### 2. Xử lý lỗi Database (Reset toàn bộ)

Nếu gặp các lỗi như:

* `Access denied`
* Sai mật khẩu Database
* Lỗi kết nối
* Muốn xóa toàn bộ Database và chạy lại file SQL mới nhất

Hãy chạy:

```bash
docker-compose down -v
```

> ⚠️ **Lưu ý:** Hậu tố `-v` rất quan trọng vì nó sẽ xóa Volume chứa dữ liệu Database hiện tại.

Sau đó khởi tạo lại Database:

```bash
docker-compose up -d
```

Docker sẽ tạo lại Database từ đầu dựa trên cấu hình và file SQL của project.

---

## 🚀 Hướng dẫn chạy Backend (Spring Boot)

Backend cung cấp các **RESTful API** và tự động kết nối tới Database MySQL được Docker khởi tạo.

Database chạy tại **port `3307`**.

### 1. Di chuyển vào thư mục Backend

Mở terminal và chạy:

```bash
cd flowershop
```

### 2. Khởi chạy Spring Boot

**Windows:**

```bash
.\gradlew bootRun
```

**Mac/Linux:**

```bash
./gradlew bootRun
```

Sau khi khởi động thành công, Backend sẽ chạy tại:

```text
http://localhost:8080
```

---

## 🎨 Hướng dẫn chạy Frontend (ReactJS - Vite)

Frontend được xây dựng bằng **ReactJS (JSX)** và sử dụng **Vite** để tối ưu tốc độ phát triển và build.

### 1. Di chuyển vào thư mục Frontend

Frontend hiện được đóng gói và phục vụ bởi Spring Boot tại cổng 8080. Nếu backend đã chạy thì chỉ cần mở http://localhost:8080, không chạy thêm server frontend. Gradle tự chạy `npm ci` và build React khi cần.

Cách khởi động thay thế từ thư mục frontend (chỉ dùng khi backend chưa chạy):

```bash
cd flower-shop-client
```

### 2. Cài đặt các thư viện

Chỉ cần thực hiện bước này **lần đầu tiên sau khi clone project** hoặc khi `package.json` có thay đổi:

```bash
npm install
```

### 3. Khởi chạy Frontend

```bash
npm run dev
```

Sau khi khởi động thành công, mở trình duyệt và truy cập:

```text
http://localhost:8080
```

---

# 🌿 Quy trình làm việc với Git (Git Workflow)

## Bước 1: Cập nhật code mới nhất

Luôn đảm bảo bạn đang ở nhánh `master` và lấy code mới nhất trước khi bắt đầu công việc:

```bash
git checkout master
git pull origin master
```

---

## Bước 2: Tạo nhánh làm việc riêng

> ⚠️ **Tuyệt đối không code trực tiếp trên nhánh `master`.**

Tên nhánh cần có tiền tố rõ ràng, phản ánh đúng loại công việc.

### Thêm tính năng mới

```bash
git checkout -b feature/ten-tinh-nang
```

### Sửa lỗi

```bash
git checkout -b bugfix/ten-loi
```

**Ví dụ:**

```bash
git checkout -b feature/cart-api
```

---

## Bước 3: Code và Commit

Thực hiện công việc và commit thường xuyên.

Commit message cần rõ nghĩa và mô tả đúng tác vụ đã thực hiện.

**Ví dụ:**

```bash
git add .
git commit -m "feat: hoàn thiện API giỏ hàng và tích hợp HMAC-SHA256"
```

---

## Bước 4: Đẩy code và tạo Pull Request (PR)

Đẩy branch cá nhân lên GitHub:

```bash
git push origin feature/ten-tinh-nang
```

Sau đó truy cập GitHub và tạo **Pull Request (PR)**:

```text
feature/ten-tinh-nang → master
```

Sau khi tạo PR, **tag Bảo** để review trước khi merge.

---

# ⚠️ Quy tắc tránh Conflict & Lưu ý chung

## 1. Giao tiếp và phân chia Task

Phân chia task độc lập giữa các thành viên.

Hạn chế tối đa việc **2 người cùng chỉnh sửa một file**, đặc biệt là các file cấu hình chung, trong cùng một khoảng thời gian.

---

## 2. Đồng bộ code thường xuyên

Nếu một task kéo dài nhiều ngày, mỗi buổi làm việc nên đồng bộ code mới nhất từ `master` vào branch hiện tại:

```bash
git pull origin master
```

Nếu xảy ra conflict, hãy giải quyết conflict ngay thay vì để dồn đến cuối task.

---

## 3. Kỷ luật `.gitignore`

**Tuyệt đối không commit** các file rác hoặc thư mục sinh ra từ IDE/build process.

Một số thư mục/file cần tránh commit:

```text
.idea/
.vscode/
node_modules/
build/
dist/
```

Hãy đảm bảo `.gitignore` của project được cấu hình đúng và tất cả thành viên tuân thủ thống nhất.

---

# 📌 Quick Start

Nếu đã cài đầy đủ **Java 17 + Node.js + Docker Desktop**, có thể chạy project theo thứ tự:

### 1. Khởi động Database

```bash
docker-compose up -d
```

### 2. Chạy Backend

```bash
cd flowershop

# Windows
.\gradlew bootRun

# Mac/Linux
./gradlew bootRun
```

Backend:

```text
http://localhost:8080
```

### 3. Chạy Frontend

Không cần terminal thứ hai: backend ở bước trên đã build và phục vụ frontend trên cùng cổng 8080. Lệnh dưới đây chỉ là cách khởi động thay thế khi backend chưa chạy:

```bash
cd flower-shop-client
npm install
npm run dev
```

Frontend:

```text
http://localhost:8080
```

---

## Test thanh toán SePay trên máy cá nhân

Mỗi thành viên dùng thông tin **Sandbox** của SePay. Chủ tài khoản test lấy Merchant ID và Secret Key trong mục Sandbox rồi chia sẻ qua kênh riêng của nhóm. Thành viên cũng có thể tạo tài khoản Sandbox riêng. Không đưa khóa vào Git hoặc file `.example`.

Trong thư mục `flowershop`, sao chép file mẫu và điền `sepay.merchant-id`, `sepay.secret-key`:

```powershell
Copy-Item application-sepay-local.properties.example application-sepay-local.properties
.\gradlew.bat bootRun
```

File `application-sepay-local.properties` đã được Git ignore. File mẫu chọn Checkout và API đối soát của Sandbox, nên có thể thử đặt hàng và thanh toán giả lập mà không dùng tiền thật. Khi thử IPN, cấu hình thêm `sepay.ipn-secret` và URL callback công khai trong SePay; `localhost` không nhận được IPN từ Internet. Nếu không có khóa Sandbox, luồng thanh toán trực tuyến sẽ không khởi tạo được.

Khóa Production từng được commit cần được thu hồi và cấp lại; xóa khóa khỏi bản hiện tại không xóa được lịch sử Git.

## 🎯 Tổng quan cấu trúc Project

```text
Flower-Shop-Marketplace/
│
├── docker-compose.yml
├── database.sql
│
├── flowershop/              # Backend - Spring Boot
│   ├── src/
│   ├── build.gradle
│   └── gradlew
│
└── flower-shop-client/      # Frontend - React + Vite
    ├── src/
    ├── package.json
    └── vite.config.js
```

**Happy Coding! 🌻**
