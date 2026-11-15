import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router'
import { useAuth } from '../context/AuthContext'
import { customerOrderService } from '../services/customerOrderService'
import orderService from '../services/orderService'
import { formatPrice } from '../models/productModel'
import Badge from '../components/ui/badge/Badge'
import ComponentCard from '../components/common/ComponentCard'
import PageMeta from '../components/common/PageMeta'
import PageBreadCrumb from '../components/common/PageBreadCrumb'

import { ghnStatusLabels, orderStatusColors as statusColors, orderStatusLabels as statusLabels } from '../models/orderStatus'

const refundStatusLabels = {
    REQUESTED: 'Chờ cửa hàng liên hệ',
    APPROVED: 'Cửa hàng đã liên hệ, đang hoàn tiền',
    PROCESSED: 'Đã hoàn tiền',
    REJECTED: 'Bị từ chối',
}
const refundStatusColors = { REQUESTED: 'warning', APPROVED: 'info', PROCESSED: 'success', REJECTED: 'error' }

/** Tổng đã trả thành công (cọc + phần còn lại, hoặc toàn bộ); dùng để biết đơn đã có hóa đơn chưa. */
const paidAmount = order => (order.payments || [])
    .filter(p => p.status === 'SUCCESS')
    .reduce((sum, p) => sum + Number(p.amount || 0), 0)

