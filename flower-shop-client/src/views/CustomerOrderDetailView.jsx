import CustomerQuoteActions from '../components/customFlowers/CustomerQuoteActions'
import CustomOrderPayment from '../components/customFlowers/CustomOrderPayment'
import CustomerCustomAddress from '../components/customFlowers/CustomerCustomAddress'
import { OrderDesignPanel } from '../components/customFlowers/OrderDesignPanel'
import OrderDiscountSummary from '../components/checkout/OrderDiscountSummary'
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
    CANCELLED: 'Bạn đã hủy yêu cầu',
}
const refundStatusColors = { REQUESTED: 'warning', APPROVED: 'info', PROCESSED: 'success', REJECTED: 'error', CANCELLED: 'light' }
const REFUND_REASONS = ['Đổi ý', 'Sai sản phẩm', 'Hoa bị dập, héo', 'Giao hàng trễ', 'Khác']
const NEEDS_EVIDENCE = ['Sai sản phẩm', 'Hoa bị dập, héo']

/** SRS 5.2: form khách gửi yêu cầu hoàn tiền (lý do, mô tả, tối đa 5 ảnh bằng chứng, mỗi ảnh ≤ 5 MB). */
function RefundRequestForm({ orderId, amount, onDone }) {
    const [reason, setReason] = useState(REFUND_REASONS[0])
    const [description, setDescription] = useState('')
    const [images, setImages] = useState([])
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState('')
    const upload = async files => {
        const list = [...files].slice(0, 5 - images.length)
        if (list.some(f => f.size > 5 * 1024 * 1024)) { setError('Mỗi ảnh tối đa 5 MB.'); return }
        setBusy(true); setError('')
        try {
            const urls = []
            for (const f of list) urls.push((await customerOrderService.uploadRefundImage(orderId, f)).url)
            setImages(current => [...current, ...urls])
        } catch (e) { setError(e.message || 'Không tải được ảnh.') } finally { setBusy(false) }
    }
    const submit = async e => {
        e.preventDefault()
        setBusy(true); setError('')
        try { await customerOrderService.requestRefund(orderId, { reason, description, images }); onDone() }
        catch (err) { setError(err.message || 'Không gửi được yêu cầu.'); setBusy(false) }
    }
    const needsImages = NEEDS_EVIDENCE.includes(reason)
    return <form onSubmit={submit} className="mt-6 space-y-3 rounded-lg border border-gray-200 p-4 text-sm dark:border-white/10" aria-label="Yêu cầu hoàn tiền">
        <h2 className="font-semibold">Yêu cầu hoàn tiền</h2>
        <p className="text-gray-500">Số tiền dự kiến hoàn: <strong>{formatPrice(amount)}</strong> (cửa hàng xác nhận số tiền cuối cùng).</p>
        <label className="block">Lý do
            <select className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-white/10 dark:bg-gray-800" value={reason} onChange={e => setReason(e.target.value)} disabled={busy}>
                {REFUND_REASONS.map(r => <option key={r} value={r}>{r}</option>)}
            </select>
        </label>
        <label className="block">Mô tả{reason === 'Khác' ? ' (bắt buộc)' : ''}
            <textarea className="mt-1 w-full rounded-lg border border-gray-300 px-3 py-2 dark:border-white/10 dark:bg-gray-800" rows={3} maxLength={1000}
                required={reason === 'Khác'} value={description} onChange={e => setDescription(e.target.value)} disabled={busy} />
        </label>
        <div>
            <label className="block">Ảnh bằng chứng{needsImages ? ' (bắt buộc)' : ''}, tối đa 5 ảnh JPG/PNG
                <input type="file" accept="image/jpeg,image/png" multiple className="mt-1 block" disabled={busy || images.length >= 5}
                    onChange={e => { upload(e.target.files); e.target.value = '' }} />
            </label>
            {images.length > 0 && <div className="mt-2 flex flex-wrap gap-2">{images.map(url => <span key={url} className="relative">
                <img src={url} alt="Ảnh bằng chứng" className="h-16 w-16 rounded object-cover" />
                <button type="button" aria-label="Bỏ ảnh này" className="absolute -right-2 -top-2 rounded-full bg-white px-1 text-xs shadow" onClick={() => setImages(list => list.filter(x => x !== url))}>✕</button>
            </span>)}</div>}
        </div>
        {error && <p role="alert" className="text-error-500">{error}</p>}
        <button type="submit" className="rounded-lg bg-brand-500 px-4 py-2 font-medium text-white disabled:opacity-50" disabled={busy || (needsImages && images.length === 0)}>
            {busy ? 'Đang gửi…' : 'Gửi yêu cầu hoàn tiền'}
        </button>
    </form>
}

/** Còn được hoàn = đã trả thành công − đã hoàn (server tính lại khi gửi yêu cầu). */
const refundableAmount = order => paidAmount(order) - (order.refunds || [])
    .filter(r => r.status === 'PROCESSED').reduce((sum, r) => sum + Number(r.amount || 0), 0)

