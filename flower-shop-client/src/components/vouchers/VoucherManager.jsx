import { useEffect, useState } from 'react'
import { voucherService as api } from '../../services/voucherService'
import { formatPrice } from '../../models/productModel'
import { useConfirm } from '../ui/confirm/ConfirmProvider'
import Badge from '../ui/badge/Badge'
import Button from '../ui/button/Button'
import Input from '../form/input/InputField'
import Label from '../form/Label'
import { Table, TableBody, TableCell, TableHeader, TableRow } from '../ui/table'

const headCell = 'px-3 py-2 text-sm font-medium text-gray-500 dark:text-gray-400 text-start'
const bodyCell = 'px-3 py-2 text-sm text-start text-gray-700 dark:text-gray-300'
const selectClass = 'h-9 w-full rounded-lg border border-gray-300 bg-transparent px-3 text-sm text-gray-800 dark:border-gray-700 dark:bg-gray-900 dark:text-white/90'
const compact = 'h-9! px-3! py-1.5!'

const local = value => {
  const d = value ? new Date(value) : new Date()
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}
const empty = () => {
  const end = new Date(); end.setDate(end.getDate() + 30)
  return { code: '', discountType: 'PERCENTAGE', discountValue: 10, minOrderValue: 0, maxDiscountValue: 50000, startDate: local(), endDate: local(end), usageLimit: 100, status: 'ACTIVE' }
}
const show = value => value ? new Date(value).toLocaleDateString('vi-VN') : ''
const discountText = v => v.discountType === 'PERCENTAGE'
  ? `${Number(v.discountValue)}%${v.maxDiscountValue != null ? `, tối đa ${formatPrice(v.maxDiscountValue)}` : ''}`
  : formatPrice(v.discountValue)
const statusOf = v => {
  if (v.status === 'DISABLED') return ['warning', 'Đã tắt']
  if (v.status === 'EXPIRED' || new Date(v.endDate) < new Date()) return ['light', 'Hết hạn']
  if (new Date(v.startDate) > new Date()) return ['info', 'Chưa bắt đầu']
  if (v.usedCount >= v.usageLimit) return ['warning', 'Hết lượt']
  return ['success', 'Đang chạy']
}

function Field({ id, label, hint, children }) {
  return (
    <div title={hint}>
      <Label htmlFor={id} className="mb-1 text-xs">{label}</Label>
      {children}
    </div>
  )
}

