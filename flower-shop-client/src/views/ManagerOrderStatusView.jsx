import { useEffect, useState } from 'react'
import { managerOrderService } from '../services/managerOrderService'
import { managerShopService } from '../services/managerShopService'
import { formatPrice } from '../models/productModel'
import Badge from '../components/ui/badge/Badge'
import Button from '../components/ui/button/Button'
import { Table, TableBody, TableCell, TableHeader, TableRow } from '../components/ui/table'
import ComponentCard from '../components/common/ComponentCard'
import { useConfirm } from '../components/ui/confirm/ConfirmProvider'
import { orderStatusColors as statusColors, orderStatusLabels as statusLabels } from '../models/orderStatus'

/**
 * Màn hình đổi trạng thái từng đơn của shop, ghi thẳng vào DB, không gọi GHN:
 * managerOrderService.setStatus -> POST .../orders/{id}/set-status -> ManagerOrderService.changeStatus.
 * Đơn đã có vận đơn GHN không lùi về trước giai đoạn giao được (backend trả lỗi, hiện ở khung báo lỗi).
 */
export default function ManagerOrderStatusView() {
    const { confirm } = useConfirm()
    const [shop, setShop] = useState(null)
    const [data, setData] = useState(null)
    const [page, setPage] = useState(0)
    const [statusFilter, setStatusFilter] = useState('')
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState('')
    const [notice, setNotice] = useState('')

    // Lấy shop đầu tiên của user: managerShopService.list('SHOP')
    useEffect(() => {
        let active = true
        managerShopService.list('SHOP').then(shops => {
            if (active && shops.length > 0) setShop(shops[0])
        }).catch(e => { if (active) setError(e.message) })
        return () => { active = false }
    }, [])

    // Tải đơn của trang pg theo bộ lọc: managerOrderService.list
    const load = async (pg, keepNotice = false) => {
        if (!shop) return
        setBusy(true); setError('')
        if (!keepNotice) setNotice('')
        try {
            setData(await managerOrderService.list(shop.id, pg, statusFilter || null))
            setPage(pg)
        } catch (e) { setError(e.message) } finally { setBusy(false) }
    }
    useEffect(() => { if (shop) load(0) }, [shop?.id, statusFilter])

    // Xác nhận rồi đổi trạng thái đơn: managerOrderService.setStatus, sau đó tải lại danh sách
    const change = async (order, next) => {
        if (!next) return
        if (!await confirm({
            title: 'Đổi trạng thái đơn',
            message: `Chuyển đơn ${order.id.substring(0, 8)} từ "${statusLabels[order.status]}" sang "${statusLabels[next]}"? Chỉ cập nhật trong database, không gọi GHN.`,
            tone: next === 'CANCELLED' ? 'danger' : 'primary',
        })) return
        setBusy(true); setError(''); setNotice('')
        try {
            const result = await managerOrderService.setStatus(shop.id, order.id, next)
            await load(page, true)
            setNotice(`Đã chuyển đơn ${order.id.substring(0, 8)} sang "${statusLabels[next]}".${result.warning ? ' ' + result.warning : ''}`)
        } catch (e) { setError(e.message) } finally { setBusy(false) }
    }

    if (!shop) return <div className="p-8"><p className="text-gray-500">{error || 'Đang tải thông tin shop…'}</p></div>

    return <div className="space-y-6">
        <ComponentCard title="Chuyển trạng thái đơn hàng">
            <p className="mb-4 text-sm text-gray-500">
                Đổi trạng thái chỉ ghi vào database, không gọi GHN. Dùng "Kiểm tra trạng thái" ở mục Quản lý đơn hàng khi muốn lấy lại trạng thái thật từ GHN.
            </p>
            <div className="flex items-center gap-4 mb-4">
                <select aria-label="Lọc theo trạng thái" className="h-10 rounded-lg border border-gray-300 px-3 text-sm dark:border-gray-700 dark:bg-gray-900" value={statusFilter} onChange={e => setStatusFilter(e.target.value)}>
                    <option value="">Tất cả trạng thái</option>
                    {Object.entries(statusLabels).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                </select>
                <Button size="sm" variant="outline" onClick={() => load(page)} disabled={busy}>Tải lại</Button>
            </div>
            {error && <div className="mb-4 rounded-lg bg-error-50 p-4 text-sm text-error-500" role="alert">{error}</div>}
            {notice && <div className="mb-4 rounded-lg bg-success-50 p-4 text-sm text-success-600" role="status">{notice}</div>}
            {busy && !data && <p className="text-gray-500">Đang tải…</p>}
            {data && data.content.length === 0 && <p className="text-gray-500">Chưa có đơn hàng nào.</p>}
            {data && data.content.length > 0 && <>
                <div className="overflow-hidden rounded-xl border border-gray-200 dark:border-white/5">
                    <Table><TableHeader><TableRow>
                        <TableCell isHeader>Mã đơn</TableCell>
                        <TableCell isHeader>Khách hàng</TableCell>
                        <TableCell isHeader>Tổng tiền</TableCell>
                        <TableCell isHeader>Trạng thái</TableCell>
                        <TableCell isHeader className="text-right">Chuyển sang</TableCell>
                    </TableRow></TableHeader>
                    <TableBody>{data.content.map(o => <TableRow key={o.id}>
                        <TableCell className="font-mono text-xs">{o.id.substring(0, 8)}</TableCell>
                        <TableCell>{o.customerName || '-'}</TableCell>
                        <TableCell>{formatPrice(o.totalAmount)}</TableCell>
                        <TableCell><Badge color={statusColors[o.status] || 'primary'}>{statusLabels[o.status] || o.status}</Badge></TableCell>
                        <TableCell className="text-right">
                            <select aria-label={`Chuyển trạng thái đơn ${o.id.substring(0, 8)}`} className="h-9 rounded-lg border border-gray-300 px-2 text-sm dark:border-gray-700 dark:bg-gray-900"
                                value="" disabled={busy} onChange={e => change(o, e.target.value)}>
                                <option value="">Chọn trạng thái</option>
                                {Object.entries(statusLabels).filter(([k]) => k !== o.status).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                            </select>
                        </TableCell>
                    </TableRow>)}</TableBody></Table>
                </div>
                <div className="flex justify-between mt-4">
                    <Button size="sm" variant="outline" disabled={busy || page === 0} onClick={() => load(page - 1)}>Trang trước</Button>
                    <span className="text-sm text-gray-500">Trang {page + 1}/{data.totalPages || 1}</span>
                    <Button size="sm" variant="outline" disabled={busy || page + 1 >= data.totalPages} onClick={() => load(page + 1)}>Trang sau</Button>
                </div>
            </>}
        </ComponentCard>
    </div>
}
