import AdminManagerApplicationsView from './AdminManagerApplicationsView'
import PageMeta from '../components/common/PageMeta'
import PageBreadCrumb from '../components/common/PageBreadCrumb'

export default function AdminApprovalsView({ user, logout, busy, error }) {
  return (
    <div className="p-4 sm:p-6 lg:p-8 w-full max-w-screen-2xl mx-auto">
      <PageMeta title="Duyệt yêu cầu | Admin" description="Duyệt yêu cầu đăng ký mở shop từ khách hàng" />
      <PageBreadCrumb pageTitle="Duyệt yêu cầu đăng ký Shop" />

      {error && (
        <div className="mb-6 rounded-lg bg-error-50 p-4 text-sm text-error-500 dark:bg-error-500/10 dark:text-error-400">
          {error}
        </div>
      )}

      <div className="mb-6 rounded-xl border border-gray-200 bg-white p-5 dark:border-white/5 dark:bg-white/3">
        <p className="text-sm text-gray-500 dark:text-gray-400 mb-2">
          Xem đơn Customer đăng ký mở shop. Duyệt đơn sẽ tạo shop và cấp quyền Manager; từ chối giữ nguyên quyền Customer.
        </p>
        <p className="text-sm text-gray-500 dark:text-gray-400">
          * Đơn xin làm nhân viên do Manager của từng shop xử lý, không nằm trong danh sách duyệt của Admin.
        </p>
      </div>

      <AdminManagerApplicationsView />
    </div>
  )
}
