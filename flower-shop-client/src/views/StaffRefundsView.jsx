import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { staffRefundService } from '../services/staffRefundService'
import { managerShopService } from '../services/managerShopService'
import Button from '../components/ui/button/Button'
import ComponentCard from '../components/common/ComponentCard'
import RefundTable from '../components/refunds/RefundTable'

/**
 * Trang Hoàn tiền của chủ shop và staff: gọi khách theo SĐT rồi đánh dấu tiến độ
 * Chờ liên hệ -> Đã liên hệ -> Đã hoàn tiền, hoặc Từ chối kèm phản hồi cho khách.
 */
export default function StaffRefundsView() {
    const { user } = useAuth()
    const role = user?.role === 'SHOP' ? 'SHOP' : 'SHOP_STAFF'
    const [shop, setShop] = useState(null)
    const [refunds, setRefunds] = useState(null)
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState('')

    // Chủ shop lấy shop của mình, staff lấy shop đầu tiên mình thuộc về
    useEffect(() => {
        if (!user) return
        managerShopService.list(role)
            .then(shops => shops.length ? setShop(shops[0]) : setError('Bạn chưa thuộc cửa hàng nào.'))
            .catch(e => setError(e.message))
    }, [user?.id, role])

    const load = async () => {
        setBusy(true); setError('')
        try { setRefunds(await staffRefundService.list(shop.id, role)) } catch (e) { setError(e.message) } finally { setBusy(false) }
    }
    useEffect(() => { if (shop) load() }, [shop?.id])

    const act = async (id, status, note) => {
        setBusy(true); setError('')
        try {
            const updated = await staffRefundService.setStatus(shop.id, id, status, role, note)
            setRefunds(list => list.map(r => r.id === id ? updated : r))
        } catch (e) { setError(e.message) } finally { setBusy(false) }
    }

    return <ComponentCard title={`Yêu cầu hoàn tiền${shop ? ` · ${shop.name}` : ''}`}>
        <div className="mb-4"><Button size="sm" variant="outline" onClick={load} disabled={busy || !shop}>Tải lại</Button></div>
        {error && <div className="mb-4 rounded-lg bg-error-50 p-4 text-sm text-error-500">{error}</div>}
        {refunds && refunds.length === 0 && <p className="text-gray-500">Không có yêu cầu hoàn tiền nào.</p>}
        {refunds && refunds.length > 0 && <RefundTable refunds={refunds} busy={busy} onAction={act} />}
    </ComponentCard>
}
