export function validateEmail(value, maxLength = 255) {
  const email = value.trim()
  if (!email) return 'Vui lòng nhập email.'
  if (email.length > maxLength) return `Email không được vượt quá ${maxLength} ký tự.`
  const parts = email.split('@')
  const local = parts[0]
  const domain = parts[1]
  if (parts.length !== 2 || local.length > 64 || !/^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*$/.test(local)
      || !domain || domain.length > 253 || !domain.includes('.')
      || domain.split('.').some(label => !/^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?$/.test(label))) {
    return 'Email không đúng định dạng, ví dụ: ten@example.com.'
  }
  return ''
}
