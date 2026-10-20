import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { useAuth } from '../context/AuthContext'
import { customerOrderService } from '../services/customerOrderService'
import { formatPrice } from '../models/productModel'
import PageMeta from '../components/common/PageMeta'
import Button from '../components/ui/button/Button'

// Màn "Đặt hàng thành công" cho đơn COD (mỗi shop một đơn).
export default function CodSuccessPage() {
  const { user } = useAuth()
  const [searchParams] = useSearchParams()
  const idsKey = searchParams.getAll('order').join(',')
  const [orders, setOrders] = useState([])

  useEffect(() => {
    if (!user || !idsKey) return undefined
    let active = true
    customerOrderService.byIds(idsKey.split(',')).then(data => { if (active) setOrders(data) }).catch(() => {})
    return () => { active = false }
  }, [user, idsKey])

  const total = orders.reduce((sum, o) => sum + Number(o.totalAmount || 0), 0)
  return (
    <div className="mx-auto w-full max-w-2xl p-4 sm:p-8">
      <PageMeta title="Đặt hàng thành công | Cửa hàng hoa" description="Đơn hàng COD đã được tạo" />
      <div className="rounded-2xl border border-success-200 bg-white p-8 text-center shadow-sm dark:border-success-500/20 dark:bg-white/3">
        <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-success-500 text-2xl text-white">✓</div>
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">{orders.length > 1 ? `Đã tạo ${orders.length} đơn hàng thành công!` : 'Đặt hàng thành công!'}</h1>
        <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">Vui lòng chuẩn bị tiền mặt khi nhận hoa.</p>
        <div className="mt-6 space-y-2 text-left">
          {orders.map(o => (
            <Link key={o.id} to={`/orders/${o.id}`} className="flex items-center justify-between gap-3 rounded-xl border border-gray-100 p-4 text-sm hover:border-brand-300 dark:border-white/5">
              <span><strong className="block text-gray-900 dark:text-white">🏪 {o.shopName}</strong><span className="font-mono text-xs text-gray-500">#{o.id.substring(0, 8).toUpperCase()}</span></span>
              <strong className="text-brand-600 dark:text-brand-400">{formatPrice(o.totalAmount)}</strong>
            </Link>
          ))}
        </div>
        {orders.length > 1 && <p className="mt-3 text-right text-sm text-gray-600 dark:text-gray-300">Tổng cộng: <strong>{formatPrice(total)}</strong></p>}
        <div className="mt-6 flex flex-wrap justify-center gap-3">
          <Link to="/orders"><Button variant="outline">Xem đơn hàng của tôi</Button></Link>
          <Link to="/"><Button>Tiếp tục mua sắm</Button></Link>
        </div>
      </div>
    </div>
  )
}