/** Danh sách và form tạo/sửa/xóa mềm voucher. scope: { shopId } cho shop, {} cho voucher sàn. */
export default function VoucherManager({ scope, disabledReason }) {
  const { confirm } = useConfirm()
  const [items, setItems] = useState([])
  const [form, setForm] = useState(empty)
  const [editing, setEditing] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [revision, setRevision] = useState(0)
  const scopeKey = scope?.shopId || 'admin'

  useEffect(() => {
    let active = true
    api.list(scope).then(data => { if (active) setItems(data) }).catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [scopeKey, revision])

  const change = (key, value) => setForm(previous => ({ ...previous, [key]: value }))
  const reset = () => { setEditing(null); setForm(empty()) }
  const percent = form.discountType === 'PERCENTAGE'

  async function save(e) {
    e.preventDefault()
    if (busy) return
    setBusy(true); setError(''); setNotice('')
    try {
      await api.save(scope, editing, {
        code: form.code.trim(),
        discountType: form.discountType,
        discountValue: Number(form.discountValue),
        minOrderValue: Number(form.minOrderValue || 0),
        maxDiscountValue: form.maxDiscountValue === '' || form.maxDiscountValue == null ? null : Number(form.maxDiscountValue),
        startDate: form.startDate,
        endDate: form.endDate,
        usageLimit: Number(form.usageLimit),
        status: form.status,
      })
      reset(); setRevision(v => v + 1); setNotice('Đã lưu mã giảm giá.')
    } catch (err) { setError(err.message) } finally { setBusy(false) }
  }

  async function remove(voucher) {
    if (!await confirm({ title: 'Xóa mã giảm giá', message: `Xóa mã ${voucher.code}? Đơn đã dùng mã này không bị ảnh hưởng.` })) return
    setBusy(true); setError(''); setNotice('')
    try {
      await api.remove(scope, voucher.id)
      if (editing === voucher.id) reset()
      setRevision(v => v + 1); setNotice('Đã xóa mã giảm giá.')
    } catch (err) { setError(err.message) } finally { setBusy(false) }
  }

  function edit(voucher) {
    setEditing(voucher.id)
    setForm({ ...voucher, maxDiscountValue: voucher.maxDiscountValue ?? '', startDate: local(voucher.startDate), endDate: local(voucher.endDate) })
    setNotice(''); setError('')
  }

  return (
    <>
      {error && <div role="alert" className="mb-3 rounded-lg bg-error-50 px-4 py-2.5 text-sm text-error-600 dark:bg-error-500/10 dark:text-error-500">{error}</div>}
      {notice && <div role="status" className="mb-3 rounded-lg bg-success-50 px-4 py-2.5 text-sm text-success-600 dark:bg-success-500/10 dark:text-success-500">{notice}</div>}
      {disabledReason && <div role="alert" className="mb-3 rounded-lg bg-warning-50 px-4 py-2.5 text-sm text-warning-800">{disabledReason}</div>}

      <div className="mb-4 overflow-hidden rounded-xl border border-gray-200 bg-white dark:border-white/5 dark:bg-white/3">
        <div className="flex items-center justify-between border-b border-gray-100 px-4 py-2.5 dark:border-white/5">
          <h3 className="text-sm font-semibold text-gray-800 dark:text-white/90">Các mã hiện có ({items.length})</h3>
          <Button size="sm" variant="outline" onClick={reset} disabled={busy || (!editing && form.code === '')}>Tạo mã mới</Button>
        </div>
        <div className="max-w-full overflow-x-auto">
          <Table className="w-full">
            <TableHeader className="border-b border-gray-100 bg-gray-50 dark:border-white/5 dark:bg-white/5">
              <TableRow>
                <TableCell isHeader className={headCell}>Mã</TableCell>
                <TableCell isHeader className={headCell}>Mức giảm</TableCell>
                <TableCell isHeader className={headCell}>Đơn tối thiểu</TableCell>
                <TableCell isHeader className={headCell}>Thời gian</TableCell>
                <TableCell isHeader className={headCell}>Đã dùng</TableCell>
                <TableCell isHeader className={headCell}>Trạng thái</TableCell>
                <TableCell isHeader className={`${headCell} text-end`}>Hành động</TableCell>
              </TableRow>
            </TableHeader>
            <TableBody className="divide-y divide-gray-100 dark:divide-white/5">
              {!items.length ? (
                <TableRow>
                  <TableCell colSpan={7} className="px-4 py-5 text-center text-gray-500 dark:text-gray-400">Chưa có mã giảm giá nào.</TableCell>
                </TableRow>
              ) : items.map(v => {
                const [color, label] = statusOf(v)
                return (
                  <TableRow key={v.id} className={editing === v.id ? 'bg-brand-50/50 dark:bg-brand-500/5' : ''}>
                    <TableCell className={`${bodyCell} font-semibold text-gray-800 dark:text-white/90`}>{v.code}</TableCell>
                    <TableCell className={bodyCell}>{discountText(v)}</TableCell>
                    <TableCell className={bodyCell}>{formatPrice(v.minOrderValue || 0)}</TableCell>
                    <TableCell className={`${bodyCell} whitespace-nowrap`}>{show(v.startDate)} - {show(v.endDate)}</TableCell>
                    <TableCell className={bodyCell}>{v.usedCount}/{v.usageLimit}</TableCell>
                    <TableCell className={bodyCell}><Badge size="sm" color={color}>{label}</Badge></TableCell>
                    <TableCell className={`${bodyCell} text-end`}>
                      <div className="flex items-center justify-end gap-2">
                        <Button size="xs" variant="outline" onClick={() => edit(v)} disabled={busy}>Sửa</Button>
                        <Button size="xs" variant="outline" className="text-error-500 hover:bg-error-50" onClick={() => remove(v)} disabled={busy}>Xóa</Button>
                      </div>
                    </TableCell>
                  </TableRow>
                )
              })}
            </TableBody>
          </Table>
        </div>
      </div>

      <form onSubmit={save} className="rounded-xl border border-gray-200 bg-white dark:border-white/5 dark:bg-white/3">
        <div className="flex flex-wrap items-baseline justify-between gap-x-4 border-b border-gray-100 px-4 py-2.5 dark:border-white/5">
          <h3 className="text-sm font-semibold text-gray-800 dark:text-white/90">{editing ? 'Sửa mã giảm giá' : 'Tạo mã giảm giá mới'}</h3>
          <p className="text-xs text-gray-500 dark:text-gray-400">Mã chỉ giảm tiền đơn hàng. Mỗi khách chỉ dùng 1 mã.</p>
        </div>
        <fieldset disabled={busy || Boolean(disabledReason)} className="grid gap-x-3 gap-y-2.5 p-4 sm:grid-cols-2 lg:grid-cols-4">
          <Field id="vc-code" label="Mã" hint="3 đến 50 ký tự chữ, số, gạch ngang hoặc gạch dưới. Tự đổi sang chữ hoa.">
            <Input id="vc-code" className={compact} required maxLength={50} placeholder="Ví dụ: TET2026" value={form.code} onChange={e => change('code', e.target.value)} />
          </Field>
          <Field id="vc-type" label="Loại giảm">
            <select id="vc-type" className={selectClass} value={form.discountType} onChange={e => change('discountType', e.target.value)}>
              <option value="PERCENTAGE">Theo phần trăm</option>
              <option value="FIXED_AMOUNT">Số tiền cố định</option>
            </select>
          </Field>
          <Field id="vc-value" label={percent ? 'Phần trăm giảm (%)' : 'Số tiền giảm (đồng)'}>
            <Input id="vc-value" className={compact} required type="number" min="0.01" max={percent ? '100' : undefined} step={percent ? 0.01 : 1} value={form.discountValue} onChange={e => change('discountValue', e.target.value)} />
          </Field>
          <Field id="vc-max" label="Giảm tối đa (đồng)" hint={percent ? 'Bắt buộc với mã theo phần trăm.' : 'Để trống nếu không giới hạn.'}>
            <Input id="vc-max" className={compact} required={percent} type="number" min="1" step={1} value={form.maxDiscountValue} onChange={e => change('maxDiscountValue', e.target.value)} />
          </Field>
          <Field id="vc-min" label="Giá trị đơn tối thiểu (đồng)">
            <Input id="vc-min" className={compact} required type="number" min="0" step={1} value={form.minOrderValue} onChange={e => change('minOrderValue', e.target.value)} />
          </Field>
          <Field id="vc-limit" label="Số lượt dùng tối đa" hint="Tổng lượt của tất cả khách.">
            <Input id="vc-limit" className={compact} required type="number" min="1" step={1} value={form.usageLimit} onChange={e => change('usageLimit', e.target.value)} />
          </Field>
          <Field id="vc-start" label="Bắt đầu">
            <Input id="vc-start" className={compact} required type="datetime-local" value={form.startDate} onChange={e => change('startDate', e.target.value)} />
          </Field>
          <Field id="vc-end" label="Kết thúc">
            <Input id="vc-end" className={compact} required type="datetime-local" value={form.endDate} onChange={e => change('endDate', e.target.value)} />
          </Field>
          <Field id="vc-status" label="Trạng thái">
            <select id="vc-status" className={selectClass} value={form.status} onChange={e => change('status', e.target.value)}>
              <option value="ACTIVE">Đang bật</option>
              <option value="DISABLED">Đã tắt</option>
            </select>
          </Field>
        </fieldset>
        <div className="flex flex-wrap justify-end gap-2 border-t border-gray-100 px-4 py-2.5 dark:border-white/5">
          {editing && <Button size="sm" variant="outline" onClick={e => { e.preventDefault(); reset() }} disabled={busy}>Hủy sửa</Button>}
          <Button size="sm" disabled={busy || Boolean(disabledReason)}>{busy ? 'Đang lưu…' : editing ? 'Lưu thay đổi' : 'Tạo mã'}</Button>
        </div>
      </form>
    </>
  )
}
