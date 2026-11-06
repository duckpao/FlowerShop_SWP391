import { useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import PageMeta from '../components/common/PageMeta'
import PageBreadCrumb from '../components/common/PageBreadCrumb'
import ChatWorkspace from '../components/chat/ChatWorkspace'

/** Trang chat phía chủ shop (/shop-admin/chat). Mở thẳng một phiên bằng ?session= hoặc ?customOrderId=. */
export default function ManagerChatView({ user, shop }) {
    const [params] = useSearchParams()
    const entry = useMemo(() => ({
        sessionId: params.get('session') || undefined,
        customOrderId: params.get('customOrderId') || undefined,
    }), [params])
    const scope = useMemo(() => ({ kind: 'shop', shopId: shop?.id }), [shop?.id])

    if (user?.role !== 'SHOP') return <p className="text-theme-sm text-gray-700 dark:text-gray-300">Chỉ chủ shop được trả lời tin nhắn của khách.</p>
    return (
        <>
            <PageMeta title="Tin nhắn | Quản lý shop" description="Trả lời tin nhắn của khách hàng" />
            <PageBreadCrumb pageTitle="Tin nhắn khách hàng" />
            <ChatWorkspace scope={scope} user={user} side="SHOP" entry={entry} />
        </>
    )
}
