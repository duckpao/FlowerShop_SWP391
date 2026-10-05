import { useEffect, useState } from 'react'
import { managerOrderService } from '../services/managerOrderService'
import { managerShopService } from '../services/managerShopService'
import { formatPrice } from '../models/productModel'
import Badge from '../components/ui/badge/Badge'
import Button from '../components/ui/button/Button'
import { Table, TableBody, TableCell, TableHeader, TableRow } from '../components/ui/table'
import ComponentCard from '../components/common/ComponentCard'

const statusColors = {
    PENDING:'warning', PROCESSING:'info', DELIVERING:'info', COMPLETED:'success', CANCELLED:'error', REFUNDED:'error'
}
const statusLabels = {
    PENDING:'Chờ xác nhận', PROCESSING:'Đang xử lý', DELIVERING:'Đang giao', COMPLETED:'Đã giao', CANCELLED:'Đã hủy', REFUNDED:'Đã hoàn tiền'
}

export default function ManagerOrdersView({ shop: initialShop, onBack }) {
    const [shop, setShop] = useState(initialShop)
    const [data, setData] = useState(null)
    const [page, setPage] = useState(0)
    const [statusFilter, setStatusFilter] = useState('')
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState('')
    const [notice, setNotice] = useState('')

    useEffect(() => {
        if (initialShop) return
        let active = true
        managerShopService.list("SHOP").then(shops => {
            if (active && shops.length > 0) setShop(shops[0])
        }).catch(e => { if (active) setError(e.message) })
        return () => { active = false }
    }, [initialShop])

    const load = pg => {
        if (!shop) return
        setBusy(true); setError('')
        managerOrderService.list(shop.id, pg, statusFilter || null)
            .then(d => { setData(d); setPage(pg) })
            .catch(e => setError(e.message))
            .finally(() => setBusy(false))
    }
    useEffect(() => { if (shop) load(0) }, [shop?.id, statusFilter])

    const ship = async orderId => {
        if (!confirm('Xác nhận & tạo vận đơn GHN?')) return
        setBusy(true); setError(''); setNotice('')
        try {
            const r = await managerOrderService.ship(shop.id, orderId)
            setNotice(`Đã tạo vận đơn: ${r.trackingCode}`)
            load(page)
        } catch (e) { setError(e.message) } finally { setBusy(false) }
    }

    const refresh = async orderId => {
        setBusy(true); setError(''); setNotice('')
        try {
            const status = await managerOrderService.refreshStatus(shop.id, orderId)
            setNotice(`Trạng thái đơn: ${status}`)
            load(page)
        } catch (e) { setError(e.message) } finally { setBusy(false) }
    }
if (!shop) return <div className="p-8"><p className="text-gray-500">Đang tải thông tin shop…</p></div>

    return <div className="space-y-6">
        <ComponentCard title="Quản lý đơn hàng">
            <div className="flex items-center gap-4 mb-4">
                {onBack && <Button size="sm" variant="outline" onClick={onBack}>← Quay lại</Button>}
                <select className="h-10 rounded-lg border border-gray-300 px-3 text-sm dark:border-gray-700 dark:bg-gray-900" value={statusFilter} onChange={e => setStatusFilter(e.target.value)}>
                    <option value="">Tất cả trạng thái</option>
                    {Object.entries(statusLabels).map(([k,v]) => <option key={k} value={k}>{v}</option>)}
                </select>
                <Button size="sm" variant="outline" onClick={() => load(page)} disabled={busy}>Tải lại</Button>
            </div>
            {error && <div className="mb-4 rounded-lg bg-error-50 p-4 text-sm text-error-500">{error}</div>}
            {notice && <div className="mb-4 rounded-lg bg-success-50 p-4 text-sm text-success-600">{notice}</div>}
            {busy && !data && <p className="text-gray-500">Đang tải…</p>}
            {data && data.content.length === 0 && <p className="text-gray-500">Chưa có đơn hàng nào.</p>}
            {data && data.content.length > 0 && <>
                <div className="overflow-hidden rounded-xl border border-gray-200 dark:border-white/5">
                    <Table><TableHeader><TableRow>
                        <TableCell isHeader>Mã đơn</TableCell>
                        <TableCell isHeader>Khách hàng</TableCell>
                        <TableCell isHeader>Tổng tiền</TableCell>
                        <TableCell isHeader>Trạng thái</TableCell>
                        <TableCell isHeader>Ngày đặt</TableCell>
                        <TableCell isHeader className="text-right">Hành động</TableCell>
                    </TableRow></TableHeader>
                    <TableBody>{data.content.map(o => <TableRow key={o.id}>
                        <TableCell className="font-mono text-xs">{o.id.substring(0,8)}</TableCell>
                        <TableCell>{o.customerName || '-'}</TableCell>
                        <TableCell>{formatPrice(o.totalAmount)}</TableCell>
                        <TableCell><Badge color={statusColors[o.status]||'primary'}>{statusLabels[o.status]||o.status}</Badge></TableCell>
                        <TableCell className="text-sm text-gray-500">{new Date(o.createdDate).toLocaleDateString('vi-VN')}</TableCell>
                        <TableCell className="text-right space-x-2">
                            {o.status==='PENDING' && <Button size="sm" onClick={()=>ship(o.id)} disabled={busy}>Xác nhận & Giao GHN</Button>}
                            {(o.status==='PROCESSING'||o.status==='DELIVERING') && <Button size="sm" variant="outline" onClick={()=>refresh(o.id)} disabled={busy}>Kiểm tra trạng thái</Button>}
                        </TableCell>
                    </TableRow>)}</TableBody></Table>
                </div>
                <div className="flex justify-between mt-4">
                    <Button size="sm" variant="outline" disabled={busy||page===0} onClick={()=>load(page-1)}>Trang trước</Button>
                    <span className="text-sm text-gray-500">Trang {page+1}/{data.totalPages||1}</span>
                    <Button size="sm" variant="outline" disabled={busy||page+1>=data.totalPages} onClick={()=>load(page+1)}>Trang sau</Button>
                </div>
            </>}
        </ComponentCard>
    </div>
}