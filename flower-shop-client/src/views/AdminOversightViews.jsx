import { useCallback, useEffect, useState } from 'react'
import { adminOversightService } from '../services/adminOversightService'
import { formatPrice } from '../models/productModel'
import { orderStatusColors, orderStatusLabels } from '../models/orderStatus'
import Badge from '../components/ui/badge/Badge'
import Button from '../components/ui/button/Button'
import { Table, TableBody, TableCell, TableHeader, TableRow } from '../components/ui/table'
import ComponentCard from '../components/common/ComponentCard'
import PageMeta from '../components/common/PageMeta'
import PageBreadCrumb from '../components/common/PageBreadCrumb'
import { useConfirm } from '../components/ui/confirm/ConfirmProvider'

const refundLabels = { REQUESTED: 'Chờ liên hệ', APPROVED: 'Đã liên hệ', PROCESSED: 'Đã hoàn tiền', REJECTED: 'Từ chối', CANCELLED: 'Khách đã hủy' }
const refundColors = { REQUESTED: 'warning', APPROVED: 'info', PROCESSED: 'success', REJECTED: 'error', CANCELLED: 'light' }
const date = value => value ? new Date(value).toLocaleString('vi-VN') : ''
const select = 'h-10 rounded-lg border border-gray-300 bg-transparent px-3 text-sm dark:border-gray-700 dark:bg-gray-900'

/** Tải một trang danh sách có lọc: dùng chung cho 3 màn hình admin bên dưới. */
function usePagedList(load, deps) {
    const [state, setState] = useState({ data: null, busy: false, error: '' })
    const [page, setPage] = useState(0)
    const reload = useCallback(async (nextPage = page) => {
        setState(s => ({ ...s, busy: true, error: '' }))
        try { setState({ data: await load(nextPage), busy: false, error: '' }) }
        catch (e) { setState(s => ({ ...s, busy: false, error: e.message })) }
    }, [page, ...deps])
    useEffect(() => { reload(page) }, [page, ...deps])
    return { ...state, page, setPage, reload, setData: data => setState(s => ({ ...s, data })) }
}

function Pager({ list }) {
    const d = list.data
    if (!d || d.totalPages <= 1) return null
    return <div className="mt-4 flex items-center justify-end gap-3 text-sm">
        <Button size="sm" variant="outline" disabled={list.busy || list.page === 0} onClick={() => list.setPage(list.page - 1)}>Trước</Button>
        <span>Trang {list.page + 1}/{d.totalPages}</span>
        <Button size="sm" variant="outline" disabled={list.busy || list.page + 1 >= d.totalPages} onClick={() => list.setPage(list.page + 1)}>Sau</Button>
    </div>
}

function Frame({ title, children }) {
    return <>
        <PageMeta title={`${title} | Hệ thống`} description={title} />
        <PageBreadCrumb pageTitle={title} />
        <ComponentCard title={title}>{children}</ComponentCard>
    </>
}

