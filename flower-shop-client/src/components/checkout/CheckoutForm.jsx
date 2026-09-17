import { useState } from 'react';
import orderApi from '../../api/orderApi';

/**
 * CheckoutForm — Modal xác nhận đặt hàng
 *
 * Props:
 *  - isOpen: boolean
 *  - onClose: () => void
 *  - userId: string
 *  - cart: CartResponse
 *  - onOrderSuccess: (order) => void
 */
export default function CheckoutForm({ isOpen, onClose, userId, cart, onOrderSuccess }) {
  const [deliveryAddressId, setDeliveryAddressId] = useState('');
  const [couponId, setCouponId] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [selectedItemIds, setSelectedItemIds] = useState([]);

  if (!isOpen || !cart) return null;

  // Tự động chọn tất cả items khi mở form
  const allItemIds = cart.items.map((i) => i.id);
  // Group items theo shopId để hiển thị
  const shopGroups = cart.items.reduce((acc, item) => {
    const shopId = item.product.shopId;
    if (!acc[shopId]) acc[shopId] = { shopName: item.product.shopName, items: [] };
    acc[shopId].items.push(item);
    return acc;
  }, {});

  const formatPrice = (amount) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);

  const handleSubmit = async (shopId) => {
    const itemIds = shopGroups[shopId].items.map((i) => i.id);

    if (!deliveryAddressId.trim()) {
      setError('Vui lòng nhập địa chỉ giao hàng ID.');
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const order = await orderApi.makeOrder(userId, {
        shopId,
        cartItemIds: itemIds,
        deliveryAddressId: deliveryAddressId.trim(),
        couponId: couponId.trim() || undefined,
      });
      onOrderSuccess(order);
      onClose();
    } catch (err) {
      setError(err.message || 'Không thể đặt hàng, vui lòng thử lại.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <div style={styles.overlay} onClick={onClose} />
      <div style={styles.modal}>
        <div style={styles.header}>
          <h2 style={styles.title}>📦 Xác nhận đặt hàng</h2>
          <button style={styles.closeBtn} onClick={onClose}>✕</button>
        </div>

        <div style={styles.body}>
          {error && <div style={styles.errorBox}>⚠️ {error}</div>}

          {/* Địa chỉ giao hàng */}
          <div style={styles.field}>
            <label style={styles.label}>ID Địa chỉ giao hàng *</label>
            <input
              style={styles.input}
              placeholder="Nhập deliveryAddressId..."
              value={deliveryAddressId}
              onChange={(e) => setDeliveryAddressId(e.target.value)}
            />
            <p style={styles.hint}>
              💡 Nhập ID địa chỉ đã lưu trong bảng Addresses (UUID).
              Khi có UI Address sẽ hiển thị dropdown thay thế.
            </p>
          </div>

          {/* Mã giảm giá */}
          <div style={styles.field}>
            <label style={styles.label}>Mã giảm giá (tuỳ chọn)</label>
            <input
              style={styles.input}
              placeholder="Nhập coupon code..."
              value={couponId}
              onChange={(e) => setCouponId(e.target.value)}
            />
          </div>

          {/* Danh sách đơn hàng theo shop */}
          {Object.entries(shopGroups).map(([shopId, group]) => {
            const groupTotal = group.items.reduce((sum, i) => sum + Number(i.itemTotal), 0);
            return (
              <div key={shopId} style={styles.shopGroup}>
                <p style={styles.shopName}>🏪 {group.shopName}</p>
                {group.items.map((item) => (
                  <div key={item.id} style={styles.orderItem}>
                    <span style={styles.itemName}>{item.product.name} × {item.quantity}</span>
                    <span style={styles.itemPrice}>{formatPrice(item.itemTotal)}</span>
                  </div>
                ))}
                <div style={styles.groupTotal}>
                  <span>Tổng shop</span>
                  <strong>{formatPrice(groupTotal)}</strong>
                </div>
                <button
                  style={styles.orderBtn}
                  onClick={() => handleSubmit(shopId)}
                  disabled={loading}
                >
                  {loading ? 'Đang đặt hàng...' : `Đặt hàng từ ${group.shopName}`}
                </button>
              </div>
            );
          })}
        </div>
      </div>
    </>
  );
}

const styles = {
  overlay: {
    position: 'fixed', inset: 0,
    background: 'rgba(0,0,0,0.5)', zIndex: 2000,
  },
  modal: {
    position: 'fixed',
    top: '50%', left: '50%',
    transform: 'translate(-50%, -50%)',
    width: 480, maxWidth: '95vw',
    maxHeight: '90vh',
    background: '#fff',
    borderRadius: 16,
    zIndex: 2001,
    display: 'flex',
    flexDirection: 'column',
    boxShadow: '0 20px 60px rgba(0,0,0,0.2)',
  },
  header: {
    display: 'flex', justifyContent: 'space-between', alignItems: 'center',
    padding: '20px 24px 16px', borderBottom: '1px solid #f0f0f0',
  },
  title: { margin: 0, fontSize: 18, fontWeight: 700 },
  closeBtn: {
    background: 'none', border: 'none', cursor: 'pointer', fontSize: 18, color: '#666',
  },
  body: { flex: 1, overflowY: 'auto', padding: '20px 24px 24px' },
  errorBox: {
    background: '#fff2f0', border: '1px solid #ffccc7',
    borderRadius: 8, padding: '10px 14px', marginBottom: 16,
    color: '#cf1322', fontSize: 13,
  },
  field: { marginBottom: 16 },
  label: { display: 'block', fontSize: 13, fontWeight: 600, marginBottom: 6, color: '#333' },
  input: {
    width: '100%', padding: '10px 12px',
    border: '1px solid #ddd', borderRadius: 8,
    fontSize: 14, outline: 'none', boxSizing: 'border-box',
  },
  hint: { fontSize: 11, color: '#aaa', marginTop: 4 },
  shopGroup: {
    border: '1px solid #f0f0f0', borderRadius: 10,
    padding: '14px 16px', marginTop: 16,
  },
  shopName: { margin: '0 0 10px', fontWeight: 600, fontSize: 14, color: '#333' },
  orderItem: {
    display: 'flex', justifyContent: 'space-between',
    fontSize: 13, color: '#555', marginBottom: 6,
  },
  itemName: {},
  itemPrice: { fontWeight: 600 },
  groupTotal: {
    display: 'flex', justifyContent: 'space-between',
    borderTop: '1px solid #f0f0f0', paddingTop: 10, marginTop: 6,
    fontSize: 14,
  },
  orderBtn: {
    width: '100%', marginTop: 12, padding: '12px 0',
    background: '#e8604c', color: '#fff', border: 'none',
    borderRadius: 8, fontSize: 14, fontWeight: 700, cursor: 'pointer',
  },
};

