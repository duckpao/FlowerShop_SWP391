import { Link } from "react-router";
import AdminShopsView from "./AdminShopsView";
import AdminManagerApplicationsView from "./AdminManagerApplicationsView";
import AdminCustomersView from "./AdminCustomersView";
import AdminProfileView from "./AdminProfileView";
import AdminCategoriesView from "./AdminCategoriesView";
import ComponentCard from "../components/common/ComponentCard";
import PageMeta from "../components/common/PageMeta";
import PageBreadCrumb from "../components/common/PageBreadCrumb";

const options = [
  {
    href: "/admin/users",
    title: "Quản lý người dùng",
    icon: (
      <svg className="w-8 h-8 text-brand-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
      </svg>
    ),
    description: "Quản lý tài khoản khách hàng: tìm kiếm, xem chi tiết, khóa hoặc mở khóa.",
  },
  {
    href: "/admin/shops",
    title: "Quản lý shop",
    icon: (
      <svg className="w-8 h-8 text-brand-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
      </svg>
    ),
    description: "Xem cửa hàng, tìm kiếm, duyệt hoạt động và khóa hoặc mở khóa shop.",
  },
  {
    href: "/admin/approvals",
    title: "Quản lý đơn",
    icon: (
      <svg className="w-8 h-8 text-brand-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
      </svg>
    ),
    description: "Xem đơn đăng ký mở shop, kiểm tra thông tin và duyệt hoặc từ chối yêu cầu.",
  },
  {
    href: "/admin/categories",
    title: "Danh mục & kiểm duyệt",
    icon: (
      <svg className="w-8 h-8 text-brand-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 5h16M4 12h16M4 19h10M6 3v4m6-4v4m6-4v4" />
      </svg>
    ),
    description: "Quản lý danh mục sản phẩm và kiểm duyệt nội dung đang bán.",
  },
];

export default function AdminDashboardView({ user, logout, busy, error, path }) {
  const current = options.find((item) => item.href === path);
  const profile = path === "/admin/profile";

  return (
    <div className="p-4 sm:p-6 lg:p-8 w-full max-w-screen-2xl mx-auto">
      {error && (
        <div className="mb-6 rounded-lg bg-error-50 p-4 text-sm text-error-500 dark:bg-error-500/10 dark:text-error-400">
          {error}
        </div>
      )}
      
      {!current && !profile && (
        <>
          <PageMeta title="Tổng quan Quản trị | Hệ thống" description="Bảng điều khiển dành cho Admin" />
          <PageBreadCrumb pageTitle="Tổng quan Admin" />
          
          <div className="mb-6">
            <p className="text-gray-500 dark:text-gray-400">
              Chọn chức năng để quản lý người dùng, cửa hàng và xử lý các yêu cầu đang chờ duyệt.
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
      
      {path === "/admin/shops" && <AdminShopsView />}
      {path === "/admin/users" && <AdminCustomersView />}
      {path === "/admin/categories" && <AdminCategoriesView />}
      {profile && <AdminProfileView user={user} />}
      {path === "/admin/approvals" && (
        <>
          <PageMeta title="Quản lý đơn duyệt | Hệ thống" description="Duyệt đơn mở shop" />
          <PageBreadCrumb pageTitle="Quản lý đơn mở shop" />
          <AdminManagerApplicationsView />
        </>
      )}
    </div>
  );
}
