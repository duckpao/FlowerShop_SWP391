import { useEffect, useState } from 'react'
import { chatApi } from '../services/chatService'
import { chatSocket } from '../services/chatSocket'

/** Tổng số tin chưa đọc cho huy hiệu trên sidebar; cập nhật qua hộp thư WebSocket, không polling. */
export function useChatUnread(scope, userId) {
    const [count, setCount] = useState(0)
    const shopId = scope?.shopId
    const kind = scope?.kind

    useEffect(() => {
        if (!userId || !kind || (kind === 'shop' && !shopId)) { setCount(0); return undefined }
        let active = true
        chatApi({ kind, shopId }).unread().then(r => { if (active) setCount(r.count) }).catch(() => {})
        const stop = chatSocket.subscribe(`/topic/chat-inbox/${userId}`, event => {
            if (active && typeof event?.totalUnread === 'number') setCount(event.totalUnread)
        })
        return () => { active = false; stop() }
    }, [kind, shopId, userId])

    return count
}
