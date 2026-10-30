import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useLocation, useSearchParams } from 'react-router'
import { catalogService } from '../../services/catalogService'
import { sortOptions } from '../../models/productModel'
import { useAuth } from '../../context/AuthContext'

// Default sections shown first; groups created or renamed by the admin follow, and categories with no group go under "Khác".
const GROUP_ORDER = ['Chủ đề', 'Hoa tươi', 'Kiểu dáng']
const OTHER = 'Khác'
const CLOSE_DELAY_MS = 120

const productsUrl = params => {
    const query = params.toString()
    return query ? `/products?${query}` : '/products'
}

const itemClass = active => `block whitespace-nowrap border-b-2 px-3 py-3 text-sm font-semibold transition-colors ${
    active
        ? 'border-brand-500 text-brand-600 dark:text-brand-400'
        : 'border-transparent text-gray-700 hover:text-brand-600 dark:text-gray-300 dark:hover:text-brand-400'}`

/**
 * Thanh điều hướng chính phía khách (thay cho sidebar): các trang để mua hoa nằm ngang dưới header.
 * "Sản phẩm" mở một panel chung gồm các nhóm danh mục + cách sắp xếp, link tới /products?categoryId=&sort=.
 * Trang cá nhân (đơn hàng, dịp đặc biệt, shop theo dõi, yêu thích) nằm trong menu tài khoản.
 */