/** Tổng đã trả thành công (cọc + phần còn lại, hoặc toàn bộ); dùng để biết đơn đã có hóa đơn chưa. */
const paidAmount = order => (order.payments || [])
    .filter(p => p.status === 'SUCCESS')
    .reduce((sum, p) => sum + Number(p.amount || 0), 0)

/** Chưa giao cho GHN thì khách còn hủy được (backend kiểm tra lại). */
const cancellableStatuses = ['PENDING', 'AWAITING_DEPOSIT', 'DEPOSIT_PAID', 'PROCESSING','AWAITING_QUOTE','QUOTED']

const trackingSteps = [
    'Cửa hàng đang chuẩn bị đơn hàng',
    'Cửa hàng đã giao hàng cho đơn vị vận chuyển',
    'Đơn vị vận chuyển đã vận chuyển thành công đến khách hàng',
]

// Trạng thái vận đơn khi giao không thành công / đang hoàn hàng: bước 3 hiện cảnh báo thay vì "đã giao"
const PROBLEM_DELIVERY = ['FAILED', 'RETURNING', 'RETURNED']
const isTrackingProblem = order => order.status === 'REFUNDED' || PROBLEM_DELIVERY.includes(order.delivery?.status)

/**
 * Bước đang sáng trên thanh theo dõi: 0 = shop chuẩn bị, 1 = đã giao GHN, 2 = đang vận chuyển, 3 = đã giao xong
 * (mọi bước đều hoàn tất). -1 = chưa bắt đầu (chờ báo giá/đặt cọc) hoặc đã hủy.
 * Ưu tiên trạng thái của ĐƠN trước trạng thái vận đơn: shop có thể đổi trạng thái thủ công (không có vận đơn)
 * hoặc job tự hoàn tất đơn mà vận đơn vẫn còn trạng thái cũ.
 */
function getTrackingStage(order) {
    const deliveryStatus = order.delivery?.status
    if (['AWAITING_QUOTE', 'QUOTED', 'AWAITING_DEPOSIT', 'CANCELLED'].includes(order.status)) return -1
    if (isTrackingProblem(order)) return 2
    if (order.status === 'COMPLETED' || deliveryStatus === 'DELIVERED') return 3
    if (['PICKED_UP', 'ON_THE_WAY'].includes(deliveryStatus)) return 2
    if (order.status === 'DELIVERING') return 1
    return 0 // PENDING, DEPOSIT_PAID, PROCESSING: shop đang xử lý
}

function getStepLabel(order, index) {
    if (index === 2 && isTrackingProblem(order)) {
        if (order.status === 'REFUNDED' || order.delivery?.status === 'RETURNED') return 'Đơn hàng đã được hoàn về cửa hàng'
        if (order.delivery?.status === 'RETURNING') return 'Đơn hàng đang được hoàn về cửa hàng'
        return 'Đơn vị vận chuyển chưa giao được đơn hàng'
    }
    return trackingSteps[index]
}

