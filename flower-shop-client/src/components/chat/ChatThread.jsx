import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import ContextCard from './ContextCard'

const MAX_LENGTH = 2000
const MAX_IMAGES = 4

const clock = iso => new Date(iso).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
const dayLabel = iso => new Date(iso).toLocaleDateString('vi-VN', { weekday: 'long', day: '2-digit', month: '2-digit', year: 'numeric' })
const sameDay = (a, b) => new Date(a).toDateString() === new Date(b).toDateString()

function Bubble({ message, mine, side }) {
    return (
        <div className={`flex ${mine ? 'justify-end' : 'justify-start'}`}>
            <div className={`flex max-w-[85%] flex-col gap-1.5 sm:max-w-[70%] ${mine ? 'items-end' : 'items-start'}`}>
                {message.contextType !== 'NONE' && message.context && (
                    <div className="w-full min-w-56">
                        <ContextCard ctx={{ type: message.contextType, id: message.contextId, ...message.context }} side={side} />
                    </div>
                )}
                {message.images?.length > 0 && (
                    <div className={`flex flex-wrap gap-1.5 ${mine ? 'justify-end' : ''}`}>
                        {message.images.map(url => (
                            <a key={url} href={url} target="_blank" rel="noreferrer" aria-label="Mở ảnh đính kèm">
                                <img src={url} alt="Ảnh đính kèm" className="max-h-48 max-w-full rounded-xl object-cover" />
                            </a>
                        ))}
                    </div>
                )}
                {message.message && (
                    <p className={`whitespace-pre-wrap break-words rounded-2xl px-3.5 py-2 text-theme-sm ${mine ? 'rounded-br-md bg-brand-600 text-white' : 'rounded-bl-md bg-gray-100 text-gray-900 dark:bg-white/5 dark:text-white'}`}>{message.message}</p>
                )}
                <span className="px-1 text-theme-xs text-gray-500">{clock(message.createdDate)}{mine && message.read ? ' · Đã xem' : ''}</span>
            </div>
        </div>
    )
}

function Composer({ c, disabled }) {
    const [text, setText] = useState('')
    const [images, setImages] = useState([])
    const [uploading, setUploading] = useState(false)
    const [uploadError, setUploadError] = useState('')
    const fileRef = useRef(null)

    const pick = async event => {
        const files = [...event.target.files].slice(0, MAX_IMAGES - images.length)
        event.target.value = ''
        if (!files.length) return
        setUploading(true); setUploadError('')
        try {
            const urls = []
            for (const file of files) urls.push(await c.uploadImage(file))
            setImages(list => [...list, ...urls])
        } catch (e) {
            setUploadError(e.message || 'Không tải được ảnh.')
        } finally {
            setUploading(false)
        }
    }

    const submit = async event => {
        event?.preventDefault()
        if ((!text.trim() && !images.length) || c.sending || uploading || disabled) return
        if (await c.send({ message: text.trim(), images })) { setText(''); setImages([]) }
    }

    const onKeyDown = event => {
        if (event.key === 'Enter' && !event.shiftKey && !event.nativeEvent.isComposing) submit(event)
    }

    return (
        <form onSubmit={submit} className="border-t border-gray-200 p-3 dark:border-gray-800">
            {c.pending && (
                <div className="mb-2"><ContextCard ctx={c.pending} side={c.side} label="Gửi kèm tin nhắn này" onDismiss={c.dismissPending} /></div>
            )}
            {images.length > 0 && (
                <ul className="mb-2 flex flex-wrap gap-2">
                    {images.map(url => (
                        <li key={url} className="relative">
                            <img src={url} alt="Ảnh sẽ gửi" className="h-16 w-16 rounded-lg object-cover" />
                            <button type="button" aria-label="Bỏ ảnh" onClick={() => setImages(list => list.filter(u => u !== url))}
                                className="absolute -right-2 -top-2 flex h-6 w-6 items-center justify-center rounded-full bg-gray-900 text-theme-xs text-white focus-visible:outline-2 focus-visible:outline-brand-600">×</button>
                        </li>
                    ))}
                </ul>
            )}
            {uploadError && <p role="alert" className="mb-2 text-theme-xs text-error-600">{uploadError}</p>}
            <div className="flex items-end gap-2">
                <input ref={fileRef} type="file" accept="image/jpeg,image/png,image/webp" multiple hidden onChange={pick} />
                <button type="button" onClick={() => fileRef.current?.click()} disabled={uploading || images.length >= MAX_IMAGES || disabled}
                    aria-label="Đính kèm ảnh"
                    className="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg text-gray-600 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus-visible:outline-2 focus-visible:outline-brand-600 disabled:opacity-50 dark:text-gray-300 dark:ring-gray-700 dark:hover:bg-white/5">
                    {uploading ? '…' : '+'}
                </button>
                <textarea value={text} onChange={e => setText(e.target.value.slice(0, MAX_LENGTH))} onKeyDown={onKeyDown} rows={1} disabled={disabled}
                    aria-label="Soạn tin nhắn" placeholder="Soạn tin…"
                    className="max-h-32 min-h-11 flex-1 resize-none rounded-lg border border-gray-300 px-3.5 py-2.5 text-theme-sm text-gray-900 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 disabled:opacity-60 dark:border-gray-700 dark:bg-gray-900 dark:text-white" />
                <button type="submit" disabled={c.sending || uploading || disabled || (!text.trim() && !images.length)}
                    className="h-11 shrink-0 rounded-lg bg-brand-600 px-4 text-theme-sm font-medium text-white hover:bg-brand-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600 disabled:cursor-not-allowed disabled:bg-brand-300">
                    {c.sending ? 'Đang gửi…' : 'Gửi'}
                </button>
            </div>
        </form>
    )
}

