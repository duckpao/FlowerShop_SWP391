import { useFavoriteShopToggle } from '../controllers/useFavoriteShopsController'

export default function FollowShopButton({
    auth,
    shopId,
}) {
    const user = auth?.user

    const {
        favorite,
        loading,
        error,
        toggle,
    } = useFavoriteShopToggle(
        user,
        shopId
    )

    if (!user) {
        return (
            <div>
                <button
                    type="button"
                    onClick={() => {
                        window.location.href = '/login'
                    }}
                >
                    ♡ Theo dõi cửa hàng
                </button>
            </div>
        )
    }

    return (
        <div>
            <button
                type="button"
                onClick={toggle}
                disabled={loading}
            >
                {loading
                    ? 'Đang xử lý...'
                    : favorite
                        ? '♥ Đang theo dõi'
                        : '♡ Theo dõi cửa hàng'}
            </button>

            {error && (
                <p role="alert">
                    {error}
                </p>
            )}
        </div>
    )
}