import Avatar from './Avatar'

const time = iso => {
    if (!iso) return ''
    const date = new Date(iso)
    const today = new Date()
    return date.toDateString() === today.toDateString()
        ? date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
        : date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' })
}

/** Danh sách hội thoại bên trái; viewer cho biết hiện tên shop (khách) hay tên khách (chủ shop). */
export default function ConversationList({ sessions, activeId, onSelect, viewer, loading, emptyText }) {
    if (loading) return <p role="status" className="p-4 text-theme-sm text-gray-500">Đang tải cuộc trò chuyện…</p>
    if (!sessions.length) return <p className="p-4 text-theme-sm text-gray-500">{emptyText}</p>
    return (
        <ul className="divide-y divide-gray-100 dark:divide-white/5">
            {sessions.map(s => {
                const name = viewer === 'CUSTOMER' ? s.shopName : s.customerName
                const avatars = viewer === 'CUSTOMER' ? [s.shopLogoUrl, s.ownerAvatarUrl] : [s.customerAvatarUrl]
                return (
                    <li key={s.id}>
                        <button
                            type="button"
                            onClick={() => onSelect(s.id)}
                            aria-current={s.id === activeId ? 'true' : undefined}
                            className={`flex min-h-16 w-full items-center gap-3 px-4 py-3 text-left focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-brand-600 ${s.id === activeId ? 'bg-brand-50 dark:bg-brand-500/10' : 'hover:bg-gray-50 dark:hover:bg-white/5'}`}
                        >
                            <Avatar urls={avatars} name={name} />
                            <span className="min-w-0 flex-1">
                                <span className="flex items-baseline justify-between gap-2">
                                    <span className="truncate text-theme-sm font-medium text-gray-900 dark:text-white">{name}</span>
                                    <span className="shrink-0 text-theme-xs text-gray-500">{time(s.lastMessageAt)}</span>
                                </span>
                                <span className="flex items-center justify-between gap-2">
                                    <span className={`truncate text-theme-sm ${s.unread > 0 ? 'font-medium text-gray-900 dark:text-white' : 'text-gray-600 dark:text-gray-400'}`}>
                                        {s.lastMessagePreview || 'Chưa có tin nhắn'}
                                    </span>
                                    {s.unread > 0 && (
                                        <span aria-label={`${s.unread} tin chưa đọc`} className="flex h-5 min-w-5 shrink-0 items-center justify-center rounded-full bg-brand-600 px-1.5 text-theme-xs font-medium text-white">{s.unread}</span>
                                    )}
                                </span>
                            </span>
                        </button>
                    </li>
                )
            })}
        </ul>
    )
}
