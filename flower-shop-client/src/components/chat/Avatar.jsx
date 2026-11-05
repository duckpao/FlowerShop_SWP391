import { useEffect, useState } from 'react'

/**
 * Ảnh đại diện trong chat. Thử lần lượt các URL; ảnh nào tải lỗi (link hỏng) thì bỏ qua,
 * hết ảnh thì hiện chữ cái đầu của tên.
 */
export default function Avatar({ urls, name, className = 'h-11 w-11' }) {
    const candidates = (urls || []).filter(Boolean)
    const key = candidates.join('|')
    const [failed, setFailed] = useState(0)
    useEffect(() => { setFailed(0) }, [key])

    const url = candidates[failed]
    if (url) {
        return <img src={url} alt="" onError={() => setFailed(n => n + 1)} className={`${className} shrink-0 rounded-full object-cover`} />
    }
    return (
        <span aria-hidden="true" className={`${className} flex shrink-0 items-center justify-center rounded-full bg-gray-100 text-theme-sm font-medium text-gray-700 dark:bg-white/10 dark:text-gray-300`}>
            {(name || '?').trim().charAt(0).toUpperCase()}
        </span>
    )
}
