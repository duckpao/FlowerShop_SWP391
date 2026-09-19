import { useEffect, useState } from 'react'
import './App.css'

async function post(path, body) {
  let csrfResponse
  try { csrfResponse = await fetch('/api/auth/csrf', { credentials: 'include', cache: 'no-store' }) }
  catch { throw new Error('Không kết nối được máy chủ. Vui lòng thử lại.') }
  if (!csrfResponse.ok) throw new Error('Không thể tạo phiên. Vui lòng thử lại.')
  const csrf = await csrfResponse.json()
  const response = await fetch(`/api/auth/${path}`, {
    method: 'POST', credentials: 'include',
    headers: { 'Content-Type': 'application/json', [csrf.headerName]: csrf.token },
    body: JSON.stringify(body),
  })
  const data = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(data.errors ? Object.values(data.errors).join(' ') :
    data.message || 'Không thể xử lý yêu cầu. Vui lòng thử lại.')
  return data
}

export default function App() {
  const [page, setPage] = useState('register')
  const [email, setEmail] = useState('')
  const [fullName, setFullName] = useState('')
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [otp, setOtp] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [seconds, setSeconds] = useState(0)
  useEffect(() => {
    if (seconds <= 0) return
    const timer = setTimeout(() => setSeconds(seconds - 1), 1000)
    return () => clearTimeout(timer)
  }, [seconds])

  function navigate(next) {
    setPage(next); setError(''); setNotice(''); setPassword(''); setConfirmation(''); setOtp('')
  }
  async function submit(event) {
    event.preventDefault()
    setBusy(true); setError(''); setNotice('')
    try {
      const address = email.trim().toLowerCase()
      if (page === 'register') {
        if (password !== confirmation) throw new Error('Mật khẩu xác nhận không khớp.')
        if (new TextEncoder().encode(password).length > 72) throw new Error('Mật khẩu không được vượt quá 72 byte UTF-8.')
        const data = await post('register', { email: address, password, fullName: fullName.trim() })
        setEmail(address); setPassword(''); setConfirmation(''); setOtp('')
        setPage('otp'); setSeconds(60); setNotice(data.message)
      } else if (page === 'otp') {
        await post('verify-email', { email: address, otp })
        setOtp(''); setPage('signin')
        setNotice('Đăng ký thành công! Email của bạn đã được xác thực.')
      } else if (page === 'forgot') {
        const data = await post('forgot-password', { email: address })
        setEmail(address); setOtp(''); setPage('reset'); setSeconds(60); setNotice(data.message)
      } else if (page === 'reset') {
        if (password !== confirmation) throw new Error('Mật khẩu xác nhận không khớp.')
        if (new TextEncoder().encode(password).length > 72) throw new Error('Mật khẩu không được vượt quá 72 byte UTF-8.')
        await post('reset-password', { email: address, otp, newPassword: password, confirmPassword: confirmation })
        setPassword(''); setConfirmation(''); setOtp(''); setPage('signin')
        setNotice('Đặt lại mật khẩu thành công. Hãy dùng mật khẩu mới khi đăng nhập.')
      }
    } catch (failure) { setError(failure.message || 'Có lỗi xảy ra. Vui lòng thử lại.') }
    finally { setBusy(false) }
  }
  async function resend() {
    setBusy(true); setError(''); setNotice('')
    try {
      const data = await post(page === 'reset' ? 'forgot-password' : 'resend-verification', { email })
      setSeconds(60); setNotice(data.message)
    } catch (failure) { setError(failure.message) }
    finally { setBusy(false) }
  }
  return <main className="auth-layout">
    <aside className="brand-panel">
      <a className="brand" href="/">✿ FlowerShop</a>
      <div><span className="eyebrow">MỖI ĐÓA HOA, MỘT LỜI GỬI</span>
        <h1>Gửi yêu thương.<br />Đón những điều đẹp.</h1>
        <p>Những bó hoa tươi và món quà dành riêng cho người bạn yêu quý.</p>
      </div><span>Hoa tươi • Quà tặng • Khoảnh khắc đáng nhớ</span>
    </aside>
    <section className="form-panel" aria-labelledby="form-title">
      <div className="card">
        <span className="eyebrow">CHÀO MỪNG ĐẾN FLOWERSHOP</span>
        <h2 id="form-title">{page === 'register' ? 'Tạo tài khoản' : page === 'otp' ? 'Xác thực email' : page === 'forgot' ? 'Quên mật khẩu' : page === 'reset' ? 'Đặt lại mật khẩu' : 'Sign in'}</h2>
        <p className="intro">{page === 'forgot' ? 'Nhập email tài khoản đã đăng ký và xác thực để nhận mã đặt lại mật khẩu.' : page === 'register' ? 'Bắt đầu bằng email bạn đang sử dụng.' : (page === 'otp' || page === 'reset')
          ? `Nhập mã gồm 6 chữ số từ email gửi đến ${email}. Mã có hiệu lực 5 phút.`
          : 'Chào mừng bạn trở lại FlowerShop.'}</p>
        {error && <p className="message error" role="alert">{error}</p>}
        {notice && <p className="message success" role="status">{notice}</p>}
        <form onSubmit={submit}>
          <fieldset disabled={busy}>
            {page === 'register' && <label>Họ và tên<input autoComplete="name" required maxLength={100}
              value={fullName} onChange={e => setFullName(e.target.value)} /></label>}
            {page !== 'otp' && page !== 'reset' && <label>Email<input type="email" autoComplete="email" required maxLength={255}
              value={email} onChange={e => setEmail(e.target.value)} /></label>}
            {page !== 'otp' && page !== 'forgot' && <label>{page === 'reset' ? 'Mật khẩu mới' : 'Mật khẩu'}<input type="password" required minLength={page !== 'signin' ? 15 : undefined}
              autoComplete={page !== 'signin' ? 'new-password' : 'current-password'}
              value={password} onChange={e => setPassword(e.target.value)} />
              {page !== 'signin' && <small>Tối thiểu 15 ký tự.</small>}</label>}
            {(page === 'register' || page === 'reset') && <label>Nhập lại mật khẩu<input type="password" autoComplete="new-password" required
              value={confirmation} onChange={e => setConfirmation(e.target.value)} /></label>}
            {(page === 'otp' || page === 'reset') && <label>Nhập mã OTP:<input className="otp" inputMode="numeric" autoComplete="one-time-code"
              required pattern="[0-9]{6}" maxLength={6} value={otp}
              onChange={e => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))} autoFocus /></label>}
            <button className="primary" type="submit" disabled={page === 'signin'}>
              {busy ? 'Đang xử lý…' : page === 'register' ? 'Đăng ký' : page === 'otp' ? 'Xác nhận OTP' : page === 'forgot' ? 'Gửi mã OTP' : page === 'reset' ? 'Đổi mật khẩu' : 'Đăng nhập'}
            </button>
          </fieldset>
        </form>
        {(page === 'otp' || page === 'reset') && <>
          <button className="text-button" onClick={resend} disabled={busy || seconds > 0}>
            {seconds > 0 ? `Gửi lại mã sau ${seconds}s` : 'Gửi lại mã OTP'}</button>
          <p className="hint">Kiểm tra cả thư mục Spam. Tối đa 3 mã trong 15 phút.</p>
          <button className="text-button" disabled={busy} onClick={() => navigate(page === 'reset' ? 'forgot' : 'register')}>Nhập lại email</button>
        </>}
        {page === 'signin' && <p className="hint">Chức năng đăng nhập sẽ được mở khi hoàn tất bước triển khai tiếp theo.</p>}
        {page === 'signin' && <button className="text-button" disabled={busy} onClick={() => navigate('forgot')}>Quên mật khẩu?</button>}
        {(page === 'forgot' || page === 'reset') && <p><button className="text-button" disabled={busy} onClick={() => navigate('signin')}>Quay lại Sign in</button></p>}
        {(page === 'register' || page === 'signin') && <p className="switch">{page === 'register' ? 'Đã có tài khoản? ' : 'Chưa có tài khoản? '}
          <button className="text-button" disabled={busy} onClick={() => navigate(page === 'register' ? 'signin' : 'register')}>
            {page === 'register' ? 'Sign in' : 'Đăng ký'}</button></p>}
      </div>
    </section>
  </main>
}
