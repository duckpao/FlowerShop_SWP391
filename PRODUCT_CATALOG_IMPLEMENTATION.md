# Module Product & Catalog — tài liệu triển khai chi tiết

Tài liệu này mô tả **những gì đã được xây dựng và vì sao**: danh sách file, luồng nghiệp vụ,
quy tắc phân quyền và các quyết định thiết kế.

Cần hướng dẫn *sử dụng và test thủ công* thì xem [PRODUCT_CATALOG_GUIDE.md](PRODUCT_CATALOG_GUIDE.md).
Bản thiết kế gốc và kế hoạch triển khai nằm ở `docs/superpowers/specs/` và `docs/superpowers/plans/`.

---

## 1. Phạm vi

Sáu chức năng theo bảng phân công:

| Chức năng | Mô tả yêu cầu | Trạng thái |
|---|---|---|
| Product List | Hiển thị danh sách sản phẩm | Xong — toàn sàn tại `/products` |
| Product Details | Chi tiết sản phẩm, review | Xong — `/products/{id}` |
| Product Management | Shop/Admin CRUD sản phẩm | Xong — Manager CRUD + ảnh; Admin danh mục + kiểm duyệt |
| Favorite Product | Lưu sản phẩm yêu thích | Xong |
| View Favorite Product | Xem danh sách yêu thích | Xong — `/favorites` |
| Product Category | Hiển thị danh sách theo danh mục | Xong — bộ lọc + Admin CRUD danh mục |

**Ngoài phạm vi**: giỏ hàng, đặt hàng, thanh toán, giao hàng, chat, blog, upload file.
Ảnh là URL HTTPS vì project chưa có hạ tầng upload (giống cách `logoUrl` của shop đang làm).

### Điều kiện chặn đã gỡ trước khi làm

Entity `Product` khai báo hai cột `images` và `videos` **không tồn tại** trong bảng `Products`.
Hậu quả: mọi truy vấn sản phẩm trên MySQL trả **500 — `Unknown column 'p1_0.images'`**.
Toàn bộ test cũ không phát hiện vì chạy H2 với `ddl-auto=create-drop` (Hibernate tự sinh bảng từ entity).
Đây là việc đầu tiên phải sửa; không có nó thì không chức năng nào chạy được.

---

## 2. Kiến trúc

### Nguyên tắc tách service

`ManagerProductService` trước đây lẫn cả phần quản trị lẫn phần công khai (method `published()`).
Nếu dồn thêm 4 nhóm chức năng mới với 4 mức quyền khác nhau vào đó, file sẽ rất khó đọc và khó
kiểm tra quyền. Vì vậy tách theo **ranh giới nghiệp vụ**, mỗi service một mức quyền:

| Service | Trách nhiệm | Ai gọi |
|---|---|---|
| `CatalogService` | Duyệt sản phẩm công khai: list toàn sàn, chi tiết, danh mục, list theo shop | Công khai |
| `ProductReviewService` | Viết/sửa/xoá đánh giá, shop trả lời | Customer, Manager |
| `FavoriteService` | Lưu / bỏ lưu / xem yêu thích | Customer |
| `AdminCatalogService` | CRUD danh mục, ẩn sản phẩm vi phạm, xem sản phẩm toàn sàn | Admin |
| `ManagerProductService` | CRUD sản phẩm của shop mình (đã có, được mở rộng thêm ảnh) | Manager |
| `ProductCardAssembler` | Hạ tầng dùng chung: quy tắc hiển thị + gom ảnh/điểm theo lô | Bốn service trên |

`published()` đã **chuyển** từ `ManagerProductService` sang `CatalogService` cho đúng ranh giới.
Ba chỗ gọi trong `ManagerApplicationProductTests` đã được cập nhật theo.

### Sơ đồ phụ thuộc

```text
PublicCatalogController ─┐
PublicProductController ─┼─> CatalogService ──────┐
                         │                        │
FavoriteController ──────┼─> FavoriteService ─────┼─> ProductCardAssembler ─> ProductImageRepository
                         │                        │                          └> ProductReviewRepository
AdminCatalogController ──┼─> AdminCatalogService ─┘
                         │
ProductReviewController ─┴─> ProductReviewService ─> ProductReviewRepository
```

