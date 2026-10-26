import { useChatController } from '../../controllers/useChatController'
import ConversationList from './ConversationList'
import ChatThread from './ChatThread'

/**
 * Trang chat hai cột: danh sách hội thoại và khung trò chuyện. Dưới lg chỉ hiện một cột tại một thời điểm.
 * side là phía người xem: 'CUSTOMER' hoặc 'SHOP'.
 */
export default function ChatWorkspace({ scope, user, side, entry }) {
    const c = useChatController({ scope, user, entry })
    const partner = c.active ? (side === 'CUSTOMER' ? c.active.shopName : c.active.customerName) : ''
    const emptyText = side === 'CUSTOMER'
        ? 'Bạn chưa trò chuyện với cửa hàng nào. Mở trang sản phẩm hoặc trang cửa hàng và bấm "Chat với shop".'
        : 'Chưa có khách nào nhắn tin. Khi khách gửi tin hoặc bạn bấm "Trao đổi với khách" ở một yêu cầu custom, cuộc trò chuyện sẽ hiện ở đây.'

    return (
        <div className="overflow-hidden rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/3">
            {c.error && !c.activeId && <p role="alert" className="border-b border-error-200 bg-error-50 px-4 py-3 text-theme-sm text-error-700 dark:bg-error-500/10 dark:text-error-300">{c.error}</p>}
            <div className="grid h-[calc(100vh-14rem)] min-h-[28rem] lg:grid-cols-[20rem_1fr]">
                <aside aria-label="Danh sách trò chuyện" className={`min-h-0 overflow-y-auto border-gray-200 lg:border-r dark:border-gray-800 ${c.activeId ? 'hidden lg:block' : ''}`}>
                    <ConversationList sessions={c.sessions} activeId={c.activeId} onSelect={c.select} viewer={side}
                        loading={c.loading} emptyText={emptyText} />
                </aside>
                <div className={`min-h-0 ${c.activeId ? '' : 'hidden lg:block'}`}>
                    {c.active
                        ? <ChatThread c={c} side={side} partnerName={partner} onBack={() => c.select(null)} />
                        : <p className="flex h-full items-center justify-center p-6 text-theme-sm text-gray-500">Chọn một cuộc trò chuyện để bắt đầu.</p>}
                </div>
            </div>
        </div>
    )
}
