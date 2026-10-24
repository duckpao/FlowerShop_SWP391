import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useOccasionsController } from '../controllers/useOccasionsController'
import { useConfirm } from '../components/ui/confirm/ConfirmProvider'
import Button from '../components/ui/button/Button'
import PageMeta from '../components/common/PageMeta'

const emptyForm = { id: null, title: '', occasionDate: '', recurringYearly: true, notifyAt: '' }

const inputClass = 'w-full rounded-lg border border-gray-300 px-3.5 py-2.5 text-sm text-gray-900 dark:border-white/10 dark:bg-gray-800 dark:text-white'

const pad = n => String(n).padStart(2, '0')

// 'yyyy-MM-dd' -> 'dd/MM/yyyy'
const formatDate = iso => (iso ? iso.split('-').reverse().join('/') : '')

// 'yyyy-MM-ddTHH:mm:ss' -> 'dd/MM/yyyy HH:mm'
const formatDateTime = iso => (iso ? `${formatDate(iso.slice(0, 10))} ${iso.slice(11, 16)}` : '')

const daysUntil = iso => {
    const [y, m, d] = iso.split('-').map(Number)
    const now = new Date()
    return Math.round((Date.UTC(y, m - 1, d) - Date.UTC(now.getFullYear(), now.getMonth(), now.getDate())) / 86400000)
}

// Giá trị cho ô datetime-local: ngày dịp trừ số ngày nhắc trước, tại giờ nhắc
const notifyInputValue = o => {
    const [y, m, d] = o.occasionDate.split('-').map(Number)
    const date = new Date(Date.UTC(y, m - 1, d - o.notifyDaysBefore))
    return `${date.getUTCFullYear()}-${pad(date.getUTCMonth() + 1)}-${pad(date.getUTCDate())}T${o.notifyTime.slice(0, 5)}`
}

function countdown(o) {
    const days = daysUntil(o.nextOccurrence)
    if (days === 0) return 'Hôm nay'
    if (days === 1) return 'Ngày mai'
    return `Còn ${days} ngày`
}

