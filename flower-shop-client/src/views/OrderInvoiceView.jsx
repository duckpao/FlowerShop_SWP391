import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { useAuth } from '../context/AuthContext'
import { customerOrderService } from '../services/customerOrderService'
import { formatPrice } from '../models/productModel'
import PageMeta from '../components/common/PageMeta'

const paymentTypeLabels = { FULL: 'Thanh toán toàn bộ', DEPOSIT: 'Tiền cọc', FINAL: 'Phần còn lại' }
const paymentMethodLabels = { ONLINE: 'Online (SePay)', COD: 'Tiền mặt khi nhận hàng' }

/**
 * Hóa đơn của đơn đã thanh toán đủ (/orders/:id/invoice), nằm ngoài AppLayout để in gọn.
 * Dữ liệu lấy từ chi tiết đơn (customerOrderService.detail); "In / Lưu PDF" dùng hộp thoại in của trình duyệt.
 */
export default function OrderInvoiceView() {
    const { id } = useParams()
    const { user, busy: authBusy } = useAuth()
    const [order, setOrder] = useState(null)
    const [error, setError] = useState('')

    useEffect(() => {
        if (!user || !id) return
        let active = true
        customerOrderService.detail(id)
            .then(data => { if (active) setOrder(data) })
            .catch(e => { if (active) setError(e.message) })
        return () => { active = false }
    }, [user, id])

    if (authBusy && !user) return <p role="status" className="p-8 text-gray-500">Đang kiểm tra đăng nhập…</p>
    if (!user) return <p className="p-8">Vui lòng <Link className="text-brand-600 underline" to={`/login?next=${encodeURIComponent(`/orders/${id}/invoice`)}`}>đăng nhập</Link> để xem hóa đơn.</p>
    if (error) return <p role="alert" className="p-8 text-error-500">{error}</p>
    if (!order) return <p role="status" className="p-8 text-gray-500">Đang tải hóa đơn…</p>

    const paidPayments = (order.payments || []).filter(p => p.status === 'SUCCESS')
    const paid = paidPayments.reduce((sum, p) => sum + Number(p.amount || 0), 0)
    const code = order.id.replace(/-/g, '').substring(0, 8).toUpperCase()
    if (paid < Number(order.totalAmount)) {
        return <div className="mx-auto max-w-2xl p-8">
            <p className="text-gray-700">Đơn hàng #{code} chưa thanh toán đủ nên chưa có hóa đơn.</p>
            <Link to={`/orders/${order.id}`} className="mt-4 inline-block text-sm text-brand-600">&larr; Quay lại đơn hàng</Link>
        </div>
    }
    const lastPaidAt = paidPayments.map(p => p.paidAt).filter(Boolean).sort().at(-1)

    return <div className="min-h-screen bg-gray-50 py-8 print:bg-white print:py-0">
        <PageMeta title={`Hóa đơn #${code} | FlowerShop`} />
        <div className="mx-auto mb-4 flex max-w-3xl justify-between px-4 print:hidden">
            <Link to={`/orders/${order.id}`} className="text-sm text-brand-600">&larr; Quay lại đơn hàng</Link>
            <button type="button" onClick={() => window.print()} className="rounded-lg bg-brand-500 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-600">In / Lưu PDF</button>
        </div>

        <article className="mx-auto max-w-3xl bg-white p-8 text-gray-900 shadow-sm print:shadow-none">
            <header className="flex flex-wrap items-start justify-between gap-4 border-b border-gray-200 pb-6">
                <div>
                    <h1 className="text-2xl font-bold">HÓA ĐƠN BÁN HÀNG</h1>
                    <p className="mt-1 text-sm text-gray-500">FlowerShop · {order.shopName}</p>
                </div>
                <dl className="text-right text-sm">
                    <div><dt className="inline text-gray-500">Số hóa đơn: </dt><dd className="inline font-mono font-semibold">#{code}</dd></div>
                    <div><dt className="inline text-gray-500">Ngày đặt: </dt><dd className="inline">{new Date(order.createdDate).toLocaleString('vi-VN')}</dd></div>
                    {lastPaidAt && <div><dt className="inline text-gray-500">Thanh toán xong: </dt><dd className="inline">{new Date(lastPaidAt).toLocaleString('vi-VN')}</dd></div>}
                </dl>
            </header>

            <section className="border-b border-gray-200 py-4 text-sm">
                <h2 className="mb-1 font-semibold">Người nhận</h2>
                <p>{order.recipientName}{order.recipientPhone && ` · ${order.recipientPhone}`}</p>
                {order.deliveryAddress && <p className="text-gray-600">{order.deliveryAddress}</p>}
            </section>

            <table className="mt-4 w-full text-sm">
                <thead><tr className="border-b border-gray-200 text-left text-gray-500">
                    <th className="py-2">Sản phẩm</th>
                    <th className="py-2 text-right">Đơn giá</th>
                    <th className="py-2 text-right">SL</th>
                    <th className="py-2 text-right">Thành tiền</th>
                </tr></thead>
                <tbody>{(order.items || []).map((item, i) => <tr key={i} className="border-b border-gray-100">
                    <td className="py-2">{item.productName || (item.productId ? 'Sản phẩm' : 'Hoa đặt theo yêu cầu')}</td>
                    <td className="py-2 text-right">{formatPrice(item.price)}</td>
                    <td className="py-2 text-right">{item.quantity}</td>
                    <td className="py-2 text-right">{formatPrice(item.price * item.quantity)}</td>
                </tr>)}</tbody>
            </table>

            <dl className="ml-auto mt-4 max-w-xs space-y-1 text-sm">
                <div className="flex justify-between"><dt>Tạm tính</dt><dd>{formatPrice(order.subTotal)}</dd></div>
                {Number(order.discountAmount) > 0 && <div className="flex justify-between"><dt>Giảm giá</dt><dd>-{formatPrice(order.discountAmount)}</dd></div>}
                <div className="flex justify-between"><dt>Phí vận chuyển (GHN)</dt><dd>{formatPrice(order.shippingFee)}</dd></div>
                <div className="flex justify-between border-t border-gray-200 pt-2 text-base font-bold"><dt>Tổng cộng</dt><dd>{formatPrice(order.totalAmount)}</dd></div>
            </dl>

            <section className="mt-6 text-sm">
                <h2 className="mb-2 font-semibold">Các lần thanh toán</h2>
                <table className="w-full">
                    <thead><tr className="border-b border-gray-200 text-left text-gray-500">
                        <th className="py-1">Khoản</th><th className="py-1">Phương thức</th><th className="py-1">Mã giao dịch</th><th className="py-1 text-right">Số tiền</th>
                    </tr></thead>
                    <tbody>{paidPayments.map((p, i) => <tr key={i} className="border-b border-gray-100">
                        <td className="py-1">{paymentTypeLabels[p.paymentType] || p.paymentType}</td>
                        <td className="py-1">{paymentMethodLabels[p.paymentMethod] || p.paymentMethod}</td>
                        <td className="py-1 font-mono text-xs">{p.gatewayTransactionNo || p.invoiceNumber}</td>
                        <td className="py-1 text-right">{formatPrice(p.amount)}</td>
                    </tr>)}</tbody>
                </table>
                <p className="mt-2 text-right font-semibold">Đã thanh toán: {formatPrice(paid)}</p>
            </section>

            <p className="mt-8 text-center text-xs text-gray-400">Cảm ơn bạn đã mua hoa tại FlowerShop.</p>
        </article>
    </div>
}
