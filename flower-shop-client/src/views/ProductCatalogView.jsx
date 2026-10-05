import React from 'react';
import { useCatalogController } from '../controllers/useCatalogController';
import { useFavoriteToggle } from '../controllers/useFavoritesController';
import { formatPrice, sortOptions } from '../models/productModel';
import { CartIcon } from '../icons';
import { useCartContext } from '../context/CartContext';
import { toast } from 'react-hot-toast';

export default function ProductCatalogView({ auth, shops, isEmbedded = false }) {
  const c = useCatalogController(shops);
  const favorites = useFavoriteToggle(auth?.user);
  const cartContext = useCartContext();

  const handleQuickAdd = async (e, product) => {
    e.preventDefault();
    e.stopPropagation();
    if (!cartContext?.addToCart) {
      toast.error('Chức năng giỏ hàng tạm thời không khả dụng.');
      return;
    }
    try {
      await cartContext.addToCart(product.id, 1, product);
      toast.success(`Đã thêm "${product.name}" vào giỏ hàng!`);
    } catch (err) {
      toast.error(err.message || 'Không thể thêm vào giỏ hàng.');
    }
  };

  return (
    <section id="products" className={isEmbedded ? 'space-y-6' : 'mx-auto w-full max-w-7xl py-8 space-y-8 px-4 sm:px-6'}>
      {/* Section Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold tracking-tight text-gray-900 dark:text-white sm:text-3xl">
            {isEmbedded ? 'Khám phá sản phẩm' : '✿ Danh mục sản phẩm'}
          </h2>
          <p className="text-sm sm:text-base text-gray-500 dark:text-gray-400 mt-1">
            Hoa tươi từ tất cả các cửa hàng uy tín đang hoạt động trên hệ thống.
          </p>
        </div>

        {!isEmbedded && (
          <div className="flex items-center gap-3">
            <a
              href="/"
              className="text-sm font-medium text-gray-600 hover:text-brand-500 dark:text-gray-400 dark:hover:text-brand-400 transition-colors"
            >
              ← Về trang chủ
            </a>
            {auth?.user?.role === 'CUSTOMER' && (
              <a
                href="/favorites"
                className="inline-flex items-center gap-1.5 rounded-xl border border-gray-200 bg-white px-3.5 py-2 text-sm font-semibold text-gray-700 shadow-xs hover:bg-gray-50 dark:border-white/10 dark:bg-gray-800 dark:text-gray-300 transition-colors"
              >
                ♥ Sản phẩm yêu thích
              </a>
            )}
          </div>
        )}
      </div>

      {/* Filter Bar */}
      <div className="rounded-2xl border border-gray-200 bg-white p-5 shadow-xs dark:border-white/10 dark:bg-gray-800">
        <form onSubmit={c.search} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 items-end">
          <div>
            <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-gray-500 dark:text-gray-400">
              Tên sản phẩm
            </label>
            <input
              type="search"
              maxLength={100}
              value={c.draft}
              onChange={e => c.setDraft(e.target.value)}
              placeholder="Nhập tên hoa..."
              className="w-full rounded-xl border border-gray-300 bg-gray-50 px-4 py-2.5 text-sm text-gray-900 outline-none transition-all focus:border-brand-500 focus:bg-white focus:ring-2 focus:ring-brand-500/20 dark:border-white/10 dark:bg-gray-900 dark:text-white dark:focus:border-brand-500"
            />
          </div>

          <div>
            <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-gray-500 dark:text-gray-400">
              Danh mục
            </label>
            <select
              value={c.filter.categoryId}
              onChange={e => c.setCategory(e.target.value)}
              className="w-full rounded-xl border border-gray-300 bg-gray-50 px-4 py-2.5 text-sm text-gray-900 outline-none transition-all focus:border-brand-500 focus:bg-white focus:ring-2 focus:ring-brand-500/20 dark:border-white/10 dark:bg-gray-900 dark:text-white dark:focus:border-brand-500"
            >
              <option value="">Tất cả danh mục</option>
              {c.categories.map(x => (
                <option key={x.id} value={x.id}>
                  {x.name} ({x.productCount})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-gray-500 dark:text-gray-400">
              Sắp xếp
            </label>
            <select
              value={c.filter.sort}
              onChange={e => c.setSort(e.target.value)}
              className="w-full rounded-xl border border-gray-300 bg-gray-50 px-4 py-2.5 text-sm text-gray-900 outline-none transition-all focus:border-brand-500 focus:bg-white focus:ring-2 focus:ring-brand-500/20 dark:border-white/10 dark:bg-gray-900 dark:text-white dark:focus:border-brand-500"
            >
              {sortOptions.map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </div>

          <button
            type="submit"
            disabled={c.busy}
            className="w-full rounded-xl bg-brand-500 py-2.5 px-6 font-semibold text-white shadow-xs transition-all hover:bg-brand-600 focus:outline-none focus:ring-2 focus:ring-brand-500/50 disabled:opacity-50"
          >
            {c.busy ? 'Đang tìm...' : 'Tìm kiếm'}
          </button>
        </form>
      </div>

      {/* Loading state */}
      {c.busy && (
        <div className="py-8 text-center text-sm font-medium text-gray-500 dark:text-gray-400">
          <div className="inline-block h-6 w-6 animate-spin rounded-full border-2 border-brand-500 border-t-transparent mr-2 align-middle"></div>
          Đang tải danh sách sản phẩm…
        </div>
      )}

      {/* Error alert */}
      {c.error && (
        <div className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700 dark:border-rose-500/20 dark:bg-rose-500/10 dark:text-rose-400 flex items-center justify-between">
          <span>{c.error}</span>
          <button
            onClick={c.retry}
            className="ml-4 font-semibold underline hover:no-underline"
          >
            Thử lại
          </button>
        </div>
      )}

      {favorites.error && (
        <div className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700 dark:border-rose-500/20 dark:bg-rose-500/10 dark:text-rose-400">
          {favorites.error}
        </div>
      )}

      {/* Product Grid */}
      {c.data && (
        <div className="space-y-6">
          <div className="flex items-center justify-between text-sm text-gray-500 dark:text-gray-400">
            <span>
              Tìm thấy <strong className="font-semibold text-gray-900 dark:text-white">{c.data.totalElements}</strong> sản phẩm
            </span>
          </div>

          {!c.data.content.length ? (
            <div className="rounded-2xl border border-dashed border-gray-300 p-12 text-center dark:border-white/10">
              <span className="text-4xl">🌷</span>
              <h3 className="mt-3 text-base font-bold text-gray-900 dark:text-white">Không có sản phẩm phù hợp</h3>
              <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                Hãy thử thay đổi từ khóa tìm kiếm hoặc chọn danh mục khác.
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
              {c.data.content.map(p => {
                const imageSrc = p.imageUrl || (p.images && p.images[0]) || '';
                const isFavorite = favorites.saved.has(p.id);

                return (
                  <div
                    key={p.id}
                    className="group relative flex flex-col overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-xs transition-all duration-300 hover:-translate-y-1 hover:border-brand-300 hover:shadow-lg dark:border-white/10 dark:bg-gray-800 dark:hover:border-brand-500/50"
                  >
                    {/* Image */}
                    <div className="relative aspect-4/3 w-full overflow-hidden bg-gray-100 dark:bg-gray-900">
                      <a href={`/products/${encodeURIComponent(p.id)}`} className="block w-full h-full">
                        {imageSrc ? (
                          <img
                            src={imageSrc}
                            alt={p.name}
                            className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
                            loading="lazy"
                          />
                        ) : (
                          <div className="flex h-full w-full items-center justify-center text-sm font-medium text-gray-400">
                            Chưa có ảnh
                          </div>
                        )}
                      </a>

                      {/* Favorite button */}
                      {(!auth?.user || auth.user.role === 'CUSTOMER') && (
                        <button
                          type="button"
                          onClick={(e) => {
                            e.preventDefault();
                            e.stopPropagation();
                            favorites.toggle(p.id);
                          }}
                          aria-label={isFavorite ? 'Bỏ lưu yêu thích' : 'Lưu yêu thích'}
                          className={`absolute top-3 right-3 flex h-9 w-9 items-center justify-center rounded-full backdrop-blur-md shadow-xs transition-all active:scale-90 ${
                            isFavorite
                              ? 'bg-rose-50 text-rose-500 hover:bg-rose-100 dark:bg-rose-500/20'
                              : 'bg-white/80 text-gray-500 hover:bg-white hover:text-rose-500 dark:bg-gray-800/80 dark:text-gray-300'
                          }`}
                        >
                          <span className="text-lg leading-none">{isFavorite ? '♥' : '♡'}</span>
                        </button>
                      )}

                      {/* Category tag */}
                      {p.categoryName && (
                        <span className="absolute bottom-3 left-3 rounded-lg bg-black/60 px-2.5 py-1 text-xs font-medium text-white backdrop-blur-xs">
                          {p.categoryName}
                        </span>
                      )}
                    </div>

                    {/* Card Content */}
                    <div className="flex flex-1 flex-col p-4">
                      <div className="mb-1 flex items-center justify-between gap-2 text-xs text-gray-500 dark:text-gray-400">
                        <span className="truncate font-medium">{p.shopName}</span>
                        {p.reviewCount > 0 ? (
                          <span className="flex items-center text-amber-500 font-semibold shrink-0">
                            ★ {p.rating} <span className="ml-0.5 text-gray-400 font-normal">({p.reviewCount})</span>
                          </span>
                        ) : (
                          <span className="text-gray-400 shrink-0">Mới</span>
                        )}
                      </div>

                      <h3 className="mb-2 line-clamp-2 text-base font-bold text-gray-900 transition-colors group-hover:text-brand-500 dark:text-white">
                        <a href={`/products/${encodeURIComponent(p.id)}`}>{p.name}</a>
                      </h3>

                      <div className="mt-auto pt-3 border-t border-gray-100 dark:border-white/5 flex items-center justify-between">
                        <div>
                          <div className="text-lg font-black text-brand-600 dark:text-brand-400">
                            {formatPrice(p.price)}
                          </div>
                          <div className="text-xs text-gray-500 dark:text-gray-400">
                            {p.stock > 0 ? `Còn ${p.stock}` : <span className="text-rose-500 font-medium">Hết hàng</span>}
                          </div>
                        </div>

                        {p.stock > 0 && (
                          <button
                            type="button"
                            onClick={(e) => handleQuickAdd(e, p)}
                            className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-500 text-white shadow-xs transition-all hover:bg-brand-600 hover:shadow-md active:scale-95 cursor-pointer"
                            title="Thêm nhanh vào giỏ hàng"
                          >
                            <CartIcon className="h-5 w-5" />
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {/* Pagination */}
          {c.data.totalPages > 1 && (
            <div className="flex justify-center items-center gap-3 pt-6">
              <button
                type="button"
                disabled={c.busy || c.data.page === 0}
                onClick={() => c.changePage(-1)}
                className="rounded-xl border border-gray-300 bg-white px-4 py-2 text-sm font-semibold text-gray-700 shadow-xs transition-colors hover:bg-gray-50 disabled:opacity-40 cursor-pointer dark:border-white/10 dark:bg-gray-800 dark:text-gray-300"
              >
                ← Trang trước
              </button>
              <span className="text-sm font-semibold text-gray-700 dark:text-gray-300 px-3">
                Trang {c.data.page + 1} / {c.data.totalPages}
              </span>
              <button
                type="button"
                disabled={c.busy || c.data.page + 1 >= c.data.totalPages}
                onClick={() => c.changePage(1)}
                className="rounded-xl border border-gray-300 bg-white px-4 py-2 text-sm font-semibold text-gray-700 shadow-xs transition-colors hover:bg-gray-50 disabled:opacity-40 cursor-pointer dark:border-white/10 dark:bg-gray-800 dark:text-gray-300"
              >
                Trang sau →
              </button>
            </div>
          )}
        </div>
      )}
    </section>
  );
}
