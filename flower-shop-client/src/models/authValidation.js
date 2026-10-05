export function validateAuth(page, values) {
  const errors = {}

  if (page !== 'otp' && page !== 'reset') {
    const email = values.email.trim()
    if (!email) errors.email = 'Vui lòng nhập email.'
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) errors.email = 'Email không đúng định dạng, ví dụ: ten@gmail.com.'
    else if (email.length > (page === 'register' ? 50 : 255)) errors.email = 'Email vượt quá độ dài cho phép.'
  }
  if (page === 'register' && !values.fullName.trim()) errors.fullName = 'Vui lòng nhập họ tên.'
  if (['register', 'reset', 'signin'].includes(page)) {
    if (!values.password) errors.password = 'Vui lòng nhập mật khẩu.'
    else if (page === 'register') {
      const missing = []
      if (values.password.length < 9 || values.password.length > 15) missing.push('từ 9 đến 15 ký tự')
      if (!/[A-Z]/.test(values.password)) missing.push('ít nhất 1 chữ hoa')
      if (!/[a-z]/.test(values.password)) missing.push('ít nhất 1 chữ thường')
      if (!/[0-9]/.test(values.password)) missing.push('ít nhất 1 số')
      if (missing.length) errors.password = `Mật khẩu cần ${missing.join(', ')}.`
    } else if (page === 'reset' && values.password.length < 15) errors.password = 'Mật khẩu mới cần ít nhất 15 ký tự.'
    if (page !== 'signin' && new TextEncoder().encode(values.password).length > 72) errors.password = 'Mật khẩu không được vượt quá 72 byte UTF-8.'
  }
  if (page === 'register' || page === 'reset') {
    if (!values.confirmation) errors.confirmation = 'Vui lòng nhập lại mật khẩu.'
    else if (values.password !== values.confirmation) errors.confirmation = 'Mật khẩu xác nhận không khớp.'
  }
  if (['otp', 'reset'].includes(page) && !/^\d{6}$/.test(values.otp)) errors.otp = 'Vui lòng nhập đủ 6 chữ số OTP.'
  return errors
}
