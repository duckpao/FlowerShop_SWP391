import { useAdminShopsController } from "../controllers/useAdminShopsController";
import { shopStatusLabels as labels, shopActions } from "../models/shopModel";

export default function AdminShopsView() {
  const c = useAdminShopsController();
  return (
    <section className="account-card">
      <h2>Quản lý cửa hàng</h2>
      <form onSubmit={c.search}>
        <fieldset disabled={c.busy}>
          <label>
            Tìm theo tên shop
            <input
              maxLength={100}
              placeholder="Nhập tên shop"
              value={c.q}
              onChange={(e) => c.setQ(e.target.value)}
            />
          </label>
          <label>
            Trạng thái
            <select
              value={c.status}
              onChange={(e) => c.setStatus(e.target.value)}
            >
              <option value="">Tất cả</option>
              {Object.entries(labels).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </label>
          <button>Tìm kiếm</button>
        </fieldset>
      </form>
      {c.busy && <p role="status">Đang tải…</p>}
      {c.error && (
        <div role="alert">
          <p className="message error">{c.error}</p>
          <button disabled={c.busy} onClick={c.retry}>
            Tải lại danh sách
          </button>
        </div>
      )}
      {c.notice && (
        <p className="message success" role="status">
          {c.notice}
        </p>
      )}
      {c.data && (
        <>
          <p>Tổng: {c.data.totalElements} cửa hàng</p>
          {!c.data.content.length && <p>Không có cửa hàng phù hợp.</p>}
          <ul className="address-list">
            {c.data.content.map((shop) => (
              <li key={shop.id}>
                <strong>{shop.name}</strong>
                <p>
                  {labels[shop.status]} · {shop.owner.email}
                </p>
                <div className="address-actions">
                  <button disabled={c.busy} onClick={() => c.detail(shop.id)}>
                    Xem chi tiết
                  </button>
                  {shopActions[shop.status] && (
                    <button
                      disabled={c.busy}
                      onClick={() => c.transition(shop)}
                    >
                      {shopActions[shop.status][1]}
                    </button>
                  )}
                </div>
              </li>
            ))}
          </ul>
          <div className="address-actions">
            <button
              disabled={c.busy || c.data.page === 0}
              onClick={() => c.next(-1)}
            >
              Trang trước
            </button>
            <span>
              Trang {c.data.page + 1} / {Math.max(1, c.data.totalPages)}
            </span>
            <button
              disabled={c.busy || c.data.page + 1 >= c.data.totalPages}
              onClick={() => c.next(1)}
            >
              Trang sau
            </button>
          </div>
        </>
      )}
      {c.selected && (
        <article className="account-card">
          <h3>{c.selected.name}</h3>
          <p>{c.selected.description || "Chưa có mô tả"}</p>
          <p>Trạng thái shop: {labels[c.selected.status]}</p>
          <h4>Chủ cửa hàng</h4>
          <p>
            {c.selected.owner.fullName} · {c.selected.owner.email}
          </p>
          <p>Điện thoại: {c.selected.owner.phone || "Chưa cập nhật"}</p>
          <p>
            Role: {c.selected.owner.role} · Trạng thái tài khoản:{" "}
            {c.selected.owner.status}
          </p>
          <p>
            Email:{" "}
            {c.selected.owner.emailVerified ? "Đã xác thực" : "Chưa xác thực"}
          </p>
          <p>
            Ngày tạo:{" "}
            {c.selected.createdAt
              ? new Date(c.selected.createdAt).toLocaleString("vi-VN")
              : "Chưa có"}
          </p>
          <button onClick={() => c.setSelected(null)}>Đóng chi tiết</button>
        </article>
      )}
    </section>
  );
}
