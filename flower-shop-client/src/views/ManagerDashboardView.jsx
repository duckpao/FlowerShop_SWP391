import { Link } from "react-router";
import ManagerShopsView from "./ManagerShopsView";
import AdminProfileView from "./AdminProfileView";
import ComponentCard from "../components/common/ComponentCard";
import PageMeta from "../components/common/PageMeta";
import PageBreadCrumb from "../components/common/PageBreadCrumb";

const options = [
  {
    href: "/shop-admin/staff",
    title: "Quản lý tài khoản Staff",
    section: "staff",
    icon: (
      <svg className="w-8 h-8 text-brand-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
      </svg>
    ),
    description: "Xem nhân viên, bật/ngừng quyền làm việc tại shop và xử lý đơn ứng tuyển.",
  },
  {
    href: "/shop-admin/products",
    title: "Quản lý mặt hàng",
    section: "products",
    icon: (
      <svg className="w-8 h-8 text-brand-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
      </svg>
    ),
    description: "Đăng, cập nhật và ẩn các sản phẩm thuộc cửa hàng của bạn.",
  },
  {
    href: "/shop-admin/shop",
    title: "Hồ sơ shop",
    section: "shop",
    icon: (
      <svg className="w-8 h-8 text-brand-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
      </svg>
    ),
    description: "Cập nhật thông tin và địa chỉ cửa hàng bạn quản lý.",
  },
];

export default function ManagerDashboardView({
  user,
  logout,
  busy,
  error,
  path,
}) {
  const current = options.find((item) => item.href === path);
  const profile = path === "/shop-admin/profile";
  
  return (
    <div className="p-4 sm:p-6 lg:p-8 w-full max-w-screen-2xl mx-auto">
      {error && (
        <div className="mb-6 rounded-lg bg-error-50 p-4 text-sm text-error-500 dark:bg-error-500/10 dark:text-error-400">
          {error}
        </div>
      )}
      
      {!current && !profile && (
        <>
          <PageMeta title="Tổng quan Quản lý | Hệ thống" description="Bảng điều khiển dành cho Shop Manager" />
          <PageBreadCrumb pageTitle="Tổng quan cửa hàng" />
          
          <div className="mb-6">
            <p className="text-gray-500 dark:text-gray-400">
              Quản lý nhân viên và mặt hàng của cửa hàng bạn sở hữu.
            </p>
          </div>
          
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-3">
            {options.map((item) => (
              <ComponentCard key={item.href} title={item.title}>
                <div className="flex flex-col gap-4 items-center text-center p-4">
                  <div className="flex h-16 w-16 items-center justify-center rounded-full bg-brand-50 dark:bg-brand-500/10">
                    {item.icon}
                  </div>
                  <p className="text-sm text-gray-500 dark:text-gray-400 min-h-[60px]">
                    {item.description}
                  </p>
                  <Link 
                    to={item.href}
                    className="inline-flex w-full items-center justify-center rounded-lg bg-brand-500 px-4 py-2.5 text-sm font-medium text-white hover:bg-brand-600 dark:bg-brand-500 dark:hover:bg-brand-600 transition-colors"
                  >
                    Mở quản lý →
                  </Link>
                </div>
              </ComponentCard>
            ))}
          </div>
        </>
      )}
      
      {current && (
        <ManagerShopsView
          key={current.section}
          role="SHOP"
          section={current.section}
        />
      )}
      
      {profile && <AdminProfileView user={user} roleLabel="Shop Manager" />}
    </div>
  );
}
