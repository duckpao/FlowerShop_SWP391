import { useEffect, useState } from 'react'
import { authService } from '../services/authService'
import { useNavigate } from 'react-router'
const post = authService.post

export function useAuthController() {
  const routerNavigate = useNavigate();
  const [user, setUser] = useState(null)
  const [initializing, setInitializing] = useState(true)
  useEffect(() => {
    let active = true
    authService.me().then(current => {
      if (active) {
        setUser(current); setPage('account');
        if (window.location.pathname === '/login') {
          if (current.role === 'ADMIN') routerNavigate('/admin');
          else if (current.role === 'SHOP' || current.role === 'SHOP_STAFF') routerNavigate('/shop-admin');
          else routerNavigate('/');
        }
      }
    }).catch(() => { }).finally(() => { if (active) setInitializing(false) })
    return () => { active = false }
  }, [])
  const [page, setPage] = useState('signin')
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
      if (page === 'signin') {
        const current = await authService.login(address, password)
        setUser(current); setPassword(''); setPage('account'); setNotice('Đăng nhập thành công.')
        if (current.role === 'ADMIN') {
          routerNavigate('/admin');
        } else if (current.role === 'SHOP' || current.role === 'SHOP_STAFF') {
          routerNavigate('/shop-admin');
        } else {
          routerNavigate('/');
        }
      } else if (page === 'register') {
        if (password !== confirmation) throw new Error('Mật khẩu xác nhận không khớp.')
        if (new TextEncoder().encode(password).length > 72) throw new Error('Mật khẩu không được vượt quá 72 byte UTF-8.')
        const account = { email: address, password, fullName: fullName.trim() }
        const data = await post('register', account)
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
        authService.clear(); setUser(null)
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
  async function logout() {
    setBusy(true); setError('')
    try { await authService.logout(); setUser(null); navigate('signin'); setNotice('Đã đăng xuất.') }
    catch (failure) { setError(failure.message) }
    finally { setBusy(false) }
  }
  async function checkSession() {
    setBusy(true); setError('')
    try { setUser(await authService.me()); setNotice('Phiên đăng nhập đang hoạt động.') }
    catch (failure) { setUser(null); navigate('signin'); setError(failure.message) }
    finally { setBusy(false) }
  }
  return { page, email, fullName, password, confirmation, otp, busy: busy || initializing, error, notice, seconds, user, logout, checkSession, setEmail, setFullName, setPassword, setConfirmation, setOtp, submit, resend, navigate }
}