`ProductCardAssembler` là điểm duy nhất định nghĩa quy tắc hiển thị công khai, nên bốn service
không thể lệch nhau.

---

## 3. Thay đổi tầng dữ liệu

### 3.1 Entity

| File | Thay đổi | Lý do |
|---|---|---|
| `entity/Product.java` | **Xoá** field `images`, `videos` | Hai cột này không tồn tại trong DB — nguyên nhân lỗi 500 |
| `entity/Product.java` | **Thêm** `boolean adminHidden` (cột `admin_hidden`) | Cờ kiểm duyệt riêng của Admin |
| `entity/ProductImage.java` | **Mới** — map bảng `Product_Images` có sẵn | Bảng đã có trong schema nhưng thiếu entity |
| `entity/ProductReview.java` | `order` thành `optional = true`, bỏ `nullable = false` | Cho phép đánh giá không gắn đơn hàng |
| `entity/ProductReview.java` | **Thêm** `uniqueConstraints` trên `(user_id, product_id)` | Ràng buộc phải có cả ở entity, không chỉ ở file SQL |

### 3.2 Vì sao phải có cột `admin_hidden` riêng

Nếu Admin ẩn sản phẩm bằng cách đặt `status = INACTIVE`, Manager chỉ cần vào sửa lại thành
`ACTIVE` là sản phẩm hiện lại — việc kiểm duyệt trở nên vô nghĩa.

Cờ `admin_hidden` tách riêng và **không nằm trong `ManagerProductService.Input`**, nên Manager
không có đường nào ghi vào nó. Khi Manager lưu sản phẩm, entity được nạp từ DB nên giá trị cũ
của cờ được giữ nguyên tự động. Có test riêng chứng minh điều này
(`AdminCatalogTests.adminHiddenRemovesProductFromPublicAndManagerCannotUndoIt`).

### 3.3 Migration

**`flowershop/sql/011_product_catalog.sql`** — 3 lệnh, thứ tự có chủ đích:

```sql
ALTER TABLE Products ADD COLUMN admin_hidden BOOLEAN NOT NULL DEFAULT FALSE;  -- 1. bắt buộc
ALTER TABLE Product_Reviews MODIFY order_id NVARCHAR(36) NULL;                -- 2.
ALTER TABLE Product_Reviews ADD CONSTRAINT uq_review_user_product UNIQUE (user_id, product_id); -- 3. có thể lỗi
```

`mysql < file` **dừng ở lệnh lỗi đầu tiên**. Lệnh 3 có thể thất bại nếu database đã có cặp
`(user_id, product_id)` trùng. Nếu để lệnh 3 lên trước, lệnh 1 sẽ không chạy, thiếu cột
`admin_hidden`, và **mọi truy vấn sản phẩm lại trả 500** — đúng lỗi mà module này sinh ra để sửa.
Vì vậy lệnh bắt buộc luôn đứng trước lệnh có rủi ro.

**`flowershop/sql/012_fix_utf8_mojibake.sql`** — sửa dữ liệu tiếng Việt bị lỗi font, xem mục 8.

---

## 4. Danh sách file đầy đủ

### 4.1 Backend — file mới (820 dòng)

| File | Dòng | Trách nhiệm |
|---|---|---|
| `entity/ProductImage.java` | 51 | Map `Product_Images`: url, isPrimary, displayOrder |
| `repository/ProductImageRepository.java` | 13 | Tìm ảnh theo 1 sản phẩm, theo lô nhiều sản phẩm, xoá theo sản phẩm |
| `repository/ProductReviewRepository.java` | 21 | Tìm review theo sản phẩm/người dùng + truy vấn gộp điểm |
| `repository/FavoriteProductRepository.java` | 14 | Danh sách yêu thích theo người dùng (khoá kép) |
| `service/ProductCardAssembler.java` | 83 | Quy tắc hiển thị + gom ảnh chính và điểm theo lô |
| `service/CatalogService.java` | 121 | List toàn sàn, chi tiết, danh mục công khai, list theo shop |
| `service/FavoriteService.java` | 68 | Lưu / bỏ lưu / xem yêu thích |
| `service/ProductReviewService.java` | 128 | CRUD đánh giá + shop trả lời |
| `service/AdminCatalogService.java` | 121 | CRUD danh mục + kiểm duyệt sản phẩm |
| `controller/PublicCatalogController.java` | 39 | 4 endpoint GET công khai |
| `controller/FavoriteController.java` | 40 | 3 endpoint Customer |
| `controller/ProductReviewController.java` | 61 | 4 endpoint Customer + 1 Manager |
| `controller/AdminCatalogController.java` | 60 | 5 endpoint Admin |

