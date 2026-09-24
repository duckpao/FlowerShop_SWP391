import { useState } from "react";
import { useProductsController } from "../controllers/useProductsController";
export default function ProductsView({ shop, manage = false }) {
  const c = useProductsController(shop.id, manage);
  const disabled = c.busy || shop.status !== "ACTIVE";
  const [videoTitle, setVideoTitle] = useState("");
  const [videoDescription, setVideoDescription] = useState("");
  return (
    <section className="account-card">
      <h2>{manage ? "Quản lý mặt hàng của shop" : "Sản phẩm của cửa hàng"}</h2>
      {c.error && <p role="alert">{c.error}</p>}
      {c.notice && <p role="status">{c.notice}</p>}
      <button disabled={c.busy} onClick={c.reload}>
        Tải lại sản phẩm
      </button>
      {manage && (
        <form onSubmit={c.save}>
          <h3>{c.editing ? "Sửa sản phẩm" : "Đăng sản phẩm mới"}</h3>
          <fieldset disabled={disabled}>
            <label>
              Tên sản phẩm
              <input
                required
                maxLength={255}
                value={c.form.name}
                onChange={(e) => c.setForm({ ...c.form, name: e.target.value })}
              />
            </label>
            <label>
              Mô tả
              <textarea
                maxLength={5000}
                value={c.form.description}
                onChange={(e) =>
                  c.setForm({ ...c.form, description: e.target.value })
                }
              />
            </label>
            <label>
              Danh mục
              <select
                required
                value={c.form.categoryId}
                onChange={(e) =>
                  c.setForm({ ...c.form, categoryId: e.target.value })
                }
              >
                <option value="">Chọn danh mục</option>
                {c.categories.map((x) => (
                  <option key={x.id} value={x.id}>
                    {x.name}
                  </option>
                ))}
              </select>
            </label>
            {!c.categories.length && (
              <p>
                Chưa có danh mục đang hoạt động. Cần bổ sung danh mục trong dữ
                liệu hệ thống trước khi đăng sản phẩm.
              </p>
            )}
            <label>
              Giá (VND)
              <input
                required
                type="number"
                min="0.01"
                max="9999999999.99"
                step="0.01"
                value={c.form.price}
                onChange={(e) =>
                  c.setForm({ ...c.form, price: e.target.value })
                }
              />
            </label>
            <label>
              Tồn kho
              <input
                required
                type="number"
                min="0"
                max="1000000"
                step="1"
                value={c.form.stock}
                onChange={(e) =>
                  c.setForm({ ...c.form, stock: e.target.value })
                }
              />
            </label>
            <label>
              Trạng thái
              <select
                value={c.form.status}
                onChange={(e) =>
                  c.setForm({ ...c.form, status: e.target.value })
                }
              >
                <option value="ACTIVE">Đăng bán</option>
                <option value="INACTIVE">Ẩn</option>
                <option value="OUT_OF_STOCK">Hết hàng</option>
              </select>
            </label>
            <button disabled={!c.categories.length}>Lưu sản phẩm</button>
            {c.editing && (
              <button type="button" onClick={c.cancel}>
                Hủy sửa
              </button>
            )}
          </fieldset>
        </form>
      )}
      {manage && c.editing && (
        <div className="account-card">
          <h3>Ảnh sản phẩm</h3>
          <input
            type="file"
            accept="image/jpeg,image/png,image/webp"
            disabled={disabled}
            onChange={(e) => {
              const file = e.target.files[0];
              e.target.value = "";
              if (file) c.uploadImage(file);
            }}
          />
          <ul className="address-list">
            {c.images.map((img) => (
              <li key={img.id}>
                <img
                  src={img.url}
                  alt=""
                  width={96}
                  height={96}
                  style={{ objectFit: "cover" }}
                />
                {img.primary && <strong> (Ảnh đại diện)</strong>}
                <button
                  type="button"
                  disabled={disabled}
                  onClick={() => c.deleteImage(img.id)}
                >
                  Xoá ảnh
                </button>
              </li>
            ))}
          </ul>
          {!c.images.length && <p>Chưa có ảnh nào cho sản phẩm này.</p>}
        </div>
      )}
      {manage && c.editing && (
        <div className="account-card">
          <h3>Video sản phẩm</h3>
          <label>
            Tiêu đề video (không bắt buộc)
            <input
              maxLength={255}
              value={videoTitle}
              onChange={(e) => setVideoTitle(e.target.value)}
            />
          </label>
          <label>
            Mô tả video (không bắt buộc)
            <input
              maxLength={1000}
              value={videoDescription}
              onChange={(e) => setVideoDescription(e.target.value)}
            />
          </label>
          <input
            type="file"
            accept="video/mp4,video/webm,video/quicktime"
            disabled={disabled}
            onChange={(e) => {
              const file = e.target.files[0];
              e.target.value = "";
              if (file) {
                c.uploadVideo(file, videoTitle, videoDescription);
                setVideoTitle("");
                setVideoDescription("");
              }
            }}
          />
          <ul className="address-list">
            {c.videos.map((v) => (
              <li key={v.id}>
                <video src={v.url} controls width={200} />
                {v.title && (
                  <p>
                    <strong>{v.title}</strong>
                  </p>
                )}
                {v.description && <p>{v.description}</p>}
                <button
                  type="button"
                  disabled={disabled}
                  onClick={() => c.deleteVideo(v.id)}
                >
                  Xoá video
                </button>
              </li>
            ))}
          </ul>
          {!c.videos.length && <p>Chưa có video nào cho sản phẩm này.</p>}
        </div>
      )}
      {c.data && (
        <>
          <p>{c.data.totalElements} sản phẩm</p>
          <ul className="address-list">
            {c.data.content.map((p) => (
              <li key={p.id}>
                <h3>{p.name}</h3>
                <p>{p.description}</p>
                <p>
                  {Number(p.price).toLocaleString("vi-VN")} đ · {p.categoryName}{" "}
                  · Còn {p.stock}
                </p>
                {manage && (
                  <>
                    <p>{p.status}</p>
                    <button disabled={disabled} onClick={() => c.edit(p)}>
                      Sửa
                    </button>
                    <button
                      disabled={disabled || p.status === "INACTIVE"}
                      onClick={() => c.hide(p.id)}
                    >
                      Ẩn sản phẩm
                    </button>
                  </>
                )}
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
