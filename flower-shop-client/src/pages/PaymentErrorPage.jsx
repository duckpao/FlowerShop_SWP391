import { useSearchParams, Link } from 'react-router';
import PageMeta from '../components/common/PageMeta';
import Button from '../components/ui/button/Button';

// Trang kết quả khi thanh toán không thành công / bị hủy (nhận cờ cancelled, deposit từ PaymentCancelPage)
export default function PaymentErrorPage() {
  const [searchParams] = useSearchParams();
  const invoiceNumber = searchParams.get('invoice_number') || searchParams.get('order_invoice_number');
  const orderId = searchParams.get('orderId');
  // cancelled=1: khách chủ động hủy ở cổng (khác với lỗi/không xác nhận được)
  const cancelled = searchParams.get('cancelled') === '1';
  const deposit = searchParams.get('deposit') === '1'; // thanh toán cọc/phần còn lại: đơn không bị hủy

  return (
    <div className="p-4 sm:p-6 lg:p-8 w-full max-w-xl mx-auto my-12">
      <PageMeta title="Thanh toán thất bại | Cửa hàng hoa" description="Thông báo lỗi thanh toán" />

      <div className="rounded-2xl border border-error-200 bg-white p-8 sm:p-10 shadow-sm dark:border-error-500/20 dark:bg-white/3 text-center space-y-6">
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-error-500 text-white text-3xl shadow-lg shadow-error-500/30">
          ✕
        </div>

        <div>
          <span className="inline-block px-3 py-1 text-xs font-semibold rounded-full bg-error-100 text-error-800 dark:bg-error-500/20 dark:text-error-400 mb-2">
            {cancelled ? 'Giao dịch đã hủy' : 'Giao dịch không thành công'}
          </span>
          <h1 className="text-3xl font-extrabold text-gray-900 dark:text-white">
            Giao dịch thất bại
          </h1>
          <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">
            {deposit
              ? 'Lần thanh toán này chưa hoàn tất. Đơn hàng của bạn không bị hủy, bạn có thể thanh toán lại trong thời hạn quy định.'
              : cancelled
              ? 'Thanh toán chưa hoàn tất. Sản phẩm đã chọn vẫn nằm trong giỏ hàng của bạn.'
              : 'Giao dịch chưa được xác nhận thành công. Sản phẩm đã chọn vẫn nằm trong giỏ hàng của bạn.'}
          </p>
        </div>

        {(orderId || invoiceNumber) && (
          <div className="rounded-xl border border-gray-100 bg-gray-50 p-4 dark:border-white/5 dark:bg-gray-800/40 text-sm flex justify-between items-center">
            <span className="text-gray-500 dark:text-gray-400">{orderId ? 'Mã đơn hàng:' : 'Mã giao dịch:'}</span>
            <span className="font-mono font-semibold text-gray-900 dark:text-white">{orderId || invoiceNumber}</span>
          </div>
        )}

        <div className="flex flex-col sm:flex-row justify-center gap-3 pt-2">
          {!deposit && (
            <Link to="/cart" className="w-full sm:w-auto">
              <Button className="w-full sm:w-auto px-6 py-2.5">
                Quay lại giỏ hàng
              </Button>
            </Link>
          )}
          <Link to="/" className="w-full sm:w-auto">
            <Button variant="outline" className="w-full sm:w-auto px-6 py-2.5">
              Về trang chủ
            </Button>
          </Link>
        </div>
      </div>
    </div>
  );
}
