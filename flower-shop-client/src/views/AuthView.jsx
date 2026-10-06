import PasswordInput from '../components/form/PasswordInput';
import { useState } from 'react';
import { validateAuth } from '../models/authValidation';
import AccountView from "./AccountView";
export default function AuthView({
  registrationRole, setRegistrationRole,
  fieldErrors = {}, captchaRequired, captcha, captchaAnswer, setCaptchaAnswer, reloadCaptcha,
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
  const [touched, setTouched] = useState({})
  const errors = validateAuth(page, {email, fullName, password, confirmation, otp})
  const field = name => ({id: 'auth-' + name, onBlur: () => setTouched(x => ({...x, [name]: true})), 'aria-invalid': !!(fieldErrors[name] || touched[name] && errors[name]), 'aria-describedby': 'auth-' + name + '-error'})
  const feedback = name => <p id={'auth-' + name + '-error'} className="text-sm text-error-500" aria-live="polite">{(touched[name] && errors[name]) || fieldErrors[name] || ''}</p>
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
    <main className="min-h-screen flex w-full">
      <aside className="hidden lg:flex flex-col justify-between w-1/2 bg-gradient-to-br from-gray-900 to-gray-800 p-12 text-white">
        <a className="text-2xl font-bold tracking-tight text-white hover:text-white/90" href="/">
          ✿ FlowerShop
        </a>
        <div className="space-y-6">
          <span className="text-sm font-medium tracking-wider text-gray-400 uppercase">MỖI ĐÓA HOA, MỘT LỜI GỬI</span>
          <h1 className="text-5xl font-bold leading-tight">
            Gửi yêu thương.
            <br />
            Đón những điều đẹp.
          </h1>
          <p className="text-lg text-gray-300">Những bó hoa tươi và món quà dành riêng cho người bạn yêu quý.</p>
        </div>
        <span className="text-sm text-gray-400">Hoa tươi • Quà tặng • Khoảnh khắc đáng nhớ</span>
      </aside>
      <section className="flex-1 flex flex-col justify-center items-center p-8 sm:p-12 bg-white dark:bg-gray-900" aria-labelledby="form-title">
        <div className="w-full max-w-md space-y-8">
          <div className="space-y-2">
            <span className="text-sm font-medium tracking-wider text-brand-500 uppercase">CHÀO MỪNG ĐẾN FLOWERSHOP</span>
            <h2 id="form-title" className="text-3xl font-bold text-gray-900 dark:text-white">
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
            <p className="text-gray-500 dark:text-gray-400">
              {page === "forgot"
                ? "Nhập email tài khoản đã đăng ký và xác thực để nhận mã đặt lại mật khẩu."
                : page === "register"
                  ? "Bắt đầu bằng email bạn đang sử dụng."
                  : page === "otp" || page === "reset"
                    ? `Nhập mã gồm 6 chữ số từ email gửi đến ${email}. Mã có hiệu lực 5 phút.`
                    : "Chào mừng bạn trở lại FlowerShop."}
            </p>
          </div>
          
          {error && (
            <div className="rounded-lg bg-error-50 dark:bg-error-500/10 p-4" role="alert">
              <p className="text-sm text-error-600 dark:text-error-500">{error}</p>
            </div>
          )}
          
          {notice && (
            <div className="rounded-lg bg-success-50 dark:bg-success-500/10 p-4" role="status">
              <p className="text-sm text-success-600 dark:text-success-500">{notice}</p>
            </div>
          )}
          
          <form noValidate onSubmit={submit} className="space-y-6">
            <fieldset disabled={busy} className="space-y-4">
              {page === 'register' && (
                <div className="space-y-1.5">
                  <label htmlFor="registration-role" className="block text-sm font-medium text-gray-700 dark:text-gray-300">Loại tài khoản</label>
                  <select id="registration-role" required value={registrationRole} onChange={e => setRegistrationRole(e.target.value)} className="block w-full rounded-lg border border-gray-300 px-4 py-3 dark:border-white/10 dark:bg-gray-800 dark:text-white">
                    <option value="CUSTOMER">Customer — Khách hàng</option>
                    <option value="SHOP">Manager shop — Quản lý cửa hàng</option>
                  </select>
                  <p className="text-sm text-gray-500">
                    {registrationRole === 'SHOP' ? 'Sau khi xác thực email, bạn có thể đăng nhập vào trang quản lý cửa hàng. Cửa hàng mới cần được quản trị viên duyệt.' : 'Đăng nhập để mua sắm và quản lý tài khoản khách hàng.'}
                  </p>
                </div>
              )}
              {page === "register" && (
                <div className="space-y-1.5">
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                    Họ và tên
                    <span className="text-error-500" aria-hidden="true"> *</span>
                  </label>
                  <input
                    className="block w-full rounded-lg border border-gray-300 px-4 py-3 text-gray-900 placeholder-gray-400 focus:border-brand-500 focus:ring-3 focus:ring-brand-500/10 dark:border-white/10 dark:bg-gray-800 dark:text-white dark:focus:border-brand-500"
                    autoComplete="name"
                    required
                    maxLength={50}
                    {...field('fullName')} value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                  />
                  {feedback('fullName')}
                </div>
              )}
              {page !== "otp" && page !== "reset" && (
                <div className="space-y-1.5">
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                    Email
                  </label>
                  <input
                    className="block w-full rounded-lg border border-gray-300 px-4 py-3 text-gray-900 placeholder-gray-400 focus:border-brand-500 focus:ring-3 focus:ring-brand-500/10 dark:border-white/10 dark:bg-gray-800 dark:text-white dark:focus:border-brand-500"
                    type="email"
                    autoComplete="email"
                    required
                    maxLength={page === "register" ? 50 : 255}
                    {...field('email')} value={email}
                    onChange={(e) => { setEmail(e.target.value); setTouched(x => ({...x, email: true})) }}
                  />
                  {feedback('email')}
                </div>
              )}
              {page !== "otp" && page !== "forgot" && (
                <div className="space-y-1.5">
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                    {page === "reset" ? "Mật khẩu mới" : "Mật khẩu"}
                  </label>
                  <PasswordInput key={page}
                    className="block w-full rounded-lg border border-gray-300 px-4 py-3 text-gray-900 placeholder-gray-400 focus:border-brand-500 focus:ring-3 focus:ring-brand-500/10 dark:border-white/10 dark:bg-gray-800 dark:text-white dark:focus:border-brand-500"
                    
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
                    {...field('password')} value={password}
                    onChange={(e) => setPassword(e.target.value)}
                  />
                  {feedback('password')}
                  {page !== "signin" && (
                    <p className="text-xs text-gray-500 mt-1">
                      {page === "register"
                        ? "9–15 ký tự, gồm chữ hoa, chữ thường và số."
                        : "Tối thiểu 15 ký tự."}
                    </p>
                  )}
                </div>
              )}
              {(page === "register" || page === "reset") && (
                <div className="space-y-1.5">
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                    Nhập lại mật khẩu
                  </label>
                  <PasswordInput key={page}
                    className="block w-full rounded-lg border border-gray-300 px-4 py-3 text-gray-900 placeholder-gray-400 focus:border-brand-500 focus:ring-3 focus:ring-brand-500/10 dark:border-white/10 dark:bg-gray-800 dark:text-white dark:focus:border-brand-500"
                    
                    autoComplete="new-password"
                    required
                    {...field('confirmation')} value={confirmation}
                    onChange={(e) => setConfirmation(e.target.value)}
                  />
                  {feedback('confirmation')}
                </div>
              )}
              {(page === "otp" || page === "reset") && (
                <div className="space-y-1.5">
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                    Nhập mã OTP:
                  </label>
                  <input
                    className="block w-full text-center text-2xl tracking-widest rounded-lg border border-gray-300 px-4 py-3 text-gray-900 placeholder-gray-400 focus:border-brand-500 focus:ring-3 focus:ring-brand-500/10 dark:border-white/10 dark:bg-gray-800 dark:text-white dark:focus:border-brand-500"
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    required
                    pattern="[0-9]{6}"
                    maxLength={6}
                    {...field('otp')} value={otp}
                    onChange={(e) =>
                      setOtp(e.target.value.replace(/\D/g, "").slice(0, 6))
                    }
                    autoFocus
                  />
                  {feedback('otp')}
                </div>
              )}
              {captchaRequired && <div className="space-y-2 rounded-lg border border-gray-300 p-3">
                {captcha && <img src={captcha.image} width="210" height="64" alt="CAPTCHA 5 ký tự" />}
                <button type="button" className="block font-bold underline underline-offset-4 text-brand-600 hover:text-brand-700 disabled:opacity-50" onClick={reloadCaptcha} disabled={busy}>Đổi ảnh</button>
                <label htmlFor="captcha-answer" className="sr-only">Nhập mã trong ảnh</label>
                <input id="captcha-answer" className="w-full rounded border p-2" required maxLength={5} autoComplete="off" value={captchaAnswer} onChange={e => setCaptchaAnswer(e.target.value.toUpperCase())} />
              </div>}
              <button 
                className="w-full bg-brand-500 hover:bg-brand-600 text-white font-medium rounded-lg px-4 py-3 transition-colors disabled:opacity-50 disabled:cursor-not-allowed mt-2" 
                disabled={captchaRequired && (!captcha || captchaAnswer.length !== 5)}
                type="submit"
              >
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
          
          <div className="flex flex-col items-center space-y-4">
            {(page === "otp" || page === "reset") && (
              <>
                <button
                  className="text-sm font-medium text-brand-500 hover:text-brand-600 transition-colors disabled:opacity-50"
                  onClick={resend}
                  disabled={busy || (captchaRequired && (!captcha || captchaAnswer.length !== 5))}
                >
                  {seconds > 0 ? `Gửi lại mã sau ${seconds}s` : "Gửi lại mã OTP"}
                </button>
                <p className="text-xs text-gray-500 text-center">
                  Kiểm tra cả thư mục Spam. Mỗi lần gửi lại sẽ tạo mã mới; mã cũ không còn hiệu lực.
                </p>
                <button
                  className="text-sm text-gray-600 hover:text-gray-900 dark:text-gray-400 dark:hover:text-white transition-colors"
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
                className="text-sm font-medium text-brand-500 hover:text-brand-600 transition-colors"
                disabled={busy}
                onClick={() => navigate("forgot")}
              >
                Quên mật khẩu?
              </button>
            )}
            {(page === "forgot" || page === "reset") && (
              <p>
                <button
                  className="text-sm font-medium text-gray-600 hover:text-gray-900 dark:text-gray-400 dark:hover:text-white transition-colors"
                  disabled={busy}
                  onClick={() => navigate("signin")}
                >
                  Quay lại Sign in
                </button>
              </p>
            )}
            {(page === "register" || page === "signin") && (
              <p className="text-sm text-gray-600 dark:text-gray-400">
                {page === "register"
                  ? "Đã có tài khoản? "
                  : "Chưa có tài khoản? "}
                <button
                  className="font-medium text-brand-500 hover:text-brand-600 transition-colors"
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
        </div>
      </section>
    </main>
  );
}
