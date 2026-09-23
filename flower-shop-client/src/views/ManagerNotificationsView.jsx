import { useEffect, useState } from 'react'
import { managerShopService } from '../services/managerShopService'
import { staffApplicationService } from '../services/staffApplicationService'
export default function ManagerNotificationsView() {
  const [count, setCount] = useState(0)
  const [error, setError] = useState('')
  useEffect(() => {
    let active = true
    let timer
    async function load() {
      try {
        const shops = await managerShopService.list('SHOP')
        const lists = await Promise.all(shops.map(s => staffApplicationService.list(s.id)))
        if (active) { setCount(lists.flat().filter(a => a.status === 'PENDING').length); setError('') }
      } catch (e) { if (active) setError(e.message) }
      if (active) timer = setTimeout(load, 15000)
    }
    load()
    return () => { active = false; clearTimeout(timer) }
  }, [])
  return <section className="account-card"><a href="/shop-admin?notifications=1">Thông báo đăng ký làm nhân viên ({count} đơn chờ duyệt)</a>{error && <p role="alert">{error}</p>}</section>
}
