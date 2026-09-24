import { useAccountController } from "../controllers/useAccountController";
export default function AdminProfileView({ user, roleLabel = "Admin" }) {
  const c = useAccountController(user);
  return (
    <section className="account-card">
      <h2>Thông tin tài khoản {roleLabel}</h2>
      {c.error && <p role="alert">{c.error}</p>}
      {c.notice && <p role="status">{c.notice}</p>}
      {c.busy && <p role="status">Đang xử lý…</p>}
      {!c.profile && !c.busy && (
        <button onClick={c.retry}>Tải lại hồ sơ</button>
      )}
      {c.profile && (
        <>
          <p>Email: {c.profile.email}</p>
          <p>
            Vai trò: {roleLabel} ·{" "}
            {c.profile.emailVerified
              ? "Đã xác thực email"
              : "Chưa xác thực email"}
          </p>
          <form onSubmit={c.saveProfile}>
            <fieldset disabled={c.busy}>
              <label>
                Họ và tên
                <input
                  required
                  maxLength={100}
                  autoComplete="name"
                  value={c.form.fullName}
                  onChange={(e) =>
                    c.setForm({ ...c.form, fullName: e.target.value })
                  }
                />
              </label>
              <label>
                Số điện thoại
                <input
                  type="tel"
                  maxLength={16}
                  pattern="[+]?[0-9]{9,15}"
                  autoComplete="tel"
                  value={c.form.phone}
                  onChange={(e) =>
                    c.setForm({ ...c.form, phone: e.target.value })
                  }
                />
              </label>
              <button>Lưu thông tin tài khoản</button>
            </fieldset>
          </form>
        </>
      )}
    </section>
  );
}