/** Chưa giao cho GHN thì khách còn hủy được (backend kiểm tra lại). */
const cancellableStatuses = ['PENDING', 'AWAITING_DEPOSIT', 'DEPOSIT_PAID', 'PROCESSING']

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
    if (order.status === 'REFUNDED') return 'Đơn vị vận chuyển đã hoàn đơn hàng về cửa hàng.'
    if (order.status === 'PENDING') return 'Đơn hàng đang chờ cửa hàng xác nhận.'
    if (order.delivery?.status === 'DELIVERED') return 'Đơn vị vận chuyển đã giao hàng thành công.'
    if (order.delivery?.status === 'ON_THE_WAY') return 'Đơn vị vận chuyển đang giao hàng đến bạn.'
    if (order.delivery?.status === 'PICKED_UP') return 'Cửa hàng đã bàn giao đơn hàng cho đơn vị vận chuyển.'
    if (order.delivery?.status === 'RETURNING') return 'Đơn hàng đang được hoàn về cửa hàng.'
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
    const [cancelOpen, setCancelOpen] = useState(false)
    const [cancelReason, setCancelReason] = useState('')
    const [cancelBusy, setCancelBusy] = useState(false)
    const [cancelError, setCancelError] = useState('')

    // Tải chi tiết đơn (customerOrderService.detail), rồi hỏi lại phí GHN hiện tại (customerOrderService.shippingFee)
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

    // Cập nhật trạng thái giao hàng: customerOrderService.refreshTracking -> POST /api/customer/orders/{id}/refresh-tracking -> GHN
    const refreshTracking = async () => {
        setTrackingBusy(true)
        setTrackingError('')
        try {
            const delivery = await customerOrderService.refreshTracking(id)
            setOrder(current => ({
                ...current,
                delivery,
                ...(delivery.status === 'DELIVERED' ? { status: 'COMPLETED' } : {}),
                ...(delivery.status === 'RETURNED' ? { status: 'REFUNDED' } : {}),
            }))
        } catch (e) {
            setTrackingError(e.message || 'Không thể cập nhật trạng thái giao hàng.')
        } finally {
            setTrackingBusy(false)
        }
    }

    // Hủy đơn: orderService.cancelOrder -> POST /api/orders/{userId}/{id}/cancel, rồi tải lại để thấy trạng thái + mục hoàn tiền
    const cancelOrder = async () => {
        setCancelBusy(true)
        setCancelError('')
        try {
            await orderService.cancelOrder(user.id, id, cancelReason.trim())
            setOrder(await customerOrderService.detail(id))
            setCancelOpen(false)
        } catch (e) {
            setCancelError(e.message || 'Không thể hủy đơn hàng.')
        } finally {
            setCancelBusy(false)
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
                            <td className="px-4 py-2">{item.productName || (item.productId ? '-' : 'Hoa đặt theo yêu cầu')}</td>
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
                    {paidAmount(order) > 0 && paidAmount(order) < Number(order.totalAmount) && (
                        <div className="flex justify-between text-gray-600 dark:text-gray-300"><span>Đã cọc / còn lại (trả khi nhận hàng):</span><span>{formatPrice(paidAmount(order))} / {formatPrice(Number(order.totalAmount) - paidAmount(order))}</span></div>
                    )}
                </div>

                {order.status !== 'CANCELLED' && paidAmount(order) >= Number(order.totalAmount) && (
                    <Link to={`/orders/${order.id}/invoice`} className="mt-4 inline-flex rounded-lg border border-brand-500 px-3 py-2 text-sm font-medium text-brand-600 hover:bg-brand-50 dark:text-brand-400">
                        Xem hóa đơn
                    </Link>
                )}

                {cancellableStatuses.includes(order.status) && (
                    <section className="mt-6 rounded-lg border border-gray-200 p-4 dark:border-white/10" aria-label="Hủy đơn hàng">
                        {!cancelOpen ? (
                            <button type="button" onClick={() => setCancelOpen(true)}
                                className="rounded-lg border border-error-500 px-3 py-2 text-sm font-medium text-error-600 hover:bg-error-50 dark:text-error-400">
                                Hủy đơn hàng
                            </button>
                        ) : (
                            <div className="space-y-3">
                                <label htmlFor="cancel-reason" className="block text-sm font-medium">Lý do hủy (không bắt buộc)</label>
                                <textarea id="cancel-reason" rows={2} maxLength={255} value={cancelReason}
                                    onChange={e => setCancelReason(e.target.value)}
                                    className="w-full rounded-lg border border-gray-300 p-2 text-sm dark:border-white/10 dark:bg-transparent" />
                                {paidAmount(order) > 0 && (
                                    <p className="text-sm text-gray-600 dark:text-gray-300">
                                        Bạn đã thanh toán {formatPrice(paidAmount(order))}. Sau khi hủy, cửa hàng sẽ liên hệ để hoàn tiền.
                                    </p>
                                )}
                                {cancelError && <p className="text-sm text-error-500" role="alert">{cancelError}</p>}
                                <div className="flex flex-wrap gap-2">
                                    <button type="button" onClick={cancelOrder} disabled={cancelBusy}
                                        className="rounded-lg bg-error-500 px-3 py-2 text-sm font-medium text-white hover:bg-error-600 disabled:opacity-50">
                                        {cancelBusy ? 'Đang hủy…' : 'Xác nhận hủy'}
                                    </button>
                                    <button type="button" onClick={() => setCancelOpen(false)} disabled={cancelBusy}
                                        className="rounded-lg border border-gray-300 px-3 py-2 text-sm font-medium dark:border-white/10">
                                        Giữ đơn
                                    </button>
                                </div>
                            </div>
                        )}
                    </section>
                )}

                {order.refunds?.length > 0 && (
                    <section className="mt-6 rounded-lg border border-gray-200 p-4 dark:border-white/10" aria-label="Hoàn tiền">
                        <h2 className="mb-3 font-semibold">Hoàn tiền</h2>
                        <ul className="space-y-3">
                            {order.refunds.map((r, i) => (
                                <li key={i} className="text-sm">
                                    <div className="flex flex-wrap items-center justify-between gap-2">
                                        <strong>{formatPrice(r.amount)}</strong>
                                        <Badge color={refundStatusColors[r.status] || 'primary'}>{refundStatusLabels[r.status] || r.status}</Badge>
                                    </div>
                                    {r.reason && <p className="mt-1 text-gray-600 dark:text-gray-300">{r.reason}</p>}
                                    <p className="mt-1 text-xs text-gray-500">
                                        Tạo lúc {new Date(r.createdDate).toLocaleString('vi-VN')}
                                        {r.status !== 'REQUESTED' && r.updatedDate && ` · Cập nhật ${new Date(r.updatedDate).toLocaleString('vi-VN')}`}
                                    </p>
                                </li>
                            ))}
                        </ul>
                        <p className="mt-3 text-xs text-gray-500">Nhân viên cửa hàng sẽ gọi cho bạn để hoàn tiền. Bạn sẽ nhận thông báo khi tiền đã được hoàn.</p>
                    </section>
                )}

                {order.delivery && <div className="mt-4 p-3 rounded-lg bg-gray-50 dark:bg-white/5 text-sm">
                    <p><strong>Vận đơn:</strong> {order.delivery.trackingCode}</p>
                    <p><strong>Đối tác:</strong> {order.delivery.deliveryPartnerId}</p>
                    {order.delivery.ghnStatus && <p><strong>Trạng thái GHN:</strong> {ghnStatusLabels[order.delivery.ghnStatus] || order.delivery.ghnStatus}</p>}
                </div>}
            </ComponentCard>
            <Link to="/orders" className="text-brand-500 text-sm">&larr; Danh sách đơn hàng</Link>
        </div>
    </div>
}