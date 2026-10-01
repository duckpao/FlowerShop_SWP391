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

export default function CustomerOrderDetailView() {
    const { id } = useParams()
    const { user } = useAuth()
    const [order, setOrder] = useState(null)
    const [busy, setBusy] = useState(true)
    const [error, setError] = useState('')

    useEffect(() => {
        if (!user || !id) return
        let active = true
        setBusy(true)
        customerOrderService.detail(id)
            .then(data => { if (active) { setOrder(data); setError('') } })
            .catch(e => { if (active) setError(e.message) })
            .finally(() => { if (active) setBusy(false) })
        return () => { active = false }
    }, [user, id])

    if (!user) return <div className="max-w-4xl mx-auto p-8"><p className="text-gray-500">Vui lòng đăng nhập.</p></div>
    if (busy) return <div className="max-w-4xl mx-auto p-8"><p className="text-gray-500">Đang tải…</p></div>
    if (error) return <div className="max-w-4xl mx-auto p-8"><p className="text-error-500">{error}</p></div>
    if (!order) return null

    return <div className="w-full">
        <PageMeta title={`Đơn #${order.id.substring(0,8)} | FlowerShop`} />
        <PageBreadCrumb pageTitle={`Chi tiết đơn #${order.id.substring(0,8)}`} />
        <div className="max-w-3xl mx-auto space-y-6">
            <ComponentCard title={`Đơn hàng từ ${order.shopName}`}>
                <div className="flex justify-between items-center mb-4">
                    <Badge color={statusColors[order.status]||'primary'}>{statusLabels[order.status]||order.status}</Badge>
                    <span className="text-sm text-gray-500">{new Date(order.createdDate).toLocaleString('vi-VN')}</span>
                </div>

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
                    <div className="flex justify-between"><span>Phí ship:</span><span>{formatPrice(order.shippingFee)}</span></div>
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