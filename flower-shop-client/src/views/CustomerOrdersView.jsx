import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { useAuth } from '../context/AuthContext'
import { customerOrderService } from '../services/customerOrderService'
import paymentApi from '../api/paymentApi'
import { formatPrice } from '../models/productModel'
import Badge from '../components/ui/badge/Badge'
import Button from '../components/ui/button/Button'
import PageMeta from '../components/common/PageMeta'
import PageBreadCrumb from '../components/common/PageBreadCrumb'

const statusColors = {
    PENDING: 'warning', AWAITING_DEPOSIT: 'warning', DEPOSIT_PAID: 'info', PROCESSING: 'info', DELIVERING: 'info',
    COMPLETED: 'success', CANCELLED: 'error', REFUNDED: 'error',
}
const statusLabels = {
    PENDING: 'Chờ xác nhận', AWAITING_DEPOSIT: 'Chờ đặt cọc', DEPOSIT_PAID: 'Đã đặt cọc', PROCESSING: 'Đang chuẩn bị',
    DELIVERING: 'Đang giao', COMPLETED: 'Hoàn thành', CANCELLED: 'Đã hủy', REFUNDED: 'Đã hoàn tiền',
}
const tabs = [
    ['', 'Tất cả'], ['PENDING', 'Chờ thanh toán'], ['PROCESSING', 'Đang chuẩn bị'],
    ['DELIVERING', 'Đang giao'], ['COMPLETED', 'Hoàn thành'], ['CANCELLED', 'Đã hủy'],
]

const statusLabel = o => o.status === 'PENDING' && o.paymentMethod === 'ONLINE' ? 'Chờ thanh toán' : statusLabels[o.status] || o.status
const paymentLabel = o => {
    if (o.paymentMethod === 'COD') return 'Thanh toán khi nhận hàng (COD)'
    if (o.paymentMethod === 'ONLINE') return o.paymentStatus === 'SUCCESS' ? 'Online · Đã thanh toán' : 'Online · Chưa thanh toán'
    return 'Chưa chọn phương thức'
}

