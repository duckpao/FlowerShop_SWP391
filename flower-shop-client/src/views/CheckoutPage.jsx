import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router';
import { useCartContext } from '../context/CartContext';
import { accountService } from '../services/accountService';
import orderService from '../services/orderService';
import paymentService from '../services/paymentService';
import PageMeta from '../components/common/PageMeta';
import PageBreadCrumb from '../components/common/PageBreadCrumb';
import Button from '../components/ui/button/Button';

/** Định dạng số tiền sang VND kiểu Việt Nam (vd 150.000 ₫); null/undefined coi như 0. */
const formatPrice = amount =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);

/**
 * Trang xác nhận đơn hàng (/checkout?item=<id>&item=<id>).
 * Luồng: lọc item đã chọn từ giỏ -> gom theo shop -> chọn địa chỉ -> tính phí ship GHN mỗi shop
 * -> handlePlaceOrder tạo đơn (mỗi shop 1 đơn) -> ONLINE sang SePay | COD sang trang thành công.
 * Chỉ CUSTOMER đã đăng nhập mới đặt được; số tiền cuối cùng do backend tính lại.
 */
export default function CheckoutPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  // Giữ nguyên các ?item=... để quay lại giỏ vẫn còn tick đúng sản phẩm đã chọn
  const returnToCart = `/cart?${searchParams.toString()}`;
  const { cart, loading, error, fetchCart, user, userId } = useCartContext();
  // Các cart item được chọn ở trang giỏ, truyền qua URL: /checkout?item=<id>&item=<id>
  const requestedIds = searchParams.getAll('item');
  const selectedIds = new Set(requestedIds);
  const selectedItems = (cart?.items || []).filter(item => selectedIds.has(item.id));
  // true nếu có item trong URL mà giỏ không còn (đã xóa/đã đặt ở tab khác)
  const missingItems = selectedItems.length !== selectedIds.size;
  // Mỗi shop là 1 đơn riêng nên gom sản phẩm đã chọn theo shopId
  const shopIds = [...new Set(selectedItems.map(item => item.product?.shopId))];
  const shopGroups = shopIds.map(shopId => {
    const items = selectedItems.filter(item => item.product?.shopId === shopId);
    return {
      shopId,
      shopName: items[0]?.product?.shopName || 'Cửa hàng',
      items,
      subtotal: items.reduce((sum, item) => sum + Number(item.itemTotal || 0), 0),
    };
  });
  const selectedQuantity = selectedItems.reduce((sum, item) => sum + Number(item.quantity || 0), 0);
  const selectedTotal = selectedItems.reduce((sum, item) => sum + Number(item.itemTotal || 0), 0);
  // Chặn đặt hàng nếu số lượng trong giỏ vượt tồn kho hiện tại
  const hasOverStockItem = selectedItems.some(item => item.product?.stock != null && item.quantity > item.product.stock);

  const [recipientName, setRecipientName] = useState('');
  const [phone, setPhone] = useState('');
  const [addresses, setAddresses] = useState([]);
  const [selectedAddressId, setSelectedAddressId] = useState('');
  const [addressesLoading, setAddressesLoading] = useState(Boolean(userId));
  const [addressError, setAddressError] = useState('');
  const [addressRefreshKey, setAddressRefreshKey] = useState(0);
  const [paymentMethod, setPaymentMethod] = useState('ONLINE');
  const [orderLoading, setOrderLoading] = useState(false);
  const [orderError, setOrderError] = useState('');

  // Luôn lấy giỏ mới nhất khi vào trang để giá/tồn kho không bị cũ
  useEffect(() => { fetchCart(); }, [fetchCart]);

  useEffect(() => {
    let active = true;
    setRecipientName(user?.fullName || '');
    setPhone(user?.phone || '');
    if (userId) {
      accountService.profile().then(profile => {
        if (!active || !profile) return;
        // Hồ sơ chỉ là giá trị dự phòng: không ghi đè tên/số điện thoại đã lấy từ địa chỉ nhận hoa.
        setRecipientName(prev => prev || profile.fullName || '');
        setPhone(prev => prev || profile.phone || '');
      }).catch(() => {});
    }
    return () => { active = false; };
  }, [userId]);

  // Người nhận và số điện thoại đi theo địa chỉ đang chọn (vẫn sửa tay được).
  useEffect(() => {
    const address = addresses.find(item => item.id === selectedAddressId);
    if (!address) return;
    if (address.recipientName) setRecipientName(address.recipientName);
    if (address.recipientPhone) setPhone(address.recipientPhone);
  }, [selectedAddressId, addresses]);

  // Tải địa chỉ đã lưu, mặc định chọn địa chỉ isDefault (hoặc địa chỉ đầu tiên). `active` chống set state sau khi unmount/đổi user
  useEffect(() => {
    let active = true;
    setAddresses([]);
    setSelectedAddressId('');
    setAddressError('');
    if (!userId || user?.role !== 'CUSTOMER') {
      setAddressesLoading(false);
      return () => { active = false; };
    }
    setAddressesLoading(true);
    accountService.addresses().then(list => {
      if (!active) return;
      const saved = Array.isArray(list) ? list : [];
      setAddresses(saved);
      setSelectedAddressId(saved.find(address => address.isDefault)?.id || saved[0]?.id || '');
    }).catch(() => {
      if (active) setAddressError('Không tải được địa chỉ. Vui lòng thử lại.');
    }).finally(() => {
      if (active) setAddressesLoading(false);
    });
    return () => { active = false; };
  }, [userId, user?.role, addressRefreshKey]);

  const [shippingFees, setShippingFees] = useState({});
  const [feeErrors, setFeeErrors] = useState({});
  const [feeLoading, setFeeLoading] = useState(false);
  // Chuỗi ổn định làm dependency của effect (mảng shopIds mỗi render là object mới)
  const shopKey = shopIds.join(',');

  // Tính phí ship GHN riêng cho từng shop (khoảng cách khác nhau); đổi địa chỉ/shop thì tính lại
  useEffect(() => {
    let active = true;
    setShippingFees({});
    setFeeErrors({});
    if (!userId || !selectedAddressId || !shopKey) return undefined;
    setFeeLoading(true);
    Promise.all(shopKey.split(',').map(shopId =>
      orderService.shippingFee(userId, shopId, selectedAddressId)
        .then(res => [shopId, Number(res.shippingFee), null])
        .catch(err => [shopId, null, err.message || 'Không tính được phí giao hàng.'])
    )).then(results => {
      if (!active) return;
      setShippingFees(Object.fromEntries(results.map(([id, fee]) => [id, fee])));
      setFeeErrors(Object.fromEntries(results.filter(r => r[2]).map(r => [r[0], r[2]])));
    }).finally(() => { if (active) setFeeLoading(false); });
    return () => { active = false; };
  }, [userId, shopKey, selectedAddressId]);

  // Chỉ cho đặt khi shop nào cũng đã có phí ship (tránh đặt với phí thiếu)
  const feesReady = shopGroups.length > 0 && shopGroups.every(group => shippingFees[group.shopId] != null);
  const totalShipping = shopGroups.reduce((sum, group) => sum + (shippingFees[group.shopId] || 0), 0);

  /**
   * Xử lý bấm "Đặt hàng":
   * 1. Validate (chống double-click, đủ tên/SĐT, địa chỉ hợp lệ, không vượt tồn kho).
   * 2. orderService.makeOrders: tạo đơn, mỗi shop 1 đơn (backend tự tính giá + ship, xóa item khỏi giỏ).
   * 3. fetchCart để cập nhật badge giỏ (ONLINE: chỉ khi không mở được cổng, vì sang SePay thì trang kết quả tự tải lại).
   * 4. ONLINE: 1 đơn -> startSePayCheckout, nhiều đơn -> startSePayGroupCheckout (1 giao dịch gộp);
   *    nếu mở cổng lỗi thì sang /payment/pending. COD: sang /payment/cod-success.
   */
  const handlePlaceOrder = async () => {
    // Chặn double-click và các trạng thái chưa hợp lệ (nút cũng bị disable, đây là lớp bảo vệ thứ hai)
    if (orderLoading || loading || feeLoading || !feesReady || !userId || requestedIds.length === 0 || missingItems || selectedItems.length === 0) return;
    if (shopIds.some(id => !id) || hasOverStockItem) return;
    if (!recipientName.trim() || !phone.trim()) {
      setOrderError('Vui lòng nhập tên và số điện thoại người nhận.');
      return;
    }
    

    setOrderLoading(true);
    setOrderError('');
    try {
      // Chỉ gửi id cart item + địa chỉ; giá/phí ship do backend tự tính lại, không tin số tiền từ client
      const created = await orderService.makeOrders(userId, {
        deliveryAddressId: selectedAddressId,
        recipientName: recipientName.trim(),
        phone: phone.trim(),
        paymentMethod,
        shops: shopGroups.map(group => ({
          shopId: group.shopId,
          cartItemIds: group.items.map(item => item.id),
        })),
      });
      const query = created.map(order => 'order=' + encodeURIComponent(order.id)).join('&');
      if (paymentMethod === 'ONLINE') {
        // Sang cổng SePay ngay. Nếu không mở được cổng thì đơn vẫn được giữ 10 phút ở màn chờ thanh toán.
        const orderIds = created.map(order => order.id);
        try {
          // 1 đơn: giao dịch đơn; nhiều đơn: gộp thành 1 giao dịch để khách chỉ thanh toán 1 lần
          if (orderIds.length === 1) await paymentService.startSePayCheckout(orderIds[0], 'FULL');
          else await paymentService.startSePayGroupCheckout(orderIds);
        } catch {
          await fetchCart();
          navigate('/payment/pending?' + query);
        }
        return;
      }
      await fetchCart(); // backend đã xóa các item vừa đặt khỏi giỏ -> cập nhật badge (ONLINE: trang success/cancel tự tải lại)
      navigate('/payment/cod-success?' + query);
    } catch (err) {
      setOrderError(err.message || 'Đặt hàng thất bại. Vui lòng thử lại.');
    } finally {
      setOrderLoading(false);
    }
  };

  return (
    <div className="mx-auto w-full max-w-6xl p-4 sm:p-6 lg:p-8">
      <PageMeta title="Xác nhận đơn hàng | Cửa hàng hoa" description="Kiểm tra thông tin giao hoa và thanh toán" />
      <PageBreadCrumb pageTitle="Xác nhận đơn hàng" />
      <Link to={returnToCart} className="mb-6 inline-flex text-sm font-medium text-brand-600 hover:underline dark:text-brand-400">← Quay lại giỏ hàng</Link>

      {!userId ? (
        <div className="rounded-2xl border border-gray-200 bg-white p-8 text-center dark:border-white/5 dark:bg-white/3">
          <p className="mb-4 text-gray-700 dark:text-gray-300">Vui lòng đăng nhập để xác nhận đơn hàng.</p>
          <Link to="/login?next=/cart"><Button>Đăng nhập</Button></Link>
        </div>
      ) : user?.role !== 'CUSTOMER' ? (
        <div role="alert" className="rounded-xl bg-warning-50 p-5 text-warning-800">Chỉ tài khoản khách hàng có thể đặt hàng. <Link to="/cart" className="font-semibold underline">Quay lại giỏ hàng</Link>.</div>
      ) : error ? (
        <div role="alert" className="rounded-xl bg-error-50 p-5 text-error-600">{error} <Link to="/cart" className="underline">Quay lại giỏ hàng</Link></div>
      ) : !cart ? (
        <p role="status" className="py-10 text-center text-gray-500">Đang tải giỏ hàng…</p>
      ) : requestedIds.length === 0 || missingItems || selectedItems.length === 0 ? (
        <div className="rounded-xl border border-warning-200 bg-warning-50 p-5 text-warning-800">
          Sản phẩm đã chọn không còn trong giỏ hàng. <Link to="/cart" className="font-semibold underline">Chọn lại sản phẩm</Link>.
        </div>
      ) : (
        <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(320px,0.85fr)] lg:items-start">
          <section className="space-y-6 rounded-2xl border border-gray-200 bg-white p-5 shadow-sm dark:border-white/5 dark:bg-white/3 sm:p-7">
            <div>
              <h1 className="text-xl font-bold text-gray-900 dark:text-white">Thông tin nhận hoa</h1>
              <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">Kiểm tra người nhận và chọn địa chỉ giao hàng.</p>
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label htmlFor="recipient-name" className="mb-1.5 block text-sm font-semibold text-gray-700 dark:text-gray-300">Họ và tên người nhận <span className="text-error-500">*</span></label>
                <input id="recipient-name" value={recipientName} onChange={e => setRecipientName(e.target.value)} maxLength={100} className="w-full rounded-lg border border-gray-300 px-3.5 py-2.5 text-sm text-gray-900 disabled:opacity-60 dark:border-white/10 dark:bg-gray-800 dark:text-white" />
              </div>
              <div>
                <label htmlFor="recipient-phone" className="mb-1.5 block text-sm font-semibold text-gray-700 dark:text-gray-300">Số điện thoại nhận hàng <span className="text-error-500">*</span></label>
                <input id="recipient-phone" type="tel" value={phone} onChange={e => setPhone(e.target.value)} maxLength={20} className="w-full rounded-lg border border-gray-300 px-3.5 py-2.5 text-sm text-gray-900 disabled:opacity-60 dark:border-white/10 dark:bg-gray-800 dark:text-white" />
              </div>
            </div>
            <div>
              <label htmlFor="delivery-address" className="mb-1.5 block text-sm font-semibold text-gray-700 dark:text-gray-300">Địa chỉ nhận hoa <span className="text-error-500">*</span></label>
              {addressesLoading ? <p role="status" className="text-sm text-gray-500">Đang tải địa chỉ…</p> : addressError ? (
                <div role="alert" className="text-sm text-error-600">{addressError} <button type="button" className="underline" onClick={() => setAddressRefreshKey(key => key + 1)}>Tải lại</button></div>
              ) : addresses.length ? (
                <select id="delivery-address" value={selectedAddressId} onChange={e => setSelectedAddressId(e.target.value)} className="w-full rounded-lg border border-gray-300 px-3.5 py-2.5 text-sm text-gray-900 disabled:opacity-60 dark:border-white/10 dark:bg-gray-800 dark:text-white">
                  {addresses.map(address => <option key={address.id} value={address.id}>{address.isDefault ? '★ Mặc định — ' : ''}{[address.addressLine, address.ward, address.district, address.city].filter(Boolean).join(', ')}</option>)}
                </select>
              ) : (
                <div className="rounded-xl border border-dashed border-gray-300 p-4 dark:border-gray-700">
                  <p className="mb-2 text-sm text-gray-500 dark:text-gray-400">Bạn chưa có địa chỉ nhận hoa.</p>
                  <Link to="/account" className="inline-flex rounded-lg bg-brand-500 px-3 py-2 text-sm font-semibold text-white hover:bg-brand-600">Thêm địa chỉ mới</Link>
                </div>
              )}
            </div>
            <div className="border-t border-gray-100 pt-5 dark:border-white/5">
              <h2 className="mb-3 text-base font-semibold text-gray-900 dark:text-white">Phương thức thanh toán</h2>
              <div className="grid gap-3 sm:grid-cols-2">
                {[
                  ['ONLINE', 'Thanh toán online', 'Qua cổng SePay'],
                  ['COD', 'Thanh toán khi nhận hàng', 'Trả tiền mặt khi nhận hoa'],
                ].map(([value, title, description]) => (
                  <label key={value} className={`flex cursor-pointer items-start gap-3 rounded-xl border p-4 ${paymentMethod === value ? 'border-brand-500 bg-brand-50/50 dark:bg-brand-500/10' : 'border-gray-200 dark:border-white/10'}`}>
                    <input type="radio" name="paymentMethod" checked={paymentMethod === value} onChange={() => setPaymentMethod(value)} className="mt-1 text-brand-600" />
                    <span><strong className="block text-sm text-gray-900 dark:text-white">{title}</strong><small className="mt-1 block text-xs text-gray-500 dark:text-gray-400">{description}</small></span>
                  </label>
                ))}
              </div>
            </div>
          </section>

          <aside className="space-y-5 rounded-2xl border border-gray-200 bg-white p-5 shadow-sm dark:border-white/5 dark:bg-white/3 sm:p-7 lg:sticky lg:top-24">
            <h2 className="border-b border-gray-100 pb-4 text-xl font-bold text-gray-900 dark:border-white/5 dark:text-white">Tóm tắt đơn hàng</h2>
            {shopGroups.map(group => (
              <div key={group.shopId} className="space-y-3 rounded-xl border border-gray-100 p-4 dark:border-white/5">
                <p className="text-sm font-bold text-gray-900 dark:text-white">🏪 {group.shopName}</p>
                <div className="space-y-3">
                  {group.items.map(item => (
                    <div key={item.id} className="flex items-start justify-between gap-3 text-sm">
                      <p className="min-w-0 font-medium text-gray-900 dark:text-white">{item.product?.name || 'Sản phẩm'} <span className="text-gray-500">× {item.quantity}</span></p>
                      <span className="shrink-0 font-semibold text-gray-900 dark:text-white">{formatPrice(item.itemTotal)}</span>
                    </div>
                  ))}
                </div>
                <div className="flex justify-between text-sm text-gray-600 dark:text-gray-400">
                  <span>Phí vận chuyển (GHN)</span>
                  <span>{feeLoading ? 'Đang tính…' : shippingFees[group.shopId] != null ? formatPrice(shippingFees[group.shopId]) : '—'}</span>
                </div>
                {feeErrors[group.shopId] && <p role="alert" className="rounded-lg bg-error-50 p-2 text-xs text-error-600">{feeErrors[group.shopId]}</p>}
              </div>
            ))}
            <div className="space-y-2 border-t border-gray-100 pt-4 text-sm dark:border-white/5">
              <div className="flex justify-between text-gray-600 dark:text-gray-400"><span>Số lượng</span><span>{selectedQuantity} sản phẩm</span></div>
              <div className="flex justify-between text-gray-600 dark:text-gray-400"><span>Tạm tính</span><span>{formatPrice(selectedTotal)}</span></div>
              <div className="flex justify-between text-gray-600 dark:text-gray-400"><span>Số đơn hàng</span><span>{shopGroups.length} đơn (tách theo shop)</span></div>
              <div className="flex justify-between text-gray-600 dark:text-gray-400"><span>Tổng phí vận chuyển</span><span>{feesReady ? formatPrice(totalShipping) : '—'}</span></div>
              <div className="flex justify-between border-t border-gray-100 pt-3 text-base font-bold text-gray-900 dark:border-white/5 dark:text-white"><span>Tổng tạm tính</span><span className="text-brand-600 dark:text-brand-400">{formatPrice(selectedTotal + totalShipping)}</span></div>
              <p className="text-xs text-gray-500 dark:text-gray-400">Số tiền cuối cùng được tính lại trên máy chủ khi đặt hàng.</p>
            </div>
            {hasOverStockItem && <p role="alert" className="rounded-lg bg-error-50 p-3 text-sm text-error-600">Có sản phẩm vượt quá tồn kho. Vui lòng điều chỉnh trong giỏ.</p>}
            {orderError && <p role="alert" className="rounded-lg bg-error-50 p-3 text-sm text-error-600">{orderError}</p>}
            <Button type="button" onClick={handlePlaceOrder} disabled={orderLoading || loading || addressesLoading || feeLoading || !feesReady || !selectedAddressId || hasOverStockItem} className="w-full justify-center py-3.5 text-base font-bold">
              {orderLoading ? 'Đang xử lý…' : paymentMethod === 'ONLINE' ? 'Thanh toán online →' : 'Xác nhận đặt hàng COD'}
            </Button>
            <p className="text-center text-xs text-gray-400 dark:text-gray-500">Thông tin thanh toán được bảo vệ an toàn</p>
          </aside>
        </div>
      )}
    </div>
  );
}
