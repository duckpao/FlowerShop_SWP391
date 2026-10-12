import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'
import { useAuth } from '../context/AuthContext'
import { customerOrderService } from '../services/customerOrderService'
import { accountService } from '../services/accountService'
import { productService } from '../services/productService'
import { formatPrice } from '../models/productModel'
import Select from '../components/form/Select'
import Input from '../components/form/input/InputField'
import Label from '../components/form/Label'
import Button from '../components/ui/button/Button'
import ComponentCard from '../components/common/ComponentCard'
import PageMeta from '../components/common/PageMeta'
import PageBreadCrumb from '../components/common/PageBreadCrumb'

export default function CheckoutView() {
    const { user } = useAuth()
    const navigate = useNavigate()
    const [searchParams] = useSearchParams()
    const productId = searchParams.get('productId') || ''

    const [product, setProduct] = useState(null)
    const [addresses, setAddresses] = useState([])
    const [addressId, setAddressId] = useState('')
    const [shippingFee, setShippingFee] = useState(null)
    const [feeBusy, setFeeBusy] = useState(false)
    const [feeError, setFeeError] = useState('')
    const [quantity, setQuantity] = useState(1)
    const [busy, setBusy] = useState(true)
    const [error, setError] = useState('')
    const [notice, setNotice] = useState('')

    useEffect(() => {
        if (!user || user.role !== 'CUSTOMER') {
            setError('Vui lòng đăng nhập để đặt hàng.')
            setBusy(false)
            return
        }
        let active = true
        setBusy(true)
        Promise.all([
            productService.detail(productId).catch(() => null),
            accountService.addresses().catch(() => [])
        ]).then(([p, addrs]) => {
            if (!active) return
            if (!p) { setError('Sản phẩm không còn được bán.'); setBusy(false); return }
            setProduct(p)
            setAddresses(addrs)
            if (addrs.length > 0) setAddressId(addrs.find(a => a.isDefault)?.id || addrs[0].id)
            setBusy(false)
        }).catch(e => { if (active) { setError(e.message); setBusy(false) } })
        return () => { active = false }
    }, [productId, user])

    // The fee is asked from GHN every time the destination changes; null means "not known yet".
    useEffect(() => {
        setShippingFee(null); setFeeError('')
        if (!productId || !addressId) return
        const addr = addresses.find(a => a.id === addressId)
        if (!addr) return
        if (!addr.ghnWardId) return

        let active = true
        setFeeBusy(true)
        customerOrderService.fee({ addressId, productId, quantity, note: '' })
            .then(res => { if (active) setShippingFee(Number(res.shippingFee)) })
            .catch(e => { if (active) setFeeError(e.message) })
            .finally(() => { if (active) setFeeBusy(false) })
        return () => { active = false }
    }, [addressId, productId, quantity, addresses])

    const handleCheckout = async e => {
        e.preventDefault()
        if (!addressId) { setError('Vui lòng chọn địa chỉ.'); return }
        if (shippingFee === null) { setError('Chưa có phí vận chuyển từ GHN cho địa chỉ này.'); return }
        setBusy(true); setError(''); setNotice('')
        try {
            const result = await customerOrderService.checkout({ 
                addressId, productId, quantity, note: '',
            })
            setNotice(`Đặt hàng thành công! Mã đơn: ${result.orderId}. Phí ship: ${formatPrice(result.shippingFee)}`)
            setTimeout(() => navigate(`/orders/${result.orderId}`), 2000)
        } catch (e) { setError(e.message) } finally { setBusy(false) }
    }

    const subTotal = product ? product.price * quantity : 0
    const total = shippingFee !== null ? subTotal + shippingFee : null

    if (!user) return <div className="max-w-3xl mx-auto p-8"><h1 className="text-xl font-bold">Đặt hàng</h1><p className="text-gray-500">Vui lòng <a href="/login" className="text-brand-500">đăng nhập</a> để đặt hàng.</p></div>
    if (!product && !busy && error) return <div className="max-w-3xl mx-auto p-8"><h1 className="text-xl font-bold">Đặt hàng</h1><p className="text-error-500">{error}</p></div>

    return <div className="w-full">
        <PageMeta title="Đặt hàng | FlowerShop" />
        <PageBreadCrumb pageTitle="Đặt hàng" />
        <ComponentCard title="Xác nhận đơn hàng" className="max-w-3xl mx-auto">
            {error && <div className="mb-4 rounded-lg bg-error-50 p-4 text-sm text-error-500">{error}</div>}
            {notice && <div className="mb-4 rounded-lg bg-success-50 p-4 text-sm text-success-600">{notice}</div>}
            {product && <div className="space-y-6">
                <div className="flex gap-4 border-b pb-4">
                    {product.images?.[0] && <img src={product.images[0]} alt={product.name} className="w-24 h-24 object-cover rounded-lg" />}
                    <div>
                        <h2 className="text-lg font-bold text-gray-900 dark:text-white">{product.name}</h2>
                        <p className="text-brand-500 font-bold text-xl">{formatPrice(product.price)}</p>
                        <p className="text-sm text-gray-500">Còn {product.stock} sản phẩm · {product.shopName}</p>
                    </div>
                </div>
                <form onSubmit={handleCheckout} className="space-y-4">
                    <div>
                        <Label>Địa chỉ giao hàng</Label>
                        {addresses.length === 0 ? <p className="text-gray-500 text-sm">Chưa có địa chỉ. <a href="/account" className="text-brand-500">Thêm</a></p> : <>
                            <Select options={addresses.map(a => ({ value: a.id,
                                label: `${a.addressLine}, ${a.ward}, ${a.city}${a.isDefault?' (MĐ)':''}` }))}
                                placeholder="Chọn địa chỉ" defaultValue={addressId} onChange={setAddressId} />
                            {addressId && !addresses.find(a => a.id === addressId)?.ghnWardId && (
                                <p className="mt-3 rounded-lg border border-yellow-100 bg-yellow-50 p-3 text-sm text-yellow-800">
                                    Địa chỉ này chưa có mã Tỉnh/Phường của GHN nên chưa tính được phí vận chuyển.{' '}
                                    <a href="/account" className="underline">Cập nhật địa chỉ trong hồ sơ</a>.
                                </p>
                            )}
                        </>}
                    </div>
                    <div className="flex gap-4 items-end">
                        <div className="flex-1"><Label>Số lượng</Label>
                            <Input type="number" min={1} max={product?.stock||1} value={quantity} onChange={e => setQuantity(parseInt(e.target.value)||1)} />
                        </div>
                        <div><Label>Đơn giá</Label>
                            <p className="text-lg font-bold">{formatPrice(product.price)}</p>
                        </div>
                    </div>
                    <dl className="space-y-1 border-t border-gray-200 pt-4 text-sm dark:border-white/10" aria-live="polite">
                        <div className="flex justify-between"><dt>Tiền hàng</dt><dd>{formatPrice(subTotal)}</dd></div>
                        <div className="flex justify-between"><dt>Phí vận chuyển (GHN)</dt>
                            <dd>{feeBusy ? 'Đang tính…' : shippingFee !== null ? formatPrice(shippingFee) : 'Chưa có'}</dd></div>
                        {feeError && <p className="text-error-500" role="alert">{feeError}</p>}
                        {!feeBusy && !feeError && shippingFee === null && addressId && (
                            <p className="text-gray-500">Chọn Phường/Xã chuẩn GHN để tính phí vận chuyển.</p>
                        )}
                        <div className="flex justify-between pt-2 text-base font-bold">
                            <dt>Tổng thanh toán</dt>
                            <dd className="text-brand-500">{total !== null ? formatPrice(total) : '---'}</dd>
                        </div>
                    </dl>
                    <Button type="submit" disabled={busy || feeBusy || !addressId || shippingFee === null} className="w-full">
                        {busy ? 'Đang xử lý…' : total !== null ? `Đặt hàng (COD) — ${formatPrice(total)}` : 'Đặt hàng (chờ tính phí ship)'}
                    </Button>
                </form>
            </div>}
        </ComponentCard>
    </div>
}