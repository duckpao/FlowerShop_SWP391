import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { useAuth } from '../context/AuthContext'
import { customerOrderService } from '../services/customerOrderService'
import { formatPrice } from '../models/productModel'
import Badge from '../components/ui/badge/Badge'
import Button from '../components/ui/button/Button'
import PageMeta from '../components/common/PageMeta'
import PageBreadCrumb from '../components/common/PageBreadCrumb'

const paymentTypeLabels = { FULL: 'Thanh toán toàn bộ', DEPOSIT: 'Tiền cọc', FINAL: 'Phần còn lại' }
const paymentMethodLabels = { ONLINE: 'Online (SePay)', COD: 'Tiền mặt khi nhận hàng' }
const statusLabels = { PENDING: 'Đang chờ', SUCCESS: 'Thành công', FAILED: 'Thất bại' }
const statusColors = { PENDING: 'warning', SUCCESS: 'success', FAILED: 'error' }

export default function CustomerTransactionsView() {
    const { user } = useAuth()
    const [data, setData] = useState(null)
    const [page, setPage] = useState(0)
    const [busy, setBusy] = useState(true)
    const [error, setError] = useState('')

    useEffect(() => {
        if (!user) return
        let active = true
        setBusy(true)
        setError('')
        customerOrderService.transactions(page)
            .then(res => { if (active) setData(res) })
            .catch(e => { if (active) setError(e.message) })
            .finally(() => { if (active) setBusy(false) })
        return () => { active = false }
    }, [user, page])

    if (!user) return <div className="max-w-4xl mx-auto p-8"><h1 className="text-xl font-bold">Lịch sử giao dịch</h1><p className="text-gray-500">Vui lòng đăng nhập.</p></div>

    return <div className="w-full">
        <PageMeta title="Lịch sử giao dịch | FlowerShop" />
        <PageBreadCrumb pageTitle="Lịch sử giao dịch" />
        <div className="mx-auto max-w-4xl space-y-4">
            {error && <div role="alert" className="rounded-lg bg-error-50 p-4 text-sm text-error-500">{error}</div>}
            {busy && <p role="status" className="py-6 text-center text-gray-500">Đang tải…</p>}
            {!busy && data && data.content.length === 0 && (
                <div className="rounded-xl border border-gray-200 bg-white p-10 text-center text-gray-500 dark:border-white/5 dark:bg-white/3">Chưa có giao dịch nào.</div>
            )}
            {data && data.content.length > 0 && (
                <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white dark:border-white/5 dark:bg-white/3">
                    <table className="w-full text-left text-sm">
                        <thead className="border-b border-gray-100 text-gray-500 dark:border-white/5">
                            <tr>
                                <th className="px-4 py-3">Thời gian</th><th className="px-4 py-3">Đơn hàng</th><th className="px-4 py-3">Khoản</th>
                                <th className="px-4 py-3">Mã giao dịch</th><th className="px-4 py-3 text-right">Số tiền</th><th className="px-4 py-3">Trạng thái</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100 text-gray-700 dark:divide-white/5 dark:text-gray-300">
                            {data.content.map(t => (
                                <tr key={t.invoiceNumber || `${t.orderId}-${t.createdDate}`}>
                                    <td className="whitespace-nowrap px-4 py-3">{new Date(t.paidAt || t.createdDate).toLocaleString('vi-VN')}</td>
                                    <td className="px-4 py-3">
                                        <Link to={`/orders/${t.orderId}`} className="font-mono text-xs text-brand-600 hover:underline">#{t.orderId.substring(0, 8).toUpperCase()}</Link>
                                        <p className="text-xs text-gray-500">{t.shopName}</p>
                                    </td>
                                    <td className="px-4 py-3">{paymentTypeLabels[t.paymentType] || t.paymentType}<p className="text-xs text-gray-500">{paymentMethodLabels[t.paymentMethod] || t.paymentMethod}</p></td>
                                    <td className="px-4 py-3 font-mono text-xs">{t.gatewayTransactionNo || t.invoiceNumber || '—'}</td>
                                    <td className="whitespace-nowrap px-4 py-3 text-right font-medium">{formatPrice(t.amount)}</td>
                                    <td className="px-4 py-3"><Badge color={statusColors[t.status] || 'primary'}>{statusLabels[t.status] || t.status}</Badge></td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
            {data && data.totalPages > 1 && (
                <div className="flex items-center justify-between">
                    <Button size="sm" variant="outline" disabled={busy || page === 0} onClick={() => setPage(p => p - 1)}>Trang trước</Button>
                    <span className="text-sm text-gray-500">Trang {page + 1} / {data.totalPages}</span>
                    <Button size="sm" variant="outline" disabled={busy || page + 1 >= data.totalPages} onClick={() => setPage(p => p + 1)}>Trang sau</Button>
                </div>
            )}
        </div>
    </div>
}
