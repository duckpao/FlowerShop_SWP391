import "../styles/auth.css";
import AccountView from "./AccountView";
export default function AuthView({
  page,
  email,
  fullName,
  password,
  confirmation,
  otp,
  busy,
  error,
  notice,
  seconds,
  user,
  logout,
  checkSession,
  setEmail,
  setFullName,
  setPassword,
  setConfirmation,
  setOtp,
  submit,
  resend,
  navigate,
}) {
  if (page === "account" && user)
    return (
      <AccountView
        user={user}
        logout={logout}
        checkSession={checkSession}
        busy={busy}
        error={error}
        notice={notice}
      />
    );
  return (
    <main className="auth-layout">
      <aside className="brand-panel">
        <a className="brand" href="/">
          ✿ FlowerShop
        </a>
        <div>
          <span className="eyebrow">MỖI ĐÓA HOA, MỘT LỜI GỬI</span>
          <h1>
            Gửi yêu thương.
            <br />
            Đón những điều đẹp.
          </h1>
          <p>Những bó hoa tươi và món quà dành riêng cho người bạn yêu quý.</p>
        </div>
        <span>Hoa tươi • Quà tặng • Khoảnh khắc đáng nhớ</span>
      </aside>
      <section className="form-panel" aria-labelledby="form-title">
        <div className="card">
          <span className="eyebrow">CHÀO MỪNG ĐẾN FLOWERSHOP</span>
          <h2 id="form-title">
            {page === "register"
              ? "Tạo tài khoản"
              : page === "otp"
                ? "Xác thực email"
                : page === "forgot"
                  ? "Quên mật khẩu"
                  : page === "reset"
                    ? "Đặt lại mật khẩu"
                    : "Sign in"}
          </h2>
          <p className="intro">
            {page === "forgot"
              ? "Nhập email tài khoản đã đăng ký và xác thực để nhận mã đặt lại mật khẩu."
              : page === "register"
                ? "Bắt đầu bằng email bạn đang sử dụng."
                : page === "otp" || page === "reset"
                  ? `Nhập mã gồm 6 chữ số từ email gửi đến ${email}. Mã có hiệu lực 5 phút.`
                  : "Chào mừng bạn trở lại FlowerShop."}
          </p>
          {error && (
            <p className="message error" role="alert">
              {error}
            </p>
          )}
          {notice && (
            <p className="message success" role="status">
              {notice}
            </p>
          )}
          <form onSubmit={submit}>
            <fieldset disabled={busy}>
              {page === "register" && (
                <label>
                  Họ và tên
                  <input
                    autoComplete="name"
                    required
                    maxLength={50}
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                  />
                </label>
              )}
              {page !== "otp" && page !== "reset" && (
                <label>
                  Email
                  <input
                    type="email"
                    autoComplete="email"
                    required
                    maxLength={page === "register" ? 50 : 255}
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                  />
                </label>
              )}
              {page !== "otp" && page !== "forgot" && (
                <label>
                  {page === "reset" ? "Mật khẩu mới" : "Mật khẩu"}
                  <input
                    type="password"
                    required
                    minLength={
                      page === "register"
                        ? 9
                        : page === "reset"
                          ? 15
                          : undefined
                    }
                    maxLength={page === "register" ? 15 : 72}
                    pattern={
                      page === "register"
                        ? "(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9]).*"
                        : undefined
                    }
                    autoComplete={
                      page !== "signin" ? "new-password" : "current-password"
                    }
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                  />
                  {page !== "signin" && (
                    <small>
                      {page === "register"
                        ? "9–15 ký tự, gồm chữ hoa, chữ thường và số."
                        : "Tối thiểu 15 ký tự."}
                    </small>
                  )}
                </label>
              )}
              {(page === "register" || page === "reset") && (
                <label>
                  Nhập lại mật khẩu
                  <input
                    type="password"
                    autoComplete="new-password"
                    required
                    value={confirmation}
                    onChange={(e) => setConfirmation(e.target.value)}
                  />
                </label>
              )}
              {(page === "otp" || page === "reset") && (
                <label>
                  Nhập mã OTP:
                  <input
                    className="otp"
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    required
                    pattern="[0-9]{6}"
                    maxLength={6}
                    value={otp}
                    onChange={(e) =>
                      setOtp(e.target.value.replace(/\D/g, "").slice(0, 6))
                    }
                    autoFocus
                  />
                </label>
              )}
              <button className="primary" type="submit">
                {busy
                  ? "Đang xử lý…"
                  : page === "register"
                    ? "Đăng ký"
                    : page === "otp"
                      ? "Xác nhận OTP"
                      : page === "forgot"
                        ? "Gửi mã OTP"
                        : page === "reset"
                          ? "Đổi mật khẩu"
                          : "Đăng nhập"}
              </button>
            </fieldset>
          </form>
          {(page === "otp" || page === "reset") && (
            <>
              <button
                className="text-button"
                onClick={resend}
                disabled={busy || seconds > 0}
              >
                {seconds > 0 ? `Gửi lại mã sau ${seconds}s` : "Gửi lại mã OTP"}
              </button>
              <p className="hint">
                Kiểm tra cả thư mục Spam. Tối đa 3 mã trong 15 phút.
              </p>
              <button
                className="text-button"
                disabled={busy}
                onClick={() =>
                  navigate(page === "reset" ? "forgot" : "register")
                }
              >
                Nhập lại email
              </button>
            </>
          )}
          {page === "signin" && (
            <button
              className="text-button"
              disabled={busy}
              onClick={() => navigate("forgot")}
            >
              Quên mật khẩu?
            </button>
          )}
          {(page === "forgot" || page === "reset") && (
            <p>
              <button
                className="text-button"
                disabled={busy}
                onClick={() => navigate("signin")}
              >
                Quay lại Sign in
              </button>
            </p>
          )}
          {(page === "register" || page === "signin") && (
            <p className="switch">
              {page === "register"
                ? "Đã có tài khoản? "
                : "Chưa có tài khoản? "}
              <button
                className="text-button"
                disabled={busy}
                onClick={() =>
                  navigate(page === "register" ? "signin" : "register")
                }
              >
                {page === "register" ? "Sign in" : "Đăng ký"}
              </button>
            </p>
          )}
        </div>
      </section>
    </main>
  );
}