### 4.2 Backend — file sửa

| File | Thay đổi |
|---|---|
| `entity/Product.java` | Bỏ `images`/`videos`, thêm `adminHidden` |
| `entity/ProductReview.java` | `order` nullable, thêm unique constraint |
| `repository/ProductRepository.java` | Thêm `JpaSpecificationExecutor` + truy vấn `countVisibleByCategory()` |
| `service/ManagerProductService.java` | Nhận danh sách ảnh; `Result` thêm `adminHidden` + `images`; bỏ `published()` |
| `controller/PublicProductController.java` | Gọi sang `CatalogService` |
| `controller/FrontendController.java` | Thêm 4 route React |
| `config/SecurityConfig.java` | GET `/api/public/**` permitAll + 4 route React |

### 4.3 Frontend — file mới (763 dòng)

| File | Dòng | Trách nhiệm |
|---|---|---|
| `models/productModel.js` | 14 | Nhãn trạng thái, tuỳ chọn sắp xếp, định dạng giá, nhãn điểm sao |
| `services/catalogService.js` | 16 | Gọi 3 endpoint công khai (không kèm token) |
| `services/favoriteService.js` | 23 | Lưu/bỏ lưu/danh sách + dựng tập id đã lưu |
| `services/reviewService.js` | 24 | Đánh giá công khai, của mình, và trả lời của shop |
| `services/adminCategoryService.js` | 11 | Danh mục và kiểm duyệt phía Admin |
| `controllers/useCatalogController.js` | 43 | State bộ lọc, phân trang, nạp danh mục |
| `controllers/useProductDetailController.js` | 25 | Nạp chi tiết, 404 thành thông báo thân thiện |
| `controllers/useFavoritesController.js` | 68 | Danh sách yêu thích + hook `useFavoriteToggle` dùng chung |
| `controllers/useProductReviewsController.js` | 67 | Danh sách, form viết/sửa/xoá, ô trả lời của shop |
| `controllers/useAdminCategoriesController.js` | 67 | CRUD danh mục + bật/tắt ẩn sản phẩm |
| `views/ProductCatalogView.jsx` | 65 | Lưới sản phẩm, bộ lọc, nút tim |
| `views/ProductDetailView.jsx` | 48 | Gallery, thông tin, nút tim |
| `views/FavoritesView.jsx` | 60 | Danh sách đã lưu, nhãn "không còn bán" |
| `views/ProductReviewsView.jsx` | 60 | Danh sách đánh giá + form + ô trả lời |
| `views/AdminCategoriesView.jsx` | 70 | Bảng danh mục + bảng kiểm duyệt |
| `styles/catalog.css` | 102 | Lưới sản phẩm, thumbnail, gallery |

### 4.4 Frontend — file sửa

| File | Thay đổi |
|---|---|
| `App.jsx` | Route `/products`, `/products/{id}`, `/favorites`, `/admin/categories` |
| `views/HomeView.jsx` | Lối vào trang sản phẩm và trang yêu thích |
| `views/ProductsView.jsx` | Quản lý nhiều ảnh; hiển thị đúng 2 dạng dữ liệu (công khai / quản trị) |
| `views/AdminDashboardView.jsx` | Thêm mục "Danh mục & kiểm duyệt" |
| `controllers/useProductsController.js` | State ảnh: thêm/xoá/chọn ảnh chính |

