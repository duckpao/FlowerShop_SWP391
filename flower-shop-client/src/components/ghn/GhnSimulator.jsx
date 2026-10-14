import { useState } from 'react'
import Button from '../ui/button/Button'
import { useConfirm } from '../ui/confirm/ConfirmProvider'
import { managerOrderService } from '../../services/managerOrderService'
import { ghnActions, ghnStages, ghnStatusLabels } from '../../models/orderStatus'

const time = () => new Date().toLocaleTimeString('vi-VN')

/**
 * Drives a GHN staging shipment. Every button calls the backend, which calls GHN's
 * switch-status API and then reads the shipment back, so the status shown is GHN's own.
 */
export default function GhnSimulator({ shopId, orderId, delivery, onChanged }) {
    const { confirm } = useConfirm()
    const [current, setCurrent] = useState(delivery.ghnStatus || 'ready_to_pick')
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState('')
    const [log, setLog] = useState([])

    const record = entry => setLog(items => [{ ...entry, at: time() }, ...items])

    const run = async (label, call) => {
        setBusy(true); setError('')
        try {
            const result = await call()
            setCurrent(result.ghnStatus || current)
            record({ label, ok: true, detail: `GHN trả về: ${ghnStatusLabels[result.ghnStatus] || result.ghnStatus}` })
            await onChanged?.(result)
        } catch (e) {
            setError(e.message)
            record({ label, ok: false, detail: e.message })
        } finally { setBusy(false) }
    }

    const push = async action => {
        const { label, danger } = ghnActions[action]
        const ok = await confirm({
            title: label,
            message: `Gửi yêu cầu "${label}" cho vận đơn ${delivery.trackingCode} sang GHN?`,
            confirmLabel: 'Gửi sang GHN',
            tone: danger ? 'danger' : 'primary',
        })
        if (ok) run(label, () => managerOrderService.pushGhnStatus(shopId, orderId, action))
    }

    const stageIndex = ghnStages.findIndex(stage => stage.statuses.includes(current))
    const available = Object.entries(ghnActions).filter(([, a]) => a.from.includes(current))

    return <section aria-label="Giả lập trạng thái GHN" className="rounded-lg border border-gray-200 p-4 dark:border-white/10">
        <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
            <div>
                <h4 className="font-semibold text-gray-900 dark:text-white">Trạng thái vận đơn GHN</h4>
                <p className="mt-1 text-sm text-gray-500">Mã vận đơn <span className="font-mono">{delivery.trackingCode}</span></p>
            </div>
            <Button size="sm" variant="outline" disabled={busy}
                onClick={() => run('Đồng bộ', () => managerOrderService.refreshStatus(shopId, orderId))}>
                {busy ? 'Đang gọi GHN…' : 'Đồng bộ từ GHN'}
            </Button>
        </div>

        <ol className="mb-4 grid grid-cols-2 gap-2 sm:grid-cols-4">
            {ghnStages.map((stage, index) => {
                const done = stageIndex > index
                const active = stageIndex === index
                return <li key={stage.label} aria-current={active ? 'step' : undefined}
                    className={`rounded-lg border px-3 py-2 text-sm ${
                        active ? 'border-brand-500 bg-brand-50 font-medium text-brand-700 dark:bg-brand-500/10 dark:text-brand-300'
                        : done ? 'border-success-200 text-success-700 dark:border-success-500/30 dark:text-success-400'
                        : 'border-gray-200 text-gray-500 dark:border-white/10'}`}>
                    {stage.label}
                </li>
            })}
        </ol>

        <p className="mb-3 text-sm text-gray-700 dark:text-gray-300" role="status">
            Hiện tại: <strong>{ghnStatusLabels[current] || current}</strong>
        </p>

        {available.length > 0
            ? <div className="flex flex-wrap gap-2">
                {available.map(([action, { label, danger }]) => <Button key={action} size="sm" disabled={busy}
                    variant={danger ? 'outline' : 'primary'} onClick={() => push(action)}>
                    {label}
                </Button>)}
            </div>
            : <p className="text-sm text-gray-500">Ở trạng thái này GHN không nhận thao tác từ shop. Dùng "Đồng bộ từ GHN" để cập nhật.</p>}

        {error && <p className="mt-3 text-sm text-error-500" role="alert">{error}</p>}

        {log.length > 0 && <ul className="mt-4 space-y-1 border-t border-gray-100 pt-3 text-xs dark:border-white/5">
            {log.map((item, index) => <li key={index} className={item.ok ? 'text-gray-600 dark:text-gray-400' : 'text-error-500'}>
                <span className="font-mono">{item.at}</span> {item.label}: {item.detail}
            </li>)}
        </ul>}
    </section>
}
