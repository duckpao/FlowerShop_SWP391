import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { useAuth } from '../context/AuthContext'
import { customerOrderService } from '../services/customerOrderService'
import paymentService from '../services/paymentService'
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
    DELIVERING: 'Đang giao', COMPLETED: 'Hoàn thành', CANCELLED: 'Đã hủy', REFUNDED: 'Hoàn hàng',
}
const tabs = [
    ['', 'Tất cả'], ['PENDING', 'Chờ thanh toán'], ['PROCESSING', 'Đang chuẩn bị'],
    ['DELIVERING', 'Đang giao'], ['COMPLETED', 'Hoàn thành'], ['CANCELLED', 'Đã hủy'],
]

const statusLabel = o => o.status === 'PENDING' && o.paymentMethod === 'ONLINE' ? 'Chờ thanh toán' : statusLabels[o.status] || o.status
const paymentLabel = o => {
    if (o.status === 'AWAITING_DEPOSIT') return 'Online · Chờ đặt cọc'
    if (o.status === 'DEPOSIT_PAID') return 'Đã đặt cọc online · Phần còn lại trả khi nhận hàng'
    if (o.paymentMethod === 'COD') return 'Thanh toán khi nhận hàng (COD)'
    if (o.paymentMethod === 'ONLINE') return o.paymentStatus === 'SUCCESS' ? 'Online · Đã thanh toán' : 'Online · Chưa thanh toán'
    return 'Chưa chọn phương thức'
}

