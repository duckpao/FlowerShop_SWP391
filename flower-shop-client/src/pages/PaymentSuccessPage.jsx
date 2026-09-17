import { useState, useEffect, useRef } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import paymentApi from '../api/paymentApi';

export default function PaymentSuccessPage() {
  const [searchParams] = useSearchParams();
  const invoiceNumber = searchParams.get('invoice_number') || searchParams.get('order_invoice_number');
  const orderId = searchParams.get('orderId');

  const [loading, setLoading] = useState(true);
  const [paymentData, setPaymentData] = useState(null);
  const [pollCount, setPollCount] = useState(0);
  const [isTimedOut, setIsTimedOut] = useState(false);

  const maxAttempts = 10;
  const pollIntervalMs = 2500;
  const timerRef = useRef(null);

  useEffect(() => {
    let attempts = 0;

    const checkStatus = async () => {
      try {
        let res;
        if (invoiceNumber) {
          res = await paymentApi.getPaymentStatusByInvoice(invoiceNumber);
        } else if (orderId) {
          res = await paymentApi.checkPaymentStatus(orderId);
        }

        const data = res?.data || res;
        if (data && (data.invoiceNumber || data.orderId || data.paymentId)) {
          setPaymentData(data);

          // Nếu đã xác nhận thanh toán thành công từ IPN
          if (data.isPaid || data.paymentStatus === 'SUCCESS' || data.orderStatus === 'PROCESSING') {
            setLoading(false);
            return;
          }
        }
      } catch (err) {
        console.warn('Đang chờ xác nhận thanh toán từ ngân hàng...', err);
      }

      attempts++;
      setPollCount(attempts);

      if (attempts < maxAttempts) {
        timerRef.current = setTimeout(checkStatus, pollIntervalMs);
      } else {
        setLoading(false);
        setIsTimedOut(true);
      }
    };

    if (invoiceNumber || orderId) {
      checkStatus();
    } else {
      setLoading(false);
    }

    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, [invoiceNumber, orderId]);

  const isConfirmed = paymentData?.isPaid || paymentData?.paymentStatus === 'SUCCESS';

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        {loading ? (
          <div style={styles.loadingBox}>
            <div style={styles.spinner}></div>
            <h2 style={styles.title}>Đang xác thực giao dịch...</h2>
            <p style={styles.desc}>
              Hệ thống đang kết nối đối soát với SePay và Ngân hàng. Vui lòng chờ trong giây lát.
              ({pollCount}/{maxAttempts})
            </p>
          </div>
        ) : isConfirmed ? (
          <div>
            <div style={styles.iconCircleSuccess}>
              <span style={styles.checkIcon}>✓</span>
            </div>
            <span style={styles.badgeSuccess}>Thanh toán thành công</span>
            <h1 style={styles.title}>Cảm ơn bạn đã mua hàng!</h1>
            <p style={styles.desc}>
              Đơn hàng của bạn đã được xác nhận thanh toán thành công qua <strong>Cổng thanh toán SePay</strong>.
              Cửa hàng hoa đang chuẩn bị đơn và sẽ sớm giao đến bạn.
            </p>

            <div style={styles.detailsCard}>
              {paymentData?.invoiceNumber && (
                <div style={styles.detailRow}>
                  <span style={styles.label}>Mã hóa đơn:</span>
                  <strong style={styles.val}>{paymentData.invoiceNumber}</strong>
                </div>
              )}
              {paymentData?.orderId && (
                <div style={styles.detailRow}>
                  <span style={styles.label}>Mã đơn hàng:</span>
                  <span style={styles.val}>{paymentData.orderId}</span>
                </div>
              )}
              {paymentData?.paidAmount && (
                <div style={styles.detailRow}>
                  <span style={styles.label}>Số tiền đã trả:</span>
                  <strong style={{ ...styles.val, color: '#e8604c' }}>
                    {new Intl.NumberFormat('vi-VN').format(paymentData.paidAmount)} đ
                  </strong>
                </div>
              )}
              {paymentData?.transactionNo && (
                <div style={styles.detailRow}>
                  <span style={styles.label}>Mã giao dịch SePay:</span>
                  <span style={styles.val}>{paymentData.transactionNo}</span>
                </div>
              )}
            </div>

            <div style={styles.actions}>
              <Link to="/" style={styles.homeBtn}>
                ← Về trang chủ
              </Link>
              <Link to="/cart" style={styles.cartBtn}>
                Giỏ hàng
              </Link>
            </div>
          </div>
        ) : isTimedOut ? (
          <div>
            <div style={styles.iconCirclePending}>
              <span style={styles.pendingIcon}>⏳</span>
            </div>
            <span style={styles.badgePending}>Đang xử lý giao dịch</span>
            <h1 style={styles.title}>Giao dịch đang được xử lý</h1>
            <p style={styles.desc}>
              Cổng thanh toán SePay đã tiếp nhận yêu cầu. Hệ thống ngân hàng có thể mất vài phút để ghi nhận tiền.
              Bạn có thể kiểm tra lại trạng thái trong Lịch sử đơn hàng.
            </p>

            {invoiceNumber && (
              <div style={styles.orderBox}>
                <span style={styles.label}>Mã hóa đơn:</span>
                <code>{invoiceNumber}</code>
              </div>
            )}

            <div style={styles.actions}>
              <Link to="/" style={styles.homeBtn}>
                ← Tiếp tục mua sắm
              </Link>
              <Link to="/cart" style={styles.cartBtn}>
                Xem giỏ hàng
              </Link>
            </div>
          </div>
        ) : (
          <div>
            <div style={styles.iconCircleError}>
              <span style={styles.errorIcon}>!</span>
            </div>
            <span style={styles.badgeError}>Chưa xác nhận thanh toán</span>
            <h1 style={styles.title}>Không tìm thấy giao dịch</h1>
            <p style={styles.desc}>
              Không thể tìm thấy thông tin thanh toán tương ứng hoặc giao dịch chưa hoàn tất.
            </p>
            <div style={styles.actions}>
              <Link to="/cart" style={styles.homeBtn}>
                Quay lại giỏ hàng
              </Link>
            </div>
          </div>
        )}
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
    border: '1px solid #e9ecef',
    boxShadow: '0 10px 30px rgba(0, 0, 0, 0.06)',
  },
  loadingBox: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    padding: '24px 0',
  },
  spinner: {
    width: 44,
    height: 44,
    border: '4px solid #f1f3f5',
    borderTop: '4px solid #e8604c',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
    marginBottom: 20,
  },
  iconCircleSuccess: {
    width: 76,
    height: 76,
    borderRadius: '50%',
    background: '#e6fcf5',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    margin: '0 auto 20px',
  },
  checkIcon: {
    fontSize: 40,
    color: '#12b886',
    fontWeight: 900,
  },
  iconCirclePending: {
    width: 76,
    height: 76,
    borderRadius: '50%',
    background: '#fff9db',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    margin: '0 auto 20px',
  },
  pendingIcon: {
    fontSize: 36,
  },
  iconCircleError: {
    width: 76,
    height: 76,
    borderRadius: '50%',
    background: '#fff5f5',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    margin: '0 auto 20px',
  },
  errorIcon: {
    fontSize: 36,
    color: '#fa5252',
    fontWeight: 900,
  },
  badgeSuccess: {
    background: '#ebfbee',
    color: '#2b8a3e',
    padding: '4px 12px',
    borderRadius: 20,
    fontSize: 12,
    fontWeight: 700,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
    display: 'inline-block',
    marginBottom: 12,
  },
  badgePending: {
    background: '#fff3bf',
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
  badgeError: {
    background: '#ffe3e3',
    color: '#e03131',
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
    fontSize: 22,
    fontWeight: 800,
    color: '#1a1a1a',
    margin: '0 0 12px',
  },
  desc: {
    fontSize: 14,
    color: '#666',
    lineHeight: 1.5,
    margin: '0 0 24px',
  },
  detailsCard: {
    background: '#f8f9fa',
    borderRadius: 12,
    padding: '16px',
    marginBottom: 24,
    textAlign: 'left',
    fontSize: 13,
    border: '1px solid #eee',
  },
  detailRow: {
    display: 'flex',
    justifyContent: 'space-between',
    padding: '6px 0',
    borderBottom: '1px dashed #e9ecef',
  },
  orderBox: {
    background: '#f8f9fa',
    borderRadius: 10,
    padding: '12px 16px',
    marginBottom: 24,
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    border: '1px solid #eee',
  },
  label: { color: '#777', fontWeight: 500 },
  val: { fontWeight: 600, color: '#222' },
  actions: {
    display: 'flex',
    gap: 12,
    justifyContent: 'center',
  },
  homeBtn: {
    flex: 1,
    padding: '12px 0',
    background: '#e8604c',
    color: '#fff',
    textDecoration: 'none',
    borderRadius: 10,
    fontWeight: 700,
    fontSize: 14,
    textAlign: 'center',
  },
  cartBtn: {
    flex: 1,
    padding: '12px 0',
    background: '#f1f3f5',
    color: '#495057',
    textDecoration: 'none',
    borderRadius: 10,
    fontWeight: 600,
    fontSize: 14,
    textAlign: 'center',
  },
};
