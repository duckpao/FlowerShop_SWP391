import { useManagerApplicationController } from "../controllers/useManagerApplicationController";
export default function ManagerApplicationView({ user }) {
  const c = useManagerApplicationController(user);
  const canApply =
    user.role === "CUSTOMER" &&
    (!c.items.length || c.items[0].status === "REJECTED");
  return (
    <section className="account-card">
      <h2>Đăng ký mở shop / trở thành Manager</h2>
      {c.error && (
        <p role="alert">
          {c.error} <a href="/login">Đăng nhập lại</a>
        </p>
      )}
      {c.notice && <p role="status">{c.notice}</p>}
      <button disabled={c.busy} onClick={c.reload}>
        Cập nhật trạng thái đơn
      </button>
      {c.items.map((a) => (
        <article key={a.id}>
          <strong>
            {a.shopName} —{" "}
            {
              {
                PENDING: "Chờ Admin duyệt",
                APPROVED: "Đã được duyệt",
                REJECTED: "Đã từ chối",
              }[a.status]
            }
          </strong>
          <p>{a.reviewNote}</p>
          {a.status === "APPROVED" && (
            <p>
              Bạn đã được cấp quyền Manager. Đăng nhập lại và chọn Vào trang
              quản trị shop để đăng sản phẩm.
            </p>
          )}
        </article>
      ))}
      {canApply && (
        <button
          disabled={c.busy}
          aria-expanded={c.open}
          onClick={() => c.setOpen(!c.open)}
        >
          {c.open ? "Đóng form" : "Đăng ký làm Manager shop"}
        </button>
      )}
      {c.open && canApply && (
        <form onSubmit={c.submit}>
          <fieldset disabled={c.busy}>
            {[
              ["fullName", "Họ và tên", 100],
              ["phone", "Số điện thoại", 16],
              ["shopName", "Tên shop", 255],
              ["addressLine", "Địa chỉ cửa hàng", 255],
              ["district", "Quận / Huyện", 100],
              ["ward", "Phường / Xã", 100],
            ].map(([key, label, max]) => (
              <label key={key}>
                {label}
                <input
                  required
                  maxLength={max}
                  pattern={key === "phone" ? "[+]?[0-9]{9,15}" : undefined}
                  value={c.form[key]}
                  onChange={(e) =>
                    c.setForm({ ...c.form, [key]: e.target.value })
                  }
                />
              </label>
            ))}
            <label>
              Thành phố
              <input required readOnly value={c.form.city} />
            </label>
            <label>
              Giới thiệu shop
              <textarea
                required
                maxLength={5000}
                value={c.form.description}
                onChange={(e) =>
                  c.setForm({ ...c.form, description: e.target.value })
                }
              />
            </label>
            <p>
              Sau khi Admin duyệt, tài khoản chuyển từ Customer sang Manager và
              chỉ quản lý shop được tạo từ đơn này.
            </p>
            <button>Gửi đơn chờ Admin duyệt</button>
          </fieldset>
        </form>
      )}
    </section>
  );
}
