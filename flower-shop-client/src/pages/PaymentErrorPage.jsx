import { useSearchParams, Link } from 'react-router';
import PageMeta from '../components/common/PageMeta';
import Button from '../components/ui/button/Button';

export default function PaymentErrorPage() {
  const [searchParams] = useSearchParams();
  const orderId = searchParams.get('order_invoice_number') || searchParams.get('orderId');

  return (
    <div className="p-4 sm:p-6 lg:p-8 w-full max-w-xl mx-auto my-12">
      <PageMeta title="Thanh toán thất bại | Cửa hàng hoa" description="Thông báo lỗi thanh toán" />

      <div className="rounded-2xl border border-error-200 bg-white p-8 sm:p-10 shadow-sm dark:border-error-500/20 dark:bg-white/3 text-center space-y-6">
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-error-500 text-white text-3xl shadow-lg shadow-error-500/30">
          ✕
        </div>

        <div>
          <span className="inline-block px-3 py-1 text-xs font-semibold rounded-full bg-error-100 text-error-800 dark:bg-error-500/20 dark:text-error-400 mb-2">
            Giao dịch không thành công
          </span>
          <h1 className="text-3xl font-extrabold text-gray-900 dark:text-white">
            Thanh toán thất bại
          </h1>
          <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">
            Đã xảy ra sự cố trong quá trình giao dịch hoặc thanh toán bị từ chối. Tài khoản của bạn chưa bị trừ tiền.
          </p>
        </div>

        {orderId && (
          <div className="rounded-xl border border-gray-100 bg-gray-50 p-4 dark:border-white/5 dark:bg-gray-800/40 text-sm flex justify-between items-center">
            <span className="text-gray-500 dark:text-gray-400">Mã đơn hàng:</span>
            <span className="font-mono font-semibold text-gray-900 dark:text-white">{orderId}</span>
          </div>
        )}

        <div className="flex flex-col sm:flex-row justify-center gap-3 pt-2">
          <Link to="/cart" className="w-full sm:w-auto">
            <Button className="w-full sm:w-auto px-6 py-2.5">
              ↻ Thử thanh toán lại
            </Button>
          </Link>
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
