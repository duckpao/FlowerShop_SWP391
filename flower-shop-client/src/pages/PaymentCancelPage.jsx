import { useSearchParams, Link } from 'react-router-dom';

export default function PaymentCancelPage() {
  const [searchParams] = useSearchParams();
  const orderId = searchParams.get('order_invoice_number') || searchParams.get('orderId');

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <div style={styles.iconCircle}>
          <span style={styles.cancelIcon}>↩</span>
        </div>
        <span style={styles.badge}>Giao dịch đã hủy</span>
        <h1 style={styles.title}>Bạn đã hủy thanh toán</h1>
        <p style={styles.desc}>
          Yêu cầu thanh toán qua cổng SePay đã được hủy bỏ theo yêu cầu của bạn.
          Đơn hàng vẫn được lưu lại, bạn có thể thanh toán lại bất kỳ lúc nào.
        </p>

        {orderId && (
          <div style={styles.orderBox}>
            <span style={styles.orderLabel}>Mã đơn hàng:</span>
            <code style={styles.orderCode}>{orderId}</code>
          </div>
        )}

        <div style={styles.actions}>
          <Link to="/cart" style={styles.payBtn}>
            🛒 Quay lại giỏ hàng
          </Link>
          <Link to="/" style={styles.homeBtn}>
            Về trang chủ
          </Link>
        </div>
      </div>
    </div>
  );
}

const styles = {
  container: {
    minHeight: '80vh',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '40px 16px',
    fontFamily: 'system-ui, sans-serif',
  },
  card: {
    background: '#fff',
    borderRadius: 20,
    padding: '48px 36px',
    maxWidth: 520,
    width: '100%',
    textAlign: 'center',
    border: '1px solid #fff3bf',
    boxShadow: '0 10px 30px rgba(245, 159, 0, 0.08)',
  },
  iconCircle: {
    width: 76,
    height: 76,
    borderRadius: '50%',
    background: '#fff9db',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    margin: '0 auto 20px',
  },
  cancelIcon: {
    fontSize: 36,
    color: '#f59f00',
    fontWeight: 900,
  },
  badge: {
    background: '#fff9db',
    color: '#f59f00',
    padding: '4px 12px',
    borderRadius: 20,
    fontSize: 12,
    fontWeight: 700,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    display: 'inline-block',
    marginBottom: 12,
  },
  title: {
    fontSize: 24,
    fontWeight: 800,
    color: '#1a1a1a',
    margin: '0 0 12px',
  },
  desc: {
    fontSize: 15,
    color: '#666',
    lineHeight: 1.5,
    margin: '0 0 24px',
  },
  orderBox: {
    background: '#f8f9fa',
    borderRadius: 10,
    padding: '12px 16px',
    marginBottom: 32,
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    border: '1px solid #eee',
  },
  orderLabel: { fontSize: 13, color: '#777', fontWeight: 500 },
  orderCode: { fontSize: 13, fontWeight: 700, color: '#222' },
  actions: {
    display: 'flex',
    gap: 12,
    justifyContent: 'center',
  },
  payBtn: {
    flex: 1,
    padding: '12px 0',
    background: '#e8604c',
    color: '#fff',
    textDecoration: 'none',
    borderRadius: 10,
    fontWeight: 700,
    fontSize: 14,
  },
  homeBtn: {
    flex: 1,
    padding: '12px 0',
    background: '#f1f3f5',
    color: '#495057',
    textDecoration: 'none',
    borderRadius: 10,
    fontWeight: 600,
    fontSize: 14,
  },
};

