import { useState, useEffect } from 'react';
import paymentApi from '../../api/paymentApi';

export default function PaymentModal({ isOpen, onClose, orderId, onPaymentSuccess }) {
  const [qrData, setQrData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [copiedField, setCopiedField] = useState(null);
  const [isPaid, setIsPaid] = useState(false);

  // 1. Lấy thông tin mã VietQR khi mở Modal
  useEffect(() => {
    if (!isOpen || !orderId) return;

    let isMounted = true;
    setLoading(true);
    setError(null);
    setIsPaid(false);

    paymentApi.createQrPayment(orderId)
      .then((data) => {
        if (isMounted) setQrData(data);
      })
      .catch((err) => {
        if (isMounted) setError(err.message || 'Không thể tạo mã QR thanh toán.');
      })
      .finally(() => {
        if (isMounted) setLoading(false);
      });

    return () => { isMounted = false; };
  }, [isOpen, orderId]);

  // 2. Polling kiểm tra trạng thái thanh toán mỗi 2.5 giây
  useEffect(() => {
    if (!isOpen || !orderId || isPaid) return;

    const interval = setInterval(() => {
      paymentApi.checkPaymentStatus(orderId)
        .then((res) => {
          if (res.isPaid) {
            setIsPaid(true);
            clearInterval(interval);
            if (onPaymentSuccess) onPaymentSuccess(res);
          }
        })
        .catch((e) => console.log('Polling check error:', e));
    }, 2500);

    return () => clearInterval(interval);
  }, [isOpen, orderId, isPaid, onPaymentSuccess]);

  const handleCopy = (text, fieldName) => {
    navigator.clipboard.writeText(text);
    setCopiedField(fieldName);
    setTimeout(() => setCopiedField(null), 2000);
  };

  const formatPrice = (amount) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);

  if (!isOpen) return null;

  return (
    <div style={styles.overlay}>
      <div style={styles.modal}>
        {/* Header */}
        <div style={styles.header}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <span style={{ fontSize: 24 }}>💳</span>
            <h3 style={styles.headerTitle}>Thanh toán chuyển khoản ngân hàng</h3>
          </div>
          <button style={styles.closeBtn} onClick={onClose}>✕</button>
        </div>

        {/* Content Body */}
        <div style={styles.body}>
          {loading && (
            <div style={styles.loadingBox}>
              <div style={styles.spinner} />
              <p style={{ marginTop: 16, color: '#666' }}>Đang khởi tạo mã QR thanh toán...</p>
            </div>
          )}

          {error && (
            <div style={styles.errorBox}>
              <p>⚠️ {error}</p>
              <button style={styles.retryBtn} onClick={onClose}>Đóng</button>
            </div>
          )}

          {/* MÀN HÌNH ĐÃ THANH TOÁN THÀNH CÔNG */}
          {isPaid && (
            <div style={styles.successView}>
              <div style={styles.successIconCircle}>
                <span style={styles.successIcon}>✓</span>
              </div>
              <h2 style={styles.successTitle}>Thanh toán thành công!</h2>
              <p style={styles.successDesc}>
                Hệ thống SePay đã ghi nhận tiền về tài khoản ngân hàng. Đơn hàng của bạn đang được xử lý ngay lập tức.
              </p>
              <div style={styles.paidInfoBox}>
                <span>Mã đơn hàng:</span>
                <strong>{orderId}</strong>
              </div>
              <button style={styles.completeBtn} onClick={onClose}>
                Hoàn tất & Tiếp tục mua sắm
              </button>
            </div>
          )}

          {/* MÀN HÌNH QUÉT MÃ QR & THÔNG TIN CHUYỂN KHOẢN (Theo thiết kế 1) */}
          {!loading && !error && !isPaid && qrData && (
            <div>
              <div style={styles.instruction}>
                Mở ứng dụng ngân hàng bất kỳ để <strong>quét mã QR</strong> hoặc chuyển khoản theo thông tin bên dưới:
              </div>

              <div style={styles.grid}>
                {/* Cột 1: Mã QR */}
                <div style={styles.qrCol}>
                  <div style={styles.qrWrapper}>
                    <img
                      src={qrData.qrUrl}
                      alt="VietQR Payment"
                      style={styles.qrImage}
                    />
                  </div>
                  <div style={styles.qrBadge}>
                    <span>Quét mã bằng App Ngân Hàng / VietQR</span>
                  </div>
                </div>

                {/* Cột 2: Thông tin chi tiết kèm nút Copy */}
                <div style={styles.infoCol}>
                  <div style={styles.infoRow}>
                    <span style={styles.infoLabel}>Ngân hàng:</span>
                    <span style={styles.infoValue}><strong>{qrData.bankId}</strong></span>
                  </div>

                  <div style={styles.infoRow}>
                    <span style={styles.infoLabel}>Chủ tài khoản:</span>
                    <span style={styles.infoValue}>{qrData.accountName}</span>
                  </div>

                  <div style={styles.infoRowHighlight}>
                    <div>
                      <span style={styles.infoLabel}>Số tài khoản:</span>
                      <div style={styles.numberValue}>{qrData.accountNumber}</div>
                    </div>
                    <button
                      style={styles.copyBtn}
                      onClick={() => handleCopy(qrData.accountNumber, 'acc')}
                    >
                      {copiedField === 'acc' ? 'Đã chép ✓' : 'Sao chép'}
                    </button>
                  </div>

                  <div style={styles.infoRowHighlight}>
                    <div>
                      <span style={styles.infoLabel}>Số tiền:</span>
                      <div style={{ ...styles.numberValue, color: '#e8604c' }}>
                        {formatPrice(qrData.amount)}
                      </div>
                    </div>
                    <button
                      style={styles.copyBtn}
                      onClick={() => handleCopy(String(qrData.amount), 'amount')}
                    >
                      {copiedField === 'amount' ? 'Đã chép ✓' : 'Sao chép'}
                    </button>
                  </div>

                  <div style={{ ...styles.infoRowHighlight, background: '#fff9db', border: '1px dashed #fab005' }}>
                    <div>
                      <span style={{ ...styles.infoLabel, color: '#d9480f' }}>Nội dung chuyển khoản (bắt buộc):</span>
                      <div style={{ ...styles.numberValue, color: '#d9480f' }}>
                        {qrData.transferContent}
                      </div>
                    </div>
                    <button
                      style={{ ...styles.copyBtn, background: '#f59f00' }}
                      onClick={() => handleCopy(qrData.transferContent, 'content')}
                    >
                      {copiedField === 'content' ? 'Đã chép ✓' : 'Sao chép'}
                    </button>
                  </div>

                  <p style={styles.warningNote}>
                    ⚠️ <em>Vui lòng giữ nguyên nội dung chuyển khoản <strong>{qrData.transferContent}</strong> để hệ thống SePay tự động nhận diện đơn hàng.</em>
                  </p>
                </div>
              </div>

              {/* Thanh trạng thái xoay vòng */}
              <div style={styles.footerStatus}>
                <div style={styles.smallSpinner} />
                <span>Trạng thái: <strong>Đang chờ thanh toán...</strong> (Tự động cập nhật khi tiền vào tài khoản)</span>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

const styles = {
  overlay: {
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    background: 'rgba(0, 0, 0, 0.65)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 9999,
    padding: 16,
  },
  modal: {
    background: '#fff',
    borderRadius: 20,
    maxWidth: 720,
    width: '100%',
    overflow: 'hidden',
    boxShadow: '0 20px 60px rgba(0,0,0,0.3)',
    animation: 'fadeIn 0.2s ease',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '18px 24px',
    borderBottom: '1px solid #eee',
    background: '#fff8f6',
  },
  headerTitle: {
    margin: 0,
    fontSize: 18,
    fontWeight: 800,
    color: '#222',
  },
  closeBtn: {
    background: 'none',
    border: 'none',
    fontSize: 20,
    cursor: 'pointer',
    color: '#888',
    padding: 4,
  },
  body: {
    padding: '24px',
  },
  instruction: {
    fontSize: 14,
    color: '#555',
    marginBottom: 20,
    textAlign: 'center',
  },
  grid: {
    display: 'flex',
    gap: 24,
    alignItems: 'stretch',
  },
  qrCol: {
    flex: '1 1 45%',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    background: '#f8f9fa',
    borderRadius: 16,
    padding: '20px 16px',
    border: '1px solid #eee',
  },
  qrWrapper: {
    background: '#fff',
    padding: 10,
    borderRadius: 12,
    boxShadow: '0 4px 14px rgba(0,0,0,0.06)',
    display: 'flex',
    justifyContent: 'center',
  },
  qrImage: {
    width: 220,
    height: 'auto',
    display: 'block',
  },
  qrBadge: {
    marginTop: 12,
    fontSize: 12,
    color: '#666',
    fontWeight: 600,
  },
  infoCol: {
    flex: '1 1 55%',
    display: 'flex',
    flexDirection: 'column',
    gap: 10,
    justifyContent: 'center',
  },
  infoRow: {
    display: 'flex',
    justifyContent: 'space-between',
    fontSize: 14,
    padding: '6px 0',
    borderBottom: '1px solid #f2f2f2',
  },
  infoLabel: { color: '#666', fontSize: 13 },
  infoValue: { color: '#222' },
  infoRowHighlight: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    background: '#f8f9fa',
    padding: '10px 14px',
    borderRadius: 10,
    border: '1px solid #eee',
  },
  numberValue: {
    fontSize: 16,
    fontWeight: 800,
    color: '#1a1a1a',
    marginTop: 2,
    letterSpacing: 0.5,
  },
  copyBtn: {
    background: '#333',
    color: '#fff',
    border: 'none',
    padding: '6px 12px',
    borderRadius: 6,
    fontSize: 12,
    fontWeight: 600,
    cursor: 'pointer',
    flexShrink: 0,
  },
  warningNote: {
    fontSize: 12,
    color: '#e03131',
    lineHeight: 1.4,
    margin: '6px 0 0',
  },
  footerStatus: {
    marginTop: 20,
    padding: '12px 16px',
    background: '#f1f3f5',
    borderRadius: 10,
    display: 'flex',
    alignItems: 'center',
    gap: 12,
    fontSize: 13,
    color: '#495057',
  },
  spinner: {
    width: 40,
    height: 40,
    border: '4px solid #eee',
    borderTopColor: '#e8604c',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
    margin: '0 auto',
  },
  smallSpinner: {
    width: 16,
    height: 16,
    border: '2px solid #ccc',
    borderTopColor: '#e8604c',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
    flexShrink: 0,
  },
  loadingBox: {
    textAlign: 'center',
    padding: '40px 0',
  },
  errorBox: {
    background: '#fff5f5',
    border: '1px solid #ffc9c9',
    borderRadius: 10,
    padding: 20,
    textAlign: 'center',
    color: '#e03131',
  },
  retryBtn: {
    marginTop: 12,
    padding: '8px 18px',
    background: '#e03131',
    color: '#fff',
    border: 'none',
    borderRadius: 8,
    cursor: 'pointer',
  },
  successView: {
    textAlign: 'center',
    padding: '30px 10px',
  },
  successIconCircle: {
    width: 72,
    height: 72,
    borderRadius: '50%',
    background: '#e6fcf5',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    margin: '0 auto 16px',
  },
  successIcon: {
    fontSize: 38,
    color: '#12b886',
    fontWeight: 900,
  },
  successTitle: {
    fontSize: 22,
    fontWeight: 800,
    color: '#1a1a1a',
    marginBottom: 8,
  },
  successDesc: {
    color: '#555',
    fontSize: 14,
    maxWidth: 480,
    margin: '0 auto 20px',
    lineHeight: 1.5,
  },
  paidInfoBox: {
    background: '#f8f9fa',
    display: 'inline-flex',
    gap: 10,
    padding: '8px 18px',
    borderRadius: 8,
    fontSize: 14,
    marginBottom: 24,
    border: '1px solid #eee',
  },
  completeBtn: {
    display: 'block',
    width: '100%',
    maxWidth: 280,
    margin: '0 auto',
    padding: '12px 0',
    background: '#12b886',
    color: '#fff',
    border: 'none',
    borderRadius: 10,
    fontSize: 15,
    fontWeight: 700,
    cursor: 'pointer',
  },
};

