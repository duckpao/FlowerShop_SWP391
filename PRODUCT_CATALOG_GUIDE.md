# Sản phẩm, đánh giá và yêu thích

File này hướng dẫn **sử dụng và test thủ công**. Cần biết đã viết những file nào, nghiệp vụ ra sao
và vì sao thiết kế như vậy thì xem [PRODUCT_CATALOG_IMPLEMENTATION.md](PRODUCT_CATALOG_IMPLEMENTATION.md).

Chạy các migration chưa áp dụng theo thứ tự trong `flowershop/sql/` trên database `flower_shop_db`,
bao gồm `013_product_type.sql`, rồi khởi động lại backend.
Không chạy lại `database.sql` trên DB đang có dữ liệu vì file đó xóa database.

Sáu chức năng trong tài liệu này: Product List, Product Details (kèm review), Product Management,
Favorite Product, View Favorite Product và Product Category.

## Luồng chính

1. Mở trang chủ hoặc http://localhost:8080/products — tab Sản phẩm hiển thị danh sách sản phẩm của
   **tất cả** shop ACTIVE; tab Cửa hàng tìm shop theo tên. Sản phẩm lọc theo tên, danh mục, shop,
   loại bó hoa (có sẵn / custom), khoảng giá và sắp xếp mới nhất / giá tăng / giá giảm; cả hai tab dùng
   được khi chưa đăng nhập. Shop chọn loại bó hoa khi đăng sản phẩm.
2. Bấm tên sản phẩm để mở `/products/{id}`: gallery ảnh, giá, tồn kho, danh mục, link tới shop,
   điểm trung bình và danh sách đánh giá.
3. Customer đăng nhập bấm **♡ Lưu yêu thích** ở trang danh sách hoặc trang chi tiết. Khách chưa đăng nhập
   bấm vào sẽ được chuyển tới `/login` rồi quay lại đúng trang.
4. Xem lại tại `/favorites`. Sản phẩm đã lưu nhưng nay bị ẩn hoặc shop bị khóa **vẫn nằm trong danh sách**
   kèm nhãn "Không còn bán", thay vì biến mất im lặng.
5. Customer viết đánh giá 1–5 sao ở trang chi tiết. Mỗi tài khoản một đánh giá cho một sản phẩm;
   mở lại trang sẽ thấy form ở chế độ sửa, kèm nút xóa.
6. Manager đăng nhập mở **Quản lý mặt hàng**: thêm nhiều URL ảnh HTTPS và chọn ảnh chính.
   Ở trang chi tiết sản phẩm, Manager thấy ô trả lời từng đánh giá.
7. Admin mở `/admin/categories`: thêm/sửa danh mục hệ thống và ẩn sản phẩm vi phạm khỏi trang công khai.

## Quy tắc hiển thị

Sản phẩm chỉ ra trang công khai khi đủ **cả ba**: `status = ACTIVE`, `admin_hidden = false`,
và shop ở trạng thái `ACTIVE`. Sản phẩm hoặc shop không đủ điều kiện đều trả 404, không phân biệt
"không tồn tại" với "bị ẩn".

Cột `admin_hidden` tách riêng khỏi `status` là có chủ đích: nếu Admin ẩn bằng cách đặt `status=INACTIVE`
thì Manager chỉ cần sửa lại ACTIVE là sản phẩm hiện lại. Cờ này không nằm trong DTO cập nhật của Manager
nên chỉ Admin ghi được.

## API

### Công khai, không cần đăng nhập

| Method | URL | Chức năng |
|---|---|---|
| GET | `/api/public/products?q=&categoryId=&shopId=&type=&minPrice=&maxPrice=&sort=&page=&size=` | Product List toàn sàn |
| GET | `/api/public/products/{id}` | Product Details, kèm ảnh và điểm trung bình |
| GET | `/api/public/products/{id}/reviews?page=` | Đánh giá của sản phẩm |
| GET | `/api/public/categories` | Product Category, kèm số sản phẩm đang bán |
| GET | `/api/public/shops/{shopId}/products?page=` | Sản phẩm trong một shop |

`minPrice` và `maxPrice` là tuỳ chọn, không âm; giá tối thiểu không được lớn hơn giá tối đa.
`type` nhận `READY_MADE` (bó hoa có sẵn) hoặc `CUSTOM` (bó hoa custom); bỏ trống để xem cả hai.
`sort` nhận `newest` (mặc định), `price_asc`, `price_desc`; giá trị khác trả 400.
`size` từ 1 đến 100, mặc định 12. Từ khóa tối đa 100 ký tự và được escape nên `%` hay `_`
không trở thành ký tự đại diện.

