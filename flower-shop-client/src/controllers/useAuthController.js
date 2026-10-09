import { useEffect, useState } from 'react'
import { authService } from '../services/authService'
import { validateAuth } from '../models/authValidation'
import { useNavigate } from 'react-router'
const post = authService.post

export function useAuthController() {
  const routerNavigate = useNavigate();
  const [user, setUser] = useState(null)
  useEffect(() => {
    const updated = event => {
      const profile = event.detail
      setUser(current => current?.id === profile?.id
        ? { ...current, fullName: profile.fullName, avatarUrl: profile.avatarUrl } : current)
    }
    window.addEventListener('flowershop-profile-updated', updated)
    return () => window.removeEventListener('flowershop-profile-updated', updated)
  }, [])
  const [initializing, setInitializing] = useState(true)
  useEffect(() => {
    let active = true
    authService.me().then(current => {
      if (active && current) {
        setUser(current); setPage('account');
        if (window.location.pathname === '/login') {
          const searchParams = new URLSearchParams(window.location.search);
          const nextUrl = searchParams.get('next');
          if (current.role === 'ADMIN') routerNavigate('/admin');
          else if (current.role === 'SHOP' || current.role === 'SHOP_STAFF') routerNavigate('/shop-admin');
          else if (nextUrl) routerNavigate(nextUrl);
          else routerNavigate('/');
        }
      }
    }).catch(() => { }).finally(() => { if (active) setInitializing(false) })
    return () => { active = false }
  }, [])
  const [page, setPage] = useState('signin')
  const [email, setEmail] = useState('')
  const [fullName, setFullName] = useState('')
  const [registrationRole, setRegistrationRole] = useState('CUSTOMER')
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [otp, setOtp] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [captchaRequired, setCaptchaRequired] = useState(false)
  const [captcha, setCaptcha] = useState(null)
  const [captchaAnswer, setCaptchaAnswer] = useState('')
  const captchaBody = () => captchaRequired ? { captchaId: captcha?.id, captchaAnswer } : {}
  async function reloadCaptcha() {
    setCaptcha(null); setCaptchaAnswer('')
    try { setCaptcha(await post('captcha', { email: email.trim(), purpose: page === 'signin' ? 'login' : page === 'otp' ? 'register' : 'reset' })) }
    catch (failure) { setError(failure.message) }
  }
  async function failed(failure) {
    setError(failure.message)
    setFieldErrors(Object.fromEntries(Object.entries(failure.fieldErrors || {}).map(([key,value]) => [key.replace(/^account\./, '').replace(/^newPassword$/, 'password').replace(/^confirmPassword$/, 'confirmation'),value])))
    if (failure.captchaRequired || captchaRequired) { setCaptchaRequired(true); await reloadCaptcha() }
  }

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
    setFieldErrors({}); setCaptchaRequired(false); setCaptcha(null); setCaptchaAnswer('')
    setPage(next); setError(''); setNotice(''); setPassword(''); setConfirmation(''); setOtp('')
  }
  async function submit(event) {
    event.preventDefault()
    if (busy) return
    const validation = validateAuth(page, { email, fullName, password, confirmation, otp, registrationRole })
    setFieldErrors(validation)
    if (Object.keys(validation).length) return
    setBusy(true); setError(''); setNotice('')
    try {
      const address = email.trim().toLowerCase()
      if (page === 'signin') {
        const current = await authService.login(address, password, captchaBody())
        setUser(current); setPassword(''); setPage('account'); setNotice('Đăng nhập thành công.')
        const searchParams = new URLSearchParams(window.location.search);
        const nextUrl = searchParams.get('next');
        if (current.role === 'ADMIN') {
          routerNavigate('/admin');
        } else if (current.role === 'SHOP' || current.role === 'SHOP_STAFF') {
          routerNavigate('/shop-admin');
        } else if (nextUrl) {
          routerNavigate(nextUrl);
        } else {
          routerNavigate('/');
        }
      } else if (page === 'register') {
        if (password !== confirmation) throw new Error('Mật khẩu xác nhận không khớp.')
        if (new TextEncoder().encode(password).length > 72) throw new Error('Mật khẩu không được vượt quá 72 byte UTF-8.')
        const account = { email: address, password, fullName: fullName.trim() }
        const data = registrationRole === 'SHOP'
          ? await post('register-shop', { account })
          : await post('register', account)
        setEmail(address); setPassword(''); setConfirmation(''); setOtp('')
        setPage('otp'); setSeconds(0); setNotice(data.message)
      } else if (page === 'otp') {
        await post('verify-email', { email: address, otp, ...captchaBody() })
        setOtp(''); setPage('signin')
        setNotice('Đăng ký thành công! Email của bạn đã được xác thực.')
      } else if (page === 'forgot') {
        const data = await post('forgot-password', { email: address, ...captchaBody() })
        setEmail(address); setOtp(''); setPage('reset'); setSeconds(0); setNotice(data.message)
      } else if (page === 'reset') {
        if (password !== confirmation) throw new Error('Mật khẩu xác nhận không khớp.')
        if (new TextEncoder().encode(password).length > 72) throw new Error('Mật khẩu không được vượt quá 72 byte UTF-8.')
        await post('reset-password', { email: address, otp, newPassword: password, confirmPassword: confirmation, ...captchaBody() })
        authService.clear(); setUser(null)
        setPassword(''); setConfirmation(''); setOtp(''); setPage('signin')
        setNotice('Đặt lại mật khẩu thành công. Hãy dùng mật khẩu mới khi đăng nhập.')
      }
      setCaptchaRequired(false); setCaptcha(null); setCaptchaAnswer('')
    } catch (failure) { await failed(failure) }
    finally { setBusy(false) }
  }
  async function resend() {
    if (busy) return
    setBusy(true); setError(''); setNotice('')
    try {
      const data = await post(page === 'reset' ? 'forgot-password' : 'resend-verification', { email, ...captchaBody() })
      setOtp(''); setFieldErrors(current => { const next = {...current}; delete next.otp; return next })
      if (page === 'reset' && captchaRequired) await reloadCaptcha()
      else { setCaptchaRequired(false); setCaptcha(null); setCaptchaAnswer('') }
      setSeconds(0); setNotice(data.message)
    } catch (failure) { await failed(failure) }
    finally { setBusy(false) }
  }
  async function logout() {
    setBusy(true); setError('')
    try { await authService.logout(); setUser(null); navigate('signin'); window.location.replace('/login') }
    catch (failure) { setError(failure.message) }
    finally { setBusy(false) }
  }
  async function checkSession() {
    setBusy(true); setError('')
    try {
      const current = await authService.me()
      if (!current) throw new Error('Phiên đăng nhập không còn hiệu lực.')
      setUser(current); setNotice('Phiên đăng nhập đang hoạt động.')
    }
    catch (failure) { setUser(null); navigate('signin'); setError(failure.message) }
    finally { setBusy(false) }
  }
  const edit = (name, setter) => value => {
    setter(value)
    setFieldErrors(current => { const next = {...current}; delete next[name]; return next })
  }
  return { registrationRole, setRegistrationRole, fieldErrors, captchaRequired, captcha, captchaAnswer, setCaptchaAnswer, reloadCaptcha, page, email, fullName, password, confirmation, otp, busy: busy || initializing, error, notice, seconds, user, logout, checkSession, setEmail: edit('email', setEmail), setFullName: edit('fullName', setFullName), setPassword: edit('password', setPassword), setConfirmation: edit('confirmation', setConfirmation), setOtp: edit('otp', setOtp), submit, resend, navigate }
}
