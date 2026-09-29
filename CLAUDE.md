# Guidelines for Claude
- Be a practical developer.
- Use standard library (stdlib) first when possible.
- Make the smallest correct change.
- Avoid unrequested abstractions.
# Hướng dẫn hành vi cho Claude
- Khi nhận được một yêu cầu ngắn gọn hoặc không cụ thể, tuyệt đối không tự đoán mò.
- Luôn chủ động đưa ra các phương án gợi ý (options) hoặc đặt câu hỏi làm rõ trước khi bắt đầu thực hiện

# Tổng quan dự án FlowerShop
Monorepo 2 phần: `flowershop/` (backend Spring Boot) + `flower-shop-client/` (frontend React). Backend build ra bundle luôn nhúng frontend (qua Gradle `buildFrontend`→`processResources`) để chạy chung port 8080 khi build production; khi dev thì chạy 2 server riêng (xem "Dev workflow" bên dưới).

## Tech stack
- Backend: Java 17, Spring Boot 4.1.1, Spring Security (JWT tự viết, không dùng OAuth2 resource-server auto-config), Spring Data JPA, MySQL (`flower_shop_db`, port 3307), Log4j2 (đã thay Logback), springdoc-openapi (Swagger UI), Lombok.
- Frontend: React 19 + Vite 8, không dùng router library (tự parse `window.location.pathname` trong `App.jsx`), không dùng UI framework (CSS thuần trong `src/styles/`), không có state management library (React state + custom hooks).
- Media upload: Cloudinary, gọi trực tiếp qua `java.net.http.HttpClient` (ký SHA-1 tay), không dùng Cloudinary SDK — theo nguyên tắc stdlib-first.

## Dev workflow
- Terminal 1: `cd flowershop && ./gradlew bootRun` (backend, port 8080).
- Terminal 2: `cd flower-shop-client && npm run dev` (Vite, port 5173, tự proxy `/api/*` sang 8080 — cấu hình ở `vite.config.js`).
- `npm run build` ở frontend hoặc build Gradle bình thường vẫn nhúng React vào Spring Boot như cũ (dùng cho production/single-port).

## Auth & bảo mật (backend)
- JWT tự viết (không dùng Spring OAuth2 resource-server): access token 10 phút gửi qua header `Authorization: Bearer`, refresh token 7 ngày lưu cookie HttpOnly `refresh_token` (`path=/api/auth`), **rotate mỗi lần refresh** + phát hiện reuse (replay) → revoke toàn bộ session. Theo dõi session trong bảng `Auth_Sessions`.
- Role (`UserRole`): `ADMIN`, `SHOP`, `CUSTOMER`, `SHOP_STAFF`, `DELIVERY` (legacy, chưa dùng — bị loại khỏi login).
- Đổi role (CUSTOMER→SHOP qua `ManagerApplication` admin duyệt, CUSTOMER→SHOP_STAFF qua `StaffApplication`/`StaffInvitation`) đều **revoke toàn bộ session**, buộc đăng nhập lại — JWT không tự cập nhật role.
- CSRF vẫn bật (dù stateless): mọi request đổi state phải `GET /api/auth/csrf` lấy token trước, gửi lại qua header `X-CSRF-TOKEN`.
- Rate limit login theo IP (`LoginRateLimiter`, in-memory, 20 lần/phút/IP — chỉ 1 instance, không share khi scale).
- Dev bypass filter (`DevAdminFilter`, `DevManagerFilter`): chỉ hoạt động khi bật Spring profile `dev-admin`/`dev-manager` **và** request từ loopback **và** không có `Authorization` header — dùng cho `/dev-admin`, `/dev-manager` ở frontend để test không cần đăng nhập thật. Không được expose qua proxy/tunnel.
- Authorization theo URL pattern: `/api/admin/**`→ADMIN, `/api/shop/**`→SHOP, `/api/customer/**`→CUSTOMER, `/api/staff/**`→SHOP_STAFF, `/api/delivery/**`→denyAll (chưa implement), `/api/public/**` GET→permitAll.
- Mọi endpoint quản lý shop (`/api/shop/mine/{shopId}/...`) đều tự re-check ownership trong service (không chỉ dựa role) để tránh SHOP user sửa shop khác qua path param.

## API surface (theo controller)
| Controller | Base path | Ghi chú |
|---|---|---|
| `AuthController` | `/api/auth` | Đăng ký/verify email (OTP), quên/đặt lại mật khẩu — public |
| `SessionController` | `/api/auth` | `login`, `refresh`, `me`, `logout`, `csrf` |
| `AccountController` | `/api/account` | Profile + địa chỉ giao hàng của chính user (CUSTOMER/SHOP/SHOP_STAFF/ADMIN dùng chung `/profile`) |
| `AdminCustomerController` | `/api/admin/customers` | ADMIN: list/detail/block-unblock customer |
| `AdminShopController` | `/api/admin/shops` | ADMIN: list/detail/approve-block-unblock shop |
| `ManagerApplicationController` | `/api/customer/manager-applications`, `/api/account/manager-applications`, `/api/admin/shops/applications` | Customer xin mở shop → Admin duyệt |
| `ManagerShopController` | `/api/shop/mine` | SHOP: profile shop, địa chỉ shop, quản lý staff (activate/deactivate) |
| `ManagerProductController` | `/api/shop/mine/{shopId}/products` | SHOP: CRUD sản phẩm + ảnh/video (`/images`, `/videos`, upload Cloudinary, ≤5MB ảnh / ≤50MB video) |
| `StaffApplicationController` | `/api/customer/shops/{shopId}/applications`, `/api/account/staff-applications`, `/api/shop/mine/{shopId}/applications` | Customer xin làm staff → Shop duyệt |
| `StaffInvitationController` | `/api/shop/mine/{id}/invitations`, `/api/account/staff-invitations/accept` | Shop mời staff qua email bằng code |
| `StaffShopController` | `/api/staff/shops` | SHOP_STAFF: xem shop mình đang làm |
| `PublicProductController` | `/api/public/products`, `/api/public/categories` | Catalog công khai, không cần đăng nhập |
| `PublicShopController` | `/api/public/shops` | Tìm kiếm/xem shop công khai |
| `FrontendController` | — | Forward các route SPA về `index.html` |
| `TestController` | `/api/hello`, `/api/db-check` | Debug/smoke test |

