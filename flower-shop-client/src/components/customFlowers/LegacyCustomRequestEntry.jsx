import { useEffect, useState } from 'react'
import { Link, Navigate, useSearchParams } from 'react-router-dom'
import { customFlowerService } from '../../services/customFlowerService'
import { CustomRequestCard } from './CustomFlowerParts'

// Old chat bookmarks remain readable; all new designs use the ordinary order workflow.
export default function LegacyCustomRequestEntry({ user, shop, authBusy = false }) {
  const [params] = useSearchParams()
  const id = params.get('open') || ''
  const [request, setRequest] = useState(null)
  const [error, setError] = useState('')
  const isShop = Boolean(shop)
  useEffect(() => {
    let active = true
    setRequest(null); setError('')
    if (!id || !user || user.role !== (isShop ? 'SHOP' : 'CUSTOMER')) return
    const result = isShop ? customFlowerService.shopDetail(shop.id, id) : customFlowerService.detail(id)
    result.then(value => { if (active) setRequest(value) }).catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [id, user?.id, user?.role, shop?.id, isShop])
  if (!id) return <Navigate replace to={isShop ? '/shop-admin/orders' : '/custom-orders/new'} />
  if (authBusy && !user) return <p role="status">Đang kiểm tra đăng nhập…</p>
  if (!user) return <p>Vui lòng <Link to={`/login?next=${encodeURIComponent('/custom-orders?open=' + id)}`}>đăng nhập</Link> để xem yêu cầu.</p>
  if (user.role !== (isShop ? 'SHOP' : 'CUSTOMER')) return <p>Bạn không có quyền xem yêu cầu này.</p>
  if (error) return <p role="alert" className="message error">{error}</p>
  if (!request) return <p role="status">Đang tải yêu cầu trước đây…</p>
  if (request.orderId) return <Navigate replace to={isShop ? `/shop-admin/orders?open=${encodeURIComponent(request.orderId)}` : `/orders/${encodeURIComponent(request.orderId)}`} />
  return <main className="catalog-page custom-flower-page"><h1>Yêu cầu hoa trước đây</h1>
    <p className="my-4">Đây là yêu cầu lưu theo luồng cũ, hiện chỉ xem lại. Yêu cầu mới được báo giá, thanh toán và giao hàng trong quản lý đơn hàng.</p>
    <CustomRequestCard request={request} />
    <Link className="mt-4 inline-block text-brand-600" to={isShop ? '/shop-admin/orders' : `/custom-orders/new?${new URLSearchParams({ shopId: request.shopId })}`}>{isShop ? 'Về quản lý đơn hàng' : 'Tạo yêu cầu mới'}</Link>
  </main>
}
