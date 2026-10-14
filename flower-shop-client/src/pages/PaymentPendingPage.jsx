import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { useAuth } from '../context/AuthContext'
import { customerOrderService } from '../services/customerOrderService'
import paymentApi from '../api/paymentApi'
import { formatPrice } from '../models/productModel'
import PageMeta from '../components/common/PageMeta'
import PageBreadCrumb from '../components/common/PageBreadCrumb'
import Button from '../components/ui/button/Button'

// Màn "Chờ thanh toán": đơn online chưa trả (không mở được cổng, mất mạng, back trình duyệt...) được giữ 10 phút; khách trả lại hoặc để đơn tự hủy khi quá hạn.
export default function PaymentPendingPage() {
  const { user, busy: authBusy } = useAuth()
  const [searchParams] = useSearchParams()
  const ids = searchParams.getAll('order')
  const idsKey = ids.join(',')

  const [orders, setOrders] = useState(null)
  const [error, setError] = useState('')
  const [paying, setPaying] = useState(false)
  const [left, setLeft] = useState(0)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    if (!user || !idsKey) return undefined
    let active = true
    customerOrderService.byIds(idsKey.split(','))
      .then(data => { if (active) { setOrders(data); setError('') } })
      .catch(e => { if (active) setError(e.message || 'Không tải được đơn hàng.') })
    return () => { active = false }
  }, [user, idsKey, reloadKey])

  const payable = (orders || []).filter(o => o.paymentSecondsLeft != null)
  const paid = (orders || []).filter(o => o.paymentStatus === 'SUCCESS' && o.status !== 'CANCELLED')
  const cancelledOrders = (orders || []).filter(o => o.status === 'CANCELLED')
  const serverLeft = payable.length ? Math.min(...payable.map(o => o.paymentSecondsLeft)) : 0

  useEffect(() => { setLeft(serverLeft) }, [serverLeft])
  useEffect(() => {
    if (!payable.length) return undefined
    if (left > 0) {
      const timer = setTimeout(() => setLeft(value => value - 1), 1000)
      return () => clearTimeout(timer)
    }
    // Hết hạn: scheduler hủy đơn trong ~30 giây, tải lại sau đó để thấy trạng thái thật.
    const timer = setTimeout(() => setReloadKey(key => key + 1), 35000)
    return () => clearTimeout(timer)
  }, [left, payable.length])

  const total = payable.reduce((sum, o) => sum + Number(o.totalAmount || 0), 0)
  const pay = async () => {
    setPaying(true)
    setError('')
    try {
      const orderIds = payable.map(o => o.id)
      if (orderIds.length === 1) await paymentApi.startSePayCheckout(orderIds[0], 'FULL')
      else await paymentApi.startSePayGroupCheckout(orderIds)
    } catch (e) {
      setError(e.message || 'Không thể tạo thanh toán. Vui lòng thử lại.')
      setPaying(false)
    }
  }

  const mm = String(Math.floor(left / 60)).padStart(2, '0')
  const ss = String(left % 60).padStart(2, '0')
  const body = () => {
    if (!idsKey) return <p className="text-center text-gray-500">Không có đơn hàng nào để thanh toán. <Link to="/orders" className="font-semibold text-brand-600 underline">Xem đơn hàng</Link></p>
    if (authBusy) return <p role="status" className="text-center text-gray-500">Đang kiểm tra đăng nhập…</p>
    if (!user) return <p className="text-center text-gray-700">Vui lòng <Link to={`/login?next=${encodeURIComponent(`/payment/pending?${searchParams.toString()}`)}`} className="font-semibold text-brand-600 underline">đăng nhập</Link> để tiếp tục thanh toán.</p>
    if (!orders) return error ? <p role="alert" className="text-center text-error-600">{error}</p> : <p role="status" className="text-center text-gray-500">Đang tải đơn hàng…</p>
    return <>
      <div className="space-y-3">
        {orders.map(o => (
          <div key={o.id} className="flex items-center justify-between gap-3 rounded-xl border border-gray-100 p-4 text-sm dark:border-white/5">
            <div className="min-w-0">
              <p className="font-semibold text-gray-900 dark:text-white">🏪 {o.shopName}</p>
              <p className="text-xs text-gray-500">#{o.id.substring(0, 8).toUpperCase()} · {o.itemCount} sản phẩm · phí ship {formatPrice(o.shippingFee)}</p>
            </div>
            <div className="text-right">
              <p className="font-bold text-brand-600 dark:text-brand-400">{formatPrice(o.totalAmount)}</p>
              <p className="text-xs text-gray-500">{o.status === 'CANCELLED' ? 'Đã hủy' : o.paymentStatus === 'SUCCESS' ? 'Đã thanh toán' : o.paymentSecondsLeft != null ? 'Chờ thanh toán' : o.status}</p>
            </div>
          </div>
        ))}
      </div>
      {payable.length > 0 && left > 0 && (
        <div className="mt-6 space-y-4 text-center">
          <p className="text-sm text-gray-600 dark:text-gray-300">Thời gian giữ đơn còn lại</p>
          <p className="font-mono text-4xl font-extrabold text-warning-600" aria-live="off">{mm}:{ss}</p>
          {error && <p role="alert" className="rounded-lg bg-error-50 p-3 text-sm text-error-600">{error}</p>}
          <Button onClick={pay} disabled={paying} className="w-full justify-center py-3.5 text-base font-bold">{paying ? 'Đang chuyển sang cổng thanh toán…' : `Thanh toán ${formatPrice(total)}`}</Button>
          <p className="text-xs text-gray-400">Quá thời gian trên, đơn sẽ tự động hủy và sản phẩm được trả về giỏ hàng.</p>
        </div>
      )}
      {payable.length > 0 && left <= 0 && (
        <p role="alert" className="mt-6 rounded-lg bg-error-50 p-4 text-center text-sm text-error-600">Đã hết thời gian thanh toán. Đơn hàng sẽ được hủy tự động trong giây lát.</p>
      )}
      {!payable.length && paid.length > 0 && (
        <p role="status" className="mt-6 rounded-lg bg-success-50 p-4 text-center text-sm text-success-700">Thanh toán đã được ghi nhận. Cảm ơn bạn!</p>
      )}
      {!payable.length && !paid.length && cancelledOrders.length > 0 && (
        <p role="alert" className="mt-6 rounded-lg bg-error-50 p-4 text-center text-sm text-error-600">Đơn hàng đã bị hủy do quá thời gian thanh toán. Sản phẩm đã được trả về giỏ hàng.</p>
      )}
      <div className="mt-6 flex flex-wrap justify-center gap-3">
        <Link to="/orders"><Button variant="outline">Xem đơn hàng của tôi</Button></Link>
        {!payable.length && <Link to="/cart"><Button>Về giỏ hàng</Button></Link>}
      </div>
    </>
  }

  return (
    <div className="mx-auto w-full max-w-xl p-4 sm:p-6 lg:p-8">
      <PageMeta title="Chờ thanh toán | Cửa hàng hoa" description="Hoàn tất thanh toán đơn hàng" />
      <PageBreadCrumb pageTitle="Chờ thanh toán" />
      <div className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm dark:border-white/5 dark:bg-white/3 sm:p-8">
        <h1 className="mb-5 text-center text-xl font-bold text-gray-900 dark:text-white">Hoàn tất thanh toán đơn hàng</h1>
        {body()}
      </div>
    </div>
  )
}
