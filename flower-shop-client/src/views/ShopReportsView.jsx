import { useEffect, useState } from 'react'
import PageMeta from '../components/common/PageMeta'
import PageBreadCrumb from '../components/common/PageBreadCrumb'
import { shopReportService } from '../services/shopReportService'

const currency = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 })
const number = new Intl.NumberFormat('vi-VN')

const localDate = () => {
  const now = new Date()
  const offset = now.getTimezoneOffset() * 60000
  return new Date(now.getTime() - offset).toISOString().slice(0, 10)
}

const currentValue = period => {
  const today = localDate()
  if (period === 'DAY') return today
  if (period === 'YEAR') return today.slice(0, 4)
  return today.slice(0, 7)
}

const periodLabels = { DAY: 'Theo ngày', MONTH: 'Theo tháng', YEAR: 'Theo năm' }

function MetricCard({ label, value, note, tone = 'brand' }) {
  const tones = {
    brand: 'bg-brand-50 text-brand-600 dark:bg-brand-500/10 dark:text-brand-400',
    success: 'bg-success-50 text-success-600 dark:bg-success-500/10 dark:text-success-400',
    warning: 'bg-warning-50 text-warning-600 dark:bg-warning-500/10 dark:text-warning-400',
    error: 'bg-error-50 text-error-600 dark:bg-error-500/10 dark:text-error-400',
  }
  return <div className="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm dark:border-gray-800 dark:bg-white/3">
    <span className={`inline-flex rounded-lg px-2.5 py-1 text-xs font-semibold ${tones[tone]}`}>{label}</span>
    <p className="mt-4 text-2xl font-bold text-gray-900 dark:text-white">{value}</p>
    {note && <p className="mt-1 text-xs text-gray-500">{note}</p>}
  </div>
}

export function ShopReportSummary({ shop }) {
  const [report, setReport] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    shopReportService.get(shop.id, 'MONTH', currentValue('MONTH'))
      .then(result => { if (active) { setReport(result); setError('') } })
      .catch(requestError => { if (active) setError(requestError.message) })
    return () => { active = false }
  }, [shop.id])

  if (error) return <div role="alert" className="mb-8 rounded-lg bg-error-50 p-4 text-sm text-error-600 dark:bg-error-500/10">{error}</div>
  if (!report) return <p role="status" className="mb-8 text-sm text-gray-500">Đang tải thống kê tháng này…</p>

  return <div className="mb-8 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
    <MetricCard label="Doanh thu thực nhận tháng" value={currency.format(report.revenue.netRevenue)} note={`Đã thu ${currency.format(report.revenue.grossReceived)}`} tone="success" />
    <MetricCard label="Tổng đơn tháng" value={number.format(report.orders.total)} note={`${number.format(report.orders.completed)} đơn hoàn thành`} />
    <MetricCard label="Đang giao" value={number.format(report.orders.delivering)} note={`${number.format(report.orders.processing)} đơn đang xử lý`} tone="warning" />
    <MetricCard label="Khách đang theo dõi" value={number.format(report.followers.current)} note={`+${number.format(report.followers.newInPeriod)} trong tháng`} />
  </div>
}

