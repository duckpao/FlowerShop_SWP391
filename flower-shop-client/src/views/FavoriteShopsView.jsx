import { Link } from 'react-router-dom'
import { useFavoriteShopsController } from '../controllers/useFavoriteShopsController'

export default function FavoriteShopsView({
    auth,
}) {
    const user = auth?.user

    const {
        shops,
        loading,
        error,
        remove,
        reload,
    } = useFavoriteShopsController(user, auth?.busy)

    if (loading || (auth?.busy && !user)) {
        return (
            <main className="catalog-page">
                <section className="catalog-panel">
                    <p>Đang tải danh sách cửa hàng...</p>
                </section>
            </main>
        )
    }

    if (!user) return null
    if (user.role !== 'CUSTOMER') return <main className="catalog-page"><p>Chỉ tài khoản khách hàng có danh sách cửa hàng theo dõi.</p></main>
    return (
        <main className="catalog-page">
            <section className="catalog-panel">
                <h1>Cửa hàng đang theo dõi</h1>
                <p>Bạn sẽ nhận email khi những cửa hàng này đăng sản phẩm mới. Bỏ theo dõi để ngừng nhận thông báo.</p>
                <div className="catalog-actions">
                    <Link
                        to="/shops"
                        className="inline-flex items-center justify-center rounded-lg bg-brand-600 px-5 py-3 text-sm font-semibold text-white transition-colors hover:bg-brand-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600"
                    >
                        Khám phá cửa hàng
                    </Link>
                </div>

                {error && (
                    <p role="alert">
                        {error}
                    </p>
                )}

                {!shops.length && !error && (
                    <div>
                        <p>
                            Bạn chưa theo dõi cửa hàng nào.
                        </p>

                        <Link to="/shops">
                            Khám phá cửa hàng
                        </Link>
                    </div>
                )}

                {shops.length > 0 && (
                    <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
                        {shops.map((shop) => (
                            <article
                                key={shop.shopId}
                                className="rounded-2xl border border-gray-200 p-5 dark:border-white/10"
                            >
                                <div>
                                    {shop.logoUrl && (
                                        <img
                                            src={shop.logoUrl}
                                            alt={shop.name}
                                            className="mb-3 h-20 w-20 rounded-xl object-cover"
                                        />
                                    )}

                                    <h2>
                                        <Link
                                            to={`/shops/${shop.shopId}`}
                                        >
                                            {shop.name}
                                        </Link>
                                    </h2>

                                    <p>
                                        {shop.description ||
                                            'Cửa hàng chưa thêm mô tả.'}
                                    </p>

                                    {!shop.active && (
                                        <p>
                                            Cửa hàng hiện không hoạt động.
                                        </p>
                                    )}
                                    {shop.followedAt && <p className="muted text-sm">Theo dõi từ {new Date(shop.followedAt).toLocaleDateString('vi-VN')}</p>}

                                    <button
                                        type="button"
                                        onClick={() =>
                                            remove(shop.shopId)
                                        }
                                    >
                                        Bỏ theo dõi
                                    </button>
                                </div>
                            </article>
                        ))}
                    </div>
                )}
            </section>
        </main>
    )
}
