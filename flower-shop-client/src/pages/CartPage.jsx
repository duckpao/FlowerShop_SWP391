import { useState, useEffect } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router';
import PageMeta from '../components/common/PageMeta';
import PageBreadCrumb from '../components/common/PageBreadCrumb';
import Button from '../components/ui/button/Button';
import { TrashBinIcon } from '../icons';

export default function CartPage({ cart, loading, error, fetchCart, updateItem, removeItem, userId, user }) {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const requestedIds = searchParams.getAll('item');
  const requestedIdsKey = searchParams.toString();
  const [selectedItemIds, setSelectedItemIds] = useState(null);

  useEffect(() => {
    if (cart?.items) {
      setSelectedItemIds(previous => {
        // Giỏ rỗng (khách chưa kịp đăng nhập) không được chốt lựa chọn thành [], nếu không giỏ thật về sau sẽ không món nào được chọn.
        if (previous === null && cart.items.length === 0) return null;
        return previous === null
          ? cart.items.filter(item => requestedIds.length === 0 || requestedIds.includes(item.id)).map(item => item.id)
          : previous.filter(id => cart.items.some(item => item.id === id));
      });
    } else {
      setSelectedItemIds(null);
    }
  }, [cart, requestedIdsKey]);

  useEffect(() => { fetchCart(); }, [fetchCart]);

  const formatPrice = amount => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);
  const items = cart?.items || [];
  const totalQuantity = items.reduce((sum, item) => sum + Number(item.quantity || 0), 0);
  const selectedItems = items.filter(item => selectedItemIds?.includes(item.id));
  const selectedQuantity = selectedItems.reduce((sum, item) => sum + Number(item.quantity || 0), 0);
  const selectedTotal = selectedItems.reduce((sum, item) => sum + Number(item.itemTotal || 0), 0);
  const shopIds = [...new Set(selectedItems.map(item => item.product?.shopId))];
  const hasMultipleShopsSelected = shopIds.length > 1;
  const hasOverStockItem = selectedItems.some(item => item.product?.stock != null && item.quantity > item.product.stock);

  const handleToggleItem = itemId => {
    setSelectedItemIds(previous => (previous || []).includes(itemId) ? previous.filter(id => id !== itemId) : [...(previous || []), itemId]);
  };
  const handleToggleSelectAll = () => {
    setSelectedItemIds(selectedItemIds?.length === items.length ? [] : items.map(item => item.id));
  };
  const continueToCheckout = () => {
    if (!selectedItems.length || hasMultipleShopsSelected || hasOverStockItem) return;
    if (!userId || !user) {
      navigate('/login?next=/cart');
      return;
    }
    if (user.role !== 'CUSTOMER') return;
    const query = new URLSearchParams();
    selectedItems.forEach(item => query.append('item', item.id));
    navigate(`/checkout?${query.toString()}`);
  };

  return (
    <div className="p-4 sm:p-6 lg:p-8 w-full max-w-screen-2xl mx-auto">
      <PageMeta title="Giỏ hàng | Cửa hàng hoa" description="Chọn sản phẩm để xác nhận đơn hàng" />
      <PageBreadCrumb pageTitle={`Giỏ hàng (${totalQuantity})`} />
      {error && <div role="alert" className="mb-6 rounded-xl border border-error-200 bg-error-50 p-4 text-error-600 dark:border-error-500/20 dark:bg-error-500/10 dark:text-error-400">{error}</div>}
      {!cart && error ? (
        <div className="py-8 text-center"><Button variant="outline" onClick={fetchCart}>Tải lại giỏ hàng</Button></div>
      ) : !cart ? (
        <p role="status" className="py-12 text-center text-gray-500">Đang tải giỏ hàng…</p>
      ) : items.length === 0 && !loading ? (
        <div className="rounded-2xl border border-gray-200 bg-white p-12 text-center shadow-sm dark:border-white/5 dark:bg-white/3 max-w-xl mx-auto my-12">
          <div className="text-6xl mb-4">🌸</div>
          <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">Giỏ hàng của bạn đang trống</h2>
          <p className="text-gray-500 dark:text-gray-400 mb-6">Hãy chọn những bó hoa tươi đẹp nhất gửi tặng người thân yêu nhé!</p>
          <Link to="/"><Button className="px-8 py-3">← Khám phá sản phẩm</Button></Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          {/* Danh sách sản phẩm trong giỏ */}

          <div className="lg:col-span-8 space-y-4">
            {hasMultipleShopsSelected && (
              <div className="rounded-xl border border-warning-200 bg-warning-50 p-4 dark:border-warning-500/20 dark:bg-warning-500/10 text-warning-800 dark:text-warning-300 text-sm flex items-start gap-3">
                <span className="text-xl">⚠️</span>
                <div>
                  <strong>Lưu ý đặt hàng đa cửa hàng:</strong>
                  <p className="mt-0.5">
                    Bạn đang chọn sản phẩm của {shopIds.length} shop khác nhau. Mỗi đơn hàng chỉ áp dụng cho 1 shop. Vui lòng bỏ chọn sản phẩm của các shop khác trước khi tiến hành thanh toán.
                  </p>
                </div>
              </div>
            )}

            {/* Thanh thao tác chọn tất cả */}
            <div className="rounded-xl border border-gray-200 bg-white px-5 py-4 shadow-sm dark:border-white/5 dark:bg-white/3 flex items-center justify-between">
              <label className="flex items-center gap-3 cursor-pointer text-sm font-semibold text-gray-800 dark:text-white">
                <input
                  type="checkbox"
                  checked={items.length > 0 && selectedItemIds?.length === items.length}
                  onChange={handleToggleSelectAll}
                  className="h-4 w-4 rounded border-gray-300 text-brand-600 focus:ring-brand-500 dark:border-white/10 dark:bg-gray-800"
                />
                <span>Chọn tất cả ({totalQuantity} sản phẩm)</span>
              </label>

              <span className="text-xs text-gray-500 dark:text-gray-400">
                Đã chọn: <strong className="text-brand-600 dark:text-brand-400">{selectedQuantity}</strong> sản phẩm
              </span>
            </div>

            {/* Danh sách từng sản phẩm */}
            <div className="space-y-3">
              {items.map((item) => {
                const isSelected = selectedItemIds?.includes(item.id);
                const product = item.product || {};
                const imageUrl = product.images?.[0] || product.image;

                return (
                  <div
                    key={item.id}
                    className={`rounded-xl border bg-white p-4 sm:p-5 shadow-sm transition-all dark:bg-white/3 ${
                      isSelected
                        ? 'border-brand-300 ring-1 ring-brand-300/30 dark:border-brand-500/40'
                        : 'border-gray-200 dark:border-white/5'
                    }`}
                  >
                    <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4">
                      {/* Checkbox */}
                      <input
                        type="checkbox"
                        checked={isSelected}
                        onChange={() => handleToggleItem(item.id)}
                        className="h-4 w-4 rounded border-gray-300 text-brand-600 focus:ring-brand-500 dark:border-white/10 dark:bg-gray-800 mt-1 sm:mt-0"
                      />

                      {/* Ảnh sản phẩm */}
                      <Link
                        to={`/products/${encodeURIComponent(product.id || '')}`}
                        className="h-20 w-20 flex-shrink-0 overflow-hidden rounded-lg border border-gray-100 bg-gray-50 dark:border-white/5 dark:bg-gray-800 flex items-center justify-center"
                      >
                        {imageUrl ? (
                          <img src={imageUrl} alt={product.name} className="h-full w-full object-cover" />
                        ) : (
                          <span className="text-2xl">💐</span>
                        )}
                      </Link>

                      {/* Tên & Shop */}
                      <div className="flex-1 min-w-0">
                        <Link
                          to={`/products/${encodeURIComponent(product.id || '')}`}
                          className="font-semibold text-gray-900 hover:text-brand-500 dark:text-white dark:hover:text-brand-400 line-clamp-1 transition-colors text-base"
                        >
                          {product.name || 'Sản phẩm'}
                        </Link>
                        <div className="flex flex-wrap items-center gap-2 mt-1">
                          {product.shopName && (
                            <span className="text-xs font-medium text-brand-600 bg-brand-50 dark:bg-brand-500/10 dark:text-brand-300 px-2 py-0.5 rounded">
                              🏪 {product.shopName}
                            </span>
                          )}
                          {product.stock != null && (
                            <span className={`text-xs px-2 py-0.5 rounded font-medium ${
                              item.quantity > product.stock
                                ? 'bg-error-50 text-error-600 dark:bg-error-500/10 dark:text-error-400 font-bold'
                                : 'bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-300'
                            }`}>
                              {item.quantity > product.stock
                                ? `⚠️ Vượt quá tồn kho (Còn ${product.stock})`
                                : `Kho: ${product.stock}`}
                            </span>
                          )}
                        </div>
                        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400 sm:hidden">
                          Đơn giá: {formatPrice(item.unitPrice || product.price)}
                        </p>
                      </div>

                      {/* Đơn giá (Desktop) */}
                      <div className="hidden sm:block text-right min-w-[100px]">
                        <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                          {formatPrice(item.unitPrice || product.price)}
                        </span>
                      </div>

                      {/* Bộ điều khiển số lượng */}
                      <div className="flex items-center border border-gray-200 dark:border-white/10 rounded-lg overflow-hidden bg-gray-50 dark:bg-gray-800">
                        <button
                          type="button"
                          onClick={() => updateItem(item.id, Math.max(1, item.quantity - 1))}
                          disabled={item.quantity <= 1 || loading}
                          className="px-2.5 py-1 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-white/10 disabled:opacity-40 transition-colors font-bold"
                        >
                          -
                        </button>
                        <span className="px-3 py-1 text-sm font-semibold text-gray-800 dark:text-white min-w-[32px] text-center">
                          {item.quantity}
                        </span>
                        <button
                          type="button"
                          onClick={() => updateItem(item.id, item.quantity + 1)}
                          disabled={loading || (product.stock != null && item.quantity >= product.stock)}
                          className="px-2.5 py-1 text-gray-600 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-white/10 disabled:opacity-40 transition-colors font-bold"
                          title={product.stock != null && item.quantity >= product.stock ? `Đã đạt số lượng tồn kho tối đa (${product.stock})` : 'Tăng số lượng'}
                        >
                          +
                        </button>
                      </div>

                      {/* Thành tiền */}
                      <div className="text-right min-w-[110px]">
                        <span className="font-bold text-brand-600 dark:text-brand-400 text-base">
                          {formatPrice(item.itemTotal || (item.unitPrice * item.quantity))}
                        </span>
                      </div>

                      {/* Nút xóa */}
                      <button
                        type="button"
                        onClick={() => removeItem(item.id)}
                        disabled={loading}
                        className="text-gray-400 hover:text-error-500 transition-colors p-1 rounded hover:bg-error-50 dark:hover:bg-error-500/10"
                        title="Xóa sản phẩm này"
                      >
                        <TrashBinIcon className="h-5 w-5" />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>

            <div className="pt-2">
              <Link to="/" className="inline-flex items-center text-sm font-medium text-brand-600 dark:text-brand-400 hover:underline gap-1">
                ← Tiếp tục xem và chọn thêm hoa
              </Link>
            </div>
          </div>

          {/* Bước chọn sản phẩm, trước trang xác nhận đơn hàng */}
          <aside className="lg:col-span-4 lg:sticky lg:top-24">
            <div className="space-y-5 rounded-2xl border border-gray-200 bg-white p-6 shadow-sm dark:border-white/5 dark:bg-white/3">
              <div>
                <h2 className="text-lg font-bold text-gray-900 dark:text-white">Sản phẩm đã chọn</h2>
                <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">Kiểm tra lựa chọn trước khi nhập thông tin nhận hoa.</p>
              </div>
              <div className="space-y-2 border-t border-gray-100 pt-4 text-sm dark:border-white/5">
                <div className="flex justify-between text-gray-600 dark:text-gray-400"><span>Số lượng</span><span>{selectedQuantity} sản phẩm</span></div>
                <div className="flex justify-between border-t border-gray-100 pt-3 text-base font-bold text-gray-900 dark:border-white/5 dark:text-white"><span>Tạm tính</span><span className="text-brand-600 dark:text-brand-400">{formatPrice(selectedTotal)}</span></div>
              </div>
              {hasOverStockItem && <p role="alert" className="rounded-lg bg-error-50 p-3 text-sm text-error-600">Có sản phẩm vượt tồn kho. Vui lòng giảm số lượng.</p>}
              <Button type="button" onClick={continueToCheckout} disabled={loading || !selectedItems.length || hasMultipleShopsSelected || hasOverStockItem || Boolean(user && user.role !== 'CUSTOMER')} className="w-full justify-center py-3.5 text-base font-bold">
                {!userId ? 'Đăng nhập để tiếp tục →' : 'Xác nhận thanh toán →'}
              </Button>
              <p className="text-center text-xs text-gray-400 dark:text-gray-500">{user && user.role !== 'CUSTOMER' ? 'Chỉ tài khoản khách hàng có thể đặt hàng.' : 'Thông tin giao hàng và phương thức thanh toán ở bước tiếp theo.'}</p>
            </div>
          </aside>
        </div>
      )}
    </div>
  );
}
