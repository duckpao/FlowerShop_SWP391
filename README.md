# Dự án Chuỗi Cửa Hàng Hoa (Flower Shop)

Hệ thống quản lý chuỗi cửa hàng bán hoa, bao gồm chức năng quản lý tồn kho đa chi nhánh, đặt hàng và giao diện khách hàng. Dự án được chia thành hai phần độc lập: Backend (Spring Boot) và Frontend (ReactJS).

## 🛠 Yêu cầu hệ thống (Prerequisites)

Để chạy được dự án này trên máy cá nhân, bạn cần cài đặt sẵn các phần mềm sau:
*   **Java 17** (Dành cho môi trường Backend).
*   **Node.js** (Khuyến nghị bản LTS để chạy Frontend).
*   **Docker Desktop**: Bắt buộc phải được bật để Spring Boot tự động khởi tạo cơ sở dữ liệu MySQL thông qua file `compose.yaml`.

## 🚀 Hướng dẫn chạy Backend (Spring Boot)

Backend cung cấp các RESTful API và tự động kết nối với Database MySQL thông qua Spring Boot Docker Compose.

1.  Mở terminal và di chuyển vào thư mục backend:
    ```bash
    cd flowershop
    ```
2.  Đảm bảo **Docker Desktop đang mở** và hoạt động.
3.  Khởi chạy ứng dụng bằng Gradle wrapper:
    *   Trên Windows: `.\gradlew bootRun`
    *   Trên Mac/Linux: `./gradlew bootRun`
4.  Backend sẽ chạy tại địa chỉ: `http://localhost:8080`

## 🎨 Hướng dẫn chạy Frontend (ReactJS - Vite)

Frontend được xây dựng bằng ReactJS (JSX) và sử dụng Vite để tối ưu tốc độ build.

1.  Mở một terminal mới (giữ nguyên terminal của backend đang chạy) và di chuyển vào thư mục frontend:
    ```bash
    cd flower-shop-client
    ```
2.  Cài đặt các thư viện cần thiết (chỉ cần chạy lần đầu tiên):
    ```bash
    npm install
    ```
3.  Khởi chạy server phát triển:
    ```bash
    npm run dev
    ```
4.  Mở trình duyệt và truy cập vào: `http://localhost:5173`
