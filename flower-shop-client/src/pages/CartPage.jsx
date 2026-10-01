import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router';
import orderApi from '../api/orderApi';
import paymentApi from '../api/paymentApi';
import { accountService } from '../services/accountService';
import PageMeta from '../components/common/PageMeta';
import PageBreadCrumb from '../components/common/PageBreadCrumb';
import Button from '../components/ui/button/Button';
import { TrashBinIcon } from '../icons';

export default function CartPage({ cart, loading, error, fetchCart, updateItem, removeItem, userId, user, isGuest }) {
  const navigate = useNavigate();
  const isUserLoggedIn = Boolean(user && (userId || user.id));
  const [selectedItemIds, setSelectedItemIds] = useState([]);
  const [recipientName, setRecipientName] = useState('');
  const [phone, setPhone] = useState('');
  const [deliveryAddress, setDeliveryAddress] = useState('');
  const [savedAddresses, setSavedAddresses] = useState([]);
  const [couponCode, setCouponCode] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('ONLINE'); // 'ONLINE' | 'COD'

  const [orderLoading, setOrderLoading] = useState(false);
  const [orderError, setOrderError] = useState(null);
  const [orderSuccess, setOrderSuccess] = useState(null);

  // Nếu đăng nhập thì tự động lấy thông tin từ Profile và Sổ địa chỉ
  useEffect(() => {
    if (user) {
      if (user.fullName) {
        setRecipientName(prev => prev || user.fullName);
      }
      if (user.phone) {
        setPhone(prev => prev || user.phone);
      }

      accountService.profile().then(p => {
        if (p) {
          if (p.fullName) setRecipientName(prev => prev || p.fullName);
          if (p.phone) setPhone(prev => prev || p.phone);
        }
      }).catch(() => {});

      if (user.role === 'CUSTOMER') {
        accountService.addresses().then(list => {
          if (list && list.length > 0) {
            setSavedAddresses(list);
            const def = list.find(a => a.isDefault) || list[0];
            const fullAddr = [def.addressLine, def.ward, def.district, def.city].filter(Boolean).join(', ');
            if (fullAddr) {
              setDeliveryAddress(prev => prev || fullAddr);
            }
          }
        }).catch(() => {});
      }
    }
  }, [user]);

  const handleRecipientNameChange = (val) => {
    setRecipientName(val);
  };

  const handlePhoneChange = (val) => {
    setPhone(val);
  };

  const handleDeliveryAddressChange = (val) => {
    setDeliveryAddress(val);
  };

  // Khi cart load xong, đồng bộ selectedItemIds
  useEffect(() => {
    if (cart && cart.items) {
      setSelectedItemIds(prev => {
        if (prev.length > 0) {
          const kept = prev.filter(id => cart.items.some(item => item.id === id));
          return kept.length > 0 ? kept : cart.items.map(item => item.id);
        }
        return cart.items.map(item => item.id);
      });
    } else {
      setSelectedItemIds([]);
    }
  }, [cart]);

  // Luôn làm mới cart khi vào trang
  useEffect(() => {
    fetchCart();
  }, [fetchCart]);

  const formatPrice = (amount) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);

  const items = cart?.items || [];
  const isEmpty = !loading && items.length === 0;

  // Xử lý chọn / bỏ chọn từng item
  const handleToggleItem = (itemId) => {
    setSelectedItemIds(prev =>
      prev.includes(itemId) ? prev.filter(id => id !== itemId) : [...prev, itemId]
    );
  };

  // Xử lý chọn / bỏ chọn tất cả
  const handleToggleSelectAll = () => {
    if (selectedItemIds.length === items.length) {
      setSelectedItemIds([]);
    } else {
      setSelectedItemIds(items.map(i => i.id));
    }
  };

  // Lấy danh sách các item đang được chọn
  const selectedItems = items.filter(item => selectedItemIds.includes(item.id));
  const totalQuantity = items.reduce((sum, item) => sum + (Number(item.quantity) || 0), 0);
  const selectedQuantity = selectedItems.reduce((sum, item) => sum + (Number(item.quantity) || 0), 0);
  const selectedTotal = selectedItems.reduce(
    (sum, item) => sum + Number(item.itemTotal || 0),
    0
  );

  // Group các item được chọn theo shopId (vì mỗi đơn hàng thuộc 1 shop)
  const selectedByShop = selectedItems.reduce((acc, item) => {
    const shopId = item.product?.shopId || 'unknown';
    if (!acc[shopId]) {
      acc[shopId] = {
        shopName: item.product?.shopName || 'Cửa hàng hoa',
        items: [],
      };
    }
    acc[shopId].items.push(item);
    return acc;
  }, {});

  const shopIds = Object.keys(selectedByShop);
  const hasMultipleShopsSelected = shopIds.length > 1;

  // Kiểm tra xem có sản phẩm nào được chọn mà số lượng vượt quá tồn kho không
  const hasOverStockItem = selectedItems.some(
    item => item.product?.stock != null && item.quantity > item.product.stock
  );

  // Xử lý thanh toán & đặt hàng
  const handlePlaceOrder = async () => {
    if (!isUserLoggedIn) {
      navigate('/login?next=/cart');
      return;
    }

    if (selectedItems.length === 0) {
      setOrderError('Vui lòng chọn ít nhất 1 sản phẩm để đặt hàng.');
      return;
    }

    if (!recipientName.trim()) {
      setOrderError('Vui lòng nhập họ và tên người nhận hoa.');
      return;
    }

    if (!phone.trim()) {
      setOrderError('Vui lòng nhập số điện thoại nhận hoa.');
      return;
    }

    const finalAddress = deliveryAddress.trim() || 'Hà Nội';

    if (hasMultipleShopsSelected) {
      setOrderError('Mỗi đơn hàng chỉ được chứa sản phẩm từ 1 cửa hàng. Vui lòng chỉ chọn sản phẩm thuộc cùng 1 shop.');
      return;
    }

    if (hasOverStockItem) {
      setOrderError('Có sản phẩm được chọn vượt quá số lượng trong kho. Vui lòng điều chỉnh lại số lượng.');
      return;
    }

    const targetShopId = shopIds[0];
    const targetItemIds = selectedByShop[targetShopId].items.map(i => i.id);

    setOrderLoading(true);
    setOrderError(null);
    try {
      // 1. Tạo đơn hàng
      const order = await orderApi.makeOrder(userId, {
        shopId: targetShopId,
        cartItemIds: targetItemIds,
        deliveryAddressId: finalAddress,
        recipientName: recipientName.trim(),
        phone: phone.trim(),
        couponId: couponCode.trim() || undefined,
        paymentMethod: paymentMethod,
      });

      // 2. Xử lý phương thức thanh toán
      if (paymentMethod === 'ONLINE') {
        const createRes = await paymentApi.createPayment({
          orderId: order.id,
          paymentType: 'FULL',
          userId: userId,
        });

        const checkoutUrl = createRes?.checkoutUrl || createRes?.data?.checkoutUrl;
        const fields = createRes?.fields || createRes?.data?.fields;

        if (!checkoutUrl || !fields) {
          throw new Error('Không nhận được thông tin cổng thanh toán từ máy chủ.');
        }

        await fetchCart();
        paymentApi.submitSePayCheckoutForm(checkoutUrl, fields);
        return;
      }

      // Luồng COD: đặt hàng thành công
      setOrderSuccess(order);
      await fetchCart();
    } catch (err) {
      setOrderError(err.message || 'Đặt hàng thất bại. Vui lòng thử lại.');
    } finally {
      setOrderLoading(false);
    }
  };

  return (
    <div className="p-4 sm:p-6 lg:p-8 w-full max-w-screen-2xl mx-auto">
      <PageMeta title="Giỏ hàng | Cửa hàng hoa" description="Xem và thanh toán giỏ hàng hoa tươi của bạn" />
      <PageBreadCrumb pageTitle={`Giỏ hàng (${totalQuantity})`} />

      {error && (
        <div className="mb-6 rounded-xl bg-error-50 dark:bg-error-500/10 p-4 border border-error-200 dark:border-error-500/20 text-error-600 dark:text-error-400">
          {error}
        </div>
      )}

      {orderSuccess && (
        <div className="mb-8 rounded-2xl border border-success-200 bg-success-50 p-6 dark:border-success-500/20 dark:bg-success-500/10 text-center">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-success-500 text-white text-2xl mb-3">
            ✓
          </div>
          <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">Đặt hàng COD thành công!</h2>
          <p className="text-gray-600 dark:text-gray-300 max-w-md mx-auto mb-4">
            Mã đơn hàng: <strong className="text-brand-600 dark:text-brand-400 font-mono">{orderSuccess.id}</strong>.
            Cửa hàng sẽ liên hệ và giao hàng tới bạn sớm nhất. Vui lòng chuẩn bị tiền mặt khi nhận hàng.
          </p>
          <div className="flex justify-center gap-4">
            <Button onClick={() => setOrderSuccess(null)} variant="outline">
              Xem lại giỏ hàng
            </Button>
            <Link to="/">
              <Button>Tiếp tục mua sắm</Button>
            </Link>
          </div>
        </div>
      )}

      {isEmpty ? (
        <div className="rounded-2xl border border-gray-200 bg-white p-12 text-center shadow-sm dark:border-white/5 dark:bg-white/3 max-w-xl mx-auto my-12">
          <div className="text-6xl mb-4">🌸</div>
          <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-2">Giỏ hàng của bạn đang trống</h2>
          <p className="text-gray-500 dark:text-gray-400 mb-6">
            Hãy dạo một vòng cửa hàng và chọn những bó hoa tươi đẹp nhất gửi tặng người thân yêu nhé!
          </p>
          <Link to="/">
            <Button className="px-8 py-3">
              ← Khám phá sản phẩm
            </Button>
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          {/* CỘT TRÁI: DANH SÁCH SẢN PHẨM TRONG GIỎ (8 cols) */}
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
                  checked={items.length > 0 && selectedItemIds.length === items.length}
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
                const isSelected = selectedItemIds.includes(item.id);
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

          {/* CỘT PHẢI: FORM THANH TOÁN & TÓM TẮT ĐƠN HÀNG (4 cols) */}
          <div className="lg:col-span-4 sticky top-24 space-y-6">
            <div className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm dark:border-white/5 dark:bg-white/3 space-y-6">
              <h3 className="text-lg font-bold text-gray-900 dark:text-white border-b border-gray-100 dark:border-white/5 pb-4">
                Tóm tắt đơn hàng
              </h3>

              {/* Họ và tên người nhận */}
              <div>
                <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-1.5">
                  Họ và tên người nhận <span className="text-error-500">*</span>
                </label>
                <input
                  type="text"
                  value={recipientName}
                  onChange={(e) => handleRecipientNameChange(e.target.value)}
                  placeholder="Nhập họ và tên người nhận hoa..."
                  className="w-full rounded-lg border border-gray-300 px-3.5 py-2.5 text-sm text-gray-900 placeholder:text-gray-400 focus:border-brand-500 focus:ring-1 focus:ring-brand-500 dark:border-white/10 dark:bg-gray-800 dark:text-white"
                />
              </div>

              {/* Số điện thoại nhận hàng */}
              <div>
                <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-1.5">
                  Số điện thoại nhận hàng <span className="text-error-500">*</span>
                </label>
                <input
                  type="tel"
                  value={phone}
                  onChange={(e) => handlePhoneChange(e.target.value)}
                  placeholder="Ví dụ: 0912345678"
                  maxLength={15}
                  className="w-full rounded-lg border border-gray-300 px-3.5 py-2.5 text-sm text-gray-900 placeholder:text-gray-400 focus:border-brand-500 focus:ring-1 focus:ring-brand-500 dark:border-white/10 dark:bg-gray-800 dark:text-white"
                />
              </div>

              {/* Địa chỉ nhận hoa */}
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300">
                    Địa chỉ nhận hoa <span className="text-error-500">*</span>
                  </label>
                  {savedAddresses.length > 0 && (
                    <select
                      onChange={(e) => {
                        if (e.target.value) handleDeliveryAddressChange(e.target.value);
                      }}
                      className="text-xs text-brand-600 dark:text-brand-400 bg-transparent border-none cursor-pointer focus:ring-0 p-0 font-medium"
                      defaultValue=""
                    >
                      <option value="" disabled>Chọn địa chỉ đã lưu</option>
                      {savedAddresses.map(addr => {
                        const str = [addr.addressLine, addr.ward, addr.district, addr.city].filter(Boolean).join(', ');
                        return (
                          <option key={addr.id} value={str}>
                            {addr.isDefault ? `★ [Mặc định] ${addr.addressLine}` : addr.addressLine}
                          </option>
                        );
                      })}
                    </select>
                  )}
                </div>
                <input
                  type="text"
                  value={deliveryAddress}
                  onChange={(e) => handleDeliveryAddressChange(e.target.value)}
                  placeholder="Nhập số nhà, tên đường, phường/xã, quận/huyện..."
                  className="w-full rounded-lg border border-gray-300 px-3.5 py-2.5 text-sm text-gray-900 placeholder:text-gray-400 focus:border-brand-500 focus:ring-1 focus:ring-brand-500 dark:border-white/10 dark:bg-gray-800 dark:text-white"
                />
                <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                  {isUserLoggedIn
                    ? '💡 Đã tự động điền từ thông tin tài khoản của bạn. Có thể chỉnh sửa nếu giao nơi khác.'
                    : '💡 Bạn có thể nhập trước thông tin nhận hàng, sau đó đăng nhập để xác nhận đặt hàng.'}
                </p>
              </div>

              {/* Phương thức thanh toán */}
              <div>
                <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">
                  Phương thức thanh toán <span className="text-error-500">*</span>
                </label>
                <div className="space-y-2.5">
                  <label
                    className={`flex items-start gap-3 p-3.5 rounded-xl border cursor-pointer transition-all ${
                      paymentMethod === 'ONLINE'
                        ? 'border-brand-500 bg-brand-50/50 ring-1 ring-brand-500/20 dark:bg-brand-500/10 dark:border-brand-500'
                        : 'border-gray-200 hover:border-gray-300 dark:border-white/10 dark:hover:border-white/20'
                    }`}
                  >
                    <input
                      type="radio"
                      name="paymentMethod"
                      value="ONLINE"
                      checked={paymentMethod === 'ONLINE'}
                      onChange={() => setPaymentMethod('ONLINE')}
                      className="mt-0.5 text-brand-600 focus:ring-brand-500"
                    />
                    <div>
                      <div className="text-sm font-semibold text-gray-900 dark:text-white">
                        🌐 Thanh toán online (Khuyên dùng)
                      </div>
                      <div className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">
                        Hỗ trợ VietQR, Thẻ Visa/Mastercard & ATM qua Cổng SePay
                      </div>
                    </div>
                  </label>

                  <label
                    className={`flex items-start gap-3 p-3.5 rounded-xl border cursor-pointer transition-all ${
                      paymentMethod === 'COD'
                        ? 'border-brand-500 bg-brand-50/50 ring-1 ring-brand-500/20 dark:bg-brand-500/10 dark:border-brand-500'
                        : 'border-gray-200 hover:border-gray-300 dark:border-white/10 dark:hover:border-white/20'
                    }`}
                  >
                    <input
                      type="radio"
                      name="paymentMethod"
                      value="COD"
                      checked={paymentMethod === 'COD'}
                      onChange={() => setPaymentMethod('COD')}
                      className="mt-0.5 text-brand-600 focus:ring-brand-500"
                    />
                    <div>
                      <div className="text-sm font-semibold text-gray-900 dark:text-white">
                        💵 Thanh toán khi nhận hàng (COD)
                      </div>
                      <div className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">
                        Nhận hoa và thanh toán tiền mặt trực tiếp cho người giao
                      </div>
                    </div>
                  </label>
                </div>
              </div>

              {/* Chi tiết tính tiền */}
              <div className="space-y-2.5 pt-4 border-t border-gray-100 dark:border-white/5 text-sm">
                <div className="flex justify-between text-gray-600 dark:text-gray-400">
                  <span>Số lượng chọn:</span>
                  <span className="font-semibold text-gray-900 dark:text-white">{selectedQuantity} sản phẩm</span>
                </div>
                <div className="flex justify-between text-gray-600 dark:text-gray-400">
                  <span>Tạm tính:</span>
                  <span className="font-semibold text-gray-900 dark:text-white">{formatPrice(selectedTotal)}</span>
                </div>
                <div className="flex justify-between text-gray-600 dark:text-gray-400">
                  <span>Phí vận chuyển:</span>
                  <span className="font-semibold text-success-600 dark:text-success-400">Miễn phí</span>
                </div>
                <div className="flex justify-between items-baseline pt-3 border-t border-gray-100 dark:border-white/5">
                  <span className="text-base font-bold text-gray-900 dark:text-white">Tổng thanh toán:</span>
                  <span className="text-2xl font-extrabold text-brand-600 dark:text-brand-400">
                    {formatPrice(selectedTotal)}
                  </span>
                </div>
              </div>

              {orderError && (
                <div className="rounded-lg bg-error-50 dark:bg-error-500/10 p-3 text-sm text-error-600 dark:text-error-400">
                  {orderError}
                </div>
              )}

              {/* Nút thanh toán hoặc đăng nhập */}
              {!isUserLoggedIn ? (
                <Button
                  type="button"
                  onClick={() => navigate('/login?next=/cart')}
                  disabled={selectedItems.length === 0}
                  className="w-full py-3.5 text-base font-bold shadow-md bg-brand-500 hover:bg-brand-600 text-white"
                >
                  Đăng nhập để thanh toán ({selectedQuantity}) →
                </Button>
              ) : (
                <Button
                  type="button"
                  onClick={handlePlaceOrder}
                  disabled={orderLoading || selectedItems.length === 0 || hasMultipleShopsSelected || hasOverStockItem}
                  className="w-full py-3.5 text-base font-bold shadow-md"
                >
                  {orderLoading
                    ? 'Đang kết nối cổng thanh toán...'
                    : hasOverStockItem
                    ? 'Có sản phẩm vượt quá tồn kho'
                    : paymentMethod === 'ONLINE'
                    ? `Thanh toán online (${selectedQuantity}) →`
                    : `Đặt hàng COD (${selectedQuantity})`}
                </Button>
              )}

              <div className="text-center text-xs text-gray-400 dark:text-gray-500">
                🔒 Thông tin thanh toán được mã hóa và bảo mật an toàn
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
