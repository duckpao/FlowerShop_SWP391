import { useEffect, useState } from 'react'
import { managerOrderService } from '../services/managerOrderService'
import { managerShopService } from '../services/managerShopService'
import { formatPrice } from '../models/productModel'
import Badge from '../components/ui/badge/Badge'
import Button from '../components/ui/button/Button'
import { Table, TableBody, TableCell, TableHeader, TableRow } from '../components/ui/table'
import ComponentCard from '../components/common/ComponentCard'
import { Modal } from '../components/ui/modal'
import { EyeIcon } from '../icons'

/**
 * Bảng màu sắc tương ứng với từng trạng thái đơn hàng
 */
const statusColors = {
    PENDING: 'warning',
    AWAITING_DEPOSIT: 'warning',
    DEPOSIT_PAID: 'info',
    PROCESSING: 'info',
    DELIVERING: 'primary',
    COMPLETED: 'success',
    CANCELLED: 'error',
    REFUNDED: 'error'
}

/**
 * Nhãn tiếng Việt tương ứng với từng trạng thái đơn hàng
 */
const statusLabels = {
    PENDING: 'Chờ xác nhận',
    AWAITING_DEPOSIT: 'Chờ đặt cọc',
    DEPOSIT_PAID: 'Đã cọc',
    PROCESSING: 'Đang chuẩn bị',
    DELIVERING: 'Đang giao',
    COMPLETED: 'Đã hoàn thành',
    CANCELLED: 'Đã hủy',
    REFUNDED: 'Đã hoàn tiền'
}

/**
 * Hàm định dạng thời gian tương đối (ví dụ: "5 phút trước", "2 giờ trước")
 */
function timeAgo(dateString) {
    const seconds = Math.floor((new Date() - new Date(dateString)) / 1000)
    let interval = seconds / 31536000
    if (interval > 1) return Math.floor(interval) + " năm trước"
    interval = seconds / 2592000
    if (interval > 1) return Math.floor(interval) + " tháng trước"
    interval = seconds / 86400
    if (interval > 1) return Math.floor(interval) + " ngày trước"
    interval = seconds / 3600
    if (interval > 1) return Math.floor(interval) + " giờ trước"
    interval = seconds / 60
    if (interval > 1) return Math.floor(interval) + " phút trước"
    return Math.floor(seconds) + " giây trước"
}

/**
 * View Quản lý Đơn hàng của Cửa hàng (OrderQueueView).
 * Thực hiện quy trình đơn hàng 4 bước:
 * 1. PENDING (Chờ xác nhận): Shop bấm "Xác nhận đơn hàng" -> Chuyển sang PROCESSING.
 * 2. PROCESSING (Đang chuẩn bị): Shop làm hoa, bấm "Tạo mã vận đơn GHN & Giao" -> Tạo vận đơn GHN, chuyển sang DELIVERING.
 * 3. DELIVERING (Đang giao): Bấm "Kiểm tra GHN" để đồng bộ hoặc "Giả lập Giao xong" trên môi trường test.
 * 4. COMPLETED (Hoàn thành): Đơn hoàn tất, tiền về tài khoản shop.
 */
