import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { managerShopService } from '../services/managerShopService'
import { productService } from '../services/productService'
import { shopPlanService as api } from '../services/shopPlanService'
import paymentService from '../services/paymentService'
import { formatPrice } from '../models/productModel'

const date = value => value ? new Date(value).toLocaleString('vi-VN') : ''
export default function ShopSubscriptionView({ auth }) {
  const [params] = useSearchParams()
  const invoice = params.get('invoice_number')
  const [shop, setShop] = useState(null)
  const [plans, setPlans] = useState([])
  const [status, setStatus] = useState(null)
  const [products, setProducts] = useState(null)
  const [page, setPage] = useState(0)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [revision, setRevision] = useState(0)
  useEffect(() => {
    if (auth?.user?.role !== 'SHOP') return
    let active = true
    managerShopService.list('SHOP').then(items => { if (active) setShop(items[0] || null) })
      .catch(e => { if (active) setError(e.message) })
    api.plans().then(items => { if (active) setPlans(items) }).catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [auth?.user?.id, auth?.user?.role])
  useEffect(() => {
    if (!shop) return
    let active = true
    Promise.all([api.status(shop.id), productService.list(shop.id, page)])
      .then(([value, items]) => { if (active) { setStatus(value); setProducts(items) } })
      .catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [shop?.id, page, revision])
  useEffect(() => {
    if (!shop || !invoice) return
    let active = true, timer, attempts = 0
    async function check() {
      try {
        const result = await api.payment(shop.id, invoice)
        if (!active) return
        if (result.status === 'SUCCESS') {
          setNotice('Thanh toán thành công. Gói Pro đã được kích hoạt/gia hạn.')
          setRevision(v => v + 1)
        } else if (result.status === 'REVIEW') {
          setError('Giao dịch cần kiểm tra số tiền. Vui lòng liên hệ hỗ trợ.')
        } else {
          setNotice('Đang chờ SePay xác nhận thanh toán. Quyền Pro chỉ bật khi nhận đủ tiền.')
          if (++attempts < 20) timer = setTimeout(check, 15000)
        }
      } catch (e) { if (active) setError(e.message) }
    }
    check()
    return () => { active = false; clearTimeout(timer) }
  }, [shop?.id, invoice])
  async function buy(planId) {
    setBusy(true); setError('')
    try {
      const result = await api.checkout(shop.id, planId)
      paymentService.submitSePayCheckoutForm(result.checkoutUrl, result.fields)
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  async function toggle(product) {
    setBusy(true); setError('')
    try {
      await api.feature(shop.id, product.id, !status.featuredProductIds.includes(product.id))
      setStatus(await api.status(shop.id))
    } catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  async function reload() {
    setError('')
    try {
      if (invoice && shop) {
        const result = await api.payment(shop.id, invoice)
        setNotice(result.status === 'SUCCESS' ? 'Gói Pro đã được kích hoạt/gia hạn.' : result.status === 'REVIEW' ? 'Giao dịch cần hỗ trợ kiểm tra số tiền.' : 'SePay chưa xác nhận nhận đủ tiền.')
      }
      setRevision(v => v + 1)
    } catch (e) { setError(e.message) }
  }
  if (auth?.busy && !auth.user) return <p role="status">Đang kiểm tra đăng nhập…</p>
  if (!auth?.user) return <p>Vui lòng <Link to="/login?next=%2Fshop-admin%2Fsubscription">đăng nhập</Link>.</p>
  if (auth.user.role !== 'SHOP') return <p>Chỉ chủ shop được quản lý gói đăng ký.</p>
  return <main className="mx-auto max-w-6xl space-y-6">
    <header><h1 className="text-2xl font-bold">Gói đăng ký của shop</h1><p className="mt-2 text-gray-500">Chọn gói phù hợp với số sản phẩm và số vị trí nổi bật bạn cần.</p></header>
    {error && <p role="alert" className="rounded-xl bg-error-50 p-4 text-error-600">{error}</p>}
    {notice && <p role="status" className="rounded-xl bg-brand-50 p-4 text-brand-700">{notice}</p>}
    {status && <section className="rounded-xl border border-gray-200 p-5 dark:border-gray-700">
      <div className="flex flex-wrap items-center justify-between gap-3"><h2 className="font-semibold">Gói hiện tại: {status.planName}</h2><button type="button" onClick={reload} disabled={busy} className="rounded-lg border px-3 py-2 text-sm">Kiểm tra / Tải lại</button></div>
      {status.expiresAt && <p className="mt-2">{status.pro ? 'Hiệu lực đến' : 'Pro đã hết hạn lúc'}: {date(status.expiresAt)}</p>}
      <p className="mt-2">Đang có {status.productCount}/{status.productLimit ?? 'không giới hạn'} sản phẩm · {status.featuredLimit} vị trí nổi bật</p>
      {status.productLimit != null && status.productCount > status.productLimit && <p role="alert" className="mt-2 text-sm font-semibold text-error-500">{status.productCount - status.productLimit} sản phẩm vượt giới hạn đang bị khóa bán. Hãy nâng cấp gói hoặc xóa bớt sản phẩm. Hệ thống ưu tiên các sản phẩm được tạo trước.</p>}
      <p className="mt-2 text-sm text-gray-500">Giới hạn tính cả sản phẩm ẩn, không đặt lại theo tháng. Xóa sản phẩm sẽ giải phóng chỗ.</p>
      <p className="mt-2 text-sm text-gray-500">Gia hạn trước hạn sẽ cộng tiếp thời gian còn lại. Khi hết hạn, shop trở về Free; sản phẩm vẫn được giữ, quyền nổi bật dừng.</p>
    </section>}
    {!shop && !error && <p role="status">Đang tải cửa hàng. Cửa hàng cần được duyệt và đang hoạt động để mua gói.</p>}
    {shop && shop.status !== 'ACTIVE' && <p className="text-sm text-warning-600">Cửa hàng cần được duyệt và đang hoạt động trước khi đăng ký hoặc gia hạn Pro.</p>}
    <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5" aria-label="Các gói đăng ký">
      {plans.map(plan => <article key={plan.id} className="flex flex-col rounded-xl border border-gray-200 p-5 dark:border-gray-700">
        <h2 className="font-semibold">{plan.name}</h2><p className="my-4 text-xl font-bold text-brand-600">{formatPrice(plan.price)}</p>
        <p className="text-sm">{plan.productLimit == null ? 'Sản phẩm không giới hạn' : 'Tối đa ' + plan.productLimit + ' sản phẩm'}</p>
        <p className="mt-2 mb-5 text-sm">{plan.featuredLimit ? plan.featuredLimit + ' sản phẩm nổi bật' : 'Không có sản phẩm nổi bật'}</p>
        {plan.durationMonths > 0 ? <button type="button" className="mt-auto rounded-lg bg-brand-600 px-3 py-2 text-white disabled:opacity-50" disabled={!shop || shop.status !== 'ACTIVE' || busy} onClick={() => buy(plan.id)}>{busy ? 'Đang xử lý…' : status?.pro ? 'Gia hạn qua SePay' : 'Đăng ký qua SePay'}</button> : <p className="mt-auto text-sm text-gray-500">Gói mặc định</p>}
      </article>)}
    </section>
    {status && products && <section className="rounded-xl border border-gray-200 p-5 dark:border-gray-700">
      <h2 className="font-semibold">Sản phẩm nổi bật · {status.pro ? status.featuredProductIds.length : 0}/{status.featuredLimit}</h2>
      <p className="my-3 text-sm text-gray-500">{status.pro ? 'Chọn sản phẩm đang bán, còn hàng và không bị ẩn. Sản phẩm sẽ xuất hiện tại mục nổi bật trên trang danh sách hoa.' : 'Đăng ký Pro để đưa sản phẩm lên mục nổi bật.'}</p>
      <ul className="divide-y divide-gray-100 dark:divide-gray-800">{products.content.map(product => {
        const selected = status.featuredProductIds.includes(product.id)
        return <li key={product.id} className="flex items-center justify-between gap-3 py-3"><span>{product.name}</span><button type="button" disabled={busy || (!selected && (!status.pro || product.status !== 'ACTIVE' || product.adminHidden || product.stock < 1 || status.featuredProductIds.length >= status.featuredLimit))} onClick={() => toggle(product)} className="rounded-lg border px-3 py-2 text-sm disabled:opacity-40">{selected ? 'Bỏ nổi bật' : 'Chọn nổi bật'}</button></li>
      })}</ul>
      <div className="mt-4 flex items-center gap-4"><button disabled={busy || page === 0} onClick={() => setPage(v => v - 1)}>Trang trước</button><span>Trang {page + 1}</span><button disabled={busy || page + 1 >= products.totalPages} onClick={() => setPage(v => v + 1)}>Trang sau</button></div>
    </section>}
  </main>
}