---

## 5. Nghiệp vụ chi tiết theo từng chức năng

### 5.1 Quy tắc hiển thị công khai — nền tảng của mọi thứ

Một sản phẩm chỉ ra ngoài công khai khi **đủ cả ba** điều kiện:

```java
// ProductCardAssembler.visible()
p.getStatus() == ProductStatus.ACTIVE
  && !p.isAdminHidden()
  && p.getShop().getStatus() == ShopStatus.ACTIVE
```

Ba điều kiện tương ứng ba chủ thể khác nhau: **Manager** tự ẩn (`status`), **Admin** kiểm duyệt
(`adminHidden`), **trạng thái shop** (bị khoá hoặc chưa duyệt). Chúng độc lập với nhau.

Áp dụng tại: `CatalogService.browse` (Specification), `CatalogService.detail`,
`CatalogService.published`, `ProductRepository.countVisibleByCategory`, `FavoriteService.add`,
`ProductReviewService.visibleProduct` (dùng cho cả `write` lẫn `list`).

Sản phẩm không đủ điều kiện và sản phẩm không tồn tại **đều trả 404**, không phân biệt — tránh
biến API thành kênh dò xem id nào có thật.

### 5.2 Product List và Product Category

`CatalogService.browse(q, categoryId, shopId, sort, page, size)`:

- **Lọc**: `q` tìm trong tên (không phân biệt hoa thường), `categoryId`, `shopId` đều tuỳ chọn.
- **Chống ký tự đại diện**: `q` được escape theo thứ tự `!` → `%` → `_`, dùng escape char `'!'`
  truyền vào `cb.like(..., '!')`. Nếu không, người dùng gõ `%` sẽ kéo về toàn bộ sản phẩm.
  Thứ tự escape quan trọng: phải thay `!` trước, nếu không sẽ escape nhầm chính ký tự escape.
- **Sắp xếp**: `newest` (mặc định, `createdDate desc, id asc`), `price_asc`, `price_desc`.
  Giá trị khác ném `IllegalArgumentException` → 400. Luôn có tiêu chí phụ `id` để phân trang ổn định.
- **Giới hạn**: `page` 0–100000, `size` 1–100 (mặc định 12), `q` tối đa 100 ký tự. Vượt → 400.

`CatalogService.categories()` trả danh mục ACTIVE kèm `productCount`, đếm bằng **một truy vấn
`GROUP BY`** chứ không đếm từng danh mục.

### 5.3 Product Details

`CatalogService.detail(productId)` trả: thông tin sản phẩm, danh sách URL ảnh, điểm trung bình,
số lượt đánh giá, tên và id shop.

**Ảnh chính luôn đứng đầu** danh sách gallery:

```java
.sorted(Comparator.comparingInt(i -> Boolean.TRUE.equals(i.getIsPrimary()) ? 0 : 1))
```

### 5.4 Điểm đánh giá — chống N+1

Điểm trung bình và số lượt tính bằng **một truy vấn gộp cho cả trang**, không phải mỗi sản phẩm
một truy vấn:

```java
@Query("select r.product.id, avg(r.rating), count(r) from ProductReview r "
     + "where r.product.id in :ids group by r.product.id")
List<Object[]> summarize(@Param("ids") Collection<String> ids);
```

Ảnh chính cũng gom theo lô bằng `findByProductIdInOrderByDisplayOrderAscIdAsc`.

Điểm được làm tròn 1 chữ số thập phân: `Math.round(avg * 10) / 10.0`.
Sản phẩm chưa có đánh giá không xuất hiện trong map nên trả `rating = 0`, `reviewCount = 0` —
**không có phép chia cho 0**.

> **Ràng buộc quan trọng**: `ProductCardAssembler.cards()` yêu cầu entity đang được **quản lý trong
> transaction**, vì `Product.shop` và `Product.category` là LAZY. Mọi caller đều nằm trong
> `@Transactional`. Ràng buộc này được ghi trong javadoc của method vì nó không thể hiện qua chữ ký hàm.

### 5.5 Product Management — phía Manager

**Ảnh sản phẩm**, `ManagerProductService.save()`:

