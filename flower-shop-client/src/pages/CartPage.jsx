import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import orderApi from '../api/orderApi';
import paymentApi from '../api/paymentApi';
import PaymentModal from '../components/payment/PaymentModal';

export default function CartPage({ cart, loading, error, fetchCart, updateItem, removeItem, userId }) {
  const [selectedItemIds, setSelectedItemIds] = useState([]);
  const [deliveryAddressId, setDeliveryAddressId] = useState('addr-customer-01');
  const [couponCode, setCouponCode] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('SEPAY_GATEWAY'); // 'SEPAY_GATEWAY' | 'VIETQR_DIRECT' | 'COD'
  
  const [orderLoading, setOrderLoading] = useState(false);
  const [orderError, setOrderError] = useState(null);
  const [orderSuccess, setOrderSuccess] = useState(null);

  // State cho Modal VietQR nhúng tại trang
  const [isQrModalOpen, setIsQrModalOpen] = useState(false);
  const [qrOrderId, setQrOrderId] = useState(null);

  // Khi cart load xong, đồng bộ selectedItemIds (giữ các item đã tick trước đó)
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

  // Tính tổng tiền các item được chọn
  const selectedTotal = selectedItems.reduce(
    (sum, item) => sum + Number(item.itemTotal || 0),
    0
  );

  // Group các item được chọn theo shopId (vì mỗi đơn hàng thuộc 1 shop)
  const selectedByShop = selectedItems.reduce((acc, item) => {
    const shopId = item.product.shopId;
    if (!acc[shopId]) {
      acc[shopId] = {
        shopName: item.product.shopName,
        items: [],
      };
    }
    acc[shopId].items.push(item);
    return acc;
  }, {});

  const shopIds = Object.keys(selectedByShop);

  // Xử lý thanh toán & đặt hàng
  const handlePlaceOrder = async () => {
    if (selectedItems.length === 0) {
      setOrderError('Vui lòng chọn ít nhất 1 sản phẩm để đặt hàng.');
      return;
    }

    if (!deliveryAddressId.trim()) {
      setOrderError('Vui lòng nhập mã địa chỉ nhận hàng.');
      return;
    }

    if (shopIds.length > 1) {
      setOrderError('Hiện tại chỉ hỗ trợ thanh toán sản phẩm của cùng 1 cửa hàng trong mỗi đơn. Vui lòng chỉ chọn sản phẩm của một shop.');
      return;
    }

    const targetShopId = shopIds[0];
    const targetItemIds = selectedByShop[targetShopId].items.map(i => i.id);

    setOrderLoading(true);
    setOrderError(null);
    try {
      // 1. Tạo đơn hàng phía Backend
      const order = await orderApi.makeOrder(userId, {
        shopId: targetShopId,
        cartItemIds: targetItemIds,
        deliveryAddressId: deliveryAddressId.trim(),
        couponId: couponCode.trim() || undefined,
      });

      // 2. Xử lý theo phương thức thanh toán đã chọn
      if (paymentMethod === 'SEPAY_GATEWAY') {
        // Luồng Cổng SePay Gateway: Khởi tạo phiên thanh toán bảo mật từ Spring Boot
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

        // Cập nhật lại giỏ hàng ngay vì các sản phẩm đã được đưa vào đơn hàng
        await fetchCart();

        // Submit form POST sang Cổng SePay Checkout theo chuẩn tài liệu chính thức
        paymentApi.submitSePayCheckoutForm(checkoutUrl, fields);
        return;
      }

      if (paymentMethod === 'VIETQR_DIRECT') {
        // Luồng VietQR trực tiếp: Mở modal quét mã ngay tại web
        setQrOrderId(order.id);
        setIsQrModalOpen(true);
        await fetchCart();
        return;
      }

      // Luồng COD: Đặt hàng thành công thông thường
      setOrderSuccess(order);
      await fetchCart();
    } catch (err) {
      setOrderError(err.message || 'Đặt hàng thất bại. Vui lòng thử lại.');
    } finally {
      setOrderLoading(false);
    }
  };

  return (
    <div style={styles.page}>
      {/* Breadcrumbs */}
      <div style={styles.breadcrumb}>
        <Link to="/" style={styles.breadcrumbLink}>Trang chủ</Link>
        <span style={styles.breadcrumbSep}>›</span>
        <span style={styles.breadcrumbCurrent}>Giỏ hàng ({items.length})</span>
      </div>

      <h1 style={styles.pageTitle}>🛒 Giỏ hàng của bạn</h1>

      {/* Thông báo đặt hàng thành công (COD) */}
      {orderSuccess && (
        <div style={styles.successCard}>
          <div style={styles.successIcon}>🎉</div>
          <div>
            <h3 style={styles.successTitle}>Đặt hàng thành công!</h3>
            <p style={styles.successDesc}>
              Mã đơn hàng: <strong>{orderSuccess.id}</strong> | Tổng thanh toán: <strong>{formatPrice(orderSuccess.totalAmount)}</strong>
            </p>
            <p style={{ fontSize: 13, color: '#2b8a3e', marginTop: 4 }}>
              Phương thức: Thanh toán khi nhận hàng (COD) • Trạng thái: <strong>{orderSuccess.status}</strong>
            </p>
          </div>
          <button style={styles.successCloseBtn} onClick={() => setOrderSuccess(null)}>
            ✕
          </button>
        </div>
      )}

      {/* Thông báo lỗi nếu có */}
      {(error || orderError) && (
        <div style={styles.errorBanner}>
          <span>⚠️ {error || orderError}</span>
          <button style={styles.errorCloseBtn} onClick={() => setOrderError(null)}>✕</button>
        </div>
      )}

      {/* Trạng thái giỏ hàng trống */}
      {isEmpty && (
        <div style={styles.emptyContainer}>
          <div style={styles.emptyEmoji}>🌸</div>
          <h2 style={styles.emptyTitle}>Giỏ hàng của bạn đang trống</h2>
          <p style={styles.emptyDesc}>Hãy dạo một vòng cửa hàng và chọn những bó hoa tươi đẹp nhất nhé!</p>
          <Link to="/" style={styles.continueBtn}>
            ← Tiếp tục chọn hoa
          </Link>
        </div>
      )}

      {/* Bảng sản phẩm & Cột thanh toán */}
      {!isEmpty && (
        <div style={styles.layout}>
          {/* CỘT TRÁI: DANH SÁCH SẢN PHẨM */}
          <div style={styles.cartContent}>
            {/* Header bảng */}
            <div style={styles.tableHeader}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12, flex: 3 }}>
                <input
                  type="checkbox"
                  style={styles.checkbox}
                  checked={items.length > 0 && selectedItemIds.length === items.length}
                  onChange={handleToggleSelectAll}
                />
                <span style={styles.headerCol}>Tất cả ({items.length} sản phẩm)</span>
              </div>
              <span style={{ ...styles.headerCol, flex: 1.5, textAlign: 'center' }}>Đơn giá</span>
              <span style={{ ...styles.headerCol, flex: 1.5, textAlign: 'center' }}>Số lượng</span>
              <span style={{ ...styles.headerCol, flex: 1.8, textAlign: 'right' }}>Số tiền</span>
              <span style={{ ...styles.headerCol, width: 60, textAlign: 'center' }}>Xóa</span>
            </div>

            {/* Danh sách items */}
            <div style={styles.itemList}>
              {items.map((item) => {
                const isChecked = selectedItemIds.includes(item.id);
                const product = item.product;
                return (
                  <div
                    key={item.id}
                    style={{
                      ...styles.itemRow,
                      backgroundColor: isChecked ? '#fff' : '#fafafa',
                    }}
                  >
                    {/* Checkbox + Info */}
                    <div style={{ display: 'flex', alignItems: 'center', gap: 14, flex: 3 }}>
                      <input
                        type="checkbox"
                        style={styles.checkbox}
                        checked={isChecked}
                        onChange={() => handleToggleItem(item.id)}
                      />
                      <div style={styles.itemImgBox}>
                        <span style={{ fontSize: 32 }}>🌹</span>
                      </div>
                      <div style={{ minWidth: 0 }}>
                        <div style={styles.shopBadge}>🏪 {product.shopName}</div>
                        <h4 style={styles.itemName}>{product.name}</h4>
                        <span style={styles.stockNote}>Còn {product.stock} sản phẩm</span>
                      </div>
                    </div>

                    {/* Đơn giá */}
                    <div style={{ flex: 1.5, textAlign: 'center', color: '#555', fontWeight: 500 }}>
                      {formatPrice(product.price)}
                    </div>

                    {/* Bộ điều khiển số lượng */}
                    <div style={{ flex: 1.5, display: 'flex', justifyContent: 'center' }}>
                      <div style={styles.qtyBox}>
                        <button
                          style={styles.qtyBtn}
                          onClick={() => {
                            if (item.quantity > 1) updateItem(item.id, item.quantity - 1);
                          }}
                          disabled={item.quantity <= 1 || loading}
                        >
                          −
                        </button>
                        <span style={styles.qtyVal}>{item.quantity}</span>
                        <button
                          style={styles.qtyBtn}
                          onClick={() => updateItem(item.id, item.quantity + 1)}
                          disabled={item.quantity >= product.stock || loading}
                        >
                          +
                        </button>
                      </div>
                    </div>

                    {/* Thành tiền */}
                    <div style={{ flex: 1.8, textAlign: 'right', fontWeight: 700, color: '#e8604c', fontSize: 16 }}>
                      {formatPrice(item.itemTotal)}
                    </div>

                    {/* Nút xóa */}
                    <div style={{ width: 60, textAlign: 'center' }}>
                      <button
                        style={styles.deleteBtn}
                        onClick={() => removeItem(item.id)}
                        title="Xóa khỏi giỏ"
                        disabled={loading}
                      >
                        🗑️
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>

            <div style={styles.cartActions}>
              <Link to="/" style={styles.backLink}>
                ← Tiếp tục mua thêm hoa
              </Link>
            </div>
          </div>

          {/* CỘT PHẢI: THÔNG TIN THANH TOÁN (ORDER SUMMARY) */}
          <div style={styles.summaryCol}>
            <div style={styles.summaryCard}>
              <h3 style={styles.summaryTitle}>Thông tin đơn hàng</h3>

              {/* Nhập địa chỉ nhận hàng */}
              <div style={styles.formGroup}>
                <label style={styles.label}>
                  Địa chỉ giao hàng <span style={{ color: '#e8604c' }}>*</span>
                </label>
                <input
                  type="text"
                  style={styles.input}
                  value={deliveryAddressId}
                  onChange={(e) => setDeliveryAddressId(e.target.value)}
                  placeholder="Nhập ID địa chỉ..."
                />
                <span style={styles.inputNote}>💡 Mặc định: <code>addr-customer-01</code></span>
              </div>

              {/* LỰA CHỌN PHƯƠNG THỨC THANH TOÁN */}
              <div style={styles.formGroup}>
                <label style={styles.label}>Phương thức thanh toán <span style={{ color: '#e8604c' }}>*</span></label>
                
                {/* 1. Cổng SePay Gateway */}
                <label
                  style={{
                    ...styles.paymentOption,
                    borderColor: paymentMethod === 'SEPAY_GATEWAY' ? '#e8604c' : '#eee',
                    backgroundColor: paymentMethod === 'SEPAY_GATEWAY' ? '#fff8f6' : '#fff',
                  }}
                >
                  <input
                    type="radio"
                    name="paymentMethod"
                    value="SEPAY_GATEWAY"
                    checked={paymentMethod === 'SEPAY_GATEWAY'}
                    onChange={() => setPaymentMethod('SEPAY_GATEWAY')}
                    style={styles.radio}
                  />
                  <div>
                    <div style={styles.optTitle}>🌐 Cổng thanh toán SePay (Khuyên dùng)</div>
                    <div style={styles.optDesc}>Hỗ trợ VietQR, Thẻ Visa/Mastercard & NAPAS</div>
                  </div>
                </label>

                {/* 2. Quét VietQR trực tiếp tại web */}
                <label
                  style={{
                    ...styles.paymentOption,
                    borderColor: paymentMethod === 'VIETQR_DIRECT' ? '#e8604c' : '#eee',
                    backgroundColor: paymentMethod === 'VIETQR_DIRECT' ? '#fff8f6' : '#fff',
                  }}
                >
                  <input
                    type="radio"
                    name="paymentMethod"
                    value="VIETQR_DIRECT"
                    checked={paymentMethod === 'VIETQR_DIRECT'}
                    onChange={() => setPaymentMethod('VIETQR_DIRECT')}
                    style={styles.radio}
                  />
                  <div>
                    <div style={styles.optTitle}>📲 Quét mã VietQR trực tiếp</div>
                    <div style={styles.optDesc}>Quét mã QR chuyển khoản ngay trên trang này</div>
                  </div>
                </label>

                {/* 3. COD */}
                <label
                  style={{
                    ...styles.paymentOption,
                    borderColor: paymentMethod === 'COD' ? '#e8604c' : '#eee',
                    backgroundColor: paymentMethod === 'COD' ? '#fff8f6' : '#fff',
                  }}
                >
                  <input
                    type="radio"
                    name="paymentMethod"
                    value="COD"
                    checked={paymentMethod === 'COD'}
                    onChange={() => setPaymentMethod('COD')}
                    style={styles.radio}
                  />
                  <div>
                    <div style={styles.optTitle}>💵 Thanh toán khi nhận hàng (COD)</div>
                    <div style={styles.optDesc}>Thanh toán tiền mặt cho shipper</div>
                  </div>
                </label>
              </div>

              {/* Nhập voucher */}
              <div style={styles.formGroup}>
                <label style={styles.label}>Mã giảm giá / Voucher</label>
                <div style={{ display: 'flex', gap: 8 }}>
                  <input
                    type="text"
                    style={{ ...styles.input, flex: 1 }}
                    value={couponCode}
                    onChange={(e) => setCouponCode(e.target.value)}
                    placeholder="Nhập mã coupon..."
                  />
                  <button style={styles.applyBtn} type="button">Áp dụng</button>
                </div>
              </div>

              <div style={styles.divider} />

              {/* Chi tiết tính tiền */}
              <div style={styles.priceRow}>
                <span style={{ color: '#666' }}>Đã chọn:</span>
                <span style={{ fontWeight: 600 }}>{selectedItems.length} sản phẩm</span>
              </div>

              <div style={styles.priceRow}>
                <span style={{ color: '#666' }}>Tạm tính:</span>
                <span style={{ fontWeight: 600 }}>{formatPrice(selectedTotal)}</span>
              </div>

              <div style={styles.priceRow}>
                <span style={{ color: '#666' }}>Phí vận chuyển:</span>
                <span style={{ color: '#2b8a3e', fontWeight: 600 }}>Miễn phí</span>
              </div>

              <div style={styles.divider} />

              <div style={styles.totalRow}>
                <span style={{ fontSize: 16, fontWeight: 700, color: '#222' }}>Tổng thanh toán:</span>
                <span style={styles.totalPrice}>{formatPrice(selectedTotal)}</span>
              </div>

              {/* Nút Đặt hàng / Thanh toán */}
              <button
                style={{
                  ...styles.checkoutBtn,
                  opacity: selectedItems.length === 0 || orderLoading ? 0.6 : 1,
                  cursor: selectedItems.length === 0 || orderLoading ? 'not-allowed' : 'pointer',
                }}
                onClick={handlePlaceOrder}
                disabled={selectedItems.length === 0 || orderLoading}
              >
                {orderLoading
                  ? 'Đang kết nối cổng thanh toán...'
                  : paymentMethod === 'SEPAY_GATEWAY'
                  ? `Thanh toán SePay (${selectedItems.length}) →`
                  : paymentMethod === 'VIETQR_DIRECT'
                  ? `Lấy mã VietQR (${selectedItems.length}) ➔`
                  : `Đặt hàng COD (${selectedItems.length})`}
              </button>

              <div style={styles.securityNote}>
                🔒 Bảo mật SSL & xác thực giao dịch SePay 3D Secure
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Modal quét VietQR trực tiếp tại web */}
      <PaymentModal
        isOpen={isQrModalOpen}
        onClose={() => {
          setIsQrModalOpen(false);
          setQrOrderId(null);
        }}
        orderId={qrOrderId}
        onPaymentSuccess={() => {
          fetchCart();
        }}
      />
    </div>
  );
}

const styles = {
  page: {
    maxWidth: 1200,
    margin: '0 auto',
    padding: '24px 16px 60px',
    fontFamily: 'system-ui, -apple-system, sans-serif',
  },
  breadcrumb: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    fontSize: 14,
    color: '#888',
    marginBottom: 20,
  },
  breadcrumbLink: {
    color: '#666',
    textDecoration: 'none',
  },
  breadcrumbSep: { color: '#ccc' },
  breadcrumbCurrent: { color: '#e8604c', fontWeight: 600 },
  pageTitle: {
    fontSize: 26,
    fontWeight: 800,
    color: '#1a1a1a',
    marginBottom: 24,
  },
  successCard: {
    display: 'flex',
    alignItems: 'flex-start',
    gap: 16,
    background: '#ebfbee',
    border: '1px solid #b2f2bb',
    borderRadius: 12,
    padding: '16px 20px',
    marginBottom: 24,
    position: 'relative',
  },
  successIcon: { fontSize: 28 },
  successTitle: { margin: 0, color: '#2b8a3e', fontSize: 16, fontWeight: 700 },
  successDesc: { margin: '4px 0 0', color: '#2f9e44', fontSize: 14 },
  successCloseBtn: {
    position: 'absolute',
    top: 14,
    right: 14,
    background: 'none',
    border: 'none',
    cursor: 'pointer',
    fontSize: 16,
    color: '#2b8a3e',
  },
  errorBanner: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    background: '#fff5f5',
    border: '1px solid #ffc9c9',
    borderRadius: 10,
    padding: '12px 18px',
    marginBottom: 20,
    color: '#e03131',
    fontSize: 14,
    fontWeight: 500,
  },
  errorCloseBtn: {
    background: 'none',
    border: 'none',
    cursor: 'pointer',
    color: '#e03131',
    fontSize: 16,
  },
  emptyContainer: {
    background: '#fff',
    borderRadius: 16,
    padding: '60px 20px',
    textAlign: 'center',
    border: '1px solid #eee',
    boxShadow: '0 2px 12px rgba(0,0,0,0.03)',
  },
  emptyEmoji: { fontSize: 64, marginBottom: 16 },
  emptyTitle: { fontSize: 22, fontWeight: 700, color: '#333', marginBottom: 8 },
  emptyDesc: { color: '#888', fontSize: 15, marginBottom: 24 },
  continueBtn: {
    display: 'inline-block',
    padding: '12px 28px',
    background: '#e8604c',
    color: '#fff',
    textDecoration: 'none',
    borderRadius: 10,
    fontWeight: 700,
    fontSize: 15,
  },
  layout: {
    display: 'flex',
    gap: 24,
    alignItems: 'flex-start',
  },
  cartContent: {
    flex: '1 1 62%',
    background: '#fff',
    borderRadius: 16,
    border: '1px solid #eee',
    overflow: 'hidden',
    boxShadow: '0 2px 12px rgba(0,0,0,0.03)',
  },
  tableHeader: {
    display: 'flex',
    alignItems: 'center',
    padding: '16px 20px',
    background: '#f8f9fa',
    borderBottom: '1px solid #eee',
  },
  headerCol: {
    fontSize: 14,
    fontWeight: 700,
    color: '#666',
  },
  checkbox: {
    width: 18,
    height: 18,
    accentColor: '#e8604c',
    cursor: 'pointer',
  },
  itemList: {
    display: 'flex',
    flexDirection: 'column',
  },
  itemRow: {
    display: 'flex',
    alignItems: 'center',
    padding: '20px',
    borderBottom: '1px solid #f2f2f2',
    transition: 'background-color 0.2s',
  },
  itemImgBox: {
    width: 70,
    height: 70,
    borderRadius: 10,
    background: '#fff4f2',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
    border: '1px solid #fde4df',
  },
  shopBadge: {
    fontSize: 11,
    color: '#888',
    marginBottom: 4,
    fontWeight: 600,
  },
  itemName: {
    margin: '0 0 6px',
    fontSize: 15,
    fontWeight: 700,
    color: '#222',
    lineHeight: 1.4,
  },
  stockNote: {
    fontSize: 12,
    color: '#999',
  },
  qtyBox: {
    display: 'flex',
    alignItems: 'center',
    border: '1px solid #ddd',
    borderRadius: 8,
    overflow: 'hidden',
    background: '#fff',
  },
  qtyBtn: {
    width: 32,
    height: 32,
    border: 'none',
    background: '#f8f9fa',
    cursor: 'pointer',
    fontSize: 16,
    fontWeight: 700,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    color: '#444',
  },
  qtyVal: {
    minWidth: 36,
    textAlign: 'center',
    fontSize: 14,
    fontWeight: 700,
  },
  deleteBtn: {
    background: 'none',
    border: 'none',
    cursor: 'pointer',
    fontSize: 18,
    opacity: 0.7,
    padding: 6,
    transition: 'opacity 0.2s',
  },
  cartActions: {
    padding: '16px 20px',
    background: '#fafafa',
  },
  backLink: {
    color: '#666',
    textDecoration: 'none',
    fontSize: 14,
    fontWeight: 600,
  },
  summaryCol: {
    flex: '1 1 38%',
    position: 'sticky',
    top: 24,
  },
  summaryCard: {
    background: '#fff',
    borderRadius: 16,
    padding: '24px',
    border: '1px solid #eee',
    boxShadow: '0 4px 20px rgba(0,0,0,0.05)',
  },
  summaryTitle: {
    margin: '0 0 20px',
    fontSize: 18,
    fontWeight: 800,
    color: '#1a1a1a',
  },
  formGroup: {
    marginBottom: 18,
  },
  label: {
    display: 'block',
    fontSize: 13,
    fontWeight: 600,
    color: '#444',
    marginBottom: 8,
  },
  paymentOption: {
    display: 'flex',
    alignItems: 'flex-start',
    gap: 12,
    padding: '12px 14px',
    borderRadius: 10,
    border: '1.5px solid #eee',
    marginBottom: 10,
    cursor: 'pointer',
    transition: 'all 0.2s',
  },
  radio: {
    marginTop: 3,
    accentColor: '#e8604c',
    cursor: 'pointer',
  },
  optTitle: {
    fontSize: 14,
    fontWeight: 700,
    color: '#222',
  },
  optDesc: {
    fontSize: 12,
    color: '#777',
    marginTop: 2,
  },
  input: {
    width: '100%',
    padding: '10px 14px',
    border: '1px solid #ddd',
    borderRadius: 8,
    fontSize: 14,
    outline: 'none',
    boxSizing: 'border-box',
    fontFamily: 'inherit',
  },
  inputNote: {
    display: 'block',
    fontSize: 11,
    color: '#888',
    marginTop: 4,
  },
  applyBtn: {
    padding: '0 16px',
    background: '#333',
    color: '#fff',
    border: 'none',
    borderRadius: 8,
    cursor: 'pointer',
    fontWeight: 600,
    fontSize: 13,
  },
  divider: {
    height: 1,
    background: '#eee',
    margin: '18px 0',
  },
  priceRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 10,
    fontSize: 14,
  },
  totalRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 20,
  },
  totalPrice: {
    fontSize: 22,
    fontWeight: 800,
    color: '#e8604c',
  },
  checkoutBtn: {
    width: '100%',
    padding: '14px 0',
    background: '#e8604c',
    color: '#fff',
    border: 'none',
    borderRadius: 10,
    fontSize: 15,
    fontWeight: 800,
    letterSpacing: 0.3,
    transition: 'background 0.2s',
  },
  securityNote: {
    textAlign: 'center',
    fontSize: 12,
    color: '#999',
    marginTop: 14,
  },
};
