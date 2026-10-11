import React, { useState } from 'react';
import { useProductDetailController } from '../controllers/useProductDetailController';
import { useFavoriteToggle } from '../controllers/useFavoritesController';
import ProductReviewsView from './ProductReviewsView';
import { formatPrice } from '../models/productModel';
import { CartIcon } from '../icons';
import { useCartContext } from '../context/CartContext';
import { toast } from 'react-hot-toast';

export default function ProductDetailView({ auth, productId, id }) {
  const targetId = productId || id;
  const c = useProductDetailController(targetId);
  const favorites = useFavoriteToggle(auth?.user);
  const cartContext = useCartContext();

  const [quantity, setQuantity] = useState(1);
  const [selectedImage, setSelectedImage] = useState(0);
  const [busyCart, setBusyCart] = useState(false);

  const p = c.product;
  const isFavorite = p ? favorites.saved.has(p.id) : false;

  const handleAddToCart = async () => {
    if (!p || p.stock <= 0) return;
    if (!cartContext?.addToCart) {
      toast.error('Chức năng giỏ hàng tạm thời không khả dụng.');
      return;
    }
    setBusyCart(true);
    try {
      await cartContext.addToCart(p.id, quantity, p);
      toast.success(`Đã thêm ${quantity} "${p.name}" vào giỏ hàng!`);
    } catch (err) {
      toast.error(err.message || 'Không thể thêm vào giỏ hàng.');
    } finally {
      setBusyCart(false);
    }
  };

  const handleBuyNow = async () => {
    if (!p || p.stock <= 0) return;
    if (!cartContext?.addToCart) {
      toast.error('Chức năng giỏ hàng tạm thời không khả dụng.');
      return;
    }
    setBusyCart(true);
    try {
      await cartContext.addToCart(p.id, quantity, p);
      window.location.href = '/cart';
    } catch (err) {
      toast.error(err.message || 'Không thể thêm vào giỏ hàng.');
      setBusyCart(false);
    }
  };

  return (
    <main className="mx-auto w-full max-w-7xl py-8 px-4 sm:px-6 space-y-8">
      {/* Top Navigation */}
      <div className="flex items-center justify-between">
        <a
          href="/"
          className="inline-flex items-center gap-2 text-sm font-semibold text-gray-600 hover:text-brand-500 dark:text-gray-400 dark:hover:text-brand-400 transition-colors"
        >
          ← Quay lại danh sách hoa
        </a>

        <div className="flex items-center gap-3">
          {auth?.user?.role === 'CUSTOMER' && (
            <a
              href="/favorites"
              className="text-sm font-medium text-gray-600 hover:text-brand-500 dark:text-gray-400 dark:hover:text-brand-400 transition-colors"
            >
              ♥ Sản phẩm yêu thích
            </a>
          )}
          {!auth?.user && (
            <a
              href="/login"
              className="text-sm font-semibold text-brand-500 hover:text-brand-600"
            >
              Đăng nhập
            </a>
          )}
        </div>
      </div>

      {/* Loading & Error */}
      {c.busy && (
        <div className="py-16 text-center text-sm font-medium text-gray-500 dark:text-gray-400">
          <div className="inline-block h-8 w-8 animate-spin rounded-full border-3 border-brand-500 border-t-transparent mr-2 align-middle"></div>
          Đang tải thông tin sản phẩm…
        </div>
      )}

      {c.error && (
        <div className="rounded-2xl border border-rose-200 bg-rose-50 p-6 text-center dark:border-rose-500/20 dark:bg-rose-500/10">
          <p className="text-sm font-medium text-rose-600 dark:text-rose-400 mb-3">{c.error}</p>
          <button
            onClick={c.reload}
            className="rounded-xl bg-rose-600 px-4 py-2 text-sm font-semibold text-white hover:bg-rose-700"
          >
            Thử lại
          </button>
        </div>
      )}

      {/* Product Detail Card */}
      {p && (
        <div className="rounded-3xl border border-gray-200 bg-white p-6 sm:p-8 shadow-xs dark:border-white/10 dark:bg-gray-800">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 lg:gap-12">
            
            {/* Gallery Column */}
            <div className="space-y-4">
              <div className="relative aspect-square w-full overflow-hidden rounded-2xl bg-gray-100 dark:bg-gray-900 border border-gray-200 dark:border-white/10">
                {p.images && p.images.length > 0 ? (
                  <img
                    src={p.images[selectedImage] || p.images[0]}
                    alt={p.name}
                    className="h-full w-full object-cover"
                  />
                ) : p.imageUrl ? (
                  <img
                    src={p.imageUrl}
                    alt={p.name}
                    className="h-full w-full object-cover"
                  />
                ) : (
                  <div className="flex h-full w-full items-center justify-center text-gray-400">
                    Chưa có ảnh
                  </div>
                )}

                {/* Favorite Button */}
                {(!auth?.user || auth.user.role === 'CUSTOMER') && (
                  <button
                    type="button"
                    onClick={() => favorites.toggle(p.id)}
                    aria-label={isFavorite ? 'Bỏ lưu yêu thích' : 'Lưu yêu thích'}
                    className={`absolute top-4 right-4 flex h-11 w-11 items-center justify-center rounded-full backdrop-blur-md shadow-md transition-all active:scale-90 ${
                      isFavorite
                        ? 'bg-rose-50 text-rose-500 hover:bg-rose-100 dark:bg-rose-500/20'
                        : 'bg-white/80 text-gray-600 hover:bg-white hover:text-rose-500 dark:bg-gray-800/80 dark:text-gray-300'
                    }`}
                  >
                    <span className="text-xl leading-none">{isFavorite ? '♥' : '♡'}</span>
                  </button>
                )}
              </div>

              {/* Thumbnails */}
              {p.images && p.images.length > 1 && (
                <div className="flex gap-3 overflow-x-auto pb-2">
                  {p.images.map((url, index) => (
                    <button
                      key={url}
                      type="button"
                      onClick={() => setSelectedImage(index)}
                      className={`relative h-20 w-20 shrink-0 overflow-hidden rounded-xl border-2 transition-all ${
                        selectedImage === index
                          ? 'border-brand-500 shadow-md ring-2 ring-brand-500/20'
                          : 'border-transparent opacity-70 hover:opacity-100'
                      }`}
                    >
                      <img src={url} alt="" className="h-full w-full object-cover" />
                    </button>
                  ))}
                </div>
              )}
            </div>

            {/* Info Column */}
            <div className="flex flex-col">
              {/* Category & Rating */}
              <div className="mb-3 flex items-center justify-between gap-4">
                <span className="rounded-full bg-brand-50 px-3 py-1 text-xs font-bold uppercase tracking-wider text-brand-700 dark:bg-brand-500/10 dark:text-brand-400">
                  {p.categoryName || 'Hoa tươi'}
                </span>

                {p.reviewCount > 0 ? (
                  <span className="flex items-center text-sm font-semibold text-amber-500">
                    ★ {p.rating} <span className="ml-1 text-gray-400 font-normal">({p.reviewCount} đánh giá)</span>
                  </span>
                ) : (
                  <span className="text-xs text-gray-400">Chưa có đánh giá</span>
                )}
              </div>

              {/* Name */}
              <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-gray-900 dark:text-white mb-3">
                {p.name}
              </h1>

              {/* Shop info */}
              <div className="mb-6 flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
                <span>Cung cấp bởi:</span>
                <a
                  href={`/shops/${encodeURIComponent(p.shopId)}`}
                  className="font-semibold text-brand-600 hover:text-brand-700 dark:text-brand-400 underline hover:no-underline"
                >
                  {p.shopName}
                </a>
              </div>

              {/* Price & Stock Banner */}
              <div className="rounded-2xl bg-gray-50 dark:bg-gray-900/50 p-5 mb-6 border border-gray-100 dark:border-white/5 flex items-baseline justify-between">
                <div>
                  <span className="text-xs font-semibold text-gray-400 uppercase tracking-wider block mb-1">Giá bán</span>
                  <div className="text-3xl font-black text-brand-600 dark:text-brand-400">
                    {formatPrice(p.price)}
                  </div>
                </div>
                <div className="text-right">
                  <span className="text-xs font-semibold text-gray-400 uppercase tracking-wider block mb-1">Tình trạng</span>
                  <span className={`inline-flex items-center text-sm font-bold ${p.stock > 0 ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-500'}`}>
                    {p.stock > 0 ? `Còn ${p.stock} sản phẩm` : 'Tạm hết hàng'}
                  </span>
                </div>
              </div>

              {/* Description */}
              <div className="mb-6 space-y-2">
                <h3 className="text-sm font-bold uppercase tracking-wider text-gray-500 dark:text-gray-400">Mô tả sản phẩm</h3>
                <p className="text-sm sm:text-base leading-relaxed text-gray-600 dark:text-gray-300 whitespace-pre-line">
                  {p.description || 'Cửa hàng chưa thêm mô tả chi tiết cho sản phẩm này.'}
                </p>
              </div>

              {p.videos && p.videos.length > 0 && (
                <div className="mb-6 space-y-3">
                  <h3 className="text-sm font-bold uppercase tracking-wider text-gray-500 dark:text-gray-400">Video sản phẩm</h3>
                  {p.videos.map(video => (
                    <video key={video.videoUrl} src={video.videoUrl} controls className="w-full rounded-xl bg-black" />
                  ))}
                </div>
              )}

              {/* Actions Section */}
              {p.stock > 0 ? (
                <div className="mt-auto pt-6 border-t border-gray-100 dark:border-white/5 space-y-4">
                  {/* Quantity selector */}
                  <div className="flex items-center gap-4">
                    <span className="text-sm font-semibold text-gray-700 dark:text-gray-300">Số lượng:</span>
                    <div className="inline-flex items-center rounded-xl border border-gray-300 bg-gray-50 dark:border-white/10 dark:bg-gray-900 overflow-hidden">
                      <button
                        type="button"
                        onClick={() => setQuantity(q => Math.max(1, q - 1))}
                        disabled={quantity <= 1 || busyCart}
                        className="px-3.5 py-2 font-bold text-gray-600 hover:bg-gray-200 disabled:opacity-30 dark:text-gray-300 dark:hover:bg-gray-800 transition-colors"
                      >
                        -
                      </button>
                      <span className="w-12 text-center text-sm font-bold text-gray-900 dark:text-white">
                        {quantity}
                      </span>
                      <button
                        type="button"
                        onClick={() => setQuantity(q => Math.min(p.stock, q + 1))}
                        disabled={quantity >= p.stock || busyCart}
                        className="px-3.5 py-2 font-bold text-gray-600 hover:bg-gray-200 disabled:opacity-30 dark:text-gray-300 dark:hover:bg-gray-800 transition-colors"
                      >
                        +
                      </button>
                    </div>
                  </div>

                  {/* Buttons */}
                  <div className="flex flex-col sm:flex-row gap-3">
                    <button
                      type="button"
                      onClick={handleAddToCart}
                      disabled={busyCart}
                      className="flex-1 inline-flex items-center justify-center gap-2 rounded-xl bg-brand-500 py-3.5 px-6 font-bold text-white shadow-xs hover:bg-brand-600 active:scale-98 transition-all disabled:opacity-50 cursor-pointer"
                    >
                      <CartIcon className="h-5 w-5" />
                      {busyCart ? 'Đang thêm…' : 'Thêm vào giỏ hàng'}
                    </button>
                    <button
                      type="button"
                      onClick={handleBuyNow}
                      disabled={busyCart}
                      className="flex-1 rounded-xl border-2 border-brand-500 py-3.5 px-6 font-bold text-brand-600 hover:bg-brand-50 active:scale-98 transition-all dark:text-brand-400 dark:hover:bg-brand-500/10 disabled:opacity-50 cursor-pointer"
                    >
                      Mua ngay
                    </button>
                  </div>
                </div>
              ) : (
                <div className="mt-auto pt-6 border-t border-gray-100 dark:border-white/5">
                  <div className="rounded-xl bg-rose-50 dark:bg-rose-500/10 p-4 text-center text-sm font-semibold text-rose-600 dark:text-rose-400">
                    Sản phẩm hiện tại đã hết hàng tại shop. Bạn có thể lưu yêu thích để theo dõi khi có hàng trở lại!
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Reviews Section */}
      {p && (
        <div className="rounded-3xl border border-gray-200 bg-white p-6 sm:p-8 shadow-xs dark:border-white/10 dark:bg-gray-800">
          <ProductReviewsView product={p} auth={auth} />
        </div>
      )}
    </main>
  );
}