- `ImageInput(url, primary)`, url bắt buộc HTTPS (`@Pattern(regexp="https://[^\\s]+")`), tối đa 10 ảnh.
- **`images = null` giữ nguyên ảnh cũ; `images` là mảng thì thay toàn bộ.** Thiết kế như vậy để
  các client cũ không gửi trường này sẽ không vô tình xoá sạch ảnh.
- **Luôn đúng một ảnh chính**: lấy phần tử đầu tiên được đánh dấu `primary`, nếu không có thì
  lấy phần tử đầu tiên. Không bao giờ có 0 hoặc 2 ảnh chính.
- `displayOrder` gán theo thứ tự gửi lên.
- `Result.images` trả về **xếp ảnh chính lên đầu**. Điều này bắt buộc vì giao diện dựng lại cờ
  `primary` theo vị trí (`index === 0`); nếu không, Manager sửa giá sản phẩm sẽ vô tình
  làm đổi ảnh chính.

**Cờ kiểm duyệt**: `adminHidden` không có trong `Input` nên Manager không ghi được, và
`Result` có trả về để giao diện hiện nhãn "Bị quản trị viên ẩn khỏi trang công khai".

### 5.6 Product Management — phía Admin

`AdminCatalogService`:

- `categories()` — tất cả danh mục (kể cả INACTIVE) kèm số sản phẩm đang bán.
- `createCategory` / `updateCategory` — **không có lệnh xoá**. Danh mục chỉ chuyển `INACTIVE`
  để không làm hỏng tham chiếu của sản phẩm đang dùng nó. Sản phẩm thuộc danh mục INACTIVE
  vẫn hiển thị bình thường; Manager chỉ không chọn được danh mục đó cho sản phẩm mới
  (code cũ đã lọc `CategoryStatus.ACTIVE`).
- `products(q, shopId, status, page)` — Admin xem **toàn sàn**, không áp quy tắc hiển thị, vì
  Admin cần thấy đúng những sản phẩm đang bị ẩn để xử lý. `status` sai giá trị → 400.
- `setHidden(productId, hidden, actor)` — ghi cờ và `lastModifyBy`.

**`ProductCard` có trường `adminHidden` riêng ngoài `available`.** Lý do: `available = false` có
thể do ba nguyên nhân khác nhau. Nếu trang kiểm duyệt chỉ dựa vào `available`, nút sẽ hiện
"Bỏ ẩn" cho cả sản phẩm mà Manager tự ẩn — bấm vào không có tác dụng gì và gây hiểu nhầm.

### 5.7 Favorite Product / View Favorite Product

- Chỉ role **CUSTOMER**, thống nhất với cách địa chỉ giao hàng đang giới hạn.
- **`PUT` và `DELETE` đều idempotent**: lưu lại thứ đã lưu, hoặc bỏ thứ chưa lưu, đều thành công.
  `PUT` kiểm tra `existsById` trước; `DELETE` dùng `deleteById` (Spring Data không ném lỗi khi
  bản ghi không tồn tại). Khoá chính kép `(user_id, product_id)` chặn trùng ở tầng DB.
- Chỉ lưu được sản phẩm **đang hiển thị công khai**.
- **Sản phẩm đã lưu nhưng nay bị ẩn vẫn nằm trong danh sách**, kèm cờ `available = false` và
  nhãn "Không còn bán". Quyết định này là có chủ đích: để sản phẩm biến mất im lặng sẽ khiến
  khách tưởng mình bị mất dữ liệu.
- Trang 12 mục, sắp xếp `createdDate desc, id.productId asc`.

### 5.8 Review

**Quyền và ràng buộc**:

| Thao tác | Ai | Ràng buộc |
|---|---|---|
| Xem | Bất kỳ ai | Sản phẩm phải đang hiển thị công khai, nếu không → 404 |
| Viết | CUSTOMER | Mỗi tài khoản **một** đánh giá cho một sản phẩm → trùng trả 409 |
| Sửa / Xoá | CUSTOMER | Chỉ đánh giá của chính mình; của người khác → 404 |
| Trả lời | Manager sở hữu shop | Shop phải ACTIVE; review phải thuộc shop đó |

