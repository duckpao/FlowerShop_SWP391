import { useAdminDashboardController } from '../controllers/useAdminDashboardController'
import ComponentCard from '../components/common/ComponentCard'
import Button from '../components/ui/button/Button'

const periods = [
  { value: 'DAY', label: 'Theo ngày', hint: '30 ngày gần nhất' },
  { value: 'MONTH', label: 'Theo tháng', hint: '12 tháng gần nhất' },
  { value: 'YEAR', label: 'Theo năm', hint: '5 năm gần nhất' },
]
const money = value => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(Number(value) || 0)
const number = value => new Intl.NumberFormat('vi-VN').format(Number(value) || 0)
const shortMoney = value => {
  const v = Number(value) || 0
  if (v >= 1e9) return `${(v / 1e9).toFixed(1)} tỷ`
  if (v >= 1e6) return `${(v / 1e6).toFixed(1)} tr`
  if (v >= 1e3) return `${Math.round(v / 1e3)}k`
  return String(v)
}

function Stat({ title, value, note, tone = 'text-gray-900 dark:text-white' }) {
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03]">
      <p className="text-sm text-gray-500 dark:text-gray-400">{title}</p>
      <p className={`mt-2 text-2xl font-semibold ${tone}`}>{value}</p>
      {note && <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">{note}</p>}
    </div>
  )
}

// Biểu đồ cột (xếp chồng) vẽ bằng HTML/CSS để không phụ thuộc thư viện ngoài.
function BarChart({ points, series, format }) {
  const totals = points.map(p => series.reduce((sum, s) => sum + (Number(s.value(p)) || 0), 0))
  const max = Math.max(...totals, 0)
  const step = Math.max(1, Math.ceil(points.length / 12))
  return (
    <div>
      <div className="mb-3 flex flex-wrap gap-4 text-xs text-gray-600 dark:text-gray-400">
        {series.map(s => (
          <span key={s.label} className="inline-flex items-center gap-1.5">
            <span className="h-2.5 w-2.5 rounded-sm" style={{ background: s.color }} />{s.label}
          </span>
        ))}
      </div>
      {max === 0 && <p className="py-10 text-center text-sm text-gray-500">Chưa có dữ liệu trong khoảng thời gian này.</p>}
      <div className={`flex h-48 items-end gap-1 ${max === 0 ? 'hidden' : ''}`}>
        {points.map((p, i) => {
          const tooltip = `${p.label}\n${series.map(s => `${s.label}: ${format(s.value(p))}`).join('\n')}`
          return (
            <div key={p.key} title={tooltip} className="flex h-full min-w-0 flex-1 flex-col justify-end">
              <div className="flex w-full flex-col-reverse overflow-hidden rounded-t" style={{ height: `${(totals[i] / max) * 100}%` }}>
                {series.map(s => {
                  const v = Number(s.value(p)) || 0
                  return v > 0 ? <div key={s.label} style={{ background: s.color, flexGrow: v }} /> : null
                })}
              </div>
            </div>
          )
        })}
      </div>
      <div className={`mt-2 flex gap-1 ${max === 0 ? 'hidden' : ''}`}>
        {points.map((p, i) => (
          <div key={p.key} className="min-w-0 flex-1 text-center text-[10px] text-gray-500 dark:text-gray-400">
            {i % step === 0 ? p.label : ''}
          </div>
        ))}
      </div>
    </div>
  )
}

