import { useEffect, useState } from 'react'
import { adminOrderService } from '../services/adminOrderService'
import { formatPrice } from '../models/productModel'
import { ghnStatusLabels, orderStatusColors, orderStatusLabels, orderTabs } from '../models/orderStatus'
import { refundStatusColors, refundStatusLabels } from '../components/refunds/RefundTable'
import Badge from '../components/ui/badge/Badge'
import Button from '../components/ui/button/Button'
import Input from '../components/form/input/InputField'
import { Modal } from '../components/ui/modal'
import { Table, TableBody, TableCell, TableHeader, TableRow } from '../components/ui/table'
import PageBreadCrumb from '../components/common/PageBreadCrumb'
import PageMeta from '../components/common/PageMeta'
import ComponentCard from '../components/common/ComponentCard'

const when = v => (v ? new Date(v).toLocaleString('vi-VN') : '-')
const Row = ({ label, children }) => <div className="flex justify-between gap-4 border-b border-gray-100 py-1.5 text-sm dark:border-white/10">
    <span className="text-gray-500 dark:text-gray-400">{label}</span>
    <span className="text-right font-medium text-gray-800 dark:text-white/90">{children ?? '-'}</span>
</div>

/** Admin xem đơn của mọi shop: tab trạng thái, tìm theo mã đơn/khách/shop, khoảng ngày; chi tiết chỉ đọc. */
export default function AdminOrdersView() {
    const [status, setStatus] = useState('')
    const [q, setQ] = useState('')
    const [from, setFrom] = useState('')
    const [to, setTo] = useState('')
    const [applied, setApplied] = useState({ q: '', from: '', to: '' })
    const [page, setPage] = useState(0)
    const [data, setData] = useState(null)
    const [detail, setDetail] = useState(null)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState('')

    useEffect(() => {
        let live = true
        setBusy(true); setError('')
        adminOrderService.list({ status, ...applied, page, size: 10 })
            .then(r => { if (live) setData(r) })
            .catch(e => { if (live) setError(e.message) })
            .finally(() => { if (live) setBusy(false) })
        return () => { live = false }
    }, [status, applied, page])

    const search = e => { e.preventDefault(); setPage(0); setApplied({ q: q.trim(), from, to }) }
    const open = async id => {
        setBusy(true); setError('')
        try { setDetail(await adminOrderService.detail(id)) } catch (e) { setError(e.message) } finally { setBusy(false) }
    }

    return <>
        <PageMeta title="Đơn hàng | Admin" description="Đơn hàng toàn hệ thống" />
        <PageBreadCrumb pageTitle="Đơn hàng" />
        <ComponentCard title="Đơn hàng toàn hệ thống">
            <div className="mb-4 flex flex-wrap gap-2">
                {orderTabs.map(t => <Button key={t.id || 'all'} size="sm" variant={status === t.id ? 'primary' : 'outline'}
                    onClick={() => { setPage(0); setStatus(t.id) }} disabled={busy}>{t.label}</Button>)}
            </div>
            <form onSubmit={search} className="mb-4 flex flex-wrap items-end gap-3">
                <div className="w-full sm:w-72"><Input label="Mã đơn, tên khách hoặc shop" value={q} onChange={e => setQ(e.target.value)} placeholder="Nhập từ khóa..." disabled={busy} /></div>
                <div className="w-40"><Input type="date" label="Từ ngày" value={from} onChange={e => setFrom(e.target.value)} disabled={busy} /></div>
                <div className="w-40"><Input type="date" label="Đến ngày" value={to} onChange={e => setTo(e.target.value)} disabled={busy} /></div>
                <Button size="sm" type="submit" disabled={busy}>Tìm kiếm</Button>
            </form>
            {error && <div className="mb-4 rounded-lg bg-error-50 p-4 text-sm text-error-500">{error}</div>}
            {data && data.content.length === 0 && <p className="text-gray-500">Không có đơn hàng phù hợp.</p>}
            {data && data.content.length > 0 && <div className="overflow-x-auto rounded-xl border border-gray-200 dark:border-white/5">
                <Table><TableHeader><TableRow>
                    <TableCell isHeader>Mã đơn</TableCell>
                    <TableCell isHeader>Khách hàng</TableCell>
                    <TableCell isHeader>Cửa hàng</TableCell>
                    <TableCell isHeader>Tổng tiền</TableCell>
                    <TableCell isHeader>Trạng thái</TableCell>
                    <TableCell isHeader>Ngày đặt</TableCell>
                    <TableCell isHeader className="text-right">Hành động</TableCell>
                </TableRow></TableHeader>
                <TableBody>{data.content.map(o => <TableRow key={o.id}>
                    <TableCell className="font-mono text-xs">{o.id.substring(0, 8)}</TableCell>
                    <TableCell>{o.customerName || '-'}</TableCell>
                    <TableCell>{o.shopName}</TableCell>
                    <TableCell>{formatPrice(o.totalAmount)}</TableCell>
                    <TableCell><Badge size="sm" color={orderStatusColors[o.status] || 'primary'}>{orderStatusLabels[o.status] || o.status}</Badge></TableCell>
                    <TableCell className="text-sm text-gray-500">{when(o.createdDate)}</TableCell>
                    <TableCell className="text-right"><Button size="xs" variant="outline" onClick={() => open(o.id)} disabled={busy}>Chi tiết</Button></TableCell>
                </TableRow>)}</TableBody></Table>
            </div>}
            {data && data.totalPages > 1 && <div className="mt-4 flex items-center gap-3 text-sm">
                <Button size="sm" variant="outline" onClick={() => setPage(p => p - 1)} disabled={busy || page === 0}>Trước</Button>
                <span>Trang {page + 1} / {data.totalPages} ({data.totalElements} đơn)</span>
                <Button size="sm" variant="outline" onClick={() => setPage(p => p + 1)} disabled={busy || page + 1 >= data.totalPages}>Sau</Button>
            </div>}
        </ComponentCard>

        <Modal isOpen={!!detail} onClose={() => setDetail(null)} className="max-w-3xl p-6">
            {detail && <div className="max-h-[80vh] overflow-y-auto pr-1">
                <h3 className="mb-3 text-lg font-semibold text-gray-900 dark:text-white">Đơn hàng #{detail.id.substring(0, 8)}</h3>
                <div className="grid gap-x-8 md:grid-cols-2">
                    <div>
                        <Row label="Trạng thái"><Badge size="sm" color={orderStatusColors[detail.status] || 'primary'}>{orderStatusLabels[detail.status] || detail.status}</Badge></Row>
                        <Row label="Ngày đặt">{when(detail.createdDate)}</Row>
                        <Row label="Khách hàng">{detail.customerName}</Row>
                        <Row label="Email">{detail.customerEmail}</Row>
                        <Row label="Điện thoại">{detail.customerPhone}</Row>
                        <Row label="Cửa hàng">{detail.shopName}</Row>
                    </div>
                    <div>
                        <Row label="Người nhận">{detail.recipientName}</Row>
                        <Row label="SĐT nhận">{detail.recipientPhone}</Row>
                        <Row label="Địa chỉ">{detail.deliveryAddress}</Row>
                        <Row label="Ghi chú khách">{detail.customerNote}</Row>
                        <Row label="Ghi chú shop">{detail.shopNote}</Row>
                        {detail.cancelReason && <Row label="Lý do hủy">{detail.cancelReason}</Row>}
                    </div>
                </div>

                <h4 className="mb-2 mt-4 font-semibold text-gray-800 dark:text-white/90">Sản phẩm</h4>
                <div className="overflow-x-auto rounded-lg border border-gray-200 dark:border-white/5">
                    <Table><TableHeader><TableRow>
                        <TableCell isHeader>Sản phẩm</TableCell><TableCell isHeader>Đơn giá</TableCell>
                        <TableCell isHeader>SL</TableCell><TableCell isHeader>Thành tiền</TableCell>
                    </TableRow></TableHeader>
                    <TableBody>{detail.items.map((i, idx) => <TableRow key={idx}>
                        <TableCell>{i.productName || 'Sản phẩm đã xóa'}</TableCell>
                        <TableCell>{formatPrice(i.price)}</TableCell><TableCell>{i.quantity}</TableCell>
                        <TableCell>{formatPrice(i.price * i.quantity)}</TableCell>
                    </TableRow>)}</TableBody></Table>
                </div>
                <div className="mt-2">
                    <Row label="Tạm tính">{formatPrice(detail.subTotal)}</Row>
                    <Row label={detail.couponCode ? `Giảm giá (mã ${detail.couponCode})` : 'Giảm giá'}>{formatPrice(detail.discountAmount || 0)}</Row>
                    <Row label="Phí vận chuyển">{formatPrice(detail.shippingFee)}</Row>
                    <Row label="Tổng cộng">{formatPrice(detail.totalAmount)}</Row>
                </div>

                <h4 className="mb-2 mt-4 font-semibold text-gray-800 dark:text-white/90">Thanh toán</h4>
                {detail.payments.length === 0 ? <p className="text-sm text-gray-500">Chưa có giao dịch.</p>
                    : detail.payments.map((p, idx) => <Row key={idx} label={`${p.paymentMethod} ${p.paymentType} (${p.status})`}>
                        {formatPrice(p.amount)}{p.invoiceNumber ? ` - ${p.invoiceNumber}` : ''}</Row>)}

                <h4 className="mb-2 mt-4 font-semibold text-gray-800 dark:text-white/90">Vận đơn</h4>
                {detail.delivery
                    ? <>
                        <Row label="Mã vận đơn">{detail.delivery.trackingCode}</Row>
                        <Row label="Trạng thái GHN">{ghnStatusLabels[detail.delivery.ghnStatus] || detail.delivery.ghnStatus || detail.delivery.status}</Row>
                    </>
                    : <p className="text-sm text-gray-500">Chưa có vận đơn.</p>}

                <h4 className="mb-2 mt-4 font-semibold text-gray-800 dark:text-white/90">Hoàn tiền</h4>
                {detail.refunds.length === 0 ? <p className="text-sm text-gray-500">Đơn này chưa có yêu cầu hoàn tiền.</p>
                    : <ul className="space-y-2 text-sm">{detail.refunds.map(r => <li key={r.id} className="rounded-lg border border-gray-200 p-3 dark:border-white/10">
                        <div className="flex items-center justify-between gap-2">
                            <span className="font-medium">{formatPrice(r.amount)} - {r.reason}</span>
                            <Badge size="sm" color={refundStatusColors[r.status]}>{refundStatusLabels[r.status] || r.status}</Badge>
                        </div>
                        {r.description && <p className="mt-1 text-gray-500">{r.description}</p>}
                        {r.shopResponse && <p className="mt-1 text-error-500">Phản hồi: {r.shopResponse}</p>}
                        <p className="mt-1 text-xs text-gray-400">{when(r.createdDate)}</p>
                    </li>)}</ul>}
                {detail.refunds.length > 0 && <a href="/admin/refunds" className="mt-2 inline-block text-sm text-brand-500">Mở trang xử lý hoàn tiền</a>}

                <div className="mt-5 flex justify-end"><Button size="sm" variant="outline" onClick={() => setDetail(null)}>Đóng</Button></div>
            </div>}
        </Modal>
    </>
}