export default function OccasionsView({ auth }) {
    const user = auth?.user
    const { confirm } = useConfirm()
    const { occasions, loading, saving, error, save, remove } = useOccasionsController(user)
    const [form, setForm] = useState(null)

    if (auth?.busy && !user) {
        return <main className="catalog-page"><section className="catalog-panel"><p>Đang tải...</p></section></main>
    }
    if (!user) {
        return (
            <main className="catalog-page"><section className="catalog-panel">
                <p>Vui lòng đăng nhập để quản lý dịp đặc biệt.</p>
                <Link to="/login?next=/occasions" className="font-semibold text-brand-600 underline">Đăng nhập</Link>
            </section></main>
        )
    }
    if (user.role !== 'CUSTOMER') return <main className="catalog-page"><p>Chỉ tài khoản khách hàng có dịp đặc biệt.</p></main>

    const edit = o => setForm({
        id: o.id, title: o.title, occasionDate: o.occasionDate, recurringYearly: o.recurringYearly,
        notifyAt: o.customNotify ? notifyInputValue(o) : '',
    })

    const submit = async e => {
        e.preventDefault()
        const ok = await save(form.id, {
            title: form.title.trim(),
            occasionDate: form.occasionDate,
            recurringYearly: form.recurringYearly,
            notifyAt: form.notifyAt || null,
        })
        if (ok) setForm(null)
    }

    const del = async o => {
        if (await confirm({ title: 'Xóa dịp đặc biệt', message: `Xóa "${o.title}"? Bạn sẽ không nhận nhắc nhở cho dịp này nữa.`, confirmLabel: 'Xóa', tone: 'danger' })) remove(o.id)
    }

    return (
        <main className="catalog-page">
            <PageMeta title="Dịp đặc biệt | Cửa hàng hoa" description="Nhận email nhắc trước các dịp đặc biệt để kịp tặng hoa" />
            <section className="catalog-panel">
                <h1>Dịp đặc biệt</h1>
                <p>Lưu sinh nhật, ngày kỷ niệm... để FlowerShop gửi email nhắc bạn đặt hoa. Mặc định nhắc trước 1 tuần lúc 08:00, bạn có thể chọn thời điểm khác.</p>
                {!form && <div className="catalog-actions"><Button onClick={() => setForm(emptyForm)}>+ Thêm dịp đặc biệt</Button></div>}

                {error && <p role="alert" className="mt-3 rounded-lg bg-error-50 p-3 text-sm text-error-600">{error}</p>}

                {form && (
                    <form onSubmit={submit} className="mt-4 space-y-4 rounded-xl border border-gray-200 p-5 dark:border-white/10">
                        <h2 className="text-lg font-semibold">{form.id ? 'Sửa dịp đặc biệt' : 'Thêm dịp đặc biệt'}</h2>
                        <div>
                            <label htmlFor="occ-title" className="mb-1.5 block text-sm font-semibold">Tên dịp <span className="text-error-500">*</span></label>
                            <input id="occ-title" className={inputClass} value={form.title} maxLength={255} required
                                placeholder="Ví dụ: Sinh nhật mẹ, Kỷ niệm ngày cưới"
                                onChange={e => setForm({ ...form, title: e.target.value })} />
                        </div>
                        <div className="grid gap-4 sm:grid-cols-2">
                            <div>
                                <label htmlFor="occ-date" className="mb-1.5 block text-sm font-semibold">Ngày diễn ra <span className="text-error-500">*</span></label>
                                <input id="occ-date" type="date" className={inputClass} value={form.occasionDate} required
                                    onChange={e => setForm({ ...form, occasionDate: e.target.value })} />
                            </div>
                            <div>
                                <label htmlFor="occ-notify" className="mb-1.5 block text-sm font-semibold">Nhắc vào lúc (không bắt buộc)</label>
                                <input id="occ-notify" type="datetime-local" className={inputClass} value={form.notifyAt}
                                    max={form.occasionDate ? `${form.occasionDate}T23:59` : undefined}
                                    onChange={e => setForm({ ...form, notifyAt: e.target.value })} />
                                <p className="mt-1 text-xs text-gray-500">Bỏ trống: nhắc trước 7 ngày lúc 08:00.</p>
                            </div>
                        </div>
                        <label className="flex items-center gap-2 text-sm">
                            <input type="checkbox" checked={form.recurringYearly} onChange={e => setForm({ ...form, recurringYearly: e.target.checked })}
                                className="h-4 w-4 rounded border-gray-300 text-brand-600" />
                            Lặp lại hằng năm (sinh nhật, kỷ niệm)
                        </label>
                        <div className="flex gap-3">
                            <Button type="submit" disabled={saving}>{saving ? 'Đang lưu...' : 'Lưu'}</Button>
                            <Button type="button" variant="outline" onClick={() => setForm(null)} disabled={saving}>Hủy</Button>
                        </div>
                    </form>
                )}

                {loading && <p className="mt-4">Đang tải danh sách...</p>}
                {!loading && !occasions.length && !error && !form && <p className="mt-4">Bạn chưa lưu dịp đặc biệt nào.</p>}

                <ul className="mt-5 space-y-3">
                    {occasions.map(o => (
                        <li key={o.id} className="flex flex-col gap-3 rounded-xl border border-gray-200 p-4 dark:border-white/10 sm:flex-row sm:items-center sm:justify-between">
                            <div>
                                <p className="font-semibold text-gray-900 dark:text-white">{o.title}
                                    {o.recurringYearly && <span className="ml-2 rounded-full bg-brand-50 px-2 py-0.5 text-xs font-medium text-brand-600">Hằng năm</span>}
                                </p>
                                <p className="text-sm text-gray-600 dark:text-gray-400">
                                    {formatDate(o.nextOccurrence)} · {countdown(o)}
                                </p>
                                <p className="text-xs text-gray-500">
                                    {o.reminded ? 'Đã gửi email nhắc' : `Sẽ nhắc lúc ${formatDateTime(o.notifyAt)}`}
                                </p>
                            </div>
                            <div className="flex gap-2">
                                <Button size="sm" variant="outline" onClick={() => edit(o)} disabled={saving}>Sửa</Button>
                                <Button size="sm" variant="outline" className="text-error-500" onClick={() => del(o)} disabled={saving}>Xóa</Button>
                            </div>
                        </li>
                    ))}
                </ul>
            </section>
        </main>
    )
}
