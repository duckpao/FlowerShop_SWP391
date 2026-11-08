import { useCallback } from 'react'
import VoucherPicker from './VoucherPicker'

export default function ShopVoucherPicker({ shopId, context, selections, setSelections, setPreviews, disabled }) {
  const change = useCallback(value => setSelections(previous => ({ ...previous, [shopId]: value })), [shopId, setSelections])
  const preview = useCallback(value => setPreviews(previous => ({ ...previous, [shopId]: value })), [shopId, setPreviews])
  return <VoucherPicker context={context} value={selections[shopId] || { couponCode: '', shippingCouponCode: '' }} onChange={change} onPreview={preview} disabled={disabled} />
}
