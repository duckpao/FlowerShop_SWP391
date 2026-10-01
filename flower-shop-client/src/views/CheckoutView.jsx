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
import GhnAddressPicker from '../components/GhnAddressPicker'

export default function CheckoutView() {
    const { user } = useAuth()
    const navigate = useNavigate()
    const [searchParams] = useSearchParams()
    const productId = searchParams.get('productId') || ''

    const [product, setProduct] = useState(null)
    const [addresses, setAddresses] = useState([])
    const [addressId, setAddressId] = useState('')
    const [ghnAddress, setGhnAddress] = useState({ city: '', district: '', ward: '', ghnDistrictId: null, ghnWardCode: '' })
    const [shippingFee, setShippingFee] = useState(0)
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

    // Calc fee effect
    useEffect(() => {
        if (!productId || !addressId) { setShippingFee(0); return }
        const addr = addresses.find(a => a.id === addressId)
        if (!addr) return
        
        let districtId = ghnAddress.ghnDistrictId || addr.ghnDistrictId
        let wardCode = ghnAddress.ghnWardCode || addr.ghnWardCode
        if (!districtId || !wardCode) { setShippingFee(0); return }
        
        let active = true
        customerOrderService.fee({ addressId, productId, quantity: 1, note: '', ghnDistrictId: districtId, ghnWardCode: wardCode })
            .then(res => active && setShippingFee(res.shippingFee))
            .catch(e => active && setError(e.message))
        return () => { active = false }
    }, [addressId, productId, ghnAddress.ghnDistrictId, ghnAddress.ghnWardCode, addresses])

    const handleCheckout = async e => {
        e.preventDefault()
        if (!addressId) { setError('Vui lòng chọn địa chỉ.'); return }
        setBusy(true); setError(''); setNotice('')
        try {
            const result = await customerOrderService.checkout({ 
                addressId, productId, quantity, note: '',
                ghnDistrictId: ghnAddress.ghnDistrictId, ghnWardCode: ghnAddress.ghnWardCode,
                city: ghnAddress.city, district: ghnAddress.district, ward: ghnAddress.ward
            })
            setNotice(`Đặt hàng thành công! Mã đơn: ${result.orderId}. Phí ship: ${formatPrice(result.shippingFee)}`)
            setTimeout(() => navigate(`/orders/${result.orderId}`), 2000)
        } catch (e) { setError(e.message) } finally { setBusy(false) }
    }

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
                                label: `${a.addressLine}, ${a.ward}, ${a.district}, ${a.city}${a.isDefault?' (MĐ)':''}` }))}
                                placeholder="Chọn địa chỉ" defaultValue={addressId} onChange={setAddressId} />
                            {addressId && addresses.find(a => a.id === addressId) && 
                             (!addresses.find(a => a.id === addressId).ghnDistrictId || !addresses.find(a => a.id === addressId).ghnWardCode) && (
                                <div className="mt-4 p-4 bg-yellow-50 rounded-lg border border-yellow-100">
                                    <p className="text-sm text-yellow-800 mb-3 font-medium">⚠️ Địa chỉ này chưa có thông tin Phường/Xã chuẩn GHN. Vui lòng chọn lại khu vực:</p>
                                    <GhnAddressPicker value={ghnAddress} onChange={setGhnAddress} />
                                </div>
                            )}
                        </>}
                    </div>
                    <div className="flex gap-4 items-end">
                        <div className="flex-1"><Label>Số lượng</Label>
                            <Input type="number" min={1} max={product?.stock||1} value={quantity} onChange={e => setQuantity(parseInt(e.target.value)||1)} />
                        </div>
                        <div><Label>Tạm tính</Label>
                            <p className="text-lg font-bold">{formatPrice(product.price * quantity)}</p>
                            <p className="text-sm text-gray-500 mt-1">Phí ship: {shippingFee ? formatPrice(shippingFee) : "---"}</p>
                        </div>
                    </div>
                    <Button type="submit" disabled={busy || !addressId} className="w-full">
                        {busy ? 'Đang xử lý…' : `Đặt hàng (COD) — ${formatPrice(product.price * quantity)}`}
                    </Button>
                </form>
            </div>}
        </ComponentCard>
    </div>
}