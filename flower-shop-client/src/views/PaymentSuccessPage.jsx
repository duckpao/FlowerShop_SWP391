import { useState, useEffect, useRef } from 'react';
import { useSearchParams, Link } from 'react-router';
import paymentService from '../services/paymentService';
import PageMeta from '../components/common/PageMeta';
import Button from '../components/ui/button/Button';

// Trang SePay redirect về sau khi thanh toán. Không tin việc redirect là đã trả tiền:
// poll backend (mỗi lần backend tự hỏi API SePay, không có webhook) tới khi isPaid = true hoặc hết số lần thử.
export default function PaymentSuccessPage() {
  const [searchParams] = useSearchParams();
  // SePay có thể trả mã hóa đơn bằng 1 trong 2 tên query; orderId là đường dự phòng khi không có hóa đơn
  const invoiceNumber = searchParams.get('invoice_number') || searchParams.get('order_invoice_number');
  const orderId = searchParams.get('orderId');

  const [loading, setLoading] = useState(true);
  const [paymentData, setPaymentData] = useState(null);
  const [pollCount, setPollCount] = useState(0);

  // Poll tối đa 10 lần x 2,5s ≈ 25s; quá hạn thì hiện "đang xác nhận" thay vì báo lỗi (SePay có thể ghi nhận trễ; scheduler vẫn đối soát tiếp)
  const maxAttempts = 10;
  const pollIntervalMs = 2500;
  const timerRef = useRef(null);

  useEffect(() => {
    let attempts = 0;

    // Hỏi backend trạng thái thanh toán (theo invoice, dự phòng theo orderId); dừng khi đã trả/đã hủy,
    // lỗi hoặc chưa xong thì hẹn gọi lại sau pollIntervalMs cho tới maxAttempts
    const checkStatus = async () => {
      try {
        let res;
        if (invoiceNumber) {
          res = await paymentService.getPaymentStatusByInvoice(invoiceNumber);
        } else if (orderId) {
          res = await paymentService.checkPaymentStatus(orderId);
        }

        const data = res?.data || res;
        if (data && (data.invoiceNumber || data.orderId || data.paymentId)) {
          setPaymentData(data);

          // Dừng poll khi đã trả tiền hoặc đơn đã bị hủy (vd: hết hạn thanh toán)
          if (data.isPaid === true || data.orderStatus === 'CANCELLED') {
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

  const isConfirmed = paymentData?.isPaid === true;
  // Đã trả xong: bỏ mã hóa đơn đang chờ. Không cần tải lại giỏ: giỏ đã bị xóa lúc tạo đơn và trang này được tải mới khi SePay chuyển về.
  useEffect(() => {
    if (isConfirmed) sessionStorage.removeItem('flower_pending_invoice');
  }, [isConfirmed]);
  // Đơn đặt cọc: lần này trả DEPOSIT, phần còn lại GHN thu hộ khi giao (COD)
  const isDeposit = paymentData?.paymentType === 'DEPOSIT';
  // Định dạng số tiền VND
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
                {isDeposit ? 'Đã đặt cọc' : 'Thanh toán thành công'}
              </span>
              <h1 className="text-3xl font-extrabold text-gray-900 dark:text-white">
                {isDeposit ? 'Cảm ơn bạn đã đặt cọc!' : 'Cảm ơn bạn đã mua hoa!'}
              </h1>
              <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">
                {isDeposit
                  ? 'Cửa hàng đã nhận tiền cọc và bắt đầu làm hoa. Phần còn lại bạn thanh toán khi nhận hàng (COD).'
                  : 'Đơn hàng của bạn đã được ghi nhận và thanh toán thành công. Cửa hàng sẽ chuẩn bị hoa tươi và giao sớm nhất!'}
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
                  <span className="font-semibold text-gray-900 dark:text-white">{isDeposit ? 'Tiền cọc đã thanh toán:' : 'Số tiền đã thanh toán:'}</span>
                  <span className="text-lg font-bold text-brand-600 dark:text-brand-400">
                    {formatPrice(paymentData.amount)}
                  </span>
                </div>
              )}
              {isDeposit && (
                <div className="flex justify-between items-baseline">
                  <span className="text-gray-500 dark:text-gray-400">Còn lại (trả khi nhận hàng):</span>
                  <span className="font-semibold text-gray-900 dark:text-white">
                    {formatPrice(paymentData.remainingAmount)}
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
                {paymentData?.orderStatus === 'CANCELLED' && paymentData?.message
                  ? paymentData.message
                  : 'Nếu bạn đã chuyển khoản hoặc thanh toán thành công, vui lòng đợi ít phút để hệ thống ngân hàng đồng bộ.'}
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