/** Khung hội thoại: đầu trang ghi người đối thoại và đơn đang trao đổi, giữa là tin nhắn, cuối là ô soạn. */
export default function ChatThread({ c, side, partnerName, onBack }) {
    const listRef = useRef(null)
    const stickRef = useRef(true)

    const onScroll = () => {
        const el = listRef.current
        if (el) stickRef.current = el.scrollHeight - el.scrollTop - el.clientHeight < 80
    }
    useEffect(() => { stickRef.current = true }, [c.activeId])
    useLayoutEffect(() => {
        const el = listRef.current
        if (el && stickRef.current) el.scrollTop = el.scrollHeight
    }, [c.messages, c.loadingThread])

    const mineType = side === 'SHOP' ? 'SHOP' : 'CUSTOMER'
    return (
        <section aria-label={`Trò chuyện với ${partnerName}`} className="flex h-full min-h-0 flex-col">
            <header className="flex items-center gap-2 border-b border-gray-200 px-3 py-2.5 dark:border-gray-800">
                <button type="button" onClick={onBack} aria-label="Quay lại danh sách"
                    className="flex h-11 w-11 items-center justify-center rounded-lg text-gray-700 hover:bg-gray-100 focus-visible:outline-2 focus-visible:outline-brand-600 lg:hidden dark:text-gray-300 dark:hover:bg-white/5">←</button>
                <h2 className="truncate text-theme-sm font-semibold text-gray-900 dark:text-white">{partnerName}</h2>
            </header>
            {c.pinned && (
                <div className="border-b border-gray-200 p-3 dark:border-gray-800">
                    <ContextCard ctx={c.pinned} side={side} label={c.pinned.type === 'PRODUCT' ? 'Đang trao đổi về sản phẩm' : 'Đang trao đổi về yêu cầu custom'} />
                </div>
            )}
            {!c.online && <p role="status" className="bg-warning-50 px-4 py-2 text-theme-xs text-warning-800 dark:bg-warning-500/10 dark:text-warning-300">Mất kết nối, đang thử nối lại. Tin mới sẽ hiện khi kết nối trở lại.</p>}
            <div ref={listRef} onScroll={onScroll} role="log" aria-live="polite" className="min-h-0 flex-1 space-y-3 overflow-y-auto p-4">
                {c.hasMore && <div className="text-center"><button type="button" onClick={c.loadOlder} className="rounded-lg px-3 py-2 text-theme-xs font-medium text-brand-600 hover:bg-brand-50 focus-visible:outline-2 focus-visible:outline-brand-600">Xem tin cũ hơn</button></div>}
                {c.loadingThread && <p role="status" className="text-center text-theme-sm text-gray-500">Đang tải tin nhắn…</p>}
                {!c.loadingThread && !c.messages.length && <p className="text-center text-theme-sm text-gray-500">Chưa có tin nhắn. Hãy gửi lời chào đầu tiên.</p>}
                {c.messages.map((m, i) => (
                    <div key={m.id} className="space-y-3">
                        {(i === 0 || !sameDay(c.messages[i - 1].createdDate, m.createdDate)) && (
                            <p className="text-center text-theme-xs text-gray-500">{dayLabel(m.createdDate)}</p>
                        )}
                        <Bubble message={m} mine={m.senderType === mineType} side={side} />
                    </div>
                ))}
            </div>
            {c.error && <p role="alert" className="border-t border-error-200 bg-error-50 px-4 py-2 text-theme-xs text-error-700 dark:bg-error-500/10 dark:text-error-300">{c.error}</p>}
            <Composer key={c.activeId} c={{ ...c, side }} />
        </section>
    )
}
