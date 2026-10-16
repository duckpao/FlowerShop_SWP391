import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router';
import PageMeta from '../components/common/PageMeta';
import paymentApi from '../api/paymentApi';
import { useCartContext } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';

// SePay redirect về đây khi khách bấm hủy trên cổng. Trang này gọi backend hủy giao dịch rồi chuyển sang /payment/error.
// Không hiển thị kết quả trực tiếp vì khách có thể đã trả tiền nhưng bấm hủy/quay lại muộn (result.paid).
export default function PaymentCancelPage() {
  const [searchParams] = useSearchParams();
  // Chốt mã hóa đơn 1 lần; ưu tiên query của SePay, dự phòng mã đã lưu lúc tạo giao dịch (redirectToSePay)
  const [invoiceNumber] = useState(() => searchParams.get('invoice_number')
    || searchParams.get('order_invoice_number') || sessionStorage.getItem('flower_pending_invoice'));
  const navigate = useNavigate();
  const { userId, fetchCart } = useCartContext();
  const { busy } = useAuth();
  const [waiting, setWaiting] = useState(false);

  useEffect(() => {
    if (busy) return;
    // Cần đăng nhập để hủy giao dịch; đăng nhập xong quay lại đúng trang này
    if (!userId) {
      const next = invoiceNumber
        ? `/payment/cancel?invoice_number=${encodeURIComponent(invoiceNumber)}`
        : '/payment/error';
      navigate(`/login?next=${encodeURIComponent(next)}`, { replace: true });
      return;
    }
    if (!invoiceNumber) {
      navigate('/payment/error', { replace: true });
      return;
    }
    let active = true;
    let timer;
    // Gọi hủy; nếu mạng lỗi thì thử lại mỗi 2 giây cho tới khi backend phản hồi
    const cancelAndReturn = async () => {
      try {
        const result = await paymentApi.cancelCheckout(invoiceNumber);
        if (!active) return;
        // Webhook báo đã trả tiền trước khi hủy kịp -> coi như thành công, không hủy
        if (result?.paid) {
          navigate(`/payment/success?invoice_number=${encodeURIComponent(invoiceNumber)}`, { replace: true });
          return;
        }
        // Thanh toán cọc/phần còn lại: hủy ở cổng không hủy đơn, chỉ bỏ lần thanh toán này.
        let deposit = false;
        try {
          const res = await paymentApi.getPaymentStatusByInvoice(invoiceNumber);
          const type = (res?.data || res)?.paymentType;
          deposit = type === 'DEPOSIT' || type === 'FINAL';
        } catch { /* không lấy được trạng thái thì coi như đơn thường */ }
        await fetchCart(); // đơn bị hủy -> sản phẩm được trả về giỏ
        if (!active) return;
        sessionStorage.removeItem('flower_pending_invoice');
        navigate(`/payment/error?cancelled=1${deposit ? '&deposit=1' : ''}&invoice_number=${encodeURIComponent(invoiceNumber)}`, { replace: true });
      } catch {
        if (!active) return;
        setWaiting(true);
        timer = setTimeout(cancelAndReturn, 2000);
      }
    };
    cancelAndReturn();
    return () => { active = false; clearTimeout(timer); };
  }, [invoiceNumber, userId, busy, fetchCart, navigate]);

  return (
    <div className="mx-auto max-w-xl p-6 py-16 text-center" role="status" aria-live="polite">
      <PageMeta title="Đang hủy thanh toán | Cửa hàng hoa" description="Kiểm tra giao dịch trước khi hiển thị kết quả" />
      <div className="mx-auto mb-5 h-10 w-10 animate-spin rounded-full border-4 border-brand-500 border-t-transparent" />
      <h1 className="text-xl font-semibold text-gray-900 dark:text-white">Đang kiểm tra trạng thái giao dịch</h1>
      <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">
        {waiting ? 'Đang chờ kết nối để hoàn tất hủy giao dịch…' : 'Sản phẩm đã chọn sẽ được giữ lại trong giỏ.'}
      </p>
    </div>
  );
}
