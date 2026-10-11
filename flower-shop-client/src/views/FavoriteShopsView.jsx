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
    } = useFavoriteShopsController(user)

    if (loading) {
        return (
            <main className="catalog-page">
                <section className="catalog-panel">
                    <p>Đang tải danh sách cửa hàng...</p>
                </section>
            </main>
        )
    }

    return (
        <main className="catalog-page">
            <section className="catalog-panel">
                <h1>Cửa hàng yêu thích</h1>

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
                    <div>
                        {shops.map((shop) => (
                            <article
                                key={shop.shopId}
                            >
                                <div>
                                    {shop.logoUrl && (
                                        <img
                                            src={shop.logoUrl}
                                            alt={shop.name}
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