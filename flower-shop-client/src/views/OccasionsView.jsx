import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useOccasionsController } from '../controllers/useOccasionsController'
import { useConfirm } from '../components/ui/confirm/ConfirmProvider'
import { Modal } from '../components/ui/modal'
import Badge from '../components/ui/badge/Badge'
import PageMeta from '../components/common/PageMeta'
import PageBreadCrumb from '../components/common/PageBreadCrumb'

const emptyForm = { id: null, title: '', occasionDate: '', recurringYearly: true, notifyAt: '' }

const field = 'mt-1.5 w-full rounded-lg border border-gray-300 px-3.5 py-2.5 text-theme-sm text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 disabled:opacity-60 dark:border-gray-700 dark:bg-gray-900 dark:text-white'
const primaryBtn = 'inline-flex min-h-11 items-center justify-center rounded-lg bg-brand-600 px-5 text-theme-sm font-medium text-white transition hover:bg-brand-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600 disabled:cursor-not-allowed disabled:bg-brand-300'
const outlineBtn = 'inline-flex min-h-11 items-center justify-center rounded-lg px-4 text-theme-sm font-medium text-gray-700 ring-1 ring-inset ring-gray-300 transition hover:bg-gray-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600 disabled:cursor-not-allowed disabled:opacity-60 dark:text-gray-300 dark:ring-gray-700 dark:hover:bg-white/5'
const dangerBtn = 'inline-flex min-h-11 items-center justify-center rounded-lg px-4 text-theme-sm font-medium text-error-600 ring-1 ring-inset ring-error-200 transition hover:bg-error-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-error-600 disabled:cursor-not-allowed disabled:opacity-60 dark:text-error-400 dark:ring-error-500/30 dark:hover:bg-error-500/10'

const pad = n => String(n).padStart(2, '0')

// 'yyyy-MM-dd' -> 'dd/MM/yyyy'
const formatDate = iso => (iso ? iso.split('-').reverse().join('/') : '')

// 'yyyy-MM-ddTHH:mm:ss' -> 'dd/MM/yyyy HH:mm'
const formatDateTime = iso => (iso ? `${formatDate(iso.slice(0, 10))} ${iso.slice(11, 16)}` : '')

const todayIso = () => {
    const now = new Date()
    return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}

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

function countdown(days) {
    if (days < 0) return `Đã qua ${-days} ngày`
    if (days === 0) return 'Hôm nay'
    if (days === 1) return 'Ngày mai'
    return `Còn ${days} ngày`
}

/** Ô ngày kiểu lịch: tháng ở trên, ngày ở giữa, để nhìn lướt biết dịp rơi vào lúc nào. */
function DateTile({ iso, past }) {
    const [, month, day] = iso.split('-')
    return (
        <div aria-hidden="true" className={`flex h-16 w-14 shrink-0 flex-col items-center justify-center rounded-xl border ${past ? 'border-gray-200 bg-gray-50 dark:border-white/10 dark:bg-white/5' : 'border-brand-200 bg-brand-50 dark:border-brand-500/30 dark:bg-brand-500/10'}`}>
            <span className={`text-theme-xs font-medium ${past ? 'text-gray-600 dark:text-gray-400' : 'text-brand-700 dark:text-brand-300'}`}>Th{Number(month)}</span>
            <span className="text-xl font-semibold leading-6 text-gray-900 dark:text-white">{Number(day)}</span>
        </div>
    )
}

function OccasionItem({ o, busy, onEdit, onDelete }) {
    const days = daysUntil(o.nextOccurrence)
    const past = days < 0
    const soon = !past && days <= 7
    const due = !o.reminded && !past && new Date(o.notifyAt) <= new Date()
    return (
        <li className="flex flex-col gap-4 p-4 sm:flex-row sm:items-center">
            <div className="flex min-w-0 flex-1 items-center gap-4">
                <DateTile iso={o.nextOccurrence} past={past} />
                <div className="min-w-0">
                    <p className="flex flex-wrap items-center gap-2 text-theme-sm font-semibold text-gray-900 dark:text-white">
                        <span className="break-words">{o.title}</span>
                        {o.recurringYearly && <Badge size="sm" color="light">Hằng năm</Badge>}
                    </p>
                    <p className={`text-theme-sm ${soon ? 'font-medium text-brand-700 dark:text-brand-300' : 'text-gray-600 dark:text-gray-400'}`}>
                        {formatDate(o.nextOccurrence)} · {countdown(days)}
                    </p>
                    {!past && (
                        <p className="text-theme-xs text-gray-600 dark:text-gray-400">
                            {o.reminded ? 'Đã gửi email nhắc' : due ? 'Sắp gửi email nhắc' : `Nhắc lúc ${formatDateTime(o.notifyAt)}`}
                        </p>
                    )}
                </div>
            </div>
            <div className="flex shrink-0 gap-2">
                <button type="button" className={outlineBtn} onClick={() => onEdit(o)} disabled={busy} aria-label={`Sửa ${o.title}`}>Sửa</button>
                <button type="button" className={dangerBtn} onClick={() => onDelete(o)} disabled={busy} aria-label={`Xóa ${o.title}`}>Xóa</button>
            </div>
        </li>
    )
}

