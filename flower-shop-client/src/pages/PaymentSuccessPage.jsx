import { useState, useEffect, useRef } from 'react';
import { useSearchParams, Link } from 'react-router';
import paymentApi from '../api/paymentApi';
import PageMeta from '../components/common/PageMeta';
import Button from '../components/ui/button/Button';

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

          // Nếu đã xác nhận thanh toán thành công
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
  const formatPrice = (amount) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);

  return (
    <div className="p-4 sm:p-6 lg:p-8 w-full max-w-2xl mx-auto my-8">
      <PageMeta title="Kết quả thanh toán | Cửa hàng hoa" description="Xác nhận thanh toán đơn hàng" />

      <div className="rounded-2xl border border-gray-200 bg-white p-8 sm:p-10 shadow-sm dark:border-white/5 dark:bg-white/3 text-center">
        {loading ? (
          <div className="py-8 space-y-4">
            <div className="mx-auto h-12 w-12 animate-spin rounded-full border-4 border-solid border-brand-500 border-t-transparent"></div>
            <h2 className="text-xl font-bold text-gray-900 dark:text-white">
              Đang xác thực giao dịch...
            </h2>
            <p className="text-sm text-gray-500 dark:text-gray-400 max-w-md mx-auto">
              Hệ thống đang đối soát với cổng thanh toán và Ngân hàng. Vui lòng chờ trong giây lát ({pollCount}/{maxAttempts}).
            </p>
          </div>
        ) : isConfirmed ? (
          <div className="space-y-6">
            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-success-500 text-white text-3xl shadow-lg shadow-success-500/30">
              ✓
            </div>
            
            <div>
              <span className="inline-block px-3 py-1 text-xs font-semibold rounded-full bg-success-100 text-success-800 dark:bg-success-500/20 dark:text-success-400 mb-2">
                Thanh toán thành công
              </span>
              <h1 className="text-3xl font-extrabold text-gray-900 dark:text-white">
                Cảm ơn bạn đã mua hoa!
              </h1>
              <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">
                Đơn hàng của bạn đã được ghi nhận và thanh toán thành công. Cửa hàng sẽ chuẩn bị hoa tươi và giao sớm nhất!
              </p>
            </div>

            {/* Chi tiết giao dịch */}
            <div className="rounded-xl border border-gray-100 bg-gray-50 p-5 dark:border-white/5 dark:bg-gray-800/40 text-left space-y-3 text-sm">
              {paymentData?.invoiceNumber && (
                <div className="flex justify-between">
                  <span className="text-gray-500 dark:text-gray-400">Mã hóa đơn:</span>
                  <span className="font-mono font-semibold text-gray-900 dark:text-white">{paymentData.invoiceNumber}</span>
                </div>
              )}
              {paymentData?.orderId && (
                <div className="flex justify-between">
                  <span className="text-gray-500 dark:text-gray-400">Mã đơn hàng:</span>
                  <span className="font-mono text-gray-900 dark:text-white">{paymentData.orderId}</span>
                </div>
              )}
              {paymentData?.amount && (
                <div className="flex justify-between items-baseline pt-2 border-t border-gray-200 dark:border-white/10">
                  <span className="font-semibold text-gray-900 dark:text-white">Số tiền đã thanh toán:</span>
                  <span className="text-lg font-bold text-brand-600 dark:text-brand-400">
                    {formatPrice(paymentData.amount)}
                  </span>
                </div>
              )}
            </div>

            <div className="flex flex-col sm:flex-row justify-center gap-3 pt-2">
              <Link to="/" className="w-full sm:w-auto">
                <Button className="w-full sm:w-auto px-8 py-3">
                  Tiếp tục mua sắm
                </Button>
              </Link>
            </div>

            
          </div>
        ) : (
          <div className="space-y-6">
            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-warning-500 text-white text-3xl shadow-lg shadow-warning-500/30">
              ⏳
            </div>
            
            <div>
              <span className="inline-block px-3 py-1 text-xs font-semibold rounded-full bg-warning-100 text-warning-800 dark:bg-warning-500/20 dark:text-warning-400 mb-2">
                Đang xử lý thanh toán
              </span>
              <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                Giao dịch đang được xác nhận
              </h1>
              <p className="mt-2 text-sm text-gray-600 dark:text-gray-400 max-w-md mx-auto">
                Nếu bạn đã chuyển khoản hoặc thanh toán thành công, vui lòng đợi ít phút để hệ thống ngân hàng đồng bộ.
              </p>
            </div>

            <div className="flex flex-col sm:flex-row justify-center gap-3 pt-2">
              <Button onClick={() => window.location.reload()} variant="outline">
                Kiểm tra lại trạng thái
              </Button>
              <Link to="/">
                <Button>Về trang chủ</Button>
              </Link>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
