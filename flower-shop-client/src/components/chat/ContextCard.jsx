import { Link } from 'react-router-dom'
import { formatPrice } from '../../models/productModel'
import { customStatus } from '../customFlowers/CustomFlowerParts'

/** Đích khi bấm thẻ: sản phẩm mở trang sản phẩm, yêu cầu custom mở chi tiết theo phía người xem. */
export function contextHref(ctx, side) {
    if (ctx.type === 'PRODUCT') return `/products/${encodeURIComponent(ctx.id)}`
    const base = side === 'SHOP' ? '/shop-admin/custom-orders' : '/custom-orders'
    return `${base}?open=${encodeURIComponent(ctx.id)}`
}

/**
 * Thẻ đơn custom hoặc sản phẩm trong chat. Giá trị đến từ ảnh chụp lúc gửi nên không đổi theo thời gian;
 * bấm vào mở dữ liệu hiện tại. onDismiss chỉ có ở thẻ đang chờ gửi.
 */
export default function ContextCard({ ctx, side, label, onDismiss }) {
    const isProduct = ctx.type === 'PRODUCT'
    return (
        <div className="flex items-stretch rounded-xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/3">
            <Link
                to={contextHref(ctx, side)}
                className="flex min-h-11 min-w-0 flex-1 items-center gap-3 rounded-xl p-2.5 hover:bg-gray-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600 dark:hover:bg-white/5"
            >
                {isProduct && (ctx.imageUrl
                    ? <img src={ctx.imageUrl} alt="" className="h-12 w-12 shrink-0 rounded-lg object-cover" />
                    : <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-lg bg-gray-100 text-theme-xs text-gray-500 dark:bg-white/5">Chưa có ảnh</span>)}
                <span className="min-w-0">
                    {label && <span className="block text-theme-xs text-gray-500 dark:text-gray-400">{label}</span>}
                    <span className="block truncate text-theme-sm font-medium text-gray-900 dark:text-white">{ctx.name}</span>
                    <span className="block text-theme-xs text-gray-600 dark:text-gray-400">
                        {ctx.price != null && (isProduct ? formatPrice(ctx.price) : `Ngân sách ${formatPrice(ctx.price)}`)}
                        {!isProduct && ctx.status && ` · ${customStatus[ctx.status] || ctx.status}`}
                    </span>
                </span>
            </Link>
            {onDismiss && (
                <button
                    type="button"
                    onClick={onDismiss}
                    aria-label="Bỏ thẻ đính kèm"
                    className="m-1.5 flex h-11 w-11 shrink-0 items-center justify-center rounded-lg text-gray-500 hover:bg-gray-100 focus-visible:outline-2 focus-visible:outline-brand-600 dark:hover:bg-white/5"
                >×</button>
            )}
        </div>
    )
}
