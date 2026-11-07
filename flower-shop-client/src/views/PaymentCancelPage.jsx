import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router';
import PageMeta from '../components/common/PageMeta';
import paymentService from '../services/paymentService';
import { useCartContext } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';

// SePay redirect về đây khi khách bấm quay lại/hủy trên cổng. Đơn KHÔNG bị hủy: chuyển sang /payment/pending để khách trả lại;
// đơn chỉ hủy khi quá 1 phút (scheduler) hoặc khách tự bấm hủy đơn. Đã trả tiền rồi thì sang trang thành công.
export default function PaymentCancelPage() {
  const [searchParams] = useSearchParams();
  // Chốt mã hóa đơn 1 lần; ưu tiên query của SePay, dự phòng mã đã lưu lúc tạo giao dịch (redirectToSePay)
  const [invoiceNumber] = useState(() => searchParams.get('invoice_number')
    || searchParams.get('order_invoice_number') || sessionStorage.getItem('flower_pending_invoice'));
  const navigate = useNavigate();
  const { userId } = useCartContext();
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
    let retries = 0;
    // Không hủy đơn: chỉ kiểm tra trạng thái rồi đưa khách về màn chờ thanh toán.
    // Lỗi thì thử lại mỗi 2 giây, tối đa 5 lần rồi sang trang lỗi (tránh lặp vô hạn khi sai invoice/không có quyền)
    const checkAndReturn = async () => {
      try {
        const res = await paymentService.getPaymentStatusByInvoice(invoiceNumber);
        const data = res?.data || res;
        if (!active) return;
        // Đã trả tiền trước khi bấm quay lại -> coi như thành công
        if (data?.isPaid) {
          navigate(`/payment/success?invoice_number=${encodeURIComponent(invoiceNumber)}`, { replace: true });
          return;
        }
        if (data?.orderStatus === 'CANCELLED') {
          navigate(`/payment/error?invoice_number=${encodeURIComponent(invoiceNumber)}`, { replace: true });
          return;
        }
        // Cọc/phần còn lại: đơn không có hạn 1 phút, trả lại từ trang chi tiết đơn
        if (data?.paymentType === 'DEPOSIT' || data?.paymentType === 'FINAL') {
          navigate(`/orders/${encodeURIComponent(data.orderId)}`, { replace: true });
          return;
        }
        const ids = data?.orderIds?.length ? data.orderIds : [data.orderId];
        navigate(`/payment/pending?${ids.map(id => `order=${encodeURIComponent(id)}`).join('&')}`, { replace: true });
      } catch {
        if (!active) return;
        if (++retries > 5) {
          navigate('/payment/error', { replace: true });
          return;
        }
        setWaiting(true);
        timer = setTimeout(checkAndReturn, 2000);
      }
    };
    checkAndReturn();
    return () => { active = false; clearTimeout(timer); };
  }, [invoiceNumber, userId, busy, navigate]);

  return (
    <div className="mx-auto max-w-xl p-6 py-16 text-center" role="status" aria-live="polite">
      <PageMeta title="Đang kiểm tra thanh toán | Cửa hàng hoa" description="Kiểm tra giao dịch trước khi hiển thị kết quả" />
      <div className="mx-auto mb-5 h-10 w-10 animate-spin rounded-full border-4 border-brand-500 border-t-transparent" />
      <h1 className="text-xl font-semibold text-gray-900 dark:text-white">Đang kiểm tra trạng thái giao dịch</h1>
      <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">
        {waiting ? 'Đang chờ kết nối để kiểm tra giao dịch…' : 'Đơn hàng vẫn được giữ, bạn có thể thanh toán lại.'}
      </p>
    </div>
  );
}
