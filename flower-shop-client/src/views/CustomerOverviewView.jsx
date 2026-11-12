import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { useAuth } from '../context/AuthContext'
import { customerOrderService } from '../services/customerOrderService'
import { accountService } from '../services/accountService'
import { formatPrice } from '../models/productModel'
import { orderStatusLabels } from '../models/orderStatus'
import PageMeta from '../components/common/PageMeta'

const code = id => id.substring(0, 8).toUpperCase()
const card = 'rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/3'

/** Một khối của dashboard: tự tải, có trạng thái đang tải / lỗi riêng để khối hỏng không che cả trang. */
function useBlock(load, enabled) {
    const [state, setState] = useState({ data: null, error: '' })
    useEffect(() => {
        if (!enabled) return
        let active = true
        load().then(data => { if (active) setState({ data, error: '' }) })
            .catch(e => { if (active) setState({ data: null, error: e.message || 'Không tải được dữ liệu.' }) })
        return () => { active = false }
    }, [enabled])
    return state
}

function Block({ title, state, children }) {
    return <section className={card} aria-label={title}>
        <h2 className="mb-3 text-lg font-semibold text-gray-900 dark:text-white">{title}</h2>
        {state.error ? <p role="alert" className="text-sm text-error-500">{state.error}</p>
            : state.data == null ? <p role="status" className="text-sm text-gray-500">Đang tải…</p> : children(state.data)}
    </section>
}

/** SRS 10.3 Customer Dashboard (/dashboard): đơn đang giao, 5 đơn gần nhất, địa chỉ mặc định. Chỉ đọc. */
export default function CustomerOverviewView() {
    const { user } = useAuth()
    const isCustomer = user?.role === 'CUSTOMER'
    const delivering = useBlock(() => customerOrderService.list(0, 'DELIVERING').then(r => r.content.slice(0, 3)), isCustomer)
    const recent = useBlock(() => customerOrderService.list(0, '').then(r => r.content.slice(0, 5)), isCustomer)
    const address = useBlock(() => accountService.addresses().then(list => ({ value: list.find(a => a.isDefault) || null })), isCustomer)

    if (!user) return <main className="mx-auto max-w-5xl p-6"><Link className="underline" to="/login?next=%2Fdashboard">Đăng nhập</Link> để xem tổng quan.</main>
    if (!isCustomer) return <main className="mx-auto max-w-5xl p-6"><p>Trang này dành cho khách hàng.</p></main>

    return <main className="mx-auto w-full max-w-5xl space-y-5 p-4 sm:p-6">
        <PageMeta title="Tổng quan | Cửa hàng hoa" description="Đơn hàng và địa chỉ của bạn" />
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Xin chào, {user.fullName || 'bạn'}</h1>

        <Block title="Đơn đang giao" state={delivering}>{list => list.length === 0
            ? <p className="text-sm text-gray-600 dark:text-gray-400">Bạn chưa có đơn nào đang giao. <Link className="font-semibold text-brand-600 underline" to="/products">Xem sản phẩm</Link></p>
            : <ul className="divide-y divide-gray-100 dark:divide-white/5">{list.map(o => <li key={o.id} className="flex flex-wrap items-center justify-between gap-2 py-3">
                <div><p className="font-medium">#{code(o.id)} · {o.shopName}</p><p className="text-sm text-gray-500">Đơn vị vận chuyển đang giao hàng đến bạn.</p></div>
                <Link className="text-sm font-semibold text-brand-600 underline" to={`/orders/${o.id}`}>Theo dõi</Link>
            </li>)}</ul>}
        </Block>

        <Block title="Đơn hàng gần đây" state={recent}>{list => list.length === 0
            ? <p className="text-sm text-gray-600 dark:text-gray-400">Bạn chưa có đơn nào.</p>
            : <>
                <div className="overflow-x-auto"><table className="w-full text-left text-sm">
                    <thead className="text-gray-500"><tr><th className="py-2 pr-3">Mã đơn</th><th className="pr-3">Cửa hàng</th><th className="pr-3">Tổng tiền</th><th className="pr-3">Trạng thái</th><th>Ngày đặt</th></tr></thead>
                    <tbody>{list.map(o => <tr key={o.id} className="border-t border-gray-100 dark:border-white/5">
                        <td className="py-2 pr-3"><Link className="font-mono text-brand-600 underline" to={`/orders/${o.id}`}>{code(o.id)}</Link></td>
                        <td className="pr-3">{o.shopName}</td><td className="pr-3">{formatPrice(o.totalAmount)}</td>
                        <td className="pr-3">{orderStatusLabels[o.status] || o.status}</td>
                        <td>{o.createdDate ? new Date(o.createdDate).toLocaleDateString('vi-VN') : ''}</td>
                    </tr>)}</tbody>
                </table></div>
                <Link className="mt-3 inline-block text-sm font-semibold text-brand-600 underline" to="/orders">Xem tất cả đơn hàng</Link>
            </>}
        </Block>

        <Block title="Địa chỉ giao hàng mặc định" state={address}>{({ value }) => value
            ? <p className="text-sm">{[value.addressLine, value.ward, value.district, value.city].filter(Boolean).join(', ')} · <Link className="font-semibold text-brand-600 underline" to="/account">Quản lý địa chỉ</Link></p>
            : <p className="text-sm text-gray-600 dark:text-gray-400">Bạn chưa có địa chỉ. Thêm địa chỉ để đặt hàng. <Link className="font-semibold text-brand-600 underline" to="/account">Thêm địa chỉ</Link></p>}
        </Block>
    </main>
}