function getTrackingMessage(order) {
    if (order.status === 'CANCELLED') return 'Đơn hàng đã bị hủy.'
    if (order.status === 'REFUNDED') return 'Đơn vị vận chuyển đã hoàn đơn hàng về cửa hàng.'
    if (order.status === 'COMPLETED') return 'Đơn hàng đã được giao thành công.'
    if (order.status === 'AWAITING_QUOTE') return 'Đơn hàng đang chờ cửa hàng báo giá.'
    if (order.status === 'QUOTED') return 'Cửa hàng đã báo giá, đang chờ bạn đồng ý.'
    if (order.status === 'AWAITING_DEPOSIT') return 'Đơn hàng đang chờ bạn đặt cọc.'
    if (order.status === 'DEPOSIT_PAID') return 'Đã nhận tiền cọc, cửa hàng sẽ chuẩn bị đơn hàng.'
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
    const [reloadKey, setReloadKey] = useState(0)
    const [refundError, setRefundError] = useState('')

    // Ghi trạng thái vận đơn mới từ GHN vào đơn đang hiển thị (backend đã đồng bộ trạng thái đơn tương ứng)
    const applyDelivery = delivery => setOrder(current => ({
        ...current,
        delivery,
        ...(delivery.status === 'DELIVERED' ? { status: 'COMPLETED' } : {}),
        ...(delivery.status === 'RETURNED' ? { status: 'REFUNDED' } : {}),
    }))

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
                // Hệ thống không có webhook GHN: đơn đang giao thì hỏi GHN ngay khi mở trang để thanh theo dõi không bị cũ
                if (data.status === 'DELIVERING' && data.delivery)
                    customerOrderService.refreshTracking(id)
                        .then(delivery => { if (active) applyDelivery(delivery) })
                        .catch(e => { if (active) setTrackingError(e.message || 'Không thể cập nhật trạng thái giao hàng từ GHN.') })
                customerOrderService.shippingFee(id)
                    .then(result => { if (active) setShippingFee(result.shippingFee) })
                    .catch(e => { if (active) setShippingFeeError(`${e.message || 'Không lấy được phí GHN hiện tại.'} Đang hiển thị phí đã lưu của đơn.`) })
            })
            .catch(e => { if (active) setError(e.message) })
            .finally(() => { if (active) setBusy(false) })
        return () => { active = false }
    }, [user, id, reloadKey])

    // Cập nhật trạng thái giao hàng: customerOrderService.refreshTracking -> POST /api/customer/orders/{id}/refresh-tracking -> GHN
    const refreshTracking = async () => {
        setTrackingBusy(true)
        setTrackingError('')
        try {
            applyDelivery(await customerOrderService.refreshTracking(id))
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
            <Link to="/orders" className="inline-flex text-sm font-medium text-brand-600 hover:underline dark:text-brand-400">&larr; Lịch sử đơn hàng</Link>
            <ComponentCard title={`Đơn hàng từ ${order.shopName}`}>
<OrderDesignPanel order={order} /><CustomerCustomAddress order={order} onChanged={async () => setOrder(await customerOrderService.detail(id))} /><CustomerQuoteActions key={order.id} order={order} user={user} onChanged={async () => setOrder(await customerOrderService.detail(id))} />{order.orderType === 'CUSTOM' && ['AWAITING_DEPOSIT', 'DEPOSIT_PAID'].includes(order.status) && <CustomOrderPayment key={order.id} orderId={order.id} />}<OrderDiscountSummary order={order} />
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
                        {trackingSteps.map((_, index) => {
                            const complete = trackingStage === 3 || index < trackingStage
                            const current = index === trackingStage
                            const problem = current && isTrackingProblem(order)
                            return <li key={index} className="flex items-start gap-3">
                                <span className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-xs font-semibold ${
                                    complete ? 'bg-success-600 text-white' : problem ? 'bg-warning-500 text-white' : current ? 'bg-brand-600 text-white' : 'border border-gray-300 text-gray-500 dark:border-gray-600'
                                }`} aria-label={complete ? 'Hoàn tất' : problem ? 'Có sự cố' : current ? 'Đang xử lý' : 'Chưa hoàn tất'}>
                                    {complete ? '✓' : problem ? '!' : index + 1}
                                </span>
                                <span className={`text-sm ${complete || current ? 'font-medium text-gray-900 dark:text-white' : 'text-gray-500 dark:text-gray-400'}`}>
                                    {getStepLabel(order, index)}
                                    <span className="ml-2 text-xs font-normal text-gray-500 dark:text-gray-400">({complete ? 'Hoàn tất' : problem ? 'Có sự cố' : current ? 'Đang xử lý' : 'Chưa hoàn tất'})</span>
                                </span>
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
                    {shippingFeeError && <p className="text-xs text-error-500" role="alert">{shippingFeeError}</p>}
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
                                    {r.description && <p className="mt-1 whitespace-pre-wrap text-gray-600 dark:text-gray-300">{r.description}</p>}
                                    {r.images?.length > 0 && <div className="mt-2 flex flex-wrap gap-2">{r.images.map(url => <a key={url} href={url} target="_blank" rel="noreferrer"><img src={url} alt="Ảnh bằng chứng" className="h-14 w-14 rounded object-cover" /></a>)}</div>}
                                    {r.status === 'REJECTED' && r.shopResponse && <p className="mt-1 text-error-500">Phản hồi của cửa hàng: {r.shopResponse}</p>}
                                    {r.status === 'REQUESTED' && r.id && <button type="button" className="mt-1 text-xs text-error-500 underline"
                                        onClick={() => customerOrderService.cancelRefund(order.id, r.id).then(() => setReloadKey(k => k + 1)).catch(e => setRefundError(e.message))}>Hủy yêu cầu</button>}
                                    <p className="mt-1 text-xs text-gray-500">
                                        Tạo lúc {new Date(r.createdDate).toLocaleString('vi-VN')}
                                        {r.status !== 'REQUESTED' && r.updatedDate && ` · Cập nhật ${new Date(r.updatedDate).toLocaleString('vi-VN')}`}
                                    </p>
                                </li>
                            ))}
                        </ul>
                        {refundError && <p role="alert" className="mt-2 text-sm text-error-500">{refundError}</p>}
                        <p className="mt-3 text-xs text-gray-500">Nhân viên cửa hàng sẽ gọi cho bạn để hoàn tiền. Bạn sẽ nhận thông báo khi tiền đã được hoàn.</p>
                    </section>
                )}

                {['COMPLETED', 'CANCELLED', 'REFUNDED'].includes(order.status)
                    && !(order.refunds || []).some(r => r.status === 'REQUESTED' || r.status === 'APPROVED')
                    && refundableAmount(order) > 0
                    && <RefundRequestForm key={reloadKey} orderId={order.id} amount={refundableAmount(order)} onDone={() => setReloadKey(k => k + 1)} />}

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
