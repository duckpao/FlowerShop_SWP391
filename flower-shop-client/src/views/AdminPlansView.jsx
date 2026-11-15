import { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { shopPlanService as api } from '../services/shopPlanService'
import { formatPrice } from '../models/productModel'
import { useConfirm } from '../components/ui/confirm/ConfirmProvider'
import PageBreadCrumb from '../components/common/PageBreadCrumb'
import PageMeta from '../components/common/PageMeta'
import Badge from '../components/ui/badge/Badge'
import Button from '../components/ui/button/Button'
import Input from '../components/form/input/InputField'
import Label from '../components/form/Label'
import { Table, TableBody, TableCell, TableHeader, TableRow } from '../components/ui/table'

const empty = () => ({ name: '', durationMonths: 1, price: 59000, productLimit: 15, featuredLimit: 3, enabled: true })

const headCell = 'px-5 py-3 font-medium text-gray-500 dark:text-gray-400 text-start'
const bodyCell = 'px-5 py-4 text-start text-gray-700 dark:text-gray-300'

function Field({ id, label, hint, children }) {
  return (
    <div>
      <Label htmlFor={id}>{label}</Label>
      {children}
      {hint && <p className="mt-1.5 text-xs text-gray-500 dark:text-gray-400">{hint}</p>}
    </div>
  )
}

export default function AdminPlansView() {
  const { user } = useAuth()
  const { confirm } = useConfirm()
  const [items, setItems] = useState([])
  const [form, setForm] = useState(empty)
  const [editing, setEditing] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [revision, setRevision] = useState(0)

  useEffect(() => {
    if (user?.role !== 'ADMIN') return
    let active = true
    api.adminPlans().then(data => { if (active) setItems(data) }).catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [user?.role, revision])

  const change = (key, value) => setForm(previous => ({ ...previous, [key]: value }))
  const reset = () => { setEditing(null); setForm(empty()) }

  async function save(e) {
    e.preventDefault()
    if (busy) return
    setBusy(true); setError(''); setNotice('')
    try {
      await api.savePlan(editing, {
        ...form,
        durationMonths: Number(form.durationMonths),
        price: Number(form.price),
        productLimit: form.productLimit === '' ? null : Number(form.productLimit),
        featuredLimit: Number(form.featuredLimit),
      })
      reset(); setRevision(v => v + 1); setNotice('Đã lưu cấu hình gói.')
    } catch (err) { setError(err.message) } finally { setBusy(false) }
  }

  async function remove(plan) {
    if (!await confirm({ title: 'Ngừng bán gói', message: `Ngừng bán ${plan.name}? Shop đã mua vẫn giữ thời hạn. Bạn có thể bật lại gói sau.` })) return
    setBusy(true); setError(''); setNotice('')
    try {
      await api.removePlan(plan.id)
      setRevision(v => v + 1)
      if (editing === plan.id) reset()
    } catch (err) { setError(err.message) } finally { setBusy(false) }
  }

  function edit(plan) {
    setEditing(plan.id)
    setForm({ ...plan, productLimit: plan.productLimit ?? '' })
    setNotice(''); setError('')
  }

  if (user?.role !== 'ADMIN') return <p className="p-6 text-gray-500">Chỉ admin được quản lý gói đăng ký.</p>

  const editingPlan = items.find(p => p.id === editing)

  return (
    <>
      <PageMeta title="Quản lý gói Pro | Admin" description="Cấu hình các gói Pro của shop" />
      <PageBreadCrumb pageTitle="Quản lý gói Pro" />

      {error && <div role="alert" className="mb-6 rounded-lg bg-error-50 p-4 text-sm text-error-600 dark:bg-error-500/10 dark:text-error-500">{error}</div>}
      {notice && <div role="status" className="mb-6 rounded-lg bg-success-50 p-4 text-sm text-success-600 dark:bg-success-500/10 dark:text-success-500">{notice}</div>}

      <div className="mb-6 overflow-hidden rounded-xl border border-gray-200 bg-white dark:border-white/5 dark:bg-white/3">
        <div className="flex items-center justify-between border-b border-gray-100 px-6 py-4 dark:border-white/5">
          <h3 className="font-semibold text-gray-800 dark:text-white/90">Các gói hiện có ({items.length})</h3>
          <Button size="sm" variant="outline" onClick={reset} disabled={busy || (!editing && form.name === '')}>Tạo gói mới</Button>
        </div>
        <div className="max-w-full overflow-x-auto">
          <Table>
            <TableHeader className="border-b border-gray-100 bg-gray-50 dark:border-white/5 dark:bg-white/5">
              <TableRow>
                <TableCell isHeader className={headCell}>Gói</TableCell>
                <TableCell isHeader className={headCell}>Giá</TableCell>
                <TableCell isHeader className={headCell}>Thời hạn</TableCell>
                <TableCell isHeader className={headCell}>Sản phẩm tối đa</TableCell>
                <TableCell isHeader className={headCell}>Nổi bật tối đa</TableCell>
                <TableCell isHeader className={headCell}>Trạng thái</TableCell>
                <TableCell isHeader className={`${headCell} text-end`}>Hành động</TableCell>
              </TableRow>
            </TableHeader>
            <TableBody className="divide-y divide-gray-100 dark:divide-white/5">
              {!items.length ? (
                <TableRow>
                  <TableCell colSpan={7} className="px-5 py-8 text-center text-gray-500 dark:text-gray-400">
                    Chưa có gói nào. Điền form bên dưới để tạo gói đầu tiên.
                  </TableCell>
                </TableRow>
              ) : items.map(p => (
                <TableRow key={p.id} className={editing === p.id ? 'bg-brand-50/50 dark:bg-brand-500/5' : ''}>
                  <TableCell className={`${bodyCell} font-medium text-gray-800 dark:text-white/90`}>{p.name}</TableCell>
                  <TableCell className={bodyCell}>{formatPrice(p.price)}</TableCell>
                  <TableCell className={bodyCell}>{p.durationMonths ? `${p.durationMonths} tháng` : 'Mặc định'}</TableCell>
                  <TableCell className={bodyCell}>{p.productLimit ?? 'Không giới hạn'}</TableCell>
                  <TableCell className={bodyCell}>{p.featuredLimit}</TableCell>
                  <TableCell className={bodyCell}>
                    <Badge size="sm" color={p.enabled ? 'success' : 'warning'}>{p.enabled ? 'Đang bán' : 'Ngừng bán'}</Badge>
                  </TableCell>
                  <TableCell className={`${bodyCell} text-end`}>
                    {p.id === 'FREE'
                      ? <span className="text-sm text-gray-500 dark:text-gray-400">Gói mặc định</span>
                      : (
                        <div className="flex items-center justify-end gap-2">
                          <Button size="xs" variant="outline" onClick={() => edit(p)} disabled={busy}>
                            {p.enabled ? 'Sửa' : 'Sửa hoặc bật lại'}
                          </Button>
                          {p.enabled && (
                            <Button size="xs" variant="outline" className="text-error-500 hover:bg-error-50" onClick={() => remove(p)} disabled={busy}>
                              Ngừng bán
                            </Button>
                          )}
                        </div>
                      )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      </div>

      <form onSubmit={save} className="rounded-xl border border-gray-200 bg-white dark:border-white/5 dark:bg-white/3">
        <div className="border-b border-gray-100 px-6 py-4 dark:border-white/5">
          <h3 className="font-semibold text-gray-800 dark:text-white/90">
            {editing ? `Sửa gói ${editingPlan?.name || ''}`.trim() : 'Tạo gói Pro mới'}
          </h3>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            Giá mới chỉ áp dụng cho lượt mua sau; hóa đơn đã tạo giữ nguyên số tiền. Quyền lợi mới áp dụng cho cả shop đang dùng gói.
          </p>
        </div>

        <fieldset disabled={busy} className="grid gap-5 p-6 sm:grid-cols-2 lg:grid-cols-3">
          <Field id="plan-name" label="Tên gói">
            <Input id="plan-name" required maxLength={100} placeholder="Ví dụ: Pro 3 tháng" value={form.name} onChange={e => change('name', e.target.value)} />
          </Field>
          <Field id="plan-months" label="Thời hạn (tháng)" hint="Từ 1 đến 120 tháng.">
            <Input id="plan-months" required type="number" min="1" max="120" value={form.durationMonths} onChange={e => change('durationMonths', e.target.value)} />
          </Field>
          <Field id="plan-price" label="Giá (đồng)">
            <Input id="plan-price" required type="number" min="1" step={1} value={form.price} onChange={e => change('price', e.target.value)} />
          </Field>
          <Field id="plan-products" label="Sản phẩm tối đa" hint="Tính trên tổng sản phẩm của shop, kể cả sản phẩm ẩn. Để trống nếu không giới hạn.">
            <Input id="plan-products" type="number" min="1" placeholder="Không giới hạn" value={form.productLimit} onChange={e => change('productLimit', e.target.value)} />
          </Field>
          <Field id="plan-featured" label="Sản phẩm nổi bật tối đa" hint="Nếu giảm số này, các mục nổi bật vượt giới hạn mới sẽ không hiển thị.">
            <Input id="plan-featured" required type="number" min="0" max="10000" value={form.featuredLimit} onChange={e => change('featuredLimit', e.target.value)} />
          </Field>
          <div className="flex items-start sm:pt-8">
            <label className="flex cursor-pointer items-center gap-3 text-sm text-gray-700 dark:text-gray-300">
              <input type="checkbox" className="h-4 w-4 rounded border-gray-300 text-brand-500 focus:ring-brand-500" checked={form.enabled} onChange={e => change('enabled', e.target.checked)} />
              Cho phép shop mua và kích hoạt gói này
            </label>
          </div>
        </fieldset>

        <div className="flex flex-wrap justify-end gap-3 border-t border-gray-100 px-6 py-4 dark:border-white/5">
          {editing && (
            <Button variant="outline" onClick={e => { e.preventDefault(); reset() }} disabled={busy}>Hủy sửa</Button>
          )}
          <Button disabled={busy}>{busy ? 'Đang lưu…' : editing ? 'Lưu thay đổi' : 'Tạo gói'}</Button>
        </div>
      </form>
    </>
  )
}
