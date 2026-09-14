# 🌻 Dự án Chuỗi Cửa Hàng Hoa (Flower Shop Marketplace)

Hệ thống quản lý chuỗi cửa hàng bán hoa đa chi nhánh (Marketplace), bao gồm chức năng mua sắm, đặt hoa theo yêu cầu (Custom Order), tích hợp thanh toán và quản lý giao hàng. Dự án được chia thành hai phần độc lập: Backend (Java Spring Boot) và Frontend (ReactJS - Vite).

## 🛠 Yêu cầu hệ thống (Prerequisites)

Để chạy được dự án này trên máy cá nhân, team cần cài đặt sẵn các công cụ sau:
* **Java 17**: Dành cho môi trường Backend.
* **Node.js**: Khuyến nghị bản LTS để chạy Frontend.
* **Docker Desktop**: Bắt buộc phải cài đặt và bật lên trước khi code để hệ thống tự động khởi tạo cơ sở dữ liệu MySQL thông qua file `docker-compose.yml`.

---

## 🐳 Hướng dẫn làm việc với Docker (Database)

Dự án sử dụng Docker để đồng bộ môi trường Database (MySQL 8.0) cho toàn bộ team. Mọi thao tác cấu hình và tạo bảng đều đã được tự động hóa.

**1. Khởi động Database**  
Mở terminal tại thư mục gốc của dự án (nơi chứa file `docker-compose.yml` và `database.sql`), chạy lệnh sau để dựng Database chạy ngầm:
```bash
docker-compose up -d
Lưu ý: Lần chạy đầu tiên sẽ mất khoảng 15-20 giây để Docker tự động tạo các bảng và chèn dữ liệu mẫu (Mock data).

2. Xử lý lỗi Database (Reset toàn bộ)

Nếu bạn gặp lỗi kết nối (Access denied), lỗi sai mật khẩu, hoặc muốn xóa sạch Database để chạy lại file script SQL mới nhất, hãy chạy lệnh sau:

Bash
docker-compose down -v
(Hậu tố -v cực kỳ quan trọng, nó sẽ xóa sạch ổ cứng ảo chứa dữ liệu và mật khẩu cũ bị lỗi. Sau khi chạy lệnh này, bạn tiến hành chạy lại lệnh docker-compose up -d để khởi tạo lại từ đầu).

🚀 Hướng dẫn chạy Backend (Spring Boot)
Backend cung cấp các RESTful API và sẽ tự động kết nối vào Database MySQL vừa được Docker dựng lên ở cổng 3307.

Mở terminal và di chuyển vào thư mục backend:

Bash
cd flowershop
Khởi chạy ứng dụng bằng Gradle wrapper:

Trên Windows: .\gradlew bootRun

Trên Mac/Linux: ./gradlew bootRun

Backend sẽ chạy tại địa chỉ: http://localhost:8080

🎨 Hướng dẫn chạy Frontend (ReactJS - Vite)
Frontend được xây dựng bằng ReactJS (JSX) và sử dụng Vite để tối ưu tốc độ build.

Mở một terminal mới (giữ nguyên terminal của backend và docker đang chạy) và di chuyển vào thư mục frontend:

Bash
cd flower-shop-client
Cài đặt các thư viện cần thiết (chỉ cần chạy lần đầu tiên sau khi clone code về):

Bash
npm install
Khởi chạy server phát triển:

Bash
npm run dev
Mở trình duyệt và truy cập vào: http://localhost:5173

🌿 Quy trình làm việc với Git (Git Workflow)
Bước 1: Cập nhật code mới nhất

Luôn đảm bảo bạn đang ở nhánh master và kéo code mới nhất về trước khi bắt đầu công việc:

Bash
git checkout master
git pull origin master
Bước 2: Tạo nhánh làm việc riêng

Tuyệt đối không code trực tiếp trên nhánh master. Tên nhánh cần có tiền tố rõ ràng phản ánh đúng công việc:

Thêm tính năng mới: git checkout -b feature/ten-tinh-nang

Sửa lỗi: git checkout -b bugfix/ten-loi

Bước 3: Code và Commit

Thực hiện công việc và commit thường xuyên. Ghi chú commit cần rõ nghĩa và mô tả đúng tác vụ:

Bash
git add .
git commit -m "feat: hoàn thiện API giỏ hàng và tích hợp HMAC-SHA256"
Bước 4: Đẩy code và Tạo Pull Request (PR)

Đẩy nhánh cá nhân lên kho chứa:

Bash
git push origin feature/ten-tinh-nang
Sau đó lên giao diện GitHub, tạo một Pull Request (PR) trỏ vào nhánh master và tag Bảo để kiểm tra trước khi merge.

⚠️ Quy tắc tránh Conflict & Lưu ý chung
Giao tiếp: Phân chia task độc lập, hạn chế 2 người cùng sửa chung một file (đặc biệt là các file cấu hình chung) trong cùng một khoảng thời gian.

Đồng bộ thường xuyên: Nếu một task kéo dài nhiều ngày, mỗi buổi sáng hãy kéo code từ master về nhánh hiện tại (git pull origin master) để đồng bộ và giải quyết conflict từ sớm.

Kỷ luật Gitignore: Tuyệt đối không commit các file rác sinh ra từ IDE (như .idea/, .vscode/) hoặc các thư viện quá nặng (như node_modules/, build/). Mọi người phải tuân thủ file .gitignore của project.