- `rating` bắt buộc, số nguyên **1–5** (`@Min(1) @Max(5)`). `comment` tuỳ chọn, tối đa 2000 ký tự.
- `order_id` để `NULL` với đánh giá tạo mới.
- **Chặn hai tầng cho ràng buộc một-đánh-giá**: kiểm tra trong Java (`findByProductIdAndUserId`)
  và ràng buộc `UNIQUE` ở DB làm chốt chặn cuối khi có hai request đồng thời.
  `DataIntegrityViolationException` đã được `GlobalExceptionHandler` map sang 409.
- `GET /api/customer/products/{id}/reviews` trả đánh giá **của chính mình** (404 nếu chưa có).
  Endpoint này cần thiết vì API công khai cố tình không trả `userId`, nên giao diện không có
  cách nào khác để biết đánh giá nào là của người đang đăng nhập mà mở sẵn chế độ sửa.

**Quyền riêng tư**: `ReviewResult` chỉ chứa `reviewerName`, **không bao giờ chứa email**.
Tài khoản chưa đặt họ tên hiển thị là `"Khách hàng"` (cột `full_name` cho phép NULL).

```java
String name = r.getUser().getFullName();
return new ReviewResult(..., name == null || name.isBlank() ? "Khách hàng" : name, ...);
```

---

## 6. Phân quyền và bảo mật

### 6.1 Hai lớp phòng vệ

Mọi controller mới đều có **cả hai**:

1. Quy tắc theo đường dẫn trong `SecurityConfig`: `/api/admin/**` → ADMIN,
   `/api/shop/**` → SHOP, `/api/customer/**` → CUSTOMER, `/api/staff/**` → SHOP_STAFF.
2. `@PreAuthorize` trên class hoặc method.

### 6.2 Danh tính luôn lấy từ JWT

Không endpoint nào nhận `userId` hay `role` từ body hoặc query. Tất cả dùng
`@AuthenticationPrincipal CurrentUser`. Ví dụ `reviewService.reply` có nhận `shopId` từ client,
nhưng backend vẫn kiểm tra lại quyền sở hữu shop từ JWT trước khi cho ghi.

### 6.3 Cách ly giữa các shop

- `ManagerProductService.owned()` khoá bản ghi shop và kiểm tra `owner.id == actor`, sai → 404.
- Sửa sản phẩm dùng `findByIdAndShopId` nên không thể chuyển sản phẩm sang shop khác qua ID.
- `ProductReviewService.reply` kiểm tra **hai điều**: shop thuộc về người gọi, **và** review đó
  thuộc đúng shop này.

### 6.4 CSRF

Mọi thao tác ghi cần header `X-CSRF-TOKEN` lấy từ `GET /api/auth/csrf`; frontend tự làm
trong `authService.authenticatedRequest`.

### 6.5 Mở đường dẫn công khai

`SecurityConfig` thay 3 matcher riêng lẻ bằng một dòng: `GET /api/public/**` → `permitAll`.
Đánh đổi: bất kỳ handler nào đặt dưới `/api/public` sau này sẽ công khai theo mặc định.
Chấp nhận được vì tên namespace đã tuyên bố ý định, nhưng người thêm endpoint mới cần biết điều này.

---

## 7. Bảng API đầy đủ

### Công khai — không cần đăng nhập

| Method | URL | Trả về |
|---|---|---|
| GET | `/api/public/products?q=&categoryId=&shopId=&sort=&page=&size=` | `Results` phân trang |
| GET | `/api/public/products/{id}` | `ProductDetail` |
| GET | `/api/public/products/{id}/reviews?page=` | `Results` đánh giá |
| GET | `/api/public/categories` | `List<CategoryCard>` |
| GET | `/api/public/shops/{shopId}/products?page=` | `Results` phân trang |

### Customer — JWT + CSRF

