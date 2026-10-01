import { useEffect } from 'react';
import CartItemRow from './CartItem';

/**
 * CartDrawer — Drawer trượt từ phải hiển thị toàn bộ giỏ hàng
 *
 * Props:
 *  - isOpen: boolean
 *  - onClose: () => void
 *  - cart: CartResponse | null
 *  - loading: boolean
 *  - error: string | null
 *  - onUpdate: (itemId, quantity) => void
 *  - onRemove: (itemId) => void
 *  - onCheckout: () => void  — mở form đặt hàng
 */
export default function CartDrawer({
  isOpen,
  onClose,
  cart,
  loading,
  error,
  onUpdate,
  onRemove,
  onCheckout,
}) {
  // Đóng drawer khi bấm Escape
  useEffect(() => {
    const handleKey = (e) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [onClose]);

  const formatPrice = (amount) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);

  const isEmpty = !cart || cart.items.length === 0;

  return (
    <>
      {/* Overlay backdrop */}
      {isOpen && (
        <div style={styles.overlay} onClick={onClose} />
      )}

      {/* Drawer panel */}
      <div style={{ ...styles.drawer, transform: isOpen ? 'translateX(0)' : 'translateX(100%)' }}>
        {/* Header */}
        <div style={styles.header}>
          <h2 style={styles.title}>
            🛒 Giỏ hàng
            {cart && cart.totalQuantity > 0 && (
              <span style={styles.badge}>{cart.totalQuantity}</span>
            )}
          </h2>
          <button style={styles.closeBtn} onClick={onClose}>✕</button>
        </div>

        {/* Body */}
        <div style={styles.body}>
          {loading && (
            <div style={styles.center}>
              <p>Đang tải...</p>
            </div>
          )}

          {error && (
            <div style={styles.errorBox}>⚠️ {error}</div>
          )}

          {!loading && isEmpty && (
            <div style={styles.center}>
              <p style={{ fontSize: 40 }}>🌸</p>
              <p style={{ color: '#999' }}>Giỏ hàng trống</p>
              <p style={{ fontSize: 13, color: '#bbb' }}>Hãy thêm sản phẩm vào giỏ nhé!</p>
            </div>
          )}

          {!loading && !isEmpty && cart.items.map((item) => (
            <CartItemRow
              key={item.id}
              item={item}
              onUpdate={onUpdate}
              onRemove={onRemove}
            />
          ))}
        </div>

        {/* Footer — Tổng tiền + nút đặt hàng */}
        {!isEmpty && (
          <div style={styles.footer}>
            <div style={styles.totalRow}>
              <span style={styles.totalLabel}>Tổng cộng</span>
              <span style={styles.totalValue}>
                {cart ? formatPrice(cart.grandTotal) : '—'}
              </span>
            </div>
            <button style={styles.checkoutBtn} onClick={onCheckout} disabled={loading}>
              Đặt hàng ngay →
            </button>
          </div>
        )}
      </div>
    </>
  );
}

const styles = {
  overlay: {
    position: 'fixed',
    inset: 0,
    background: 'rgba(0,0,0,0.4)',
    zIndex: 1000,
  },
  drawer: {
    position: 'fixed',
    top: 0,
    right: 0,
    height: '100vh',
    width: 420,
    maxWidth: '95vw',
    background: '#fff',
    zIndex: 1001,
    boxShadow: '-4px 0 24px rgba(0,0,0,0.15)',
    display: 'flex',
    flexDirection: 'column',
    transition: 'transform 0.3s ease',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '20px 24px 16px',
    borderBottom: '1px solid #f0f0f0',
  },
  title: { margin: 0, fontSize: 18, fontWeight: 700, display: 'flex', alignItems: 'center', gap: 8 },
  badge: {
    background: '#e8604c',
    color: '#fff',
    borderRadius: 12,
    padding: '2px 8px',
    fontSize: 13,
    fontWeight: 600,
  },
  closeBtn: {
    background: 'none',
    border: 'none',
    cursor: 'pointer',
    fontSize: 18,
    color: '#666',
    padding: 4,
  },
  body: {
    flex: 1,
    overflowY: 'auto',
    padding: '0 24px',
  },
  center: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    height: 300,
  },
  errorBox: {
    background: '#fff2f0',
    border: '1px solid #ffccc7',
    borderRadius: 8,
    padding: '12px 16px',
    margin: '16px 0',
    color: '#cf1322',
    fontSize: 13,
  },
  footer: {
    padding: '16px 24px 24px',
    borderTop: '1px solid #f0f0f0',
  },
  totalRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  totalLabel: { fontSize: 16, color: '#666' },
  totalValue: { fontSize: 20, fontWeight: 700, color: '#222' },
  checkoutBtn: {
    width: '100%',
    padding: '14px 0',
    background: '#e8604c',
    color: '#fff',
    border: 'none',
    borderRadius: 10,
    fontSize: 16,
    fontWeight: 700,
    cursor: 'pointer',
    letterSpacing: 0.5,
  },
};