/** SRS 4.4: Admin duyệt, từ chối và đánh dấu đã hoàn mọi yêu cầu hoàn tiền (cùng bước chuyển trạng thái với shop). */
export function AdminRefundsView() {
    const [status, setStatus] = useState('')
    const [notes, setNotes] = useState({})
    const list = usePagedList(page => adminOversightService.refunds(status, page), [status])
    const change = async (id, next) => {
        try {
            const updated = await adminOversightService.setRefundStatus(id, next, notes[id])
            list.setData({ ...list.data, content: list.data.content.map(r => r.id === id ? updated : r) })
        } catch (e) { alert(e.message) }
    }
    return <Frame title="Hoàn tiền">
        <div className="mb-4 flex flex-wrap items-center gap-3">
            <select aria-label="Lọc trạng thái hoàn tiền" className={select} value={status} onChange={e => { list.setPage(0); setStatus(e.target.value) }}>
                <option value="">Tất cả trạng thái</option>
                {Object.entries(refundLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </select>
            <Button size="sm" variant="outline" disabled={list.busy} onClick={() => list.reload()}>Tải lại</Button>
        </div>
        {list.error && <p role="alert" className="mb-4 rounded-lg bg-error-50 p-3 text-sm text-error-500">{list.error}</p>}
        {list.data?.content.length === 0 && <p className="text-gray-500">Không có yêu cầu hoàn tiền nào.</p>}
        {list.data?.content.length > 0 && <div className="overflow-x-auto rounded-xl border border-gray-200 dark:border-white/5">
            <Table><TableHeader><TableRow>
                <TableCell isHeader>Đơn · Shop</TableCell><TableCell isHeader>Khách hàng</TableCell><TableCell isHeader>Cần hoàn</TableCell>
                <TableCell isHeader>Lý do</TableCell><TableCell isHeader>Trạng thái</TableCell><TableCell isHeader className="text-right">Hành động</TableCell>
            </TableRow></TableHeader>
            <TableBody>{list.data.content.map(r => <TableRow key={r.id}>
                <TableCell><span className="font-mono text-xs">{r.orderId.substring(0, 8)}</span><br />{r.shopName}</TableCell>
                <TableCell>{r.customerName || '-'}<br />{r.customerPhone || '-'}</TableCell>
                <TableCell>{formatPrice(r.amount)}</TableCell>
                <TableCell className="text-sm text-gray-500">
                    <span className="font-medium text-gray-700 dark:text-gray-300">{r.reason}</span>
                    {r.description && <p className="whitespace-pre-wrap">{r.description}</p>}
                    {r.images?.length > 0 && <p className="space-x-2">{r.images.map((url, i) => <a key={url} className="text-brand-500 underline" href={url} target="_blank" rel="noreferrer">Ảnh {i + 1}</a>)}</p>}
                    {r.shopResponse && <p>Phản hồi: {r.shopResponse}</p>}
                    {date(r.createdDate)}
                </TableCell>
                <TableCell><Badge color={refundColors[r.status]}>{refundLabels[r.status] || r.status}</Badge></TableCell>
                <TableCell className="space-x-2 text-right">
                    {r.status === 'REQUESTED' && <Button size="sm" onClick={() => change(r.id, 'APPROVED')}>Đã liên hệ</Button>}
                    {r.status === 'APPROVED' && <Button size="sm" onClick={() => change(r.id, 'PROCESSED')}>Đã hoàn tiền</Button>}
                    {(r.status === 'REQUESTED' || r.status === 'APPROVED') && <>
                        <input aria-label="Phản hồi cho khách khi từ chối" className="mt-2 w-44 rounded-lg border border-gray-300 px-2 py-1 text-sm dark:border-white/10 dark:bg-gray-800" maxLength={1000}
                            placeholder="Lý do từ chối" value={notes[r.id] || ''} onChange={e => setNotes(n => ({ ...n, [r.id]: e.target.value }))} />
                        <Button size="sm" variant="outline" disabled={!notes[r.id]?.trim()} onClick={() => change(r.id, 'REJECTED')}>Từ chối</Button>
                    </>}
                </TableCell>
            </TableRow>)}</TableBody></Table>
        </div>}
        <Pager list={list} />
    </Frame>
}

/** SRS 4.4: Admin xem mọi đơn hàng và chi tiết (chỉ đọc). */
export function AdminOrdersView() {
    const [status, setStatus] = useState('')
    const [detail, setDetail] = useState(null)
    const [detailError, setDetailError] = useState('')
    const list = usePagedList(page => adminOversightService.orders(status, page), [status])
    const open = async id => {
        setDetailError(''); setDetail(null)
        try { setDetail(await adminOversightService.order(id)) } catch (e) { setDetailError(e.message) }
    }
    return <Frame title="Đơn hàng toàn hệ thống">
        <div className="mb-4 flex flex-wrap items-center gap-3">
            <select aria-label="Lọc trạng thái đơn" className={select} value={status} onChange={e => { list.setPage(0); setStatus(e.target.value) }}>
                <option value="">Tất cả trạng thái</option>
                {Object.entries(orderStatusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </select>
            <Button size="sm" variant="outline" disabled={list.busy} onClick={() => list.reload()}>Tải lại</Button>
        </div>
        {list.error && <p role="alert" className="mb-4 rounded-lg bg-error-50 p-3 text-sm text-error-500">{list.error}</p>}
        {list.data?.content.length === 0 && <p className="text-gray-500">Không có đơn nào.</p>}
        {list.data?.content.length > 0 && <div className="overflow-x-auto rounded-xl border border-gray-200 dark:border-white/5">
            <Table><TableHeader><TableRow>
                <TableCell isHeader>Mã đơn</TableCell><TableCell isHeader>Shop</TableCell><TableCell isHeader>Khách hàng</TableCell>
                <TableCell isHeader>Tổng tiền</TableCell><TableCell isHeader>Trạng thái</TableCell><TableCell isHeader>Ngày đặt</TableCell><TableCell isHeader className="text-right">Chi tiết</TableCell>
            </TableRow></TableHeader>
            <TableBody>{list.data.content.map(o => <TableRow key={o.id}>
                <TableCell className="font-mono text-xs">{o.id.substring(0, 8)}</TableCell>
                <TableCell>{o.shopName}</TableCell>
                <TableCell>{o.customerName || 'Khách ẩn danh'}<br />{o.customerPhone || ''}</TableCell>
                <TableCell>{formatPrice(o.totalAmount)}</TableCell>
                <TableCell><Badge color={orderStatusColors[o.status]}>{orderStatusLabels[o.status] || o.status}</Badge></TableCell>
                <TableCell>{date(o.createdDate)}</TableCell>
                <TableCell className="text-right"><Button size="sm" variant="outline" onClick={() => open(o.id)}>Xem</Button></TableCell>
            </TableRow>)}</TableBody></Table>
        </div>}
        <Pager list={list} />
        {detailError && <p role="alert" className="mt-4 text-sm text-error-500">{detailError}</p>}
        {detail && <section className="mt-6 rounded-xl border border-gray-200 p-4 text-sm dark:border-white/5" aria-label="Chi tiết đơn">
            <div className="mb-2 flex items-center justify-between">
                <h3 className="font-semibold">Đơn {detail.id.substring(0, 8)} · {orderStatusLabels[detail.status] || detail.status}</h3>
                <Button size="sm" variant="outline" onClick={() => setDetail(null)}>Đóng</Button>
            </div>
            <p>Khách: {detail.customerName || '-'} · {detail.customerPhone || '-'}</p>
            <p>Người nhận: {detail.recipientName || '-'} · {detail.recipientPhone || '-'}</p>
            <p>Giao đến: {detail.deliveryAddress || '-'}</p>
            <ul className="my-2 list-disc pl-5">{detail.items.map((item, i) => <li key={i}>{item.productName || 'Hoa đặt theo yêu cầu'} × {item.quantity} · {formatPrice(item.price)}</li>)}</ul>
            <p>Tạm tính {formatPrice(detail.subTotal)} · Phí ship {formatPrice(detail.shippingFee)} · <strong>Tổng {formatPrice(detail.totalAmount)}</strong></p>
            <p>Thanh toán: {detail.payment ? `${detail.payment.method} · ${detail.payment.status}` : 'Chưa có'} · Vận đơn: {detail.delivery?.trackingCode || 'Chưa có'}</p>
            {detail.cancelReason && <p>Lý do hủy: {detail.cancelReason}</p>}
        </section>}
    </Frame>
}

/** SRS 4.4: Admin xem đánh giá và xóa đánh giá vi phạm. */
export function AdminReviewsView() {
    const { confirm } = useConfirm()
    const list = usePagedList(page => adminOversightService.reviews(page), [])
    const remove = async review => {
        if (!await confirm({ title: 'Xóa đánh giá', message: `Xóa đánh giá ${review.rating}★ của ${review.userName || 'khách hàng'}?`, confirmLabel: 'Xóa', tone: 'danger' })) return
        try { await adminOversightService.deleteReview(review.id); list.reload() } catch (e) { alert(e.message) }
    }
    return <Frame title="Đánh giá sản phẩm">
        {list.error && <p role="alert" className="mb-4 rounded-lg bg-error-50 p-3 text-sm text-error-500">{list.error}</p>}
        {list.data?.content.length === 0 && <p className="text-gray-500">Chưa có đánh giá nào.</p>}
        {list.data?.content.length > 0 && <div className="overflow-x-auto rounded-xl border border-gray-200 dark:border-white/5">
            <Table><TableHeader><TableRow>
                <TableCell isHeader>Sản phẩm · Shop</TableCell><TableCell isHeader>Người đánh giá</TableCell><TableCell isHeader>Sao</TableCell>
                <TableCell isHeader>Nội dung</TableCell><TableCell isHeader>Ngày</TableCell><TableCell isHeader className="text-right">Hành động</TableCell>
            </TableRow></TableHeader>
            <TableBody>{list.data.content.map(r => <TableRow key={r.id}>
                <TableCell>{r.productName}<br /><span className="text-xs text-gray-500">{r.shopName}</span></TableCell>
                <TableCell>{r.userName || 'Khách hàng'}</TableCell>
                <TableCell>{r.rating}★</TableCell>
                <TableCell className="max-w-xs whitespace-pre-wrap text-sm">{r.comment || <span className="text-gray-400">(không có nội dung)</span>}{r.shopReply && <p className="text-gray-500">Shop trả lời: {r.shopReply}</p>}</TableCell>
                <TableCell>{date(r.createdDate)}</TableCell>
                <TableCell className="text-right"><Button size="sm" variant="outline" className="text-error-500" onClick={() => remove(r)}>Xóa</Button></TableCell>
            </TableRow>)}</TableBody></Table>
        </div>}
        <Pager list={list} />
    </Frame>
}
