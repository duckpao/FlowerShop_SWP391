import { useAuth } from '../context/AuthContext'
import PageBreadCrumb from '../components/common/PageBreadCrumb'
import PageMeta from '../components/common/PageMeta'
import VoucherManager from '../components/vouchers/VoucherManager'

export default function AdminVouchersView() {
  const { user } = useAuth()
  if (user?.role !== 'ADMIN') return <p className="p-6 text-gray-500">Chỉ admin được quản lý mã giảm giá của sàn.</p>
  return (
    <>
      <PageMeta title="Mã giảm giá của sàn | Admin" description="Quản lý mã giảm giá áp dụng cho mọi shop" />
      <PageBreadCrumb pageTitle="Mã giảm giá của sàn" />
      <VoucherManager scope={{}} />
    </>
  )
}
