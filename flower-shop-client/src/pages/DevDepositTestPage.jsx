import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import orderApi from '../api/orderApi';
import paymentApi from '../api/paymentApi';
import Button from '../components/ui/button/Button';
import { useAuth } from '../context/AuthContext';

// Trang test luồng đặt cọc (chỉ có khi chạy `npm run dev`). Phần tạo đơn custom do nhóm custom làm;
// tạm thời tạo đơn CUSTOM / AWAITING_DEPOSIT bằng SQL trong docs/DEPOSIT_PAYMENT_CONTRACT.md.
export default function DevDepositTestPage() {
  const { user, busy } = useAuth();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [payingId, setPayingId] = useState(null);

  useEffect(() => {
    if (!user?.id) return;
    setLoading(true);
    orderApi.getOrderHistory(user.id)
      .then((data) => setOrders((Array.isArray(data) ? data : []).filter((o) => o.orderType === 'CUSTOM')))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [user?.id]);

  const pay = async (orderId, paymentType) => {
    setPayingId(orderId);
    setError('');
    try {
      await paymentApi.startSePayCheckout(orderId, paymentType);
    } catch (err) {
      setError(err.message || 'Không thể tạo thanh toán.');
      setPayingId(null);
    }
  };

  const formatPrice = (amount) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);

  if (busy) return <p role="status" className="p-6">Đang kiểm tra đăng nhập…</p>;
  if (!user) return <p className="p-6">Hãy <Link to="/login" className="text-brand-600 underline">đăng nhập</Link> bằng tài khoản khách hàng.</p>;

  return (
    <div className="p-4 sm:p-6 lg:p-8 w-full max-w-3xl mx-auto my-8 space-y-4">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Test thanh toán đặt cọc (dev)</h1>
      <p className="text-sm text-warning-600">
        Chỉ dùng khi phát triển. Thanh toán đi qua cổng SePay đang cấu hình — dùng key sandbox, key live sẽ trừ tiền thật.
      </p>
      {error && <p className="text-sm text-error-600">{error}</p>}
      {loading ? (
        <p role="status">Đang tải đơn hàng…</p>
      ) : orders.length === 0 ? (
        <p className="text-sm text-gray-600 dark:text-gray-400">
          Tài khoản này chưa có đơn CUSTOM. Tạo đơn test bằng SQL trong docs/DEPOSIT_PAYMENT_CONTRACT.md rồi tải lại trang.
        </p>
      ) : (
        <ul className="space-y-3">
          {orders.map((o) => (
            <li key={o.id} className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 rounded-xl border border-gray-200 bg-white p-4 dark:border-white/5 dark:bg-white/3">
              <div className="text-sm">
                <div className="font-mono text-gray-900 dark:text-white">{o.id}</div>
                <div className="text-gray-600 dark:text-gray-400">
                  {o.status} · Tổng {formatPrice(o.totalAmount)}
                </div>
              </div>
              {(o.status === 'AWAITING_DEPOSIT' || o.status === 'PENDING') && (
                <Button onClick={() => pay(o.id, 'DEPOSIT')} disabled={payingId !== null}>
                  {payingId === o.id ? 'Đang chuyển…' : 'Trả cọc'}
                </Button>
              )}
              {o.status === 'DEPOSIT_PAID' && (
                <Button onClick={() => pay(o.id, 'FINAL')} disabled={payingId !== null}>
                  {payingId === o.id ? 'Đang chuyển…' : 'Trả phần còn lại'}
                </Button>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