export default function CustomerOrdersView() {
    const { user } = useAuth()
    const [orders, setOrders] = useState(null)
    const [page, setPage] = useState(0)
    const [status, setStatus] = useState('')
    const [busy, setBusy] = useState(true)
    const [error, setError] = useState('')
    const [payingId, setPayingId] = useState('')
    const [reloadKey, setReloadKey] = useState(0)

    useEffect(() => {
        if (!user) return
        let active = true
        setBusy(true)
        setError('')
        customerOrderService.list(page, status)
            .then(data => { if (active) { setOrders(data); setError('') } })
            .catch(e => { if (active) setError(e.message) })
            .finally(() => { if (active) setBusy(false) })
        return () => { active = false }
    }, [user, page, status, reloadKey])

    const changeTab = value => { setStatus(value); setPage(0) }
    const payAgain = async order => {
        setPayingId(order.id)
        setError('')
        try {
            await paymentApi.startSePayCheckout(order.id, 'FULL')
        } catch (e) {
            setError(e.message || 'Không thể tạo thanh toán. Vui lòng thử lại.')
            setPayingId('')
        }
    }

    if (!user) return <div className="max-w-4xl mx-auto p-8"><h1 className="text-xl font-bold">Đơn hàng của tôi</h1><p className="text-gray-500">Vui lòng đăng nhập.</p></div>

    return <div className="w-full">
        <PageMeta title="Đơn hàng của tôi | FlowerShop" />
        <PageBreadCrumb pageTitle="Đơn hàng của tôi" />
        <div className="mx-auto max-w-4xl space-y-4">
            <div role="tablist" aria-label="Lọc theo trạng thái" className="flex gap-1 overflow-x-auto rounded-xl border border-gray-200 bg-white p-1 dark:border-white/5 dark:bg-white/3">
                {tabs.map(([value, label]) => (
                    <button key={value} type="button" role="tab" aria-selected={status === value} onClick={() => changeTab(value)}
                        className={`shrink-0 rounded-lg px-4 py-2 text-sm font-medium transition-colors ${status === value ? 'bg-brand-500 text-white' : 'text-gray-600 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-white/10'}`}>
                        {label}
                    </button>
                ))}
            </div>

            {error && <div role="alert" className="rounded-lg bg-error-50 p-4 text-sm text-error-500">{error}</div>}
            {busy && <p role="status" className="py-6 text-center text-gray-500">Đang tải…</p>}
            {!busy && orders && orders.content.length === 0 && (
                <div className="rounded-xl border border-gray-200 bg-white p-10 text-center text-gray-500 dark:border-white/5 dark:bg-white/3">Chưa có đơn hàng nào.</div>
            )}

            {orders && orders.content.map(o => (
                <article key={o.id} className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm dark:border-white/5 dark:bg-white/3">
                    <header className="flex flex-wrap items-center justify-between gap-2 border-b border-gray-100 bg-gray-50 px-5 py-3 dark:border-white/5 dark:bg-white/5">
                        <span className="text-sm font-bold text-gray-900 dark:text-white">🏪 {o.shopName}</span>
                        <span className="flex items-center gap-3">
                            <span className="font-mono text-xs text-gray-500">#{o.id.substring(0, 8).toUpperCase()}</span>
                            <Badge color={statusColors[o.status] || 'primary'}>{statusLabel(o)}</Badge>
                        </span>
                    </header>
                    <Link to={`/orders/${o.id}`} className="block divide-y divide-gray-100 dark:divide-white/5">
                        {(o.items || []).map((item, index) => (
                            <div key={`${item.productId}-${index}`} className="flex items-center gap-4 px-5 py-3">
                                <div className="flex h-16 w-16 shrink-0 items-center justify-center overflow-hidden rounded-lg border border-gray-100 bg-gray-50 dark:border-white/5 dark:bg-gray-800">
                                    {item.imageUrl ? <img src={item.imageUrl} alt={item.productName} className="h-full w-full object-cover" /> : <span className="text-2xl">💐</span>}
                                </div>
                                <div className="min-w-0 flex-1">
                                    <p className="line-clamp-1 text-sm font-semibold text-gray-900 dark:text-white">{item.productName || 'Sản phẩm'}</p>
                                    <p className="text-xs text-gray-500">x{item.quantity}</p>
                                </div>
                                <span className="text-sm font-medium text-gray-700 dark:text-gray-300">{formatPrice(item.price)}</span>
                            </div>
                        ))}
                    </Link>
                    <footer className="space-y-3 border-t border-gray-100 px-5 py-4 dark:border-white/5">
                        <div className="flex flex-wrap items-center justify-between gap-2 text-sm text-gray-500">
                            <span>{new Date(o.createdDate).toLocaleString('vi-VN')} · {paymentLabel(o)}</span>
                            <span>Tiền hàng {formatPrice(o.subTotal)} + Phí ship {formatPrice(o.shippingFee)}</span>
                        </div>
                        <div className="flex flex-wrap items-center justify-between gap-3">
                            <span className="text-sm text-gray-700 dark:text-gray-300">Thành tiền: <strong className="text-lg text-brand-600 dark:text-brand-400">{formatPrice(o.totalAmount)}</strong></span>
                            <span className="flex gap-2">
                                {o.status === 'PENDING' && o.paymentMethod === 'ONLINE' && o.paymentStatus !== 'SUCCESS' && (
                                    <Button size="sm" disabled={Boolean(payingId)} onClick={() => payAgain(o)}>{payingId === o.id ? 'Đang chuyển…' : 'Thanh toán'}</Button>
                                )}
                                <Link to={`/orders/${o.id}`}><Button size="sm" variant="outline">Xem chi tiết</Button></Link>
                            </span>
                        </div>
                    </footer>
                </article>
            ))}

            {orders && orders.totalPages > 1 && (
                <div className="flex items-center justify-between">
                    <Button size="sm" variant="outline" disabled={busy || page === 0} onClick={() => setPage(p => p - 1)}>Trang trước</Button>
                    <span className="text-sm text-gray-500">Trang {page + 1} / {orders.totalPages}</span>
                    <Button size="sm" variant="outline" disabled={busy || page + 1 >= orders.totalPages} onClick={() => setPage(p => p + 1)}>Trang sau</Button>
                </div>
            )}
            <div className="flex justify-center">
                <Button size="sm" variant="outline" disabled={busy} onClick={() => setReloadKey(key => key + 1)}>{busy ? 'Đang tải…' : 'Tải lại'}</Button>
            </div>
        </div>
    </div>
}