### Customer (JWT + CSRF)

| Method | URL | Chức năng |
|---|---|---|
| GET | `/api/customer/favorites?page=` | View Favorite Product |
| PUT / DELETE | `/api/customer/favorites/{productId}` | Lưu / bỏ lưu, đều idempotent |
| GET | `/api/customer/products/{id}/reviews` | Đánh giá của chính mình, 404 nếu chưa có |
| POST / PUT / DELETE | `/api/customer/products/{id}/reviews` | Viết / sửa / xóa đánh giá của mình |

### Manager và Admin (JWT + CSRF)

| Method | URL | Chức năng |
|---|---|---|
| PUT | `/api/shop/mine/{shopId}/reviews/{reviewId}/reply` | Shop trả lời đánh giá |
| GET / POST / PUT | `/api/admin/categories`, `/api/admin/categories/{id}` | Quản lý danh mục |
| GET | `/api/admin/products?q=&shopId=&page=` | Xem sản phẩm toàn sàn |
| PUT | `/api/admin/products/{id}/hidden` | Body `{"hidden":true}` hoặc `false` |

Body sản phẩm của Manager nhận thêm `images`, tối đa 10 phần tử:

```json
{"name":"Bó hoa hồng","description":"Hoa tươi","categoryId":"cat-01","price":250000,"stock":10,
 "status":"ACTIVE","type":"READY_MADE","images":[{"url":"https://.../a.jpg","primary":true},{"url":"https://.../b.jpg","primary":false}]}
```

Sản phẩm cũ và request không gửi `type` được xem là `READY_MADE`. Khi cập nhật sản phẩm, Manager có thể
chuyển giữa `READY_MADE` và `CUSTOM`.

Bỏ trường `images` (hoặc gửi `null`) thì ảnh cũ được giữ nguyên; gửi một mảng thì thay toàn bộ danh sách.
Luôn có đúng một ảnh chính: nếu không đánh dấu, ảnh đầu tiên được chọn.

Danh mục **không có lệnh xóa**, chỉ chuyển `INACTIVE` để không làm hỏng tham chiếu của sản phẩm đang dùng.
Sản phẩm thuộc danh mục INACTIVE vẫn hiển thị bình thường; Manager chỉ không chọn được danh mục đó cho sản phẩm mới.

## Quyền riêng tư của đánh giá

Response đánh giá chỉ trả `fullName` của người viết, **không bao giờ trả email**.
Tài khoản chưa đặt họ tên hiển thị là "Khách hàng".

## Kiểm tra cách ly

- Manager B gọi API sản phẩm hoặc trả lời đánh giá của shop A phải bị từ chối (404).
- Customer và Staff gọi API Admin hoặc Manager phải trả 403.
- Manager lưu lại sản phẩm đang bị Admin ẩn: cờ ẩn vẫn giữ nguyên, sản phẩm không hiện lại.
- Không đánh giá hay lưu yêu thích được sản phẩm đang bị ẩn (404).
- Gửi đánh giá thứ hai cho cùng một sản phẩm trả 409.

## Test tự động

```powershell
.\gradlew.bat test --tests com.example.flowershop.CatalogTests --tests com.example.flowershop.FavoriteTests --tests com.example.flowershop.ProductReviewTests --tests com.example.flowershop.AdminCatalogTests
```

Frontend: `npm.cmd run build` và `npm.cmd run lint`.

**Lưu ý quan trọng**: test chạy trên H2 với `ddl-auto=create-drop`, nghĩa là Hibernate tự sinh bảng từ entity
nên **không phát hiện được lệch schema với MySQL**. Đây chính là lý do lỗi cột `images` từng lọt qua toàn bộ
test suite. Sau khi đổi entity, luôn chạy backend thật trên MySQL và gọi thử endpoint liên quan.

## Cấu trúc code

Backend tách theo ranh giới nghiệp vụ, mỗi service một mức quyền:
`CatalogService` (công khai), `ProductReviewService`, `FavoriteService`, `AdminCatalogService`,
và `ManagerProductService` giữ phần CRUD của shop. `ProductCardAssembler` gom ảnh chính và điểm đánh giá
theo lô để tránh N+1; nó cần entity đang được quản lý trong transaction vì `Product.shop` là LAZY.

Frontend theo MVC sẵn có: `models/productModel.js`,
`services/{catalogService,favoriteService,reviewService,adminCategoryService}.js`,
`controllers/use{Catalog,ProductDetail,Favorites,ProductReviews,AdminCategories}Controller.js`,
`views/{ProductCatalogView,ProductDetailView,FavoritesView,ProductReviewsView,AdminCategoriesView}.jsx`.
