import { useEffect, useState } from 'react'
import { adminReviewService } from '../services/adminReviewService'
import Badge from '../components/ui/badge/Badge'
import Button from '../components/ui/button/Button'
import Input from '../components/form/input/InputField'
import { Modal } from '../components/ui/modal'
import { Table, TableBody, TableCell, TableHeader, TableRow } from '../components/ui/table'
import PageBreadCrumb from '../components/common/PageBreadCrumb'
import PageMeta from '../components/common/PageMeta'
import ComponentCard from '../components/common/ComponentCard'
import { useConfirm } from '../components/ui/confirm/ConfirmProvider'

const stars = ['', '1', '2', '3', '4', '5']
const ratingColor = n => (n >= 4 ? 'success' : n === 3 ? 'warning' : 'error')

/** Admin xem đánh giá sản phẩm của mọi shop và xóa đánh giá vi phạm (khách được báo kèm lý do). */
export default function AdminReviewsView() {
    const { confirm } = useConfirm()
    const [rating, setRating] = useState('')
    const [q, setQ] = useState('')
    const [applied, setApplied] = useState('')
    const [page, setPage] = useState(0)
    const [data, setData] = useState(null)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState('')
    const [notice, setNotice] = useState('')
    const [target, setTarget] = useState(null)
    const [reason, setReason] = useState('')
    const [reload, setReload] = useState(0)

    useEffect(() => {
        let live = true
        setBusy(true); setError('')
        adminReviewService.list({ rating, q: applied, page, size: 10 })
            .then(r => { if (live) setData(r) })
            .catch(e => { if (live) setError(e.message) })
            .finally(() => { if (live) setBusy(false) })
        return () => { live = false }
    }, [rating, applied, page, reload])

    const search = e => { e.preventDefault(); setPage(0); setApplied(q.trim()) }
    const close = () => { setTarget(null); setReason('') }

    const remove = async () => {
        const ok = await confirm({
            title: 'Xóa đánh giá', tone: 'danger', confirmLabel: 'Xóa đánh giá',
            message: `Xóa hẳn đánh giá ${target.rating} sao của ${target.reviewerName} về "${target.productName}"? Khách sẽ nhận thông báo và có thể đánh giá lại.`,
        })
        if (!ok) return
        setBusy(true); setError('')
        try {
            await adminReviewService.remove(target.id, reason.trim())
            setNotice('Đã xóa đánh giá và thông báo cho khách.')
            close()
            if (data && data.content.length === 1 && page > 0) setPage(p => p - 1)
            else setReload(n => n + 1)
        } catch (e) { setError(e.message) } finally { setBusy(false) }
    }

    return <>
        <PageMeta title="Đánh giá sản phẩm | Admin" description="Kiểm duyệt đánh giá sản phẩm" />
        <PageBreadCrumb pageTitle="Đánh giá sản phẩm" />
        <ComponentCard title="Đánh giá của khách hàng">
            <div className="mb-4 flex flex-wrap gap-2">
                {stars.map(s => <Button key={s || 'all'} size="sm" variant={rating === s ? 'primary' : 'outline'}
                    onClick={() => { setPage(0); setRating(s) }} disabled={busy}>{s ? `${s} sao` : 'Tất cả'}</Button>)}
            </div>
            <form onSubmit={search} className="mb-4 flex flex-wrap items-end gap-3">
                <div className="w-full sm:w-80"><Input label="Từ khóa (nội dung, sản phẩm, shop, khách)" value={q} onChange={e => setQ(e.target.value)} placeholder="Nhập từ khóa..." disabled={busy} /></div>
                <Button size="sm" type="submit" disabled={busy}>Tìm kiếm</Button>
            </form>
            {error && <div className="mb-4 rounded-lg bg-error-50 p-4 text-sm text-error-500">{error}</div>}
            {notice && <div className="mb-4 rounded-lg bg-success-50 p-4 text-sm text-success-500">{notice}</div>}
            {data && data.content.length === 0 && <p className="text-gray-500">Không có đánh giá phù hợp.</p>}
            {data && data.content.length > 0 && <div className="overflow-x-auto rounded-xl border border-gray-200 dark:border-white/5">
                <Table><TableHeader><TableRow>
                    <TableCell isHeader>Sản phẩm / Shop</TableCell>
                    <TableCell isHeader>Khách hàng</TableCell>
                    <TableCell isHeader>Sao</TableCell>
                    <TableCell isHeader>Nội dung</TableCell>
                    <TableCell isHeader>Ngày</TableCell>
                    <TableCell isHeader className="text-right">Hành động</TableCell>
                </TableRow></TableHeader>
                <TableBody>{data.content.map(r => <TableRow key={r.id}>
                    <TableCell><p className="font-medium text-gray-800 dark:text-white/90">{r.productName}</p><p className="text-xs text-gray-500">{r.shopName}</p></TableCell>
                    <TableCell>{r.reviewerName}</TableCell>
                    <TableCell><Badge size="sm" color={ratingColor(r.rating)}>{r.rating} sao</Badge></TableCell>
                    <TableCell className="max-w-xs text-sm text-gray-500">
                        <p className="whitespace-pre-wrap break-words">{r.comment || '(không có nội dung)'}</p>
                        {r.shopReply && <p className="mt-1 text-xs">Shop phản hồi: {r.shopReply}</p>}
                    </TableCell>
                    <TableCell className="text-sm text-gray-500">{r.createdDate ? new Date(r.createdDate).toLocaleDateString('vi-VN') : '-'}</TableCell>
                    <TableCell className="text-right"><Button size="xs" variant="outline" className="text-error-500" onClick={() => { setNotice(''); setTarget(r) }} disabled={busy}>Xóa</Button></TableCell>
                </TableRow>)}</TableBody></Table>
            </div>}
            {data && data.totalPages > 1 && <div className="mt-4 flex items-center gap-3 text-sm">
                <Button size="sm" variant="outline" onClick={() => setPage(p => p - 1)} disabled={busy || page === 0}>Trước</Button>
                <span>Trang {page + 1} / {data.totalPages} ({data.totalElements} đánh giá)</span>
                <Button size="sm" variant="outline" onClick={() => setPage(p => p + 1)} disabled={busy || page + 1 >= data.totalPages}>Sau</Button>
            </div>}
        </ComponentCard>

        <Modal isOpen={!!target} onClose={close} className="max-w-lg p-6">
            {target && <div>
                <h3 className="mb-2 text-lg font-semibold text-gray-900 dark:text-white">Xóa đánh giá</h3>
                <p className="text-sm text-gray-600 dark:text-gray-400">{target.reviewerName} đánh giá {target.rating} sao cho "{target.productName}".</p>
                <p className="mt-2 whitespace-pre-wrap rounded-lg bg-gray-50 p-3 text-sm text-gray-700 dark:bg-white/5 dark:text-gray-300">{target.comment || '(không có nội dung)'}</p>
                <label htmlFor="review-reason" className="mb-1 mt-4 block text-sm font-medium text-gray-700 dark:text-gray-400">Lý do (không bắt buộc, khách sẽ thấy)</label>
                <textarea id="review-reason" rows={3} maxLength={500} value={reason} onChange={e => setReason(e.target.value)}
                    className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm dark:border-white/10 dark:bg-gray-800" placeholder="Ví dụ: Nội dung quảng cáo, ngôn từ xúc phạm" />
                <div className="mt-5 flex justify-end gap-3">
                    <Button size="sm" variant="outline" onClick={close} disabled={busy}>Hủy</Button>
                    <Button size="sm" className="bg-error-500 hover:bg-error-600 text-white" onClick={remove} disabled={busy}>Xóa đánh giá</Button>
                </div>
            </div>}
        </Modal>
    </>
}