function Section({ title, count, children }) {
    return (
        <section aria-label={title} className="mt-8">
            <h2 className="mb-3 text-theme-sm font-semibold text-gray-700 dark:text-gray-300">{title} <span className="font-normal text-gray-600 dark:text-gray-400">({count})</span></h2>
            <ul className="divide-y divide-gray-100 overflow-hidden rounded-2xl border border-gray-200 bg-white dark:divide-white/5 dark:border-gray-800 dark:bg-white/3">{children}</ul>
        </section>
    )
}

function OccasionForm({ form, setForm, saving, error, onSubmit, onCancel }) {
    const minDate = form.id && form.occasionDate && form.occasionDate < todayIso() ? undefined : todayIso()
    return (
        <form onSubmit={onSubmit} className="space-y-5">
            <div>
                <h2 className="text-lg font-semibold text-gray-900 dark:text-white">{form.id ? 'Sửa dịp đặc biệt' : 'Thêm dịp đặc biệt'}</h2>
                <p className="mt-1 text-theme-sm text-gray-600 dark:text-gray-400">FlowerShop sẽ gửi email nhắc để bạn kịp đặt hoa.</p>
            </div>
            <label className="block text-theme-sm font-medium text-gray-700 dark:text-gray-300">
                <span>Tên dịp <span className="text-error-600" aria-hidden="true">*</span></span>
                <input className={field} value={form.title} maxLength={255} required disabled={saving} autoFocus
                    placeholder="Ví dụ: Sinh nhật mẹ, Kỷ niệm ngày cưới"
                    onChange={e => setForm({ ...form, title: e.target.value })} />
            </label>
            <div className="grid gap-5 sm:grid-cols-2">
                <label className="block text-theme-sm font-medium text-gray-700 dark:text-gray-300">
                    <span>Ngày diễn ra <span className="text-error-600" aria-hidden="true">*</span></span>
                    <input type="date" className={field} value={form.occasionDate} required disabled={saving} min={minDate}
                        onChange={e => setForm({ ...form, occasionDate: e.target.value })} />
                </label>
                <div>
                    <label className="block text-theme-sm font-medium text-gray-700 dark:text-gray-300">
                        <span>Nhắc vào lúc</span>
                        <input type="datetime-local" className={field} value={form.notifyAt} disabled={saving}
                            max={form.occasionDate ? `${form.occasionDate}T23:59` : undefined}
                            aria-describedby="occasion-notify-hint"
                            onChange={e => setForm({ ...form, notifyAt: e.target.value })} />
                    </label>
                    <p id="occasion-notify-hint" className="mt-1.5 text-theme-xs text-gray-600 dark:text-gray-400">
                        {form.notifyAt ? 'Dùng thời điểm bạn chọn. ' : 'Bỏ trống: nhắc trước 7 ngày lúc 08:00. '}
                        {form.notifyAt && <button type="button" className="font-medium text-brand-700 underline focus-visible:outline-2 focus-visible:outline-brand-600 dark:text-brand-300" onClick={() => setForm({ ...form, notifyAt: '' })}>Dùng mặc định</button>}
                    </p>
                </div>
            </div>
            <label className="flex items-start gap-3 text-theme-sm text-gray-700 dark:text-gray-300">
                <input type="checkbox" className="mt-0.5 h-5 w-5 shrink-0 rounded border-gray-300 text-brand-600 focus:ring-brand-500" checked={form.recurringYearly} disabled={saving}
                    onChange={e => setForm({ ...form, recurringYearly: e.target.checked })} />
                <span><span className="font-medium">Lặp lại hằng năm</span><br /><span className="text-theme-xs text-gray-600 dark:text-gray-400">Phù hợp với sinh nhật và ngày kỷ niệm.</span></span>
            </label>
            {error && <p role="alert" className="rounded-lg bg-error-50 p-3 text-theme-sm text-error-700 dark:bg-error-500/10 dark:text-error-300">{error}</p>}
            <div className="flex justify-end gap-3 pt-1">
                <button type="button" className={outlineBtn} onClick={onCancel} disabled={saving}>Hủy</button>
                <button type="submit" className={primaryBtn} disabled={saving}>{saving ? 'Đang lưu…' : 'Lưu'}</button>
            </div>
        </form>
    )
}