// Đếm ngược thời hạn thanh toán (giây còn lại do server tính). Hết hạn thì ẩn nút; server tự hủy đơn trong ít giây.
function PayAction({ order, paying, onPay, onExpire }) {
    const [left, setLeft] = useState(order.paymentSecondsLeft)
    useEffect(() => { setLeft(order.paymentSecondsLeft) }, [order.paymentSecondsLeft])
    useEffect(() => {
        if (left > 0) {
            const timer = setTimeout(() => setLeft(value => value - 1), 1000)
            return () => clearTimeout(timer)
        }
        onExpire()
    }, [left])
    if (left <= 0) return <span className="text-sm text-error-500">Đã hết hạn thanh toán, đơn sẽ tự hủy</span>
    const mm = String(Math.floor(left / 60)).padStart(2, '0')
    const ss = String(left % 60).padStart(2, '0')
    return <>
        <span className="text-sm text-warning-600">Còn <strong className="font-mono">{mm}:{ss}</strong> để thanh toán</span>
        <Button size="sm" disabled={paying} onClick={() => onPay(order)}>{paying ? 'Đang chuyển…' : 'Thanh toán'}</Button>
    </>
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

    // Đơn hết hạn được scheduler hủy trong ~30 giây; tải lại sau đó để thấy trạng thái "Đã hủy".
    const refreshSoon = () => { setTimeout(() => setReloadKey(key => key + 1), 35000) }
    const changeTab = value => { setStatus(value); setPage(0) }
    // Một lần thanh toán cho mọi đơn đang chờ của cùng lần đặt hàng (nhiều shop = một giao dịch SePay).
    const payGroup = async group => {
        setPayingId(group.id)
        setError('')
        try {
            if (group.ids.length === 1) await paymentService.startSePayCheckout(group.ids[0], 'FULL')
            else await paymentService.startSePayGroupCheckout(group.ids)
        } catch (e) {
            setError(e.message || 'Không thể tạo thanh toán. Vui lòng thử lại.')
            setPayingId('')
        }
    }
    // Đơn hoa theo yêu cầu: trả cọc online (AWAITING_DEPOSIT); phần còn lại GHN thu hộ khi giao. Số tiền do server tính.
    const payStep = async (order, type) => {
        setPayingId(order.id)
        setError('')
        try {
            await paymentService.startSePayCheckout(order.id, type)
        } catch (e) {
            setError(e.message || 'Không thể tạo thanh toán. Vui lòng thử lại.')
            setPayingId('')
        }
    }

    // Các đơn cùng checkoutGroup hiển thị như một đơn hàng gồm nhiều cửa hàng.
    const groups = []
    const byKey = new Map()
    for (const o of orders?.content || []) {
        const key = o.checkoutGroup || o.id
        if (!byKey.has(key)) { const g = { key, orders: [] }; byKey.set(key, g); groups.push(g) }
        byKey.get(key).orders.push(o)
    }
    const sum = (list, field) => list.reduce((total, o) => total + Number(o[field] || 0), 0)

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

            {groups.map(g => {
                const multi = g.orders.length > 1
                const payable = g.orders.filter(o => o.paymentSecondsLeft > 0)
                const secondsLeft = payable.length ? Math.min(...payable.map(o => o.paymentSecondsLeft)) : null
                const first = g.orders[0]
                return <article key={g.key} className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm dark:border-white/5 dark:bg-white/3">
                    {multi && (
                        <header className="flex flex-wrap items-center justify-between gap-2 border-b border-gray-100 bg-gray-50 px-5 py-3 dark:border-white/5 dark:bg-white/5">
                            <span className="text-sm font-bold text-gray-900 dark:text-white">Đơn hàng gồm {g.orders.length} cửa hàng</span>
                            <span className="font-mono text-xs text-gray-500">#{g.key.substring(0, 8).toUpperCase()}</span>
                        </header>
                    )}
                    {g.orders.map(o => (
                        <section key={o.id} className={multi ? 'border-b border-gray-100 last:border-b-0 dark:border-white/5' : ''}>
                            <div className="flex flex-wrap items-center justify-between gap-2 border-b border-gray-100 px-5 py-3 dark:border-white/5">
                                <span className="text-sm font-bold text-gray-900 dark:text-white">🏪 {o.shopName}</span>
                                <span className="flex items-center gap-3">
                                    <span className="font-mono text-xs text-gray-500">#{o.id.substring(0, 8).toUpperCase()}</span>
                                    <Badge color={statusColors[o.status] || 'primary'}>{statusLabel(o)}</Badge>
                                    <Link to={`/orders/${o.id}`} className="text-xs font-medium text-brand-600 hover:underline">Chi tiết</Link>
                                </span>
                            </div>
                            <Link to={`/orders/${o.id}`} className="block divide-y divide-gray-100 dark:divide-white/5">
                                {(o.items || []).map((item, index) => (
                                    <div key={`${item.productId}-${index}`} className="flex items-center gap-4 px-5 py-3">
                                        <div className="flex h-16 w-16 shrink-0 items-center justify-center overflow-hidden rounded-lg border border-gray-100 bg-gray-50 dark:border-white/5 dark:bg-gray-800">
                                            {item.imageUrl ? <img src={item.imageUrl} alt={item.productName} className="h-full w-full object-cover" /> : <span className="text-2xl">💐</span>}
                                        </div>
                                        <div className="min-w-0 flex-1">
                                            <p className="line-clamp-1 text-sm font-semibold text-gray-900 dark:text-white">{item.productName || (item.productId ? 'Sản phẩm' : 'Hoa đặt theo yêu cầu')}</p>
                                            <p className="text-xs text-gray-500">x{item.quantity}</p>
                                        </div>
                                        <span className="text-sm font-medium text-gray-700 dark:text-gray-300">{formatPrice(item.price)}</span>
                                    </div>
                                ))}
                            </Link>
                            {multi && <p className="px-5 pb-3 text-right text-xs text-gray-500">Phí ship {formatPrice(o.shippingFee)} · Thành tiền {formatPrice(o.totalAmount)}</p>}
                        </section>
                    ))}
                    <footer className="space-y-3 border-t border-gray-100 px-5 py-4 dark:border-white/5">
                        <div className="flex flex-wrap items-center justify-between gap-2 text-sm text-gray-500">
                            <span>{new Date(first.createdDate).toLocaleString('vi-VN')} · {paymentLabel(first)}</span>
                            <span>Tiền hàng {formatPrice(sum(g.orders, 'subTotal'))} + Phí ship {formatPrice(sum(g.orders, 'shippingFee'))}</span>
                        </div>
                        <div className="flex flex-wrap items-center justify-between gap-3">
                            <span className="text-sm text-gray-700 dark:text-gray-300">Thành tiền: <strong className="text-lg text-brand-600 dark:text-brand-400">{formatPrice(sum(g.orders, 'totalAmount'))}</strong></span>
                            <span className="flex flex-wrap items-center justify-end gap-2">
                                {secondsLeft != null && (
                                    <PayAction order={{ id: g.key, paymentSecondsLeft: secondsLeft, ids: payable.map(o => o.id) }}
                                        paying={payingId === g.key} onPay={payGroup} onExpire={refreshSoon} />
                                )}
                                {!multi && first.status === 'AWAITING_DEPOSIT' && (
                                    <Button size="sm" disabled={payingId === first.id} onClick={() => payStep(first, 'DEPOSIT')}>{payingId === first.id ? 'Đang chuyển…' : 'Đặt cọc'}</Button>
                                )}
                            </span>
                        </div>
                    </footer>
                </article>
            })}

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
