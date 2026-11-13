export default function CustomPaymentOptions({ value, onChange, disabled, name }) {
  return <fieldset disabled={disabled} className="space-y-3">
    <legend className="mb-3 text-sm font-semibold">Cách thanh toán đơn hoa</legend>
    {[
      ['DEPOSIT', 'Đặt cọc trước, phần còn lại COD', 'Thanh toán tiền cọc qua SePay. Trả phần còn lại cho nhân viên giao hàng khi nhận hoa.'],
      ['FULL', 'Thanh toán toàn bộ qua SePay', 'Trả đủ tổng tiền sau ưu đãi ngay bây giờ. Không thu thêm COD khi giao hoa.'],
    ].map(([type, title, description]) => <label key={type} className={`custom-payment-choice flex cursor-pointer items-start gap-3 rounded-xl border p-4 text-sm ${value === type ? 'border-brand-500 bg-brand-50/40 dark:bg-brand-500/10' : 'border-gray-200 dark:border-gray-700'}`}>
      <input style={{ width: '1rem', height: '1rem', flexShrink: 0, marginTop: '.2rem' }} type="radio" name={name} checked={value === type} onChange={() => onChange(type)} />
      <span><span className="font-semibold">{title}</span><span className="mt-1 block text-xs leading-5 text-gray-500">{description}</span></span>
    </label>)}
  </fieldset>
}
