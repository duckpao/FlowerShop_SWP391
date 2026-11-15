import { useState } from 'react'
import { formatPrice } from '../../models/productModel'
import Badge from '../ui/badge/Badge'
import Button from '../ui/button/Button'
import { Table, TableBody, TableCell, TableHeader, TableRow } from '../ui/table'

export const refundStatusLabels = { REQUESTED: 'Chờ liên hệ', APPROVED: 'Đã liên hệ', PROCESSED: 'Đã hoàn tiền', REJECTED: 'Từ chối', CANCELLED: 'Khách đã hủy' }
export const refundStatusColors = { REQUESTED: 'warning', APPROVED: 'info', PROCESSED: 'success', REJECTED: 'error', CANCELLED: 'light' }

/**
 * Bảng yêu cầu hoàn tiền dùng chung cho chủ shop, staff và admin.
 * onAction(id, status, note) trả Promise; từ chối bắt buộc nhập phản hồi cho khách.
 */
export default function RefundTable({ refunds, busy, onAction, showShop = false }) {
    const [rejecting, setRejecting] = useState(null)
    const [note, setNote] = useState('')

    const reject = async id => {
        if (!note.trim()) return
        await onAction(id, 'REJECTED', note.trim())
        setRejecting(null); setNote('')
    }

    return <div className="overflow-x-auto rounded-xl border border-gray-200 dark:border-white/5">
        <Table><TableHeader><TableRow>
            <TableCell isHeader>Mã đơn</TableCell>
            {showShop && <TableCell isHeader>Cửa hàng</TableCell>}
            <TableCell isHeader>Khách hàng</TableCell>
            <TableCell isHeader>Cần hoàn</TableCell>
            <TableCell isHeader>Lý do</TableCell>
            <TableCell isHeader>Trạng thái</TableCell>
            <TableCell isHeader className="text-right">Hành động</TableCell>
        </TableRow></TableHeader>
        <TableBody>{refunds.map(r => <TableRow key={r.id}>
            <TableCell className="font-mono text-xs">{r.orderId.substring(0, 8)}</TableCell>
            {showShop && <TableCell>{r.shopName || '-'}</TableCell>}
            <TableCell>{r.customerName || '-'}<br />{r.customerPhone ? <a className="text-brand-500" href={`tel:${r.customerPhone}`}>{r.customerPhone}</a> : '-'}</TableCell>
            <TableCell>{formatPrice(r.amount)}</TableCell>
            <TableCell className="max-w-xs text-sm text-gray-500">
                <p>{r.reason}</p>
                {r.description && <p className="mt-1 whitespace-pre-wrap">{r.description}</p>}
                {r.images?.length > 0 && <div className="mt-1 flex flex-wrap gap-1">{r.images.map(url => <a key={url} href={url} target="_blank" rel="noreferrer"><img src={url} alt="Ảnh bằng chứng" className="h-10 w-10 rounded object-cover" /></a>)}</div>}
                {r.shopResponse && <p className="mt-1 text-error-500">Phản hồi: {r.shopResponse}</p>}
                <p className="mt-1 text-xs">{new Date(r.createdDate).toLocaleString('vi-VN')}</p>
            </TableCell>
            <TableCell><Badge color={refundStatusColors[r.status]}>{refundStatusLabels[r.status] || r.status}</Badge></TableCell>
            <TableCell className="text-right">
                {rejecting === r.id
                    ? <div className="flex flex-col items-end gap-2">
                        <textarea className="w-56 rounded-lg border border-gray-300 px-3 py-2 text-sm dark:border-white/10 dark:bg-gray-800" rows={2} maxLength={1000}
                            placeholder="Lý do từ chối (khách sẽ thấy)" value={note} onChange={e => setNote(e.target.value)} />
                        <div className="space-x-2">
                            <Button size="xs" onClick={() => reject(r.id)} disabled={busy || !note.trim()}>Xác nhận từ chối</Button>
                            <Button size="xs" variant="outline" onClick={() => { setRejecting(null); setNote('') }}>Hủy</Button>
                        </div>
                    </div>
                    : <div className="space-x-2">
                        {r.status === 'REQUESTED' && <Button size="xs" onClick={() => onAction(r.id, 'APPROVED')} disabled={busy}>Đã liên hệ</Button>}
                        {r.status === 'APPROVED' && <Button size="xs" onClick={() => onAction(r.id, 'PROCESSED')} disabled={busy}>Đã hoàn tiền</Button>}
                        {(r.status === 'REQUESTED' || r.status === 'APPROVED') && <Button size="xs" variant="outline" onClick={() => { setRejecting(r.id); setNote('') }} disabled={busy}>Từ chối</Button>}
                    </div>}
            </TableCell>
        </TableRow>)}</TableBody></Table>
    </div>
}
