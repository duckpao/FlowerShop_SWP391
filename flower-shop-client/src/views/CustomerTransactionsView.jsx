import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { useAuth } from '../context/AuthContext'
import { customerTransactionService } from '../services/customerTransactionService'
import { formatPrice } from '../models/productModel'
import Badge from '../components/ui/badge/Badge'
import Button from '../components/ui/button/Button'
import Input from '../components/form/input/InputField'
import Label from '../components/form/Label'
import { Table, TableBody, TableCell, TableHeader, TableRow } from '../components/ui/table'
import PageMeta from '../components/common/PageMeta'
import PageBreadCrumb from '../components/common/PageBreadCrumb'

const typeLabels = { FULL: 'Thanh toán toàn bộ', DEPOSIT: 'Tiền cọc', FINAL: 'Phần còn lại' }
const methodLabels = { ONLINE: 'Online (SePay)', COD: 'Tiền mặt khi nhận hàng' }
const statusLabels = { PENDING: 'Đang chờ', SUCCESS: 'Thành công', FAILED: 'Thất bại' }
const statusColors = { PENDING: 'warning', SUCCESS: 'success', FAILED: 'error' }
const refundLabels = { REQUESTED: 'Chờ duyệt', APPROVED: 'Đã duyệt, chờ chuyển tiền', PROCESSED: 'Đã hoàn tiền', REJECTED: 'Bị từ chối' }
const refundColors = { REQUESTED: 'warning', APPROVED: 'info', PROCESSED: 'success', REJECTED: 'error' }
const tabs = [['', 'Tất cả'], ['SUCCESS', 'Thành công'], ['PENDING', 'Đang chờ'], ['FAILED', 'Thất bại']]

const when = v => (v ? new Date(v).toLocaleString('vi-VN') : '-')
const code = id => `#${id.substring(0, 8).toUpperCase()}`

