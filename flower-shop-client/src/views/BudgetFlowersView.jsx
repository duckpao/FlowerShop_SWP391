import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { staffApplicationService } from '../services/staffApplicationService'
import { BudgetSuggestions, customButton } from '../components/customFlowers/CustomFlowerParts'
import '../styles/custom-flowers.css'

export default function BudgetFlowersView() {
  const [params] = useSearchParams()
  const [shops, setShops] = useState([])
  const [error, setError] = useState('')
  const [draft, setDraft] = useState({ budget: '500000', quantity: '1', shopId: params.get('shopId') || '' })
  const [search, setSearch] = useState(draft)
  useEffect(() => { let active = true; staffApplicationService.search('').then(v => { if (active) setShops(v) }).catch(e => { if (active) setError(e.message) }); return () => { active = false } }, [])
  return <main className="catalog-page custom-flower-page">
    <header className="catalog-page-header"><div><p className="text-sm text-brand-600">Hoa dành cho bạn</p><h1>Chọn hoa theo ngân sách</h1></div><Link to="/custom-orders/new" className={customButton}>Tự mô tả thiết kế</Link></header>
    <section className="catalog-panel space-y-6">
      <p>Nhập tổng ngân sách và số bó/mẫu cần đặt. Hệ thống chỉ gợi ý các sản phẩm còn đủ hàng, không vượt mức bạn chọn.</p>
      {error && <p role="alert" className="message error">{error}</p>}
      <form className="grid items-end gap-4 md:grid-cols-4" onSubmit={e => { e.preventDefault(); setSearch({ ...draft }) }}>
        <label>Ngân sách tổng (VND)<input type="number" min="1" max="9999999999.99" step="0.01" required value={draft.budget} onChange={e => setDraft({ ...draft, budget: e.target.value })} /></label>
        <label>Số bó/mẫu<input type="number" min="1" max="100" step="1" required value={draft.quantity} onChange={e => setDraft({ ...draft, quantity: e.target.value })} /></label>
        <label>Cửa hàng<select value={draft.shopId} onChange={e => setDraft({ ...draft, shopId: e.target.value })}><option value="">Tất cả cửa hàng</option>{shops.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}</select></label>
        <button className={customButton}>Tìm mẫu phù hợp</button>
      </form>
      <BudgetSuggestions {...search} />
    </section>
  </main>
}
