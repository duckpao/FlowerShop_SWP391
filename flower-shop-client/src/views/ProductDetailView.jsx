import { useState } from "react";
import { useProductDetailController } from "../controllers/useProductDetailController";
import { Link, useNavigate } from "react-router";
import PageMeta from "../components/common/PageMeta";
import ComponentCard from "../components/common/ComponentCard";
import Badge from "../components/ui/badge/Badge";
import Button from "../components/ui/button/Button";
import { CartIcon } from "../icons";
import { useCartContext } from "../context/CartContext";
import { toast } from "react-hot-toast";

export default function ProductDetailView({ id }) {
  const { product, busy, error } = useProductDetailController(id);
  const { addToCart } = useCartContext();
  const navigate = useNavigate();
  const [quantity, setQuantity] = useState(1);
  const [adding, setAdding] = useState(false);

  const handleAddToCart = async () => {
    if (!product || product.stock <= 0) return;
    setAdding(true);
    try {
      await addToCart(product.id, quantity, product);
      toast.success(`Đã thêm ${quantity} "${product.name}" vào giỏ hàng!`);
    } catch (err) {
      toast.error(err.message || "Không thể thêm vào giỏ hàng.");
    } finally {
      setAdding(false);
    }
  };

  const handleBuyNow = async () => {
    if (!product || product.stock <= 0) return;
    setAdding(true);
    try {
      await addToCart(product.id, quantity, product);
      navigate("/cart");
    } catch (err) {
      toast.error(err.message || "Không thể thêm vào giỏ hàng.");
      setAdding(false);
    }
  };

  return (
    <div className="p-4 sm:p-6 lg:p-8 w-full max-w-screen-xl mx-auto">
      <div className="mb-6">
        <Link to="/" className="inline-flex items-center text-sm font-medium text-gray-500 hover:text-brand-500 dark:text-gray-400 dark:hover:text-brand-400 transition-colors">
          <svg className="mr-2 w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
          Quay lại trang chủ
        </Link>
      </div>

      {busy && (
        <div className="flex justify-center p-12">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-solid border-brand-500 border-t-transparent"></div>
        </div>
      )}

      {error && (
        <div className="rounded-lg bg-error-50 p-4 text-sm text-error-500 dark:bg-error-500/10 dark:text-error-400">
          {error}
        </div>
      )}

      {product && (
        <>
          <PageMeta title={`${product.name} | Sản phẩm`} description={product.description} />
          
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-8">
            {/* Gallery Section */}
            <div className="space-y-4">
              {product.images && product.images.length > 0 ? (
                <>
                  <div className="rounded-2xl border border-gray-200 bg-white overflow-hidden flex items-center justify-center p-4 aspect-square dark:border-white/5 dark:bg-white/3">
                    <img
                      src={product.images[0]}
                      alt={product.name}
                      className="max-w-full max-h-full object-contain"
                    />
                  </div>
                  {product.images.length > 1 && (
                    <div className="grid grid-cols-4 gap-4">
                      {product.images.map((url, i) => (
                        <div key={i} className={`rounded-lg border bg-white overflow-hidden aspect-square ${i === 0 ? 'border-brand-500 ring-2 ring-brand-500/20' : 'border-gray-200 hover:border-brand-300'} dark:bg-white/3 dark:border-white/5`}>
                          <img
                            src={url}
                            alt=""
                            className="w-full h-full object-cover"
                          />
                        </div>
                      ))}
                    </div>
                  )}
                </>
              ) : (
                <div className="rounded-2xl border border-gray-200 bg-gray-50 flex items-center justify-center aspect-square dark:border-white/5 dark:bg-white/5">
                  <svg className="w-24 h-24 text-gray-300 dark:text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                  </svg>
                </div>
              )}
            </div>

            {/* Info Section */}
            <div>
              <div className="mb-2">
                <Badge color="primary">{product.categoryName}</Badge>
              </div>
              <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-4">
                {product.name}
              </h1>
              
              <div className="flex items-center gap-2 mb-6 text-gray-600 dark:text-gray-400">
                <svg className="w-5 h-5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                </svg>
                <span>Cửa hàng: <span className="font-semibold text-gray-900 dark:text-white">{product.shopName}</span></span>
              </div>
              
              <div className="rounded-xl border border-brand-100 bg-brand-50/50 p-6 mb-6 dark:border-brand-500/20 dark:bg-brand-500/5">
                <p className="text-3xl font-bold text-brand-600 dark:text-brand-400 mb-2">
                  {Number(product.price).toLocaleString("vi-VN")} đ
                </p>
                <p className="text-sm text-gray-600 dark:text-gray-400">
                  Tình trạng: <span className="font-semibold">{product.stock > 0 ? `Còn ${product.stock} sản phẩm` : "Hết hàng"}</span>
                </p>
              </div>

              {/* Quantity selector & Add to cart */}
              {product.stock > 0 && (
                <div className="space-y-4 mb-8">
                  <div className="flex items-center gap-4">
                    <span className="text-sm font-semibold text-gray-700 dark:text-gray-300">Số lượng:</span>
                    <div className="flex items-center border border-gray-300 dark:border-white/10 rounded-lg overflow-hidden bg-gray-50 dark:bg-gray-800">
                      <button
                        type="button"
                        onClick={() => setQuantity(q => Math.max(1, q - 1))}
                        disabled={quantity <= 1 || adding}
                        className="px-3.5 py-2 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-white/10 disabled:opacity-40 transition-colors font-bold"
                      >
                        -
                      </button>
                      <span className="px-4 py-2 text-sm font-semibold text-gray-800 dark:text-white min-w-[40px] text-center">
                        {quantity}
                      </span>
                      <button
                        type="button"
                        onClick={() => setQuantity(q => Math.min(product.stock, q + 1))}
                        disabled={quantity >= product.stock || adding}
                        className="px-3.5 py-2 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-white/10 disabled:opacity-40 transition-colors font-bold"
                      >
                        +
                      </button>
                    </div>
                  </div>

                  <div className="flex flex-col sm:flex-row gap-3">
                    <Button
                      onClick={handleAddToCart}
                      disabled={adding}
                      className="flex-1 py-3.5 text-base font-bold shadow-md flex items-center justify-center gap-2"
                    >
                      <CartIcon className="h-5 w-5" />
                      {adding ? "Đang xử lý..." : "Thêm vào giỏ hàng"}
                    </Button>
                    <Button
                      variant="outline"
                      onClick={handleBuyNow}
                      disabled={adding}
                      className="flex-1 py-3.5 text-base font-bold"
                    >
                      Mua ngay
                    </Button>
                  </div>
                </div>
              )}
              
              <ComponentCard title="Mô tả sản phẩm" className="mb-8">
                <div className="prose prose-sm sm:prose-base dark:prose-invert max-w-none text-gray-600 dark:text-gray-300 whitespace-pre-line">
                  {product.description}
                </div>
              </ComponentCard>
            </div>
          </div>

          {/* Videos Section */}
          {product.videos && product.videos.length > 0 && (
            <ComponentCard title="Video sản phẩm">
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {product.videos.map((v, i) => (
                  <div key={i} className="rounded-xl border border-gray-200 overflow-hidden dark:border-white/5">
                    <video src={v.url} controls className="w-full aspect-video bg-black object-contain" />
                    {(v.title || v.description) && (
                      <div className="p-4 bg-white dark:bg-white/5">
                        {v.title && (
                          <h4 className="font-semibold text-gray-900 dark:text-white mb-1">
                            {v.title}
                          </h4>
                        )}
                        {v.description && (
                          <p className="text-sm text-gray-500 dark:text-gray-400">
                            {v.description}
                          </p>
                        )}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </ComponentCard>
          )}
        </>
      )}
    </div>
  );
}
