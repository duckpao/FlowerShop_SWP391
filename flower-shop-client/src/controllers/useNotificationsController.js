import { useCallback, useEffect, useState } from 'react'
import { notificationService } from '../services/notificationService'

/**
 * Controller dùng cho:
 * NotificationDropdown (chuông thông báo)
 */
export function useNotificationsController(user) {
    const [notifications, setNotifications] = useState([])
    const [unreadCount, setUnreadCount] = useState(0)
    const [loading, setLoading] = useState(false)

    const load = useCallback(async () => {
        if (!user) {
            setNotifications([])
            setUnreadCount(0)
            return
        }

        setLoading(true)

        try {
            const [list, unread] = await Promise.all([
                notificationService.mine(),
                notificationService.unreadCount(),
            ])

            setNotifications(Array.isArray(list) ? list : [])
            setUnreadCount(Number(unread?.count) || 0)
        } catch (err) {
            console.error('Failed to load notifications:', err)
        } finally {
            setLoading(false)
        }
    }, [user])

    useEffect(() => {
        load()
    }, [load])

    const markAllAsRead = async () => {
        if (unreadCount === 0) return

        try {
            await notificationService.markAllAsRead()
            setUnreadCount(0)
            setNotifications((current) =>
                current.map((n) => ({ ...n, read: true }))
            )
        } catch (err) {
            console.error('Failed to mark notifications as read:', err)
        }
    }

    return {
        notifications,
        unreadCount,
        loading,
        reload: load,
        markAllAsRead,
    }
}