export default function CustomerTransactionsView() {
    const { user } = useAuth()
    const [data, setData] = useState(null)
    const [page, setPage] = useState(0)
    const [status, setStatus] = useState('')
    const [from, setFrom] = useState('')
    const [to, setTo] = useState('')
    const [busy, setBusy] = useState(true)
    const [error, setError] = useState('')

    const badRange = Boolean(from && to && to < from)

    useEffect(() => {
        if (!user || badRange) return
        let active = true
        setBusy(true)
        setError('')
        customerTransactionService.list({ page, status, from, to })
            .then(res => { if (active) setData(res) })
            .catch(e => { if (active) setError(e.message) })
            .finally(() => { if (active) setBusy(false) })
        return () => { active = false }
    }, [user, page, status, from, to, badRange])

    if (!user) return <div className="mx-auto max-w-4xl p-8"><h1 className="text-xl font-bold">Lịch sử giao dịch</h1><p className="text-gray-500">Vui lòng đăng nhập.</p></div>

    const filtered = Boolean(status || from || to)
    const rows = data?.content || []
    const refunds = data?.refunds || []
    const clear = () => { setStatus(''); setFrom(''); setTo(''); setPage(0) }

    return <div className="w-full">
        <PageMeta title="Lịch sử giao dịch | FlowerShop" />
        <PageBreadCrumb pageTitle="Lịch sử giao dịch" />
        <div className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2">
                <div className="rounded-xl border border-gray-200 bg-white p-5 dark:border-white/5 dark:bg-white/3">
                    <p className="text-sm text-gray-500">Đã thanh toán</p>
                    <p className="mt-1 text-2xl font-semibold text-gray-800 dark:text-white/90">{formatPrice(data?.totalPaid ?? 0)}</p>
                </div>
                <div className="rounded-xl border border-gray-200 bg-white p-5 dark:border-white/5 dark:bg-white/3">
                    <p className="text-sm text-gray-500">Đã hoàn tiền</p>
                    <p className="mt-1 text-2xl font-semibold text-gray-800 dark:text-white/90">{formatPrice(data?.totalRefunded ?? 0)}</p>
                </div>
            </div>
            <p className="text-xs text-gray-500">Tổng tính theo khoảng ngày đã chọn, không phụ thuộc tab trạng thái.</p>

            <div className="flex flex-wrap gap-2">
                {tabs.map(([id, label]) => <Button key={id || 'all'} size="xs" variant={status === id ? 'primary' : 'outline'}
                    disabled={busy} onClick={() => { setStatus(id); setPage(0) }}>{label}</Button>)}
            </div>
            <div className="flex flex-wrap items-end gap-3">
                <div className="w-40"><Label htmlFor="tx-from">Từ ngày</Label><Input id="tx-from" type="date" value={from} max={to || undefined} onChange={e => { setFrom(e.target.value); setPage(0) }} /></div>
                <div className="w-40"><Label htmlFor="tx-to">Đến ngày</Label><Input id="tx-to" type="date" value={to} min={from || undefined} onChange={e => { setTo(e.target.value); setPage(0) }} /></div>
                {filtered && <Button size="xs" variant="outline" onClick={clear}>Xóa bộ lọc</Button>}
            </div>
            {badRange && <div role="alert" className="rounded-lg bg-error-50 p-4 text-sm text-error-500">Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.</div>}
            {error && <div role="alert" className="rounded-lg bg-error-50 p-4 text-sm text-error-500">{error}</div>}
            {busy && !badRange && <p role="status" className="py-4 text-center text-gray-500">Đang tải…</p>}

            {!busy && !badRange && data && rows.length === 0 && (
                <div className="rounded-xl border border-gray-200 bg-white p-10 text-center text-gray-500 dark:border-white/5 dark:bg-white/3">
                    {filtered
                        ? <><p>Không có giao dịch nào khớp bộ lọc hiện tại.</p><p className="mt-1 text-sm">Thử chọn tab Tất cả hoặc mở rộng khoảng ngày.</p></>
                        : <><p>Bạn chưa có giao dịch nào.</p><p className="mt-1 text-sm">Giao dịch xuất hiện sau khi bạn thanh toán một đơn hàng. <Link to="/" className="font-semibold text-brand-600 underline">Chọn hoa để đặt đơn đầu tiên</Link></p></>}
                </div>
            )}

            {rows.length > 0 && (
                <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white dark:border-white/5 dark:bg-white/3">
                    <Table>
                        <TableHeader><TableRow>
                            <TableCell isHeader>Thời gian</TableCell>
                            <TableCell isHeader>Đơn hàng</TableCell>
                            <TableCell isHeader>Khoản</TableCell>
                            <TableCell isHeader>Mã giao dịch</TableCell>
                            <TableCell isHeader>Số tiền</TableCell>
                            <TableCell isHeader>Trạng thái</TableCell>
                            <TableCell isHeader className="text-right">Hóa đơn</TableCell>
                        </TableRow></TableHeader>
                        <TableBody>
                            {rows.map(t => <TableRow key={t.id}>
                                <TableCell className="whitespace-nowrap">{when(t.paidAt || t.createdDate)}</TableCell>
                                <TableCell>
                                    <Link to={`/orders/${t.orderId}`} className="font-mono text-xs text-brand-600 hover:underline">{code(t.orderId)}</Link>
                                    <p className="text-xs text-gray-500">{t.shopName}</p>
                                </TableCell>
                                <TableCell>{typeLabels[t.paymentType] || t.paymentType}<p className="text-xs text-gray-500">{methodLabels[t.paymentMethod] || t.paymentMethod}</p></TableCell>
                                <TableCell className="font-mono text-xs">{t.gatewayTransactionNo || t.invoiceNumber || '-'}</TableCell>
                                <TableCell className="whitespace-nowrap font-medium">{formatPrice(t.amount)}</TableCell>
                                <TableCell><Badge color={statusColors[t.status] || 'primary'}>{statusLabels[t.status] || t.status}</Badge></TableCell>
                                <TableCell className="text-right">
                                    {t.invoiceAvailable
                                        ? <Link to={`/orders/${t.orderId}/invoice`}><Button size="xs" variant="outline">Hóa đơn</Button></Link>
                                        : <span className="text-xs text-gray-400">-</span>}
                                </TableCell>
                            </TableRow>)}
                        </TableBody>
                    </Table>
                </div>
            )}

            {data && data.totalPages > 1 && (
                <div className="flex items-center justify-between">
                    <Button size="xs" variant="outline" disabled={busy || page === 0} onClick={() => setPage(p => p - 1)}>Trang trước</Button>
                    <span className="text-sm text-gray-500">Trang {page + 1} / {data.totalPages}</span>
                    <Button size="xs" variant="outline" disabled={busy || page + 1 >= data.totalPages} onClick={() => setPage(p => p + 1)}>Trang sau</Button>
                </div>
            )}

            {refunds.length > 0 && (
                <div className="space-y-2">
                    <h2 className="text-base font-semibold text-gray-800 dark:text-white/90">Hoàn tiền</h2>
                    <div className="overflow-x-auto rounded-xl border border-gray-200 bg-white dark:border-white/5 dark:bg-white/3">
                        <Table>
                            <TableHeader><TableRow>
                                <TableCell isHeader>Thời gian</TableCell>
                                <TableCell isHeader>Đơn hàng</TableCell>
                                <TableCell isHeader>Lý do</TableCell>
                                <TableCell isHeader>Số tiền</TableCell>
                                <TableCell isHeader>Trạng thái</TableCell>
                            </TableRow></TableHeader>
                            <TableBody>
                                {refunds.map(r => <TableRow key={r.id}>
                                    <TableCell className="whitespace-nowrap">{when(r.createdDate)}</TableCell>
                                    <TableCell>
                                        <Link to={`/orders/${r.orderId}`} className="font-mono text-xs text-brand-600 hover:underline">{code(r.orderId)}</Link>
                                        <p className="text-xs text-gray-500">{r.shopName}</p>
                                    </TableCell>
                                    <TableCell>{r.reason}</TableCell>
                                    <TableCell className="whitespace-nowrap font-medium">{formatPrice(r.amount)}</TableCell>
                                    <TableCell><Badge color={refundColors[r.status] || 'primary'}>{refundLabels[r.status] || r.status}</Badge></TableCell>
                                </TableRow>)}
                            </TableBody>
                        </Table>
                    </div>
                </div>
            )}
        </div>
    </div>
}
