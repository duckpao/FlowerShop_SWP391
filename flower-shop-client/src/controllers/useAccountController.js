import { useEffect, useState } from 'react'
import { accountService } from '../services/accountService'
import { emptyAddress, profileInput } from '../models/accountModel'

export function useAccountController(user) {
  const [profile, setProfile] = useState(null)
  const [form, setForm] = useState({ fullName: '', phone: '' })
  const [addresses, setAddresses] = useState([])
  const [cities, setCities] = useState([])
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
    setBusy(true); setError('')
    Promise.all([accountService.profile(), ...(user.role === 'CUSTOMER' ? [accountService.addresses(), accountService.cities()] : [])])
      .then(([p, list = [], allowed = []]) => {
        if (!active) return
        setProfile(p); setForm(profileInput(p)); setAddresses(list); setCities(allowed)
        setAddress(emptyAddress(allowed[0] || '')); setEditing(null)
      }).catch(e => { if (active) setError(e.message) })
      .finally(() => { if (active) setBusy(false) })
    return () => { active = false }
  }, [user.id, user.role, reload])
  async function run(action) {
    setBusy(true); setError(''); setNotice('')
    try { await action() } catch (e) {
      if (e.fieldErrors?.phone) setServerPhoneError(e.fieldErrors.phone)
      else setError(e.message)
    } finally { setBusy(false) }
  }
  const cancel = () => { setEditing(null); setAddress(emptyAddress(cities[0] || '')) }
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
      await accountService.saveAddress(editing, { ...address, addressLine: address.addressLine.trim(), ward: address.ward.trim(), district: address.district.trim() })
      setAddresses(await accountService.addresses()); cancel(); setNotice('Đã lưu địa chỉ.')
    })
  }
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
    setAddress({ addressLine: item.addressLine || '', city: item.city || '', district: item.district || '', ward: item.ward || '', isDefault: item.isDefault })
    setError(''); setNotice('')
  }
  return { profile, form, setForm: next => { setServerPhoneError(''); setForm(next) }, phoneError, addresses, cities, address, setAddress, editing, busy, error, notice,
    saveProfile, saveAddress, remove, makeDefault, edit, cancel, retry: () => setReload(x => x + 1) }
}