export default function OrderQueueView({ shop: initialShop }) {
    const [shop, setShop] = useState(initialShop)
    const [data, setData] = useState(null)
    const [page, setPage] = useState(0)
    const [statusFilter, setStatusFilter] = useState('') // Bộ lọc trạng thái tab
    const [busy, setBusy] = useState(false)
    const [error, setError] = useState('')
    const [notice, setNotice] = useState('')
    
    // State quản lý Modal xem chi tiết và Modal hủy đơn
    const [detailModalOpen, setDetailModalOpen] = useState(false)
    const [cancelModalOpen, setCancelModalOpen] = useState(false)
    const [selectedOrder, setSelectedOrder] = useState(null)
    const [cancelReason, setCancelReason] = useState('')
    const [orderDetail, setOrderDetail] = useState(null)
    
    // State quản lý Modal Cập nhật vận đơn GHN
    const [updateGhnModalOpen, setUpdateGhnModalOpen] = useState(false)
    const [updatePayload, setUpdatePayload] = useState({ to_name: '', to_phone: '', weight: 2000, note: 'CHOXEMHANGKHONGTHU' })

    // Nếu không truyền shop từ props, tự động lấy shop đầu tiên thuộc quyền quản lý của user
    useEffect(() => {
        if (initialShop) return
        let active = true
        managerShopService.list("SHOP").then(shops => {
            if (active && shops.length > 0) setShop(shops[0])
        }).catch(e => { if (active) setError(e.message) })
        return () => { active = false }
    }, [initialShop])

    /**
     * Tải danh sách đơn hàng từ backend theo trang và bộ lọc trạng thái
     */
    const load = pg => {
        if (!shop) return
        setBusy(true); setError(''); setNotice('')
        managerOrderService.list(shop.id, pg, statusFilter || null)
            .then(d => { setData(d); setPage(pg) })
            .catch(e => setError(e.message))
            .finally(() => setBusy(false))
    }
    useEffect(() => { if (shop) load(0) }, [shop?.id, statusFilter])

    /**
     * Bước 1: Xác nhận đơn hàng (PENDING -> PROCESSING)
     */
    const confirmOrder = async orderId => {
        setBusy(true); setError(''); setNotice('')
        try {
            await managerOrderService.confirm(shop.id, orderId)
            setNotice(`Đã xác nhận đơn hàng, bắt đầu chuẩn bị hoa.`)
            load(page)
        } catch (e) { setError(e.message) } finally { setBusy(false) }
    }

    /**
     * Bước 2: Tạo vận đơn GHN & Chuyển giao hàng (PROCESSING -> DELIVERING)
     */
    const ship = async orderId => {
        if (!confirm('Xác nhận tạo mã vận đơn GHN & chuyển sang giao hàng?')) return
        setBusy(true); setError(''); setNotice('')
        try {
            const r = await managerOrderService.ship(shop.id, orderId)
            setNotice(`Đã tạo vận đơn GHN thành công: ${r.trackingCode}`)
            load(page)
        } catch (e) { setError(e.message) } finally { setBusy(false) }
    }

    /**
     * Hủy đơn hàng trước khi giao (kèm lý do bắt buộc)
     */
    const cancel = async () => {
        if (!cancelReason.trim()) {
            alert('Vui lòng nhập lý do hủy đơn')
            return
        }
        setBusy(true); setError(''); setNotice('')
        try {
            await managerOrderService.cancelOrder(shop.id, selectedOrder.id, cancelReason)
            setNotice(`Đã hủy đơn hàng ${selectedOrder.id.substring(0,8)} thành công.`)
            setCancelModalOpen(false)
            setCancelReason('')
            load(page)
        } catch (e) { setError(e.message) } finally { setBusy(false) }
    }

    /**
     * Mở modal xem thông tin chi tiết đơn hàng (sản phẩm, địa chỉ người gửi, người nhận, GHN tracking)
     */
    const viewDetail = async orderSummary => {
        setSelectedOrder(orderSummary)
        setDetailModalOpen(true)
        setOrderDetail(null)
        try {
            const detail = await managerOrderService.detail(shop.id, orderSummary.id)
            setOrderDetail(detail)
        } catch (e) {
            alert(e.message)
        }
    }

    /**
     * Đồng bộ trạng thái mới nhất từ hệ thống GHN về hệ thống FlowerShop
     */
    const refreshShippingStatus = async orderSummary => {
        setBusy(true); setError(''); setNotice('')
        try {
            const status = await managerOrderService.refreshStatus(shop.id, orderSummary.id)
            setNotice(`Cập nhật trạng thái GHN thành công. Trạng thái hiện tại: ${status}`)
            load(page)
        } catch (e) { setError(e.message) } finally { setBusy(false) }
    }

    /**
     * Giả lập GHN giao thành công (cho môi trường test/sandbox) -> chuyển sang COMPLETED
     */
    const simulateDelivered = async orderSummary => {
        if (!confirm('Bạn có muốn giả lập GHN giao thành công cho đơn hàng này?')) return
        setBusy(true); setError(''); setNotice('')
        try {
            await managerOrderService.simulateDelivered(shop.id, orderSummary.id)
            setNotice(`Đã giả lập giao thành công đơn hàng ${orderSummary.id.substring(0,8)} trên GHN. Đơn chuyển sang Hoàn thành!`)
            load(page)
        } catch (e) { setError(e.message) } finally { setBusy(false) }
    }

    /**
     * Mở modal nhập lý do hủy đơn
     */
    const openCancelModal = (orderSummary) => {
        setSelectedOrder(orderSummary)
        setCancelReason('')
        setCancelModalOpen(true)
    }

    /**
     * Mở modal cập nhật thông tin vận đơn GHN
     */
    const openUpdateGhnModal = () => {
        if (!orderDetail) return;
        setUpdatePayload({
            to_name: orderDetail.recipientName || orderDetail.customerName || '',
            to_phone: orderDetail.recipientPhone || orderDetail.customerPhone || '',
            weight: 2000,
            note: 'CHOXEMHANGKHONGTHU'
        })
        setUpdateGhnModalOpen(true)
    }

    /**
     * Xử lý gọi API cập nhật vận đơn GHN
     */
    const handleUpdateGhn = async () => {
        if (!updatePayload.to_phone) {
            alert('Vui lòng nhập số điện thoại'); return;
        }
        setBusy(true); setError(''); setNotice('');
        try {
            await managerOrderService.updateGhnOrder(shop.id, orderDetail.id, {
                to_name: updatePayload.to_name,
                to_phone: updatePayload.to_phone,
                weight: parseInt(updatePayload.weight) || 2000,
                required_note: updatePayload.note
            })
            setNotice(`Cập nhật thông tin vận đơn GHN thành công!`)
            setUpdateGhnModalOpen(false)
            setDetailModalOpen(false)
            load(page)
        } catch (e) { setError(e.message) } finally { setBusy(false) }
    }

    if (!shop) return <div className="p-8"><p className="text-gray-500">Đang tải thông tin shop…</p></div>

    return (
        <div className="space-y-6">
            <ComponentCard title={
                <div className="flex items-center gap-3">
                    Quản lý đơn hàng
                    {data?.totalElements > 0 && (
                        <span className="flex items-center justify-center rounded-full bg-brand-500 px-2 py-0.5 text-xs font-medium text-white">
                            {data.totalElements}
                        </span>
                    )}
                </div>
            }>
                {/* Tabs / Filters */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
                    <div className="flex space-x-2 border-b border-gray-200 dark:border-gray-800 overflow-x-auto whitespace-nowrap pb-1">
                        {[
                            { id: '', label: 'Tất cả' },
                            { id: 'PENDING', label: 'Chờ xác nhận' },
                            { id: 'PROCESSING', label: 'Đang chuẩn bị' },
                            { id: 'DELIVERING', label: 'Đang giao' },
                            { id: 'COMPLETED', label: 'Hoàn thành' },
                            { id: 'CANCELLED', label: 'Đã hủy' }
                        ].map(tab => (
                            <button
                                key={tab.id}
                                onClick={() => setStatusFilter(tab.id)}
                                className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${statusFilter === tab.id ? 'border-brand-500 text-brand-500' : 'border-transparent text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'}`}
                            >
                                {tab.label}
                            </button>
                        ))}
                    </div>
                    <Button size="sm" variant="outline" onClick={() => load(page)} disabled={busy}>
                        Tải lại dữ liệu
                    </Button>
                </div>

                {error && <div className="mb-4 rounded-lg bg-error-50 p-4 text-sm text-error-500">{error}</div>}
                {notice && <div className="mb-4 rounded-lg bg-success-50 p-4 text-sm text-success-600">{notice}</div>}
                
                {busy && !data && <p className="text-gray-500">Đang tải danh sách chờ…</p>}
                {data && data.content.length === 0 && (
                    <div className="flex flex-col items-center justify-center py-12 text-gray-500">
                        <svg className="h-16 w-16 text-gray-300 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
                        </svg>
                        <p>Hiện không có đơn hàng nào.</p>
                    </div>
                )}
                
                {data && data.content.length > 0 && (
                    <>
                        <div className="overflow-x-auto rounded-xl border border-gray-200 dark:border-white/5">
                            <Table>
                                <TableHeader>
                                    <TableRow>
                                        <TableCell isHeader>Mã đơn</TableCell>
                                        <TableCell isHeader>Khách hàng</TableCell>
                                        <TableCell isHeader>Tổng tiền</TableCell>
                                        <TableCell isHeader>Thời gian đặt</TableCell>
                                        <TableCell isHeader>Trạng thái</TableCell>
                                        <TableCell isHeader className="text-right">Hành động</TableCell>
                                    </TableRow>
                                </TableHeader>
                                <TableBody>
                                    {data.content.map(o => (
                                        <TableRow key={o.id} className="hover:bg-gray-50 dark:hover:bg-gray-800/50">
                                            <TableCell className="font-mono text-xs">{o.id.substring(0,8)}</TableCell>
                                            <TableCell>
                                                <div className="font-medium text-gray-900 dark:text-white">{o.customerName || 'Khách ẩn danh'}</div>
                                                <div className="text-xs text-gray-500">{o.customerPhone || ''}</div>
                                            </TableCell>
                                            <TableCell className="font-medium">{formatPrice(o.totalAmount)}</TableCell>
                                            <TableCell>
                                                <div className="text-sm font-medium text-gray-900 dark:text-white">{timeAgo(o.createdDate)}</div>
                                                <div className="text-xs text-gray-500">{new Date(o.createdDate).toLocaleString('vi-VN')}</div>
                                            </TableCell>
                                            <TableCell>
                                                <Badge color={statusColors[o.status] || 'primary'}>
                                                    {statusLabels[o.status] || o.status}
                                                </Badge>
                                            </TableCell>
                                            <TableCell className="text-right">
                                                <div className="flex items-center justify-end gap-2">
                                                    <Button 
                                                        size="sm" 
                                                        variant="outline"
                                                        onClick={() => viewDetail(o)} 
                                                        disabled={busy}
                                                    >
                                                        <EyeIcon className="w-4 h-4 mr-1 inline" />
                                                        Chi tiết
                                                    </Button>
                                                    
                                                    {o.status === 'PENDING' && (
                                                        <Button size="sm" onClick={() => confirmOrder(o.id)} disabled={busy}>
                                                            Xác nhận đơn hàng
                                                        </Button>
                                                    )}
                                                    
                                                    {o.status === 'PROCESSING' && (
                                                        <Button size="sm" className="bg-brand-600 hover:bg-brand-700 text-white" onClick={() => ship(o.id)} disabled={busy}>
                                                            Tạo mã vận đơn GHN & Giao
                                                        </Button>
                                                    )}
                                                    
                                                    {o.status === 'DELIVERING' && (
                                                        <>
                                                            <Button size="sm" variant="outline" className="text-brand-600 border-brand-200 hover:bg-brand-50" onClick={() => refreshShippingStatus(o)} disabled={busy}>
                                                                Kiểm tra GHN
                                                            </Button>
                                                            <Button size="sm" className="bg-success-600 hover:bg-success-700 text-white" onClick={() => simulateDelivered(o)} disabled={busy}>
                                                                Giả lập Giao xong
                                                            </Button>
                                                        </>
                                                    )}
                                                    
                                                    {(o.status === 'PENDING' || o.status === 'AWAITING_DEPOSIT' || o.status === 'PROCESSING') && (
                                                        <Button size="sm" variant="outline" className="text-error-500 hover:bg-error-50 hover:border-error-200" onClick={() => openCancelModal(o)} disabled={busy}>
                                                            Hủy đơn
                                                        </Button>
                                                    )}
                                                </div>
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        </div>
                        <div className="flex justify-between items-center mt-4">
                            <Button size="sm" variant="outline" disabled={busy || page === 0} onClick={() => load(page - 1)}>Trang trước</Button>
                            <span className="text-sm text-gray-500">Trang {page + 1} / {data.totalPages || 1}</span>
                            <Button size="sm" variant="outline" disabled={busy || page + 1 >= data.totalPages} onClick={() => load(page + 1)}>Trang sau</Button>
                        </div>
                    </>
                )}
            </ComponentCard>

            {/* Detail Modal */}
            <Modal isOpen={detailModalOpen} onClose={() => setDetailModalOpen(false)} className="max-w-2xl p-6">
                <h3 className="mb-4 text-lg font-semibold text-gray-900 dark:text-white">
                    Chi tiết đơn hàng {selectedOrder?.id.substring(0,8)}
                </h3>
                {!orderDetail ? (
                    <p className="text-gray-500">Đang tải chi tiết...</p>
                ) : (
                    <div className="space-y-6">
                        <div className="grid grid-cols-2 gap-4 text-sm bg-gray-50 dark:bg-gray-800/50 p-4 rounded-lg">
                            <div>
                                <p className="text-gray-500 font-medium mb-1">Địa chỉ người gửi (Shop)</p>
                                <p className="font-medium text-gray-900 dark:text-white">{shop?.name || 'Shop'}</p>
                                <p className="text-gray-600 dark:text-gray-400 mt-1 line-clamp-2">{orderDetail.shopAddress || 'Chưa cấu hình địa chỉ Shop'}</p>
                            </div>
                            <div>
                                <p className="text-gray-500 font-medium mb-1">Địa chỉ người nhận</p>
                                <p className="font-medium text-gray-900 dark:text-white">
                                    {orderDetail.recipientName || orderDetail.customerName}
                                    <span className="text-gray-500 ml-2 font-normal">{orderDetail.recipientPhone || orderDetail.customerPhone}</span>
                                </p>
                                <p className="text-gray-600 dark:text-gray-400 mt-1 line-clamp-2">{orderDetail.deliveryAddress}</p>
                            </div>
                        </div>

                        <div className="grid grid-cols-2 gap-4 text-sm px-2">
                            <div>
                                <p className="text-gray-500">Mã vận đơn (GHN):</p>
                                <p className="font-medium text-gray-900 dark:text-white">{orderDetail.delivery?.trackingCode || 'Chưa có'}</p>
                            </div>
                            <div>
                                <p className="text-gray-500">Trạng thái thanh toán:</p>
                                <p className="font-medium text-gray-900 dark:text-white">{orderDetail.payment?.status === 'SUCCESS' ? 'Đã thanh toán' : 'Chưa thanh toán'}</p>
                            </div>
                        </div>
                        
                        <div>
                            <p className="text-gray-500 mb-2 text-sm px-2">Sản phẩm</p>
                            <div className="border border-gray-200 dark:border-gray-800 rounded-lg divide-y divide-gray-200 dark:divide-gray-800">
                                {orderDetail.items.map((item, idx) => (
                                    <div key={idx} className="flex justify-between p-3 text-sm">
                                        <div>
                                            <span className="font-medium text-gray-900 dark:text-white">{item.productName}</span>
                                            <span className="text-gray-500 ml-2">x{item.quantity}</span>
                                        </div>
                                        <div className="font-medium">{formatPrice(item.price)}</div>
                                    </div>
                                ))}
                            </div>
                        </div>
                        
                        <div className="flex justify-between items-center pt-4 border-t border-gray-200 dark:border-gray-800">
                            <div>
                                <p className="text-gray-500 text-sm">Tổng cộng</p>
                                <p className="text-xl font-bold text-brand-500">{formatPrice(orderDetail.totalAmount)}</p>
                            </div>
                            <div className="space-x-3">
                                <Button variant="outline" onClick={() => setDetailModalOpen(false)}>Đóng</Button>
                                {orderDetail.status === 'PENDING' && (
                                    <Button onClick={() => { setDetailModalOpen(false); confirmOrder(orderDetail.id); }}>
                                        Xác nhận đơn hàng
                                    </Button>
                                )}
                                {orderDetail.status === 'PROCESSING' && (
                                    <Button className="bg-brand-600 hover:bg-brand-700 text-white" onClick={() => { setDetailModalOpen(false); ship(orderDetail.id); }}>
                                        Tạo mã vận đơn GHN & Giao
                                    </Button>
                                )}
                                {orderDetail.status === 'DELIVERING' && (
                                    <>
                                        <Button variant="outline" className="text-brand-600 border-brand-200 hover:bg-brand-50" onClick={openUpdateGhnModal}>
                                            Sửa vận đơn GHN
                                        </Button>
                                        <Button variant="outline" className="text-brand-600 border-brand-200 hover:bg-brand-50" onClick={() => { setDetailModalOpen(false); refreshShippingStatus(orderDetail); }}>
                                            Cập nhật trạng thái giao hàng
                                        </Button>
                                    </>
                                )}
                            </div>
                        </div>
                    </div>
                )}
            </Modal>

            {/* Cancel Modal */}
            <Modal isOpen={cancelModalOpen} onClose={() => setCancelModalOpen(false)} className="max-w-md p-6">
                <h3 className="mb-2 text-lg font-semibold text-gray-900 dark:text-white">
                    Hủy đơn hàng {selectedOrder?.id.substring(0,8)}
                </h3>
                <p className="text-sm text-gray-500 mb-4">
                    Bạn có chắc chắn muốn hủy đơn hàng này không? Hành động này không thể hoàn tác.
                </p>
                <div className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                            Lý do hủy (bắt buộc)
                        </label>
                        <textarea
                            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-700 dark:bg-gray-900"
                            rows={3}
                            placeholder="Ví dụ: Hết hoa nguyên liệu, cửa hàng tạm nghỉ..."
                            value={cancelReason}
                            onChange={(e) => setCancelReason(e.target.value)}
                        />
                    </div>
                    <div className="flex justify-end gap-3 pt-2">
                        <Button variant="outline" onClick={() => setCancelModalOpen(false)} disabled={busy}>
                            Quay lại
                        </Button>
                        <Button className="bg-error-500 hover:bg-error-600 text-white" onClick={cancel} disabled={busy || !cancelReason.trim()}>
                            {busy ? 'Đang xử lý...' : 'Xác nhận hủy'}
                        </Button>
                    </div>
                </div>
            </Modal>

            {/* Update GHN Modal */}
            <Modal isOpen={updateGhnModalOpen} onClose={() => setUpdateGhnModalOpen(false)} className="max-w-md p-6">
                <h3 className="mb-2 text-lg font-semibold text-gray-900 dark:text-white">
                    Cập nhật vận đơn GHN
                </h3>
                <p className="text-sm text-gray-500 mb-4">
                    Thay đổi thông tin giao hàng trên hệ thống GHN. Lưu ý: Không thể thay đổi COD nếu không có mã OTP từ GHN.
                </p>
                <div className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                            Tên người nhận
                        </label>
                        <input
                            type="text"
                            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-700 dark:bg-gray-900"
                            value={updatePayload.to_name}
                            onChange={(e) => setUpdatePayload({...updatePayload, to_name: e.target.value})}
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                            Số điện thoại
                        </label>
                        <input
                            type="text"
                            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-700 dark:bg-gray-900"
                            value={updatePayload.to_phone}
                            onChange={(e) => setUpdatePayload({...updatePayload, to_phone: e.target.value})}
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                            Trọng lượng (gram)
                        </label>
                        <input
                            type="number"
                            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-700 dark:bg-gray-900"
                            value={updatePayload.weight}
                            onChange={(e) => setUpdatePayload({...updatePayload, weight: e.target.value})}
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                            Ghi chú giao hàng
                        </label>
                        <select
                            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-700 dark:bg-gray-900"
                            value={updatePayload.note}
                            onChange={(e) => setUpdatePayload({...updatePayload, note: e.target.value})}
                        >
                            <option value="CHOTHUHANG">Cho thử hàng</option>
                            <option value="CHOXEMHANGKHONGTHU">Cho xem hàng không thử</option>
                            <option value="KHONGCHOXEMHANG">Không cho xem hàng</option>
                        </select>
                    </div>
                    <div className="flex justify-end gap-3 pt-2">
                        <Button variant="outline" onClick={() => setUpdateGhnModalOpen(false)} disabled={busy}>
                            Hủy
                        </Button>
                        <Button className="bg-brand-600 hover:bg-brand-700 text-white" onClick={handleUpdateGhn} disabled={busy}>
                            {busy ? 'Đang cập nhật...' : 'Cập nhật GHN'}
                        </Button>
                    </div>
                </div>
            </Modal>
        </div>
    )
}