| Method | URL | Ghi chú |
|---|---|---|
| GET | `/api/customer/favorites?page=` | 12 mục/trang |
| PUT / DELETE | `/api/customer/favorites/{productId}` | 204, idempotent |
| GET | `/api/customer/products/{id}/reviews` | Đánh giá của mình, 404 nếu chưa có |
| POST | `/api/customer/products/{id}/reviews` | 201; trùng → 409 |
| PUT / DELETE | `/api/customer/products/{id}/reviews` | Chỉ của mình |

### Manager / Admin — JWT + CSRF

| Method | URL | Ghi chú |
|---|---|---|
| PUT | `/api/shop/mine/{shopId}/reviews/{reviewId}/reply` | Chuỗi rỗng để xoá phản hồi |
| GET / POST / PUT | `/api/admin/categories`, `/api/admin/categories/{id}` | Không có DELETE |
| GET | `/api/admin/products?q=&shopId=&status=&page=` | Toàn sàn, 20 mục/trang |
| PUT | `/api/admin/products/{id}/hidden` | Body `{"hidden": true\|false}` |

### Cấu trúc dữ liệu chính

```java
ProductCard(id, name, price, stock, status, shopId, shopName, categoryId, categoryName,
            imageUrl, rating, reviewCount, available, adminHidden)
ProductDetail(id, name, description, price, stock, shopId, shopName, categoryId, categoryName,
              images, rating, reviewCount)
CategoryCard(id, name, description, productCount)
ReviewResult(id, productId, reviewerName, rating, comment, shopReply, createdAt)
```

---

## 8. Sửa lỗi font tiếng Việt

Phát hiện trong quá trình làm: toàn bộ dữ liệu mẫu bị lỗi font
("Hoa Khai Trương" thành "Hoa Khai TrÆ°Æ¡ng").

**Chẩn đoán**: chữ "ư" đúng là byte `C6B0`, trong DB là `C386C2B0` — mỗi byte UTF-8 bị hiểu
thành một ký tự Latin-1 rồi mã hoá lại. Tái hiện được bằng cách nạp cùng chuỗi qua client
khai báo `latin1`, cho ra byte trùng khít.

**Nguyên nhân gốc**: `database.sql` lưu UTF-8 nhưng thiếu `SET NAMES utf8mb4`, nên client mysql
trong container Docker đọc file như latin1.

**Đã sửa hai tầng**:
1. Thêm `SET NAMES utf8mb4` vào `database.sql` và `db.sql` → database tạo mới không còn lỗi.
2. `012_fix_utf8_mojibake.sql` chữa database đang chạy tại chỗ, 30 cột gồm cả cột JSON.

**Ba điều kiện bảo vệ** trong script, để không phá dữ liệu vốn đã đúng:

```sql
AND <chuyển_đổi> IS NOT NULL                 -- kết quả phải là UTF-8 hợp lệ
AND <chuyển_đổi> <> <giá_trị_hiện_tại>       -- bỏ qua chuỗi ASCII thuần
AND <số_dấu_hỏi_sau> = <số_dấu_hỏi_trước>    -- không sinh thêm '?' do mất dữ liệu
```

Điều kiện thứ ba phải **đếm** chứ không thể đòi bằng 0: câu chat
*"Shop ơi bó hồng đỏ còn hàng không ạ?"* có dấu hỏi thật, bản đầu tiên của script đã bỏ sót nó.

**Tác động phụ tích cực**: địa chỉ mẫu trước đây lưu là `"HÃ  Ná»™i"`, không khớp danh sách khu vực
giao hàng nên chức năng địa chỉ của Customer bị lỗi. Nay đã hoạt động trở lại.

---

## 9. Kiểm thử

### 9.1 Test tự động — 164 test, 0 lỗi

| Lớp test | Số test | Nội dung |
|---|---|---|
| `CatalogTests` | 17 | Lọc, sắp xếp, phân trang, quy tắc hiển thị, escape ký tự đại diện, sản phẩm không ảnh/không review |
| `ProductReviewTests` | 9 | Một review/người/sản phẩm, sửa/xoá của mình, không lộ email, review sản phẩm bị ẩn, shop reply |
| `AdminCatalogTests` | 5 | CRUD danh mục, cờ kiểm duyệt, Manager không gỡ được cờ, lọc trạng thái |
| `FavoriteTests` | 3 | Idempotent, chỉ Customer, cờ `available` |
| `ManagerApplicationProductTests` | 10 | Mở rộng thêm: ảnh, thứ tự ảnh chính, cách ly giữa shop |
| `FrontendRoutingTests` | 1 | 4 route React mới forward về index.html |

