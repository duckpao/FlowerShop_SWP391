import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router'
import { useAuth } from '../context/AuthContext'
import { customerOrderService } from '../services/customerOrderService'
import { formatPrice } from '../models/productModel'
import Badge from '../components/ui/badge/Badge'
import ComponentCard from '../components/common/ComponentCard'
import PageMeta from '../components/common/PageMeta'
import PageBreadCrumb from '../components/common/PageBreadCrumb'

const statusColors = {
    PENDING:'warning', PROCESSING:'info', DELIVERING:'info', COMPLETED:'success', CANCELLED:'error', REFUNDED:'error'
}
const statusLabels = {
    PENDING:'Chờ xác nhận', PROCESSING:'Đang chuẩn bị', DELIVERING:'Đang giao', COMPLETED:'Đã giao', CANCELLED:'Đã hủy', REFUNDED:'Đã hoàn tiền'
}
const trackingSteps = [
    'Cửa hàng đang chuẩn bị đơn hàng',
    'Cửa hàng đã giao hàng cho đơn vị vận chuyển',
    'Đơn vị vận chuyển đã vận chuyển thành công đến khách hàng',
]

function getTrackingStage(order) {
    const deliveryStatus = order.delivery?.status
    if (deliveryStatus === 'DELIVERED') return 3
    if (['PICKED_UP', 'ON_THE_WAY'].includes(deliveryStatus)) return 2
    if (order.status === 'DELIVERING') return 1
    if (order.status === 'PROCESSING') return 0
    return -1
}

function getTrackingMessage(order) {
    if (order.status === 'CANCELLED') return 'Đơn hàng đã bị hủy.'
    if (order.status === 'REFUNDED') return 'Đơn hàng đã được hoàn tiền.'
    if (order.status === 'PENDING') return 'Đơn hàng đang chờ cửa hàng xác nhận.'
    if (order.delivery?.status === 'DELIVERED') return 'Đơn vị vận chuyển đã giao hàng thành công.'
    if (order.delivery?.status === 'ON_THE_WAY') return 'Đơn vị vận chuyển đang giao hàng đến bạn.'
    if (order.delivery?.status === 'PICKED_UP') return 'Cửa hàng đã bàn giao đơn hàng cho đơn vị vận chuyển.'
    if (order.delivery?.status === 'FAILED') return 'Đơn vị vận chuyển chưa thể giao đơn hàng.'
    if (order.status === 'DELIVERING') return 'Đã tạo vận đơn, đang chờ đơn vị vận chuyển tiếp nhận.'
    if (order.status === 'PROCESSING') return 'Cửa hàng đang chuẩn bị đơn hàng.'
    return 'Chưa có cập nhật vận chuyển.'
}

