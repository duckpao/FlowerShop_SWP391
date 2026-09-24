import { useProductCatalogController } from "../controllers/useProductCatalogController";
export default function ProductCatalogView() {
  const c = useProductCatalogController();
  return (
    <section className="account-card">
      <h2>Danh mục sản phẩm toàn hệ thống</h2>
      <form onSubmit={c.search}>
        <label>
          Tìm sản phẩm
          <input
            maxLength={100}
            value={c.q}
            onChange={(e) => c.setQ(e.target.value)}
          />
        </label>
        <label>
          Danh mục
          <select
            value={c.categoryId}
            onChange={(e) => c.setCategoryId(e.target.value)}
          >
            <option value="">Tất cả danh mục</option>
            {c.categories.map((x) => (
              <option key={x.id} value={x.id}>
                {x.name}
              </option>
            ))}
          </select>
        </label>
        <button disabled={c.busy}>Tìm kiếm</button>
      </form>
      {c.error && (
        <p role="alert" className="message error">
          {c.error}
        </p>
      )}
      {c.data && (
        <>
          <p>{c.data.totalElements} sản phẩm</p>
          <ul className="address-list">
            {c.data.content.map((p) => (
              <li key={p.id}>
                <h3>
                  <a href={`/products/${encodeURIComponent(p.id)}`}>{p.name}</a>
                </h3>
                <p>{p.description}</p>
                <p>
                  {Number(p.price).toLocaleString("vi-VN")} đ · {p.categoryName}{" "}
                  · Còn {p.stock}
                </p>
                <p>Cửa hàng: {p.shopName}</p>
              </li>
            ))}
          </ul>
          <button
            disabled={c.busy || c.page === 0}
            onClick={() => c.setPage((x) => x - 1)}
          >
            Trang trước
          </button>
          <button
            disabled={c.busy || c.page + 1 >= c.data.totalPages}
            onClick={() => c.setPage((x) => x + 1)}
          >
            Trang sau
          </button>
        </>
      )}
    </section>
  );
}