Chạy nhóm test của module:

```powershell
.\gradlew.bat test --tests com.example.flowershop.CatalogTests --tests com.example.flowershop.FavoriteTests --tests com.example.flowershop.ProductReviewTests --tests com.example.flowershop.AdminCatalogTests
```

### 9.2 Vì sao test xanh vẫn chưa đủ

Test chạy H2 với `ddl-auto=create-drop`: **Hibernate tự sinh bảng từ entity**, nên schema test
luôn khớp entity và **không bao giờ phát hiện lệch schema với MySQL**. Đây chính xác là lý do
lỗi cột `images` lọt qua toàn bộ test suite trước đây.

Quy tắc rút ra: sau khi đổi entity, **bắt buộc chạy backend thật trên MySQL** và gọi thử endpoint
liên quan. Mỗi giai đoạn của module này đều đã làm bước đó.

### 9.3 Review độc lập

Toàn bộ thay đổi đã được một lượt review với ngữ cảnh mới soi lại. Kết quả: 1 lỗi Critical
và 4 lỗi Important, đều đã sửa kèm test hồi quy:

| # | Lỗi | Hậu quả nếu không sửa |
|---|---|---|
| 1 | `GET /api/public/products/{id}/reviews` không áp quy tắc hiển thị | Sản phẩm bị Admin ẩn vẫn lộ đánh giá kèm **tên người viết** cho khách vãng lai; id bịa đặt trả 200 thành kênh dò dữ liệu |
| 2 | Migration chạy lệnh rủi ro trước lệnh bắt buộc | Migration dừng giữa chừng → thiếu cột → mọi truy vấn sản phẩm 500 |
| 3 | Ràng buộc UNIQUE chỉ có trong file SQL, không có trong entity | Chốt chặn cuối là ảo ở mọi môi trường dựng schema từ entity |
| 4 | Sửa bất kỳ trường nào của sản phẩm làm đổi ảnh chính | Ảnh đại diện âm thầm nhảy sang ảnh khác |
| 5 | Thiếu bộ lọc `status` trong `/api/admin/products` | Admin không lọc được sản phẩm cần xử lý |

---

## 10. Hạn chế đã biết

| Vấn đề | Đánh giá |
|---|---|
| N+1 khi nạp `shop`/`category`/ảnh của Manager/`user` của review | Không sai kết quả; ở quy mô dữ liệu hiện tại không cảm nhận được. Cần `join fetch` nếu dữ liệu lớn |
| `PUT /api/customer/favorites/{id}` có thể trả 409 nếu hai request **thật sự** đồng thời | Hiếm; spec yêu cầu idempotent nên đây là sai lệch nhỏ |
| `ProductCard` công khai mang cờ `adminHidden`/`status` | Hiện không lộ gì vì danh sách công khai đã lọc; là hazard nếu sau này nới bộ lọc |
| Mọi tài khoản SHOP đều thấy ô trả lời trên review của shop khác | Backend vẫn chặn đúng bằng 404; chỉ là giao diện gây hiểu nhầm |
| `favoriteService.ids()` có thể gọi tới 10 request mỗi lần mở trang | Do không có endpoint trả riêng danh sách id; khách có hơn 120 mục thì các mục cũ không hiện trạng thái đã lưu |
| Chưa có index cho truy vấn catalog chính | Quét toàn bảng khi dữ liệu lớn |
| Migration `011` giả định lineage `database.sql` (NVARCHAR) | Database dựng từ `db.sql` (VARCHAR utf8mb4) cần đổi kiểu trong lệnh; đã ghi chú trong file |
| Chưa có upload file ảnh | Ảnh là URL HTTPS, giống cách `logoUrl` của shop đang làm |
