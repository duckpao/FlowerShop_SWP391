import { useEffect, useState } from 'react';
import { voucherService } from '../../services/voucherService';

const formatPrice = amount =>
  new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);

const describe = choice => choice.discountType === 'PERCENTAGE'
  ? `Giảm ${Number(choice.discountValue)}%${choice.maxDiscountValue != null ? `, tối đa ${formatPrice(choice.maxDiscountValue)}` : ''}`
  : `Giảm ${formatPrice(choice.discountValue)}`;

/**
 * Chọn mã giảm giá cho đơn của một shop. Mỗi khách chỉ dùng 1 mã: việc chọn mã được CheckoutPage giữ chung
 * cho mọi shop, chọn ở shop khác sẽ thay mã đang chọn. Danh sách do backend kiểm tra (chỉ đọc, chưa giữ lượt dùng).
 * onChange nhận { shopId, code, saving } hoặc null khi bỏ chọn.
 */
export default function VoucherPicker({ userId, shopId, cartItemIds, selectedCode, onChange }) {
  const [preview, setPreview] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const idsKey = cartItemIds.join(',');

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    voucherService.preview(userId, shopId, idsKey.split(','), null)
      .then(result => { if (active) setPreview(result); })
      .catch(err => { if (active) { setPreview(null); setError(err.message || 'Không tải được danh sách mã giảm giá.'); } })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [userId, shopId, idsKey]);

  // Mã đang chọn không còn dùng được (đổi giỏ, hết lượt...) thì bỏ chọn
  useEffect(() => {
    if (!preview || !selectedCode) return;
    const current = preview.vouchers.find(v => v.code === selectedCode);
    if (current && !current.eligible) onChange(null);
  }, [preview]);

  const choose = choice => onChange(choice ? { shopId, code: choice.code, saving: Number(choice.saving) } : null);
  const vouchers = preview?.vouchers || [];
  const recommended = vouchers.find(v => v.code === preview?.recommendedCode);

  return (
    <div className="space-y-2 border-t border-gray-100 pt-3 dark:border-white/5">
      <p className="text-sm font-semibold text-gray-900 dark:text-white">Mã giảm giá</p>
      {loading && <p role="status" className="text-xs text-gray-500">Đang tải mã giảm giá…</p>}
      {error && <p role="alert" className="rounded-lg bg-error-50 p-2 text-xs text-error-600">{error}</p>}
      {!loading && !error && !vouchers.length && <p className="text-xs text-gray-500 dark:text-gray-400">Chưa có mã giảm giá nào cho shop này.</p>}
      {recommended && selectedCode !== recommended.code && (
        <div className="flex items-center justify-between gap-2 rounded-lg bg-brand-50 p-2.5 text-xs text-brand-700 dark:bg-brand-500/10 dark:text-brand-300">
          <span>Gợi ý: mã <strong>{recommended.code}</strong> tiết kiệm {formatPrice(recommended.saving)}</span>
          <button type="button" onClick={() => choose(recommended)} className="shrink-0 rounded-md bg-brand-500 px-2.5 py-1 font-semibold text-white hover:bg-brand-600">Dùng mã này</button>
        </div>
      )}
      {vouchers.length > 0 && (
        <ul className="space-y-2">
          {vouchers.map(v => {
            const checked = selectedCode === v.code;
            return (
              <li key={v.id}>
                <label className={`flex items-start gap-3 rounded-lg border p-3 text-sm ${v.eligible ? 'cursor-pointer' : 'cursor-not-allowed opacity-60'} ${checked ? 'border-brand-500 bg-brand-50/50 dark:bg-brand-500/10' : 'border-gray-200 dark:border-white/10'}`}>
                  <input type="radio" name="voucher" className="mt-1 text-brand-600" disabled={!v.eligible} checked={checked} onChange={() => choose(v)} />
                  <span className="min-w-0">
                    <strong className="block text-gray-900 dark:text-white">{v.code} <span className="font-normal text-xs text-gray-500">({v.scope === 'PLATFORM' ? 'Của sàn' : 'Của shop'})</span></strong>
                    <small className="block text-xs text-gray-600 dark:text-gray-400">{describe(v)}{Number(v.minOrderValue) > 0 ? `, đơn từ ${formatPrice(v.minOrderValue)}` : ''}</small>
                    {v.eligible
                      ? <small className="block text-xs font-medium text-success-600">Tiết kiệm {formatPrice(v.saving)}</small>
                      : <small className="block text-xs text-error-600">{v.reason}</small>}
                  </span>
                </label>
              </li>
            );
          })}
        </ul>
      )}
      {selectedCode && (
        <button type="button" onClick={() => choose(null)} className="text-xs font-medium text-gray-600 underline dark:text-gray-400">Bỏ chọn mã</button>
      )}
    </div>
  );
}
