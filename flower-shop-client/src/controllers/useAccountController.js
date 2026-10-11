import { useEffect, useState } from 'react'
import { accountService } from '../services/accountService'
import { emptyAddress, profileInput } from '../models/accountModel'

export function useAccountController(user) {
  const [profile, setProfile] = useState(null)
  const [form, setForm] = useState({ fullName: '', phone: '' })
  const [addresses, setAddresses] = useState([])
  const [addressesLoaded, setAddressesLoaded] = useState(false)
  const [address, setAddress] = useState(emptyAddress())
  const [editing, setEditing] = useState(null)
  const [busy, setBusy] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [reload, setReload] = useState(0)
  const [serverPhoneError, setServerPhoneError] = useState('')
  const phoneError = form.phone && !/^0[0-9]{9}$/.test(form.phone)
    ? 'Số điện thoại phải bắt đầu bằng 0 và có đúng 10 chữ số' : serverPhoneError
  useEffect(() => {
    let active = true
    setBusy(true); setError(''); setAddressesLoaded(false)
    Promise.allSettled([accountService.profile(), user.role === 'CUSTOMER' ? accountService.addresses() : Promise.resolve([])])
      .then(([profileResult, addressesResult]) => {
        if (!active) return
        if (profileResult.status === 'fulfilled' && profileResult.value) {
          setProfile(profileResult.value)
          setForm(profileInput(profileResult.value))
        } else {
          setProfile(null)
        }
        if (addressesResult.status === 'fulfilled') {
          setAddresses(Array.isArray(addressesResult.value) ? addressesResult.value : [])
          setAddressesLoaded(true)
        } else {
          setAddresses([])
        }
        const failures = [profileResult, addressesResult]
          .filter(result => result.status === 'rejected')
          .map(result => result.reason?.message || 'Không tải được dữ liệu tài khoản.')
        if (profileResult.status === 'fulfilled' && !profileResult.value) failures.push('Không tải được hồ sơ cá nhân.')
        if (failures.length) setError(failures.join(' '))
        setAddress(emptyAddress()); setEditing(null)
      })
      .finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [user.id, user.role, reload])
  async function run(action) {
    setBusy(true); setError(''); setNotice('')
    try { await action(); return true } catch (e) {
      if (e.fieldErrors?.phone) setServerPhoneError(e.fieldErrors.phone)
      else setError(e.message)
      return false
    } finally { setBusy(false) }
  }
  const cancel = () => { setEditing(null); setAddress(emptyAddress()) }
  const saveProfile = e => {
    e.preventDefault()
    if (phoneError) return
    return run(async () => {
      const p = await accountService.updateProfile({ fullName: form.fullName.trim(), phone: form.phone.trim() })
      setProfile(p); setForm(profileInput(p)); setNotice('Đã cập nhật hồ sơ.')
    })
  }
  const saveAddress = e => {
    e.preventDefault()
    return run(async () => {
      await accountService.saveAddress(editing, { ...address, recipientName: address.recipientName.trim(), recipientPhone: address.recipientPhone.trim(), addressLine: address.addressLine.trim(), ward: address.ward.trim(), district: address.district.trim(), city: address.city.trim() })
      setAddresses(await accountService.addresses()); cancel(); setNotice('Đã lưu địa chỉ.')
    })
  }
  const uploadAvatar = file => run(async () => {
    const updated = await accountService.uploadAvatar(file)
    setProfile(updated)
    setNotice('Đã cập nhật ảnh đại diện.')
  })
  const remove = item => {
    if (!window.confirm(`Xóa địa chỉ ${item.addressLine}?`)) return
    return run(async () => {
      await accountService.deleteAddress(item.id)
      setAddresses(await accountService.addresses()); if (editing === item.id) cancel()
      setNotice('Đã xóa địa chỉ.')
    })
  }
  const makeDefault = id => run(async () => {
    await accountService.setDefault(id); setAddresses(await accountService.addresses()); setNotice('Đã đổi địa chỉ mặc định.')
  })
  const edit = item => {
    setEditing(item.id)
    setAddress({ ...emptyAddress(), ...item, recipientName: item.recipientName || profile.fullName || '', recipientPhone: item.recipientPhone || profile.phone || '', addressType: item.addressType || 'HOME' })
    setError(''); setNotice('')
  }
  return {
    profile, form, setForm: next => { setServerPhoneError(''); setForm(next) }, phoneError, addresses, addressesLoaded, address, setAddress, editing, busy, error, notice,
    saveProfile, saveAddress, uploadAvatar, remove, makeDefault, edit, cancel, retry: () => setReload(x => x + 1)
  }
}
