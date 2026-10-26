import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useLocation, useSearchParams } from 'react-router'
import { catalogService } from '../../services/catalogService'
import { sortOptions } from '../../models/productModel'

// Default sections shown first; groups created or renamed by the admin follow, and categories with no group go under "Khác".
const GROUP_ORDER = ['Chủ đề', 'Hoa tươi', 'Kiểu dáng']
const OTHER = 'Khác'
const CLOSE_DELAY_MS = 120

const productsUrl = params => {
    const query = params.toString()
    return query ? `/products?${query}` : '/products'
}

/**
 * Storefront category bar. Each section opens one shared panel on hover, focus or click, so the
 * panel is never clipped by the bar on small screens. Links go to /products?categoryId=&sort=,
 * which the catalog page reads.
 */
export default function CategoryMenu() {
    const [categories, setCategories] = useState([])
    const [openKey, setOpenKey] = useState(null)
    const closeTimer = useRef(null)
    const location = useLocation()
    const [searchParams] = useSearchParams()

    useEffect(() => {
        let active = true
        catalogService.categories()
            .then(list => { if (active) setCategories(list) })
            .catch(() => { if (active) setCategories([]) })
        return () => { active = false }
    }, [])

    useEffect(() => { setOpenKey(null) }, [location.pathname, location.search])

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

    const cancelClose = () => clearTimeout(closeTimer.current)
    const open = key => { cancelClose(); setOpenKey(key) }
    const scheduleClose = () => {
        cancelClose()
        closeTimer.current = setTimeout(() => setOpenKey(null), CLOSE_DELAY_MS)
    }
    const onKeyDown = event => {
        if (event.key === 'Escape') setOpenKey(null)
    }

    const sortHref = value => {
        const next = new URLSearchParams(onProducts ? searchParams : undefined)
        next.set('sort', value)
        next.delete('page')
        return productsUrl(next)
    }

    const sortSection = { key: 'Sắp xếp', sort: true }
    const panels = [...sections, sortSection]
    const current = panels.find(p => p.key === openKey)

    if (categories.length === 0) return null

    return <nav aria-label="Danh mục hoa" onMouseLeave={scheduleClose} onMouseEnter={cancelClose} onKeyDown={onKeyDown}
        className="relative border-b border-gray-200 bg-gray-50 dark:border-gray-800 dark:bg-gray-900/60">
        <ul className="mx-auto flex max-w-(--breakpoint-2xl) flex-wrap items-center gap-x-1 px-4 md:px-6">
            <li>
                <Link to="/" className="block px-3 py-3 text-sm font-semibold text-gray-700 hover:text-brand-600 dark:text-gray-300 dark:hover:text-brand-400">
                    Trang chủ
                </Link>
            </li>
            {panels.map(panel => {
                const expanded = openKey === panel.key
                const hasActive = !panel.sort && panel.items.some(c => c.id === activeCategory)
                return <li key={panel.key} onMouseEnter={() => open(panel.key)}>
                    <button type="button" aria-expanded={expanded} aria-controls="category-panel"
                        onClick={() => (expanded ? setOpenKey(null) : open(panel.key))}
                        onFocus={() => open(panel.key)}
                        className={`flex items-center gap-1 border-b-2 px-3 py-3 text-sm font-semibold transition-colors ${
                            expanded || hasActive
                                ? 'border-brand-500 text-brand-600 dark:text-brand-400'
                                : 'border-transparent text-gray-700 hover:text-brand-600 dark:text-gray-300 dark:hover:text-brand-400'}`}>
                        {panel.key}
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"
                            strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"
                            className={`transition-transform ${expanded ? 'rotate-180' : ''}`}>
                            <path d="m6 9 6 6 6-6" />
                        </svg>
                    </button>
                </li>
            })}
        </ul>

        {current && <div id="category-panel" role="region" aria-label={current.key}
            className="absolute inset-x-0 top-full z-50 border-b border-gray-200 bg-white shadow-theme-md dark:border-gray-800 dark:bg-gray-900">
            <div className="mx-auto max-w-(--breakpoint-2xl) px-4 py-5 md:px-6">
                {current.sort
                    ? <ul className="flex flex-wrap gap-2">
                        {sortOptions.map(([value, label]) => <li key={value}>
                            <Link to={sortHref(value)} aria-current={onProducts && activeSort === value ? 'true' : undefined}
                                className={`block rounded-lg px-4 py-2 text-sm font-medium ${
                                    onProducts && activeSort === value
                                        ? 'bg-brand-50 text-brand-700 dark:bg-brand-500/10 dark:text-brand-300'
                                        : 'text-gray-700 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-white/5'}`}>
                                {label}
                            </Link>
                        </li>)}
                    </ul>
                    : <ul className="grid grid-cols-2 gap-x-6 gap-y-1 sm:grid-cols-3 lg:grid-cols-4">
                        {current.items.map(c => <li key={c.id}>
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
                    </ul>}
            </div>
        </div>}
    </nav>
}