export default function ShopReportsView({ shop }) {
  const [period, setPeriod] = useState('MONTH')
  const [value, setValue] = useState(currentValue('MONTH'))
  const [report, setReport] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    setLoading(true)
    shopReportService.get(shop.id, period, value)
      .then(result => { if (active) { setReport(result); setError('') } })
      .catch(requestError => { if (active) setError(requestError.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [shop.id, period, value])

  const changePeriod = nextPeriod => {
    setPeriod(nextPeriod)
    setValue(currentValue(nextPeriod))
  }

  const periodInput = period === 'DAY'
    ? <input type="date" value={value} onChange={event => setValue(event.target.value)} className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm dark:border-gray-700 dark:bg-gray-900" />
    : period === 'MONTH'
      ? <input type="month" value={value} onChange={event => setValue(event.target.value)} className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm dark:border-gray-700 dark:bg-gray-900" />
      : <input type="number" min="2000" max="2100" value={value} onChange={event => setValue(event.target.value)} className="w-28 rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm dark:border-gray-700 dark:bg-gray-900" aria-label="Năm báo cáo" />

  return <div className="space-y-6">
    <PageMeta title="Báo cáo thống kê | FlowerShop" description="Báo cáo hoạt động kinh doanh của cửa hàng" />
    <PageBreadCrumb pageTitle="Báo cáo thống kê" />

    <section className="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/3">
      <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-center">
        <div>
          <h2 className="text-xl font-bold text-gray-900 dark:text-white">Báo cáo của {shop.name}</h2>
          <p className="mt-1 text-sm text-gray-500">Theo dõi đơn hàng, doanh thu, người theo dõi và tình trạng hoàn sản phẩm.</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {Object.entries(periodLabels).map(([key, label]) => <button key={key} type="button" onClick={() => changePeriod(key)} className={`rounded-lg px-3 py-2 text-sm font-medium ${period === key ? 'bg-brand-500 text-white' : 'border border-gray-200 text-gray-600 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-gray-800'}`}>{label}</button>)}
          {periodInput}
        </div>
      </div>
      {report && <p className="mt-4 text-xs text-gray-500">Khoảng thống kê: {new Date(`${report.period.startDate}T00:00:00`).toLocaleDateString('vi-VN')} – {new Date(`${report.period.endDate}T00:00:00`).toLocaleDateString('vi-VN')}</p>}
    </section>

    {error && <div role="alert" className="rounded-lg bg-error-50 p-4 text-sm text-error-600 dark:bg-error-500/10">{error}</div>}
    {loading && <p role="status" className="text-sm text-gray-500">Đang tổng hợp báo cáo…</p>}

    {report && <>
      <section aria-labelledby="report-summary-heading">
        <h3 id="report-summary-heading" className="mb-3 text-lg font-semibold text-gray-900 dark:text-white">Tổng quan</h3>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <MetricCard label="Khách đang theo dõi" value={number.format(report.followers.current)} note={`+${number.format(report.followers.newInPeriod)} trong kỳ`} />
          <MetricCard label="Tổng số đơn" value={number.format(report.orders.total)} note={`${number.format(report.orders.completed)} đơn hoàn thành`} />
          <MetricCard label="Doanh thu thực nhận" value={currency.format(report.revenue.netRevenue)} note="Thu thành công trừ tiền đã hoàn" tone="success" />
          <MetricCard label="Sản phẩm hoàn trả" value={number.format(report.products.reduce((sum, product) => sum + product.returnedQuantity, 0))} note={`${number.format(report.orders.refunded)} đơn hoàn trả`} tone="error" />
        </div>
      </section>

      <section className="grid gap-6 xl:grid-cols-2">
        <div className="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/3">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Trạng thái đơn hàng</h3>
          <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-3">
            {[
              ['Đang xử lý', report.orders.processing, 'text-blue-600'],
              ['Đang giao', report.orders.delivering, 'text-brand-600'],
              ['Hoàn thành', report.orders.completed, 'text-success-600'],
              ['Đã hủy', report.orders.cancelled, 'text-gray-500'],
              ['Hoàn trả', report.orders.refunded, 'text-error-600'],
            ].map(([label, count, color]) => <div key={label} className="rounded-xl bg-gray-50 p-4 dark:bg-gray-900"><p className="text-xs text-gray-500">{label}</p><p className={`mt-1 text-xl font-bold ${color}`}>{number.format(count)}</p></div>)}
          </div>
        </div>

        <div className="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/3">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Doanh thu</h3>
          <dl className="mt-4 space-y-3 text-sm">
            <div className="flex justify-between gap-4 border-b border-gray-100 pb-3 dark:border-gray-800"><dt className="text-gray-500">Thanh toán thành công</dt><dd className="font-semibold text-gray-900 dark:text-white">{currency.format(report.revenue.grossReceived)}</dd></div>
            <div className="flex justify-between gap-4 border-b border-gray-100 pb-3 dark:border-gray-800"><dt className="text-gray-500">Tiền đã hoàn khách</dt><dd className="font-semibold text-error-600">− {currency.format(report.revenue.refundedAmount)}</dd></div>
            <div className="flex justify-between gap-4 pt-1"><dt className="font-semibold text-gray-900 dark:text-white">Doanh thu thực nhận</dt><dd className="text-lg font-bold text-success-600">{currency.format(report.revenue.netRevenue)}</dd></div>
          </dl>
        </div>
      </section>

      <section className="overflow-hidden rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/3">
        <div className="border-b border-gray-100 p-5 dark:border-gray-800">
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">Hoàn trả theo sản phẩm</h3>
          <p className="mt-1 text-sm text-gray-500">So sánh số lượng thuộc đơn giao thành công và đơn hoàn trả trong kỳ.</p>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full min-w-200 text-left text-sm">
            <thead className="bg-gray-50 text-xs uppercase text-gray-500 dark:bg-gray-900"><tr><th className="px-5 py-3">Sản phẩm</th><th className="px-5 py-3 text-right">Giao thành công</th><th className="px-5 py-3 text-right">Hoàn trả</th><th className="px-5 py-3 text-right">Chênh lệch</th><th className="px-5 py-3 text-right">Số đơn thành công/hoàn</th><th className="px-5 py-3 text-right">Tỷ lệ hoàn</th></tr></thead>
            <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
              {report.products.map(product => <tr key={product.productId}><td className="px-5 py-4 font-medium text-gray-900 dark:text-white">{product.productName}</td><td className="px-5 py-4 text-right text-success-600">{number.format(product.completedQuantity)}</td><td className="px-5 py-4 text-right text-error-600">{number.format(product.returnedQuantity)}</td><td className={`px-5 py-4 text-right font-semibold ${product.quantityDifference < 0 ? 'text-error-600' : 'text-gray-700 dark:text-gray-300'}`}>{number.format(product.quantityDifference)}</td><td className="px-5 py-4 text-right">{number.format(product.completedOrderCount)} / {number.format(product.returnedOrderCount)}</td><td className="px-5 py-4 text-right font-medium">{number.format(product.returnRate)}%</td></tr>)}
            </tbody>
          </table>
        </div>
        {report.products.length === 0 && <p className="p-8 text-center text-sm text-gray-500">Chưa có sản phẩm giao thành công hoặc hoàn trả trong kỳ này.</p>}
      </section>

      <p className="text-xs leading-5 text-gray-500">Đơn hàng và sản phẩm được tính theo ngày tạo đơn. Doanh thu được tính theo thời điểm thanh toán chuyển sang thành công; tiền hoàn được tính khi yêu cầu hoàn tiền đã xử lý.</p>
    </>}
  </div>
}
