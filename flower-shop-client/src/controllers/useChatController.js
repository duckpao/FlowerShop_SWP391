import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { chatApi } from '../services/chatService'
import { chatSocket } from '../services/chatSocket'
import { catalogService } from '../services/catalogService'
import { customFlowerService } from '../services/customFlowerService'

const PAGE = 30

const customOrderLabel = id => `Yêu cầu hoa custom #${id.replace(/-/g, '').slice(0, 8).toUpperCase()}`

/** Thẻ đang chờ gửi kèm tin đầu tiên, dựng từ dữ liệu hiện tại của sản phẩm hoặc yêu cầu custom. */
async function describeContext(scope, type, id) {
    if (type === 'PRODUCT') {
        const p = await catalogService.detail(id)
        return { type, id, name: p.name, price: p.price, imageUrl: p.imageUrl || p.images?.[0] || null }
    }
    const r = scope.kind === 'shop' ? await customFlowerService.shopDetail(scope.shopId, id) : await customFlowerService.detail(id)
    return { type, id, name: customOrderLabel(id), price: r.budget, status: r.status }
}

/**
 * Controller trang chat. entry mô tả cách vào trang từ URL:
 * { sessionId?, shopId? (khách), customOrderId?, productId? }.
 * Khách và chủ shop dùng chung controller; khác nhau ở scope và cách mở phiên.
 */
export function useChatController({ scope, user, entry }) {
    const api = useMemo(() => chatApi(scope), [scope.kind, scope.shopId])
    const [sessions, setSessions] = useState([])
    const [activeId, setActiveId] = useState(entry.sessionId || null)
    const [messages, setMessages] = useState([])
    const [hasMore, setHasMore] = useState(false)
    const [loading, setLoading] = useState(true)
    const [loadingThread, setLoadingThread] = useState(false)
    const [sending, setSending] = useState(false)
    const [error, setError] = useState('')
    const [pending, setPending] = useState(null)
    const [online, setOnline] = useState(true)
    const activeRef = useRef(activeId)
    activeRef.current = activeId

    const reloadSessions = useCallback(async () => {
        const list = await api.sessions()
        setSessions(Array.isArray(list) ? list : [])
        return list
    }, [api])

    // Tải danh sách, rồi mở phiên theo URL nếu có (nút "Chat với shop" hoặc "Trao đổi về yêu cầu này")
    useEffect(() => {
        let active = true
        setLoading(true); setError('')
        ;(async () => {
            try {
                let list = await reloadSessions()
                if (!active) return
                const wantsOpen = scope.kind === 'customer' ? entry.shopId : entry.customOrderId
                if (wantsOpen) {
                    const opened = await api.open(scope.kind === 'customer' ? { shopId: entry.shopId } : { customOrderId: entry.customOrderId })
                    if (!active) return
                    if (!list.some(s => s.id === opened.id)) list = await reloadSessions()
                    setActiveId(opened.id)
                    const type = entry.customOrderId ? 'CUSTOM_ORDER' : entry.productId ? 'PRODUCT' : null
                    const id = entry.customOrderId || entry.productId
                    if (type) setPending(await describeContext(scope, type, id).catch(() => null))
                }
            } catch (e) {
                if (active) setError(e.message || 'Không tải được danh sách trò chuyện.')
            } finally {
                if (active) setLoading(false)
            }
        })()
        return () => { active = false }
    }, [api, scope.kind, entry.shopId, entry.customOrderId, entry.productId, reloadSessions])

    useEffect(() => chatSocket.onState(setOnline), [])

    const markRead = useCallback(id => {
        api.read(id).catch(() => {})
        setSessions(list => list.map(s => (s.id === id ? { ...s, unread: 0 } : s)))
    }, [api])

    // Hộp thư: tin mới hoặc đã đọc ở bất kỳ phiên nào cập nhật danh sách bên trái
    useEffect(() => {
        if (!user?.id) return undefined
        return chatSocket.subscribe(`/topic/chat-inbox/${user.id}`, event => {
            setSessions(list => {
                if (!list.some(s => s.id === event.sessionId)) { reloadSessions().catch(() => {}); return list }
                const updated = list.map(s => (s.id !== event.sessionId ? s : {
                    ...s,
                    unread: event.sessionId === activeRef.current ? 0 : event.unread,
                    ...(event.preview != null ? { lastMessagePreview: event.preview, lastMessageAt: event.lastMessageAt } : {}),
                }))
                return updated.sort((a, b) => String(b.lastMessageAt || '').localeCompare(String(a.lastMessageAt || '')))
            })
        })
    }, [user?.id, reloadSessions])

    // Phiên đang mở: tải lịch sử, nghe tin mới và trạng thái đã xem
    useEffect(() => {
        if (!activeId) { setMessages([]); return undefined }
        let active = true
        setLoadingThread(true); setMessages([]); setHasMore(false)
        api.messages(activeId).then(list => {
            if (!active) return
            setMessages(list); setHasMore(list.length === PAGE)
            markRead(activeId)
        }).catch(e => { if (active) setError(e.message) }).finally(() => { if (active) setLoadingThread(false) })

        const stopMessages = chatSocket.subscribe(`/topic/chat/${activeId}`, message => {
            if (!active) return
            setMessages(list => (list.some(m => m.id === message.id) ? list : [...list, message]))
            if (message.senderType !== (scope.kind === 'shop' ? 'SHOP' : 'CUSTOMER')) markRead(activeId)
        })
        const stopRead = chatSocket.subscribe(`/topic/chat/${activeId}/read`, reader => {
            if (!active) return
            const mine = scope.kind === 'shop' ? 'SHOP' : 'CUSTOMER'
            if (reader !== mine) setMessages(list => list.map(m => (m.senderType === mine ? { ...m, read: true } : m)))
        })
        return () => { active = false; stopMessages(); stopRead() }
    }, [activeId, api, scope.kind, markRead])

    const loadOlder = async () => {
        if (!messages.length) return
        const older = await api.messages(activeId, messages[0].createdDate)
        setMessages(list => [...older, ...list])
        setHasMore(older.length === PAGE)
    }

    const select = id => { setActiveId(id); setPending(null); setError('') }

    const send = async ({ message, images }) => {
        setSending(true); setError('')
        try {
            const sent = await api.send(activeId, {
                message, images,
                context: pending ? { type: pending.type, id: pending.id } : null,
            })
            setMessages(list => (list.some(m => m.id === sent.id) ? list : [...list, sent]))
            setPending(null)
            return true
        } catch (e) {
            setError(e.message || 'Không gửi được tin nhắn.')
            return false
        } finally {
            setSending(false)
        }
    }

    const uploadImage = file => api.uploadImage(activeId, file).then(r => r.url)

    // Thẻ ghim phía trên: thẻ đang chờ gửi, nếu không có thì thẻ của tin gần nhất có đính kèm
    const pinned = useMemo(() => {
        if (pending) return pending
        const last = [...messages].reverse().find(m => m.contextType !== 'NONE' && m.context)
        return last ? { type: last.contextType, id: last.contextId, ...last.context } : null
    }, [pending, messages])

    return {
        sessions, activeId, active: sessions.find(s => s.id === activeId) || null, messages, hasMore, loading, loadingThread,
        sending, error, online, pending, pinned,
        select, send, uploadImage, loadOlder, dismissPending: () => setPending(null),
        clearError: () => setError(''),
    }
}