export default function CustomerOrderDetailView() {
    const { id } = useParams()
    const { user } = useAuth()
    const [order, setOrder] = useState(null)
    const [busy, setBusy] = useState(true)
    const [error, setError] = useState('')
    const [shippingFee, setShippingFee] = useState(null)
    const [shippingFeeError, setShippingFeeError] = useState('')
    const [trackingBusy, setTrackingBusy] = useState(false)
    const [trackingError, setTrackingError] = useState('')

    useEffect(() => {
        if (!user || !id) return
        let active = true
        setBusy(true)
        setShippingFeeError('')
        customerOrderService.detail(id)
            .then(data => {
                if (!active) return
                setOrder(data)
                setShippingFee(data.shippingFee)
                setError('')
                customerOrderService.shippingFee(id)
                    .then(result => { if (active) setShippingFee(result.shippingFee) })
                    .catch(() => { if (active) setShippingFeeError('Không lấy được phí GHN hiện tại; đang hiển thị phí đã lưu của đơn.') })
            })
            .catch(e => { if (active) setError(e.message) })
            .finally(() => { if (active) setBusy(false) })
        return () => { active = false }
    }, [user, id])

    const refreshTracking = async () => {
        setTrackingBusy(true)
        setTrackingError('')
        try {
            const delivery = await customerOrderService.refreshTracking(id)
            setOrder(current => ({
                ...current,
                delivery,
                ...(delivery.status === 'DELIVERED' ? { status: 'COMPLETED' } : {}),
            }))
        } catch (e) {
            setTrackingError(e.message || 'Không thể cập nhật trạng thái giao hàng.')
        } finally {
            setTrackingBusy(false)
        }
    }

    if (!user) return <div className="max-w-4xl mx-auto p-8"><p className="text-gray-500">Vui lòng đăng nhập.</p></div>
    if (busy) return <div className="max-w-4xl mx-auto p-8"><p className="text-gray-500">Đang tải…</p></div>
    if (error) return <div className="max-w-4xl mx-auto p-8"><p className="text-error-500">{error}</p></div>
    if (!order) return null

    const trackingStage = getTrackingStage(order)

    return <div className="w-full">
        <PageMeta title={`Đơn #${order.id.substring(0,8)} | FlowerShop`} />
        <PageBreadCrumb pageTitle={`Chi tiết đơn #${order.id.substring(0,8)}`} />
        <div className="max-w-3xl mx-auto space-y-6">
            <ComponentCard title={`Đơn hàng từ ${order.shopName}`}>
                <div className="flex justify-between items-center mb-4">
                    <Badge color={statusColors[order.status]||'primary'}>{statusLabels[order.status]||order.status}</Badge>
                    <span className="text-sm text-gray-500">{new Date(order.createdDate).toLocaleString('vi-VN')}</span>
                </div>

                <section className="mb-6 rounded-lg border border-gray-200 p-4 dark:border-white/10" aria-label="Theo dõi giao hàng">
                    <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                        <div>
                            <h2 className="font-semibold">Theo dõi đơn hàng</h2>
                            <p className="mt-1 text-sm text-gray-600 dark:text-gray-300" role="status">{getTrackingMessage(order)}</p>
                        </div>
                        {order.delivery && !['CANCELLED', 'REFUNDED'].includes(order.status) && (
                            <button type="button" onClick={refreshTracking} disabled={trackingBusy}
                                className="rounded-lg border border-gray-300 px-3 py-2 text-sm font-medium disabled:opacity-50 dark:border-white/10"
                            >
                                {trackingBusy ? 'Đang cập nhật…' : 'Cập nhật trạng thái GHN'}
                            </button>
                        )}
                    </div>
                    {trackingError && <p className="mb-3 text-sm text-error-500" role="alert">{trackingError}</p>}
                    <ol className="space-y-3">
                        {trackingSteps.map((label, index) => {
                            const complete = trackingStage === 3 || index < trackingStage
                            const current = index === trackingStage
                            return <li key={label} className="flex items-start gap-3">
                                <span className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-xs font-semibold ${
                                    complete ? 'bg-success-600 text-white' : current ? 'bg-brand-600 text-white' : 'border border-gray-300 text-gray-500 dark:border-gray-600'
                                }`} aria-label={complete ? 'Hoàn tất' : current ? 'Đang xử lý' : 'Chưa hoàn tất'}>
                                    {complete ? '✓' : index + 1}
                                </span>
                                <span className={`text-sm ${complete || current ? 'font-medium text-gray-900 dark:text-white' : 'text-gray-500 dark:text-gray-400'}`}>{label}</span>
                            </li>
                        })}
                    </ol>
                </section>

                <div className="border rounded-lg overflow-hidden mb-4">
                    <table className="min-w-full">
                        <thead><tr className="bg-gray-50 dark:bg-white/5">
                            <th className="px-4 py-2 text-left text-sm">Sản phẩm</th>
                            <th className="px-4 py-2 text-right text-sm">Đơn giá</th>
                            <th className="px-4 py-2 text-right text-sm">Số lượng</th>
                            <th className="px-4 py-2 text-right text-sm">Thành tiền</th>
                        </tr></thead>
                        <tbody>{order.items?.map((item, i) => <tr key={i} className="border-t">
                            <td className="px-4 py-2">{item.productName || '-'}</td>
                            <td className="px-4 py-2 text-right">{formatPrice(item.price)}</td>
                            <td className="px-4 py-2 text-right">{item.quantity}</td>
                            <td className="px-4 py-2 text-right font-medium">{formatPrice(item.price * item.quantity)}</td>
                        </tr>)}</tbody>
                    </table>
                </div>

                <div className="space-y-1 text-sm">
                    <div className="flex justify-between"><span>Tạm tính:</span><span>{formatPrice(order.subTotal)}</span></div>
                    <div className="flex justify-between"><span>Phí ship (GHN):</span><span>{formatPrice(shippingFee ?? order.shippingFee)}</span></div>
                    {shippingFeeError && <p className="text-xs text-gray-500" role="status">{shippingFeeError}</p>}
                    {shippingFee !== null && Number(shippingFee) !== Number(order.shippingFee) && (
                        <p className="text-xs text-gray-500">Phí GHN hiện tại có thể khác phí đã chốt khi đặt hàng; tổng tiền đơn không thay đổi.</p>
                    )}
                    <div className="flex justify-between font-bold text-base"><span>Tổng cộng:</span><span className="text-brand-500">{formatPrice(order.totalAmount)}</span></div>
                </div>

                {order.delivery && <div className="mt-4 p-3 rounded-lg bg-gray-50 dark:bg-white/5 text-sm">
                    <p><strong>Vận đơn:</strong> {order.delivery.trackingCode}</p>
                    <p><strong>Đối tác:</strong> {order.delivery.deliveryPartnerId}</p>
                </div>}
            </ComponentCard>
            <Link to="/orders" className="text-brand-500 text-sm">&larr; Danh sách đơn hàng</Link>
        </div>
    </div>
}