export default function OccasionsView({ auth }) {
    const user = auth?.user
    const { confirm } = useConfirm()
    const { occasions, loading, saving, error, save, remove } = useOccasionsController(user)
    const [form, setForm] = useState(null)

    if (auth?.busy && !user) return <main className="mx-auto w-full max-w-4xl p-4 sm:p-6"><p role="status" className="text-theme-sm text-gray-600">Đang tải…</p></main>
    if (!user) {
        return (
            <main className="mx-auto w-full max-w-4xl p-4 sm:p-6">
                <p className="mb-3 text-theme-sm text-gray-700 dark:text-gray-300">Đăng nhập để lưu các dịp đặc biệt và nhận email nhắc.</p>
                <Link to="/login?next=/occasions" className="font-semibold text-brand-700 underline dark:text-brand-300">Đăng nhập</Link>
            </main>
        )
    }
    if (user.role !== 'CUSTOMER') return <main className="mx-auto w-full max-w-4xl p-4 sm:p-6"><p className="text-theme-sm text-gray-700 dark:text-gray-300">Chỉ tài khoản khách hàng có dịp đặc biệt.</p></main>

    const upcoming = occasions.filter(o => daysUntil(o.nextOccurrence) >= 0)
    const past = occasions.filter(o => daysUntil(o.nextOccurrence) < 0)

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

    const items = list => list.map(o => <OccasionItem key={o.id} o={o} busy={saving} onEdit={edit} onDelete={del} />)

    return (
        <main className="mx-auto w-full max-w-4xl p-4 sm:p-6">
            <PageMeta title="Dịp đặc biệt | Cửa hàng hoa" description="Nhận email nhắc trước các dịp đặc biệt để kịp tặng hoa" />
            <PageBreadCrumb pageTitle="Dịp đặc biệt" />

            <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
                <p className="max-w-xl text-theme-sm text-gray-600 dark:text-gray-400">
                    Lưu sinh nhật, ngày kỷ niệm để FlowerShop nhắc bạn qua email. Mặc định nhắc trước 1 tuần lúc 08:00, bạn có thể chọn thời điểm khác.
                </p>
                <button type="button" className={primaryBtn} onClick={() => setForm(emptyForm)}>Thêm dịp đặc biệt</button>
            </header>

            {error && !form && <p role="alert" className="mt-5 rounded-lg bg-error-50 p-3 text-theme-sm text-error-700 dark:bg-error-500/10 dark:text-error-300">{error}</p>}
            {loading && <p role="status" className="mt-8 text-theme-sm text-gray-600 dark:text-gray-400">Đang tải danh sách…</p>}

            {!loading && !occasions.length && !error && (
                <div className="mt-8 rounded-2xl border border-dashed border-gray-300 p-8 text-center dark:border-gray-700">
                    <p className="text-theme-sm font-medium text-gray-900 dark:text-white">Bạn chưa lưu dịp đặc biệt nào</p>
                    <p className="mx-auto mt-1 max-w-md text-theme-sm text-gray-600 dark:text-gray-400">Thêm một dịp, ví dụ sinh nhật mẹ, để nhận email nhắc trước 1 tuần.</p>
                    <button type="button" className={`${primaryBtn} mt-4`} onClick={() => setForm(emptyForm)}>Thêm dịp đầu tiên</button>
                </div>
            )}

            {upcoming.length > 0 && <Section title="Sắp tới" count={upcoming.length}>{items(upcoming)}</Section>}
            {past.length > 0 && <Section title="Đã qua" count={past.length}>{items(past)}</Section>}

            <Modal isOpen={Boolean(form)} onClose={() => !saving && setForm(null)} className="max-w-lg p-6 sm:p-8">
                {form && <OccasionForm form={form} setForm={setForm} saving={saving} error={error}
                    onSubmit={submit} onCancel={() => setForm(null)} />}
            </Modal>
        </main>
    )
}
