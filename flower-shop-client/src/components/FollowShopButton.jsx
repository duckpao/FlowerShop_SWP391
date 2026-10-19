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

    if (user && user.role !== 'CUSTOMER') return null

    if (!user) {
        return (
            <div>
                <button
                    type="button"
                    onClick={() => {
                        window.location.href = `/login?next=${encodeURIComponent(window.location.pathname)}`
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

            <p className="muted text-sm">{favorite
                ? 'Bạn sẽ nhận email khi cửa hàng đăng sản phẩm mới. Bỏ theo dõi để ngừng nhận.'
                : 'Theo dõi để nhận email thông báo sản phẩm mới.'}</p>

            {error && (
                <p role="alert">
                    {error}
                </p>
            )}
        </div>
    )
}
