import { useEffect, useState } from 'react'
import { staffRefundService } from '../services/staffRefundService'
import { managerShopService } from '../services/managerShopService'
import { formatPrice } from '../models/productModel'
import Badge from '../components/ui/badge/Badge'
import Button from '../components/ui/button/Button'
import { Table, TableBody, TableCell, TableHeader, TableRow } from '../components/ui/table'
import ComponentCard from '../components/common/ComponentCard'

const statusLabels = { REQUESTED: 'Chờ liên hệ', APPROVED: 'Đã liên hệ', PROCESSED: 'Đã hoàn tiền', REJECTED: 'Từ chối', CANCELLED: 'Khách đã hủy' }
const statusColors = { REQUESTED: 'warning', APPROVED: 'info', PROCESSED: 'success', REJECTED: 'error', CANCELLED: 'light' }

/**
 * Trang chủ shop và Staff: đơn đã hủy cần hoàn tiền. Người xử lý gọi khách theo SĐT rồi đánh dấu tiến độ:
 * Chờ liên hệ -> Đã liên hệ -> Đã hoàn tiền (hoặc Từ chối).
 */
export default function StaffRefundsView({ role }) {
    const [shop, setShop] = useState(null)
    const [refunds, setRefunds] = useState(null)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState('')
    const [notes, setNotes] = useState({})

    // Shop của chủ shop hoặc shop staff đang làm: lấy shop đầu tiên.
    // Chờ có role (user tải sau lần render đầu) để không gọi nhầm API của staff khi là chủ shop.
    useEffect(() => {
        if (!role) return
        setError('')
        managerShopService.list(role)
            .then(shops => shops.length ? setShop(shops[0]) : setError('Bạn chưa thuộc cửa hàng nào.'))
            .catch(e => setError(e.message))
    }, [role])

    const load = async () => {
        setBusy(true); setError('')
        try { setRefunds(await staffRefundService.list(shop.id, role)) } catch (e) { setError(e.message) } finally { setBusy(false) }
    }
    useEffect(() => { if (shop) load() }, [shop?.id])

    const setStatus = async (id, status) => {
        setBusy(true); setError('')
        try {
            const updated = await staffRefundService.setStatus(shop.id, id, status, role, notes[id])
            setRefunds(list => list.map(r => r.id === id ? updated : r))
        } catch (e) { setError(e.message) } finally { setBusy(false) }
    }

    return <ComponentCard title={`Đơn hủy cần hoàn tiền${shop ? ` · ${shop.name}` : ''}`}>
        <div className="mb-4"><Button size="sm" variant="outline" onClick={load} disabled={busy || !shop}>Tải lại</Button></div>
        {error && <div className="mb-4 rounded-lg bg-error-50 p-4 text-sm text-error-500">{error}</div>}
        {refunds && refunds.length === 0 && <p className="text-gray-500">Không có đơn nào cần hoàn tiền.</p>}
        {refunds && refunds.length > 0 && <div className="overflow-hidden rounded-xl border border-gray-200 dark:border-white/5">
            <Table><TableHeader><TableRow>
                <TableCell isHeader>Mã đơn</TableCell>
                <TableCell isHeader>Khách hàng</TableCell>
                <TableCell isHeader>Người nhận</TableCell>
                <TableCell isHeader>Cần hoàn</TableCell>
                <TableCell isHeader>Lý do</TableCell>
                <TableCell isHeader>Trạng thái</TableCell>
                <TableCell isHeader className="text-right">Hành động</TableCell>
            </TableRow></TableHeader>
            <TableBody>{refunds.map(r => <TableRow key={r.id}>
                <TableCell className="font-mono text-xs">{r.orderId.substring(0, 8)}</TableCell>
                <TableCell>{r.customerName || '-'}<br />{r.customerPhone ? <a className="text-brand-500" href={`tel:${r.customerPhone}`}>📞 {r.customerPhone}</a> : '-'}</TableCell>
                <TableCell>{r.recipientName || '-'}<br />{r.recipientPhone && <a className="text-brand-500" href={`tel:${r.recipientPhone}`}>📞 {r.recipientPhone}</a>}</TableCell>
                <TableCell>{formatPrice(r.amount)}</TableCell>
                <TableCell className="text-sm text-gray-500">
                    <span className="font-medium text-gray-700 dark:text-gray-300">{r.reason}</span>
                    {r.description && <p className="whitespace-pre-wrap">{r.description}</p>}
                    {r.images?.length > 0 && <p className="space-x-2">{r.images.map((url, i) => <a key={url} className="text-brand-500 underline" href={url} target="_blank" rel="noreferrer">Ảnh {i + 1}</a>)}</p>}
                    {r.shopResponse && <p>Phản hồi: {r.shopResponse}</p>}
                    {new Date(r.createdDate).toLocaleString('vi-VN')}
                </TableCell>
                <TableCell><Badge color={statusColors[r.status]}>{statusLabels[r.status] || r.status}</Badge></TableCell>
                <TableCell className="text-right space-x-2">
                    {r.status === 'REQUESTED' && <Button size="sm" onClick={() => setStatus(r.id, 'APPROVED')} disabled={busy}>Đã liên hệ</Button>}
                    {r.status === 'APPROVED' && <Button size="sm" onClick={() => setStatus(r.id, 'PROCESSED')} disabled={busy}>Đã hoàn tiền</Button>}
                    {(r.status === 'REQUESTED' || r.status === 'APPROVED') && <>
                        <input aria-label="Phản hồi cho khách khi từ chối" className="mt-2 w-48 rounded-lg border border-gray-300 px-2 py-1 text-sm dark:border-white/10 dark:bg-gray-800" maxLength={1000}
                            placeholder="Lý do từ chối" value={notes[r.id] || ''} onChange={e => setNotes(n => ({ ...n, [r.id]: e.target.value }))} />
                        <Button size="sm" variant="outline" onClick={() => setStatus(r.id, 'REJECTED')} disabled={busy || !notes[r.id]?.trim()}>Từ chối</Button>
                    </>}
                </TableCell>
            </TableRow>)}</TableBody></Table>
        </div>}
    </ComponentCard>
}
