import { useMemo } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import PageMeta from '../components/common/PageMeta'
import PageBreadCrumb from '../components/common/PageBreadCrumb'
import ChatWorkspace from '../components/chat/ChatWorkspace'

/**
 * Trang chat phía khách (/chat). Vào từ nút "Chat với shop" (?shopId=&productId=) hoặc từ yêu cầu custom
 * (?shopId=&customOrderId=); thông báo mở thẳng một phiên bằng ?session=.
 */
export default function ChatView({ auth }) {
    const [params] = useSearchParams()
    const entry = useMemo(() => ({
        sessionId: params.get('session') || undefined,
        shopId: params.get('shopId') || undefined,
        productId: params.get('productId') || undefined,
        customOrderId: params.get('customOrderId') || undefined,
    }), [params])
    const scope = useMemo(() => ({ kind: 'customer' }), [])
    const user = auth?.user

    if (auth?.busy && !user) return <main className="mx-auto w-full max-w-6xl p-4 sm:p-6"><p role="status" className="text-theme-sm text-gray-500">Đang kiểm tra đăng nhập…</p></main>
    if (!user) {
        return (
            <main className="mx-auto w-full max-w-6xl p-4 sm:p-6">
                <p className="mb-3 text-theme-sm text-gray-700 dark:text-gray-300">Đăng nhập để trò chuyện với cửa hàng.</p>
                <Link to="/login?next=%2Fchat" className="font-semibold text-brand-600 underline">Đăng nhập</Link>
            </main>
        )
    }
    if (user.role !== 'CUSTOMER') return <main className="mx-auto w-full max-w-6xl p-4 sm:p-6"><p className="text-theme-sm text-gray-700 dark:text-gray-300">Trang này dành cho khách hàng. Chủ shop trả lời tại mục Tin nhắn trong trang quản lý shop.</p></main>

    return (
        <main className="mx-auto w-full max-w-6xl p-4 sm:p-6">
            <PageMeta title="Tin nhắn | Cửa hàng hoa" description="Trò chuyện với cửa hàng về sản phẩm và yêu cầu hoa riêng" />
            <PageBreadCrumb pageTitle="Tin nhắn" />
            <ChatWorkspace scope={scope} user={user} side="CUSTOMER" entry={entry} />
        </main>
    )
}
