import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { managerShopService } from '../services/managerShopService'
import PageBreadCrumb from '../components/common/PageBreadCrumb'
import PageMeta from '../components/common/PageMeta'
import VoucherManager from '../components/vouchers/VoucherManager'

export default function ShopVouchersView() {
  const { user } = useAuth()
  const [shop, setShop] = useState(null)
  const [loaded, setLoaded] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (user?.role !== 'SHOP') return
    let active = true
    managerShopService.list('SHOP').then(items => { if (active) setShop(items[0] || null) })
      .catch(e => { if (active) setError(e.message) }).finally(() => { if (active) setLoaded(true) })
    return () => { active = false }
  }, [user?.id, user?.role])

  if (user?.role !== 'SHOP') return <p className="p-6 text-gray-500">Chỉ chủ shop được quản lý mã giảm giá của shop.</p>
  return (
    <>
      <PageMeta title="Mã giảm giá | Quản lý shop" description="Quản lý mã giảm giá của cửa hàng" />
      <PageBreadCrumb pageTitle="Mã giảm giá" />
      {error && <div role="alert" className="mb-6 rounded-lg bg-error-50 p-4 text-sm text-error-600">{error}</div>}
      {!loaded ? <p role="status" className="text-gray-500">Đang tải…</p>
        : !shop ? <p className="text-gray-500">Bạn chưa có cửa hàng để tạo mã giảm giá.</p>
        : <VoucherManager scope={{ shopId: shop.id }}
            disabledReason={shop.status !== 'ACTIVE' ? 'Chỉ cửa hàng đang hoạt động mới được tạo hoặc sửa mã giảm giá.' : ''} />}
    </>
  )
}