export default function CategoryMenu() {
    const [categories, setCategories] = useState([])
    const [open, setOpen] = useState(false)
    const closeTimer = useRef(null)
    const location = useLocation()
    const [searchParams] = useSearchParams()
    const { user } = useAuth()

    useEffect(() => {
        let active = true
        catalogService.categories()
            .then(list => { if (active) setCategories(list) })
            .catch(() => { if (active) setCategories([]) })
        return () => { active = false }
    }, [])

    useEffect(() => { setOpen(false) }, [location.pathname, location.search])

    const sections = useMemo(() => {
        const byGroup = new Map()
        for (const c of categories) {
            const key = c.group || OTHER
            if (!byGroup.has(key)) byGroup.set(key, [])
            byGroup.get(key).push(c)
        }
        // Nhóm mặc định đứng trước theo thứ tự cố định, nhóm do admin tạo/đổi tên theo sau, "Khác" luôn cuối
        const extra = [...byGroup.keys()].filter(key => !GROUP_ORDER.includes(key) && key !== OTHER).sort((a, b) => a.localeCompare(b, 'vi'))
        const ordered = [...GROUP_ORDER, ...extra, OTHER].filter(key => byGroup.has(key))
        return ordered.map(key => ({ key, items: byGroup.get(key) }))
    }, [categories])

    const activeCategory = searchParams.get('categoryId') || ''
    const activeSort = searchParams.get('sort') || 'newest'
    const onProducts = location.pathname === '/products'

    const openedByHover = useRef(false)
    const cancelClose = () => clearTimeout(closeTimer.current)
    const show = () => { cancelClose(); setOpen(true) }
    const hoverOpen = () => { if (!open) openedByHover.current = true; show() }
    // Rê chuột đã mở thì cú bấm ngay sau đó giữ panel mở (không đóng ngược lại); bấm lần nữa mới đóng
    const toggle = () => {
        if (open && !openedByHover.current) setOpen(false)
        else show()
        openedByHover.current = false
    }
    const scheduleClose = () => {
        cancelClose()
        closeTimer.current = setTimeout(() => setOpen(false), CLOSE_DELAY_MS)
    }

    const sortHref = value => {
        const next = new URLSearchParams(onProducts ? searchParams : undefined)
        next.set('sort', value)
        next.delete('page')
        return productsUrl(next)
    }

    const isActive = path => path === '/' ? location.pathname === '/' : location.pathname.startsWith(path)
    // Đặt hoa riêng chỉ dành cho khách hàng (khách vãng lai bấm vào sẽ được mời đăng nhập)
    const links = [
        ['/shops', 'Cửa hàng'],
        ['/budget-flowers', 'Hoa theo ngân sách'],
        ...(!user || user.role === 'CUSTOMER' ? [['/custom-orders', 'Đặt hoa theo yêu cầu']] : []),
        ['/floral-care', 'Cẩm nang chăm hoa'],
    ]

    return <nav aria-label="Điều hướng cửa hàng" onMouseLeave={scheduleClose} onMouseEnter={cancelClose}
        onKeyDown={event => { if (event.key === 'Escape') setOpen(false) }}
        className="relative border-b border-gray-200 bg-gray-50 dark:border-gray-800 dark:bg-gray-900/60">
        <ul className="no-scrollbar mx-auto flex max-w-(--breakpoint-2xl) items-center gap-x-1 overflow-x-auto px-4 md:px-6">
            <li>
                <Link to="/" aria-current={isActive('/') ? 'page' : undefined} className={itemClass(isActive('/'))}>Trang chủ</Link>
            </li>
            <li onMouseEnter={hoverOpen}>
                <button type="button" aria-expanded={open} aria-controls="category-panel"
                    onClick={toggle}
                    className={`flex items-center gap-1 ${itemClass(open || onProducts)}`}>
                    Sản phẩm
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"
                        strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"
                        className={`transition-transform ${open ? 'rotate-180' : ''}`}>
                        <path d="m6 9 6 6 6-6" />
                    </svg>
                </button>
            </li>
            {links.map(([path, label]) => <li key={path}>
                <Link to={path} aria-current={isActive(path) ? 'page' : undefined} className={itemClass(isActive(path))}>{label}</Link>
            </li>)}
        </ul>

        {open && <div id="category-panel" role="region" aria-label="Sản phẩm"
            className="absolute inset-x-0 top-full z-50 border-b border-gray-200 bg-white shadow-theme-md dark:border-gray-800 dark:bg-gray-900">
            <div className="mx-auto max-w-(--breakpoint-2xl) space-y-5 px-4 py-5 md:px-6">
                {sections.length > 0 && <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
                    {sections.map(section => <div key={section.key}>
                        <h3 className="mb-2 px-2 text-xs font-semibold uppercase tracking-wide text-gray-500 dark:text-gray-400">{section.key}</h3>
                        <ul className="space-y-0.5">
                            {section.items.map(c => <li key={c.id}>
                                <Link to={productsUrl(new URLSearchParams({ categoryId: c.id }))}
                                    aria-current={activeCategory === c.id ? 'true' : undefined}
                                    className={`flex items-baseline justify-between gap-3 rounded-md px-2 py-2 text-sm ${
                                        activeCategory === c.id
                                            ? 'font-semibold text-brand-600 dark:text-brand-400'
                                            : 'text-gray-700 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-white/5'}`}>
                                    <span>{c.name}</span>
                                    <span className="text-xs text-gray-400">{c.productCount}</span>
                                </Link>
                            </li>)}
                        </ul>
                    </div>)}
                </div>}
                <div className="flex flex-wrap items-center gap-2 border-t border-gray-100 pt-4 dark:border-gray-800">
                    <span className="me-1 text-sm font-semibold text-gray-500 dark:text-gray-400">Sắp xếp:</span>
                    {sortOptions.map(([value, label]) => <Link key={value} to={sortHref(value)}
                        aria-current={onProducts && activeSort === value ? 'true' : undefined}
                        className={`rounded-lg px-4 py-2 text-sm font-medium ${
                            onProducts && activeSort === value
                                ? 'bg-brand-50 text-brand-700 dark:bg-brand-500/10 dark:text-brand-300'
                                : 'text-gray-700 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-white/5'}`}>
                        {label}
                    </Link>)}
                    <Link to="/products" className="ms-auto rounded-lg px-4 py-2 text-sm font-semibold text-brand-600 hover:bg-brand-50 dark:text-brand-400 dark:hover:bg-brand-500/10">
                        Xem tất cả sản phẩm →
                    </Link>
                </div>
            </div>
        </div>}
    </nav>
}