export default function AdminOverviewView() {
  const c = useAdminDashboardController()
  const { data } = c
  const active = periods.find(p => p.value === c.granularity)
  const points = data?.series || []
  const t = data?.totals
  const s = data?.summary

  return (
    <div className="mb-10 space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-lg font-semibold text-gray-800 dark:text-white/90">Báo cáo hệ thống</h2>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {c.range ? 'Khoảng tuỳ chọn' : active.hint}{data ? ` (${data.from.split('-').reverse().join('/')} – ${data.to.split('-').reverse().join('/')})` : ''}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <div className="inline-flex rounded-lg border border-gray-200 p-1 dark:border-gray-700" role="tablist" aria-label="Kiểu báo cáo">
            {periods.map(p => (
              <button key={p.value} type="button" role="tab" aria-selected={c.granularity === p.value}
                disabled={c.busy && c.granularity === p.value}
                onClick={() => c.setGranularity(p.value)}
                className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${c.granularity === p.value
                  ? 'bg-brand-500 text-white' : 'text-gray-600 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-white/5'}`}>
                {p.label}
              </button>
            ))}
          </div>
          <Button size="sm" variant="outline" disabled={c.busy} onClick={c.reload}>{c.busy ? 'Đang tải…' : 'Tải lại'}</Button>
        </div>
      </div>

      <form onSubmit={c.applyRange} className="flex flex-wrap items-end gap-3 rounded-xl border border-gray-200 p-4 dark:border-gray-800">
        <div>
          <label htmlFor="report-from" className="mb-1 block text-xs font-medium text-gray-600 dark:text-gray-400">Từ ngày</label>
          <input id="report-from" type="date" value={c.draft.from} max={c.draft.to || undefined}
            onChange={e => c.setDraft({ ...c.draft, from: e.target.value })}
            className="h-10 rounded-lg border border-gray-300 bg-transparent px-3 text-sm dark:border-gray-700 dark:text-white" />
        </div>
        <div>
          <label htmlFor="report-to" className="mb-1 block text-xs font-medium text-gray-600 dark:text-gray-400">Đến ngày</label>
          <input id="report-to" type="date" value={c.draft.to} min={c.draft.from || undefined}
            onChange={e => c.setDraft({ ...c.draft, to: e.target.value })}
            className="h-10 rounded-lg border border-gray-300 bg-transparent px-3 text-sm dark:border-gray-700 dark:text-white" />
        </div>
        <Button size="sm" type="submit" disabled={c.busy}>Xem báo cáo</Button>
        {c.range && <Button size="sm" variant="outline" type="button" disabled={c.busy} onClick={c.resetRange}>Về mặc định</Button>}
        {c.range && <p className="w-full text-xs text-gray-500">Đang xem khoảng tuỳ chọn. Kiểu "Theo tháng/năm" sẽ gom trọn các tháng/năm chứa khoảng này.</p>}
        {c.rangeError && <p role="alert" className="w-full text-sm text-error-500">{c.rangeError}</p>}
      </form>

      {c.error && (
        <div role="alert" className="rounded-lg bg-error-50 p-4 text-sm text-error-500 dark:bg-error-500/10 dark:text-error-400">
          {c.error}
        </div>
      )}

      {!data && c.busy && <p className="py-10 text-center text-gray-500" role="status">Đang tải báo cáo…</p>}

      {data && (
        <>
          <section aria-label="Tổng quan toàn hệ thống">
            <h3 className="mb-3 text-sm font-semibold uppercase tracking-wide text-gray-500">Toàn hệ thống</h3>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
              <Stat title="Khách hàng" value={number(t.customers)} />
              <Stat title="Cửa hàng" value={number(t.shops)}
                note={`${number(t.activeShops)} hoạt động · ${number(t.pendingShops)} chờ duyệt · ${number(t.bannedShops)} bị khóa`} />
              <Stat title="Đơn xin mở shop chờ duyệt" value={number(t.pendingApplications)}
                tone={t.pendingApplications > 0 ? 'text-warning-600' : undefined} />
              <Stat title="Doanh thu hệ thống" value={money(t.orders.revenue)} tone="text-success-600" note="Tổng các đơn đã hoàn thành" />
            </div>
            <div className="mt-4 grid grid-cols-2 gap-4 xl:grid-cols-4">
              <Stat title="Đơn đang đặt" value={number(t.orders.ordersPlacing)} note="Chờ xác nhận, đặt cọc, đang chuẩn bị" />
              <Stat title="Đơn đang giao" value={number(t.orders.ordersDelivering)} />
              <Stat title="Đơn hoàn thành" value={number(t.orders.ordersCompleted)} />
              <Stat title="Đơn đã hủy / hoàn tiền" value={number(t.orders.ordersClosed)} />
            </div>
          </section>

          <section aria-label="Số liệu trong kỳ báo cáo">
            <h3 className="mb-3 text-sm font-semibold uppercase tracking-wide text-gray-500">Trong kỳ báo cáo ({c.range ? 'khoảng tuỳ chọn' : active.hint.toLowerCase()})</h3>
            <div className="grid grid-cols-2 gap-4 xl:grid-cols-4">
              <Stat title="Khách hàng mới" value={number(s.newCustomers)} />
              <Stat title="Chủ shop mới" value={number(s.newShops)} />
              <Stat title="Tổng đơn" value={number(s.ordersPlacing + s.ordersDelivering + s.ordersCompleted + s.ordersClosed)}
                note={`${number(s.ordersPlacing)} đang đặt · ${number(s.ordersDelivering)} đang giao · ${number(s.ordersCompleted)} hoàn thành`} />
              <Stat title="Doanh thu trong kỳ" value={money(s.revenue)} tone="text-success-600" />
            </div>
          </section>

          <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
            <ComponentCard title="Doanh thu">
              <BarChart points={points} format={money}
                series={[{ label: 'Doanh thu', color: '#12b76a', value: p => p.counts.revenue }]} />
              <p className="mt-2 text-xs text-gray-500">Cột cao nhất: {shortMoney(Math.max(...points.map(p => Number(p.counts.revenue) || 0), 0))}</p>
            </ComponentCard>
            <ComponentCard title="Đơn hàng theo trạng thái">
              <BarChart points={points} format={number}
                series={[
                  { label: 'Đang đặt', color: '#fdb022', value: p => p.counts.ordersPlacing },
                  { label: 'Đang giao', color: '#465fff', value: p => p.counts.ordersDelivering },
                  { label: 'Hoàn thành', color: '#12b76a', value: p => p.counts.ordersCompleted },
                  { label: 'Hủy / hoàn tiền', color: '#98a2b3', value: p => p.counts.ordersClosed },
                ]} />
            </ComponentCard>
            <ComponentCard title="Người dùng mới">
              <BarChart points={points} format={number}
                series={[
                  { label: 'Khách hàng', color: '#465fff', value: p => p.counts.newCustomers },
                  { label: 'Chủ shop', color: '#f04438', value: p => p.counts.newShops },
                ]} />
            </ComponentCard>
          </div>
        </>
      )}
    </div>
  )
}