## Domain entities chính (nhóm theo chức năng)
- **Auth/onboarding**: `User`, `AuthSession`, `PendingRegistration`, `OtpChallenge`, `ManagerApplication`, `StaffApplication`, `StaffInvitation`, `ShopStaff`.
- **Shop/Product**: `Shop`, `Category`, `Product`, `ProductImage`, `ProductVideo`, `Coupon`, `Address` (dùng chung cho user và shop, FK nullable 2 phía).
- **Order/Payment**: `Order`, `OrderDetail`, `Payment`, `Refund`, `Delivery`, `CartItem`, `CustomOrderRequest`, `CustomerOccasion`.
- **Review/Favorite**: `ProductReview`, `FavoriteProduct`, `FavoriteShop`.
- **Blog**: `Blog`, `BlogComment`, `BlogInteraction`.
- **Chat**: `ChatSession`, `ChatMessage`.
- Quy ước chung: không hard-delete — dùng status enum (`ProductStatus`, `ShopStatus`, `UserStatus`...) để ẩn/khoá. Audit columns (`createdDate/By`, `lastModifyDate/By`) set tay trong service, không dùng `@EntityListeners`.

## Database (MySQL) — bảng chính
`database.sql` (schema gốc) + 10 file migration cộng thêm trong `flowershop/sql/*.sql`.
- Core: `Users`, `Shops` (unique `owner_id` — 1 shop/owner), `Addresses`, `Categories`, `Products`, `Product_Images`, `Product_Videos`, `Coupons`, `Cart_Items`.
- Order: `Orders`, `Order_Details`, `Deliveries`, `Payments`, `Refunds`, `Custom_Order_Requests`.
- Khác: `Chat_Sessions`, `Chat_Messages`, `Product_Reviews`, `Favorite_Products`, `Favorite_Shops`, `Blogs`, `Blog_Images`, `Blog_Interactions`, `Blog_Comments`, `Customer_Occasions`.
- Migration nổi bật: `Otp_Challenges`, `Pending_Registrations`, `Auth_Sessions`, `Shop_Staff`, `Staff_Applications`, `Staff_Invitations`, `Manager_Applications` (đều unique theo user/shop để chặn trùng lặp).

## Frontend — cấu trúc (mô hình MVC tự quy ước)
- `models/` — state client thuần (không gọi API): `authModel` (access token **chỉ giữ trong memory**, không localStorage; refresh token nằm ở cookie HttpOnly do backend set), `accountModel`, `shopModel` (label/mapping tĩnh).
- `services/` — gọi API (`authService`, `accountService`, `adminCustomerService`, `adminShopService`, `managerApplicationService`, `managerShopService`, `productService`, `staffApplicationService`). Mọi request đổi state tự lấy CSRF token trước (`authService`).
- `controllers/` — custom hook `useXController.js` kết hợp service + state cho 1 view (13 hook, xem chi tiết trong code khi cần).
- `views/` — component JSX hiển thị (20 file).
- Routing thủ công trong `App.jsx` (không có router lib) — bảng route chính: `/` (Home, public), `/login` (Auth), `/account`, `/products/:id` (public detail), `/admin/**` (ADMIN), `/shop-admin/**` (SHOP/SHOP_STAFF), `/dev-admin` + `/dev-manager` (chỉ DEV, bypass login).
- Lưu ý: `ShopInvitationsView`/`useInvitationsController` và `AcceptInvitationView` đã code xong nhưng **chưa gắn route nào** trong `App.jsx` (tính năng mồ côi, cần nối route nếu muốn dùng).

## Config quan trọng (env vars)
- `JWT_SECRET` (Base64 ≥32 byte) + `JWT_ALLOW_EPHEMERAL_KEY=false` — bắt buộc set khi deploy thật, nếu không JWT dùng key ngẫu nhiên sinh lại mỗi lần restart (mất hết token cũ).
- `MAIL_USERNAME`, `MAIL_PASSWORD` — SMTP gửi OTP/email xác thực.
- `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` — upload ảnh/video sản phẩm. **Không đặt giá trị thật làm default trong `application.properties`** (đã từng bị lộ secret thật trong file này — luôn dùng dạng `${VAR:}` rỗng, set giá trị qua biến môi trường thật).
- `SESSION_COOKIE_SECURE` — bật `true` khi chạy HTTPS thật.
- `app.delivery.allowed-cities` — danh sách thành phố được giao hàng (hiện chỉ có Hà Nội).