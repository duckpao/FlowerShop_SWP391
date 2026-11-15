import { useEffect, useState } from 'react'
import { staffRefundService } from '../services/staffRefundService'
import Button from '../components/ui/button/Button'
import PageBreadCrumb from '../components/common/PageBreadCrumb'
import PageMeta from '../components/common/PageMeta'
import ComponentCard from '../components/common/ComponentCard'
import RefundTable, { refundStatusLabels } from '../components/refunds/RefundTable'

const filters = ['', 'REQUESTED', 'APPROVED', 'PROCESSED', 'REJECTED', 'CANCELLED']

/** Admin xem và xử lý yêu cầu hoàn tiền của mọi shop. */
export default function AdminRefundsView() {
    const [status, setStatus] = useState('')
    const [page, setPage] = useState(0)
    const [data, setData] = useState(null)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState('')

    const load = async () => {
        setBusy(true); setError('')
        try { setData(await staffRefundService.adminList(status, page)) } catch (e) { setError(e.message) } finally { setBusy(false) }
    }
    useEffect(() => { load() }, [status, page])

    const act = async (id, next, note) => {
        setBusy(true); setError('')
        try {
            const updated = await staffRefundService.adminSetStatus(id, next, note)
            setData(d => ({ ...d, content: d.content.map(r => r.id === id ? updated : r) }))
        } catch (e) { setError(e.message) } finally { setBusy(false) }
    }

    return <>
        <PageMeta title="Hoàn tiền" description="Yêu cầu hoàn tiền của hệ thống" />
        <PageBreadCrumb pageTitle="Hoàn tiền" />
        <ComponentCard title="Yêu cầu hoàn tiền">
            <div className="mb-4 flex flex-wrap gap-2">
                {filters.map(f => <Button key={f || 'all'} size="sm" variant={status === f ? 'primary' : 'outline'}
                    onClick={() => { setPage(0); setStatus(f) }} disabled={busy}>{f ? refundStatusLabels[f] : 'Tất cả'}</Button>)}
                <Button size="sm" variant="outline" onClick={load} disabled={busy}>Tải lại</Button>
            </div>
            {error && <div className="mb-4 rounded-lg bg-error-50 p-4 text-sm text-error-500">{error}</div>}
            {data && data.content.length === 0 && <p className="text-gray-500">Không có yêu cầu hoàn tiền nào.</p>}
            {data && data.content.length > 0 && <RefundTable refunds={data.content} busy={busy} onAction={act} showShop />}
            {data && data.totalPages > 1 && <div className="mt-4 flex items-center gap-3 text-sm">
                <Button size="sm" variant="outline" onClick={() => setPage(p => p - 1)} disabled={busy || page === 0}>Trước</Button>
                <span>Trang {page + 1} / {data.totalPages}</span>
                <Button size="sm" variant="outline" onClick={() => setPage(p => p + 1)} disabled={busy || page + 1 >= data.totalPages}>Sau</Button>
            </div>}
        </ComponentCard>
    </>
}
