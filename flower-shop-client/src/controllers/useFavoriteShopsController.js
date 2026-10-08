import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { favoriteShopService } from '../services/favoriteShopService'

/**
 * Controller dùng cho trang:
 * View Favorite Shop
 */
export function useFavoriteShopsController(user) {
    const navigate = useNavigate()

    const [shops, setShops] = useState([])
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState('')

    const load = useCallback(async () => {
        if (!user) {
            setShops([])
            return
        }

        setLoading(true)
        setError('')

        try {
            const data = await favoriteShopService.mine()

            setShops(
                Array.isArray(data)
                    ? data
                    : []
            )
        } catch (err) {
            console.error('Failed to load favorite shops:', err)

            setError(
                err?.message ||
                'Không thể tải danh sách cửa hàng đã theo dõi.'
            )
        } finally {
            setLoading(false)
        }
    }, [user])

    useEffect(() => {
        if (!user) {
            navigate('/login')
            return
        }

        load()
    }, [user, navigate, load])

    const remove = async (shopId) => {
        try {
            setError('')

            await favoriteShopService.remove(shopId)

            setShops((current) =>
                current.filter(
                    (shop) => shop.shopId !== shopId
                )
            )
        } catch (err) {
            console.error('Failed to unfollow shop:', err)

            setError(
                err?.message ||
                'Không thể bỏ theo dõi cửa hàng.'
            )
        }
    }

    return {
        shops,
        loading,
        error,
        reload: load,
        remove,
    }
}


/**
 * Controller dùng cho nút:
 * Follow / Unfollow Shop
 */
export function useFavoriteShopToggle(user, shopId) {
    const [favorite, setFavorite] = useState(false)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState('')

    const loadStatus = useCallback(async () => {
        if (!user || !shopId) {
            setFavorite(false)
            return
        }

        try {
            setError('')

            const result =
                await favoriteShopService.status(shopId)

            setFavorite(
                Boolean(result?.favorite)
            )
        } catch (err) {
            console.error(
                'Failed to load favorite shop status:',
                err
            )

            setError(
                err?.message ||
                'Không thể kiểm tra trạng thái theo dõi.'
            )
        }
    }, [user, shopId])

    useEffect(() => {
        loadStatus()
    }, [loadStatus])

    const toggle = async () => {
        if (!user) {
            setError('Vui lòng đăng nhập để theo dõi cửa hàng.')
            return
        }

        if (!shopId) {
            return
        }

        setLoading(true)
        setError('')

        try {
            if (favorite) {
                await favoriteShopService.remove(shopId)
                setFavorite(false)
            } else {
                await favoriteShopService.add(shopId)
                setFavorite(true)
            }
        } catch (err) {
            console.error(
                'Failed to toggle favorite shop:',
                err
            )

            setError(
                err?.message ||
                'Không thể thay đổi trạng thái theo dõi.'
            )
        } finally {
            setLoading(false)
        }
    }

    return {
        favorite,
        loading,
        error,
        toggle,
    }
}