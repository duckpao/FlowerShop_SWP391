import { useProductDetailController } from "../controllers/useProductDetailController";
export default function ProductDetailView({ id }) {
  const { product, busy, error } = useProductDetailController(id);
  return (
    <main className="account-page">
      <a href="/">← Trang chủ</a>
      {busy && <p role="status">Đang tải…</p>}
      {error && (
        <p role="alert" className="message error">
          {error}
        </p>
      )}
      {product && (
        <section className="account-card">
          <h1>{product.name}</h1>
          <p>
            Cửa hàng: {product.shopName} · Danh mục: {product.categoryName}
          </p>
          {product.images.length > 0 && (
            <div>
              <img
                src={product.images[0]}
                alt={product.name}
                style={{
                  maxWidth: "100%",
                  maxHeight: 400,
                  objectFit: "contain",
                }}
              />
              {product.images.length > 1 && (
                <ul className="address-list">
                  {product.images.map((url, i) => (
                    <li key={i}>
                      <img
                        src={url}
                        alt=""
                        width={96}
                        height={96}
                        style={{ objectFit: "cover" }}
                      />
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )}
          <p>
            {Number(product.price).toLocaleString("vi-VN")} đ · Còn{" "}
            {product.stock}
          </p>
          <h2>Mô tả sản phẩm</h2>
          <p>{product.description}</p>
          {product.videos && product.videos.length > 0 && (
            <div>
              <h2>Video sản phẩm</h2>
              <ul className="address-list">
                {product.videos.map((v, i) => (
                  <li key={i}>
                    <video src={v.url} controls width={320} />
                    {v.title && (
                      <p>
                        <strong>{v.title}</strong>
                      </p>
                    )}
                    {v.description && <p>{v.description}</p>}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </section>
      )}
    </main>
  );
}
