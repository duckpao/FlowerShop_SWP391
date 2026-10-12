import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { useAuth } from '../context/AuthContext'
import { customerOrderService } from '../services/customerOrderService'
import { formatPrice } from '../models/productModel'
import Badge from '../components/ui/badge/Badge'
import Button from '../components/ui/button/Button'
import { Table, TableBody, TableCell, TableHeader, TableRow } from '../components/ui/table'
import ComponentCard from '../components/common/ComponentCard'
import PageMeta from '../components/common/PageMeta'
import PageBreadCrumb from '../components/common/PageBreadCrumb'

import { orderStatusColors as statusColors, orderStatusLabels as statusLabels, orderTabs } from '../models/orderStatus'

export default function CustomerOrdersView() {
    const { user } = useAuth()
    const [orders, setOrders] = useState(null)
    const [page, setPage] = useState(0)
    const [status, setStatus] = useState('')
    const [busy, setBusy] = useState(true)
    const [error, setError] = useState('')
    const [reloadKey, setReloadKey] = useState(0)

    useEffect(() => {
        if (!user) return
        let active = true
        setBusy(true)
        setError('')
        customerOrderService.list(page, status)
            .then(data => { if (active) { setOrders(data); setError('') } })
            .catch(e => { if (active) setError(e.message) })
            .finally(() => { if (active) setBusy(false) })
        return () => { active = false }
    }, [user, page, status, reloadKey])

    if (!user) return <div className="max-w-4xl mx-auto p-8"><h1 className="text-xl font-bold">Đơn hàng của tôi</h1><p className="text-gray-500">Vui lòng đăng nhập.</p></div>

    return <div className="w-full">
        <PageMeta title="Đơn hàng của tôi | FlowerShop" />
        <PageBreadCrumb pageTitle="Đơn hàng của tôi" />
        <ComponentCard title="Danh sách đơn hàng">
            <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div role="tablist" aria-label="Lọc đơn hàng theo trạng thái" className="flex gap-2 overflow-x-auto whitespace-nowrap border-b border-gray-200 dark:border-gray-800">
                    {orderTabs.map(tab => <button key={tab.id} type="button" role="tab" aria-selected={status === tab.id}
                        onClick={() => { setStatus(tab.id); setPage(0) }}
                        className={`border-b-2 px-4 py-2 text-sm font-medium transition-colors ${status === tab.id ? 'border-brand-500 text-brand-500' : 'border-transparent text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'}`}>
                        {tab.label}
                    </button>)}
                </div>
                <Button size="sm" variant="outline" disabled={busy} onClick={() => setReloadKey(key => key + 1)}>
                    {busy ? 'Đang tải…' : 'Tải lại đơn hàng'}
                </Button>
            </div>
            {error && <div className="mb-4 rounded-lg bg-error-50 p-4 text-sm text-error-500">{error}</div>}
            {busy && <p className="text-gray-500">Đang tải…</p>}
            {orders && !busy && orders.content.length === 0 && <p className="text-gray-500">{status ? `Không có đơn nào ở trạng thái "${statusLabels[status]}".` : 'Chưa có đơn hàng nào.'}</p>}
            {orders && orders.content.length > 0 && <>
                <div className="overflow-hidden rounded-xl border border-gray-200 dark:border-white/5">
                    <Table>
                        <TableHeader><TableRow>
                            <TableCell isHeader>Mã đơn</TableCell>
                            <TableCell isHeader>Cửa hàng</TableCell>
                            <TableCell isHeader>Tổng tiền</TableCell>
                            <TableCell isHeader>Trạng thái</TableCell>
                            <TableCell isHeader>Ngày đặt</TableCell>
                            <TableCell isHeader className="text-right">Hành động</TableCell>
                        </TableRow></TableHeader>
                        <TableBody>{orders.content.map(o => <TableRow key={o.id}>
                            <TableCell className="font-mono text-xs">{o.id.substring(0,8)}</TableCell>
                            <TableCell>{o.shopName}</TableCell>
                            <TableCell>{formatPrice(o.totalAmount)}</TableCell>
                            <TableCell><Badge color={statusColors[o.status] || 'primary'}>{statusLabels[o.status] || o.status}</Badge></TableCell>
                            <TableCell className="text-sm text-gray-500">{new Date(o.createdDate).toLocaleDateString('vi-VN')}</TableCell>
                            <TableCell className="text-right"><Link to={`/orders/${o.id}`}><Button size="sm" variant="outline">Xem</Button></Link></TableCell>
                        </TableRow>)}</TableBody>
                    </Table>
                </div>
                <div className="flex justify-between mt-4">
                    <Button size="sm" variant="outline" disabled={busy || page === 0} onClick={() => setPage(p => p - 1)}>Trang trước</Button>
                    <span className="text-sm text-gray-500">Trang {page + 1} / {orders.totalPages || 1}</span>
                    <Button size="sm" variant="outline" disabled={busy || page + 1 >= orders.totalPages} onClick={() => setPage(p => p + 1)}>Trang sau</Button>
                </div>
            </>}
        </ComponentCard>
    </div>
}