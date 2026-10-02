import { Link } from "react-router";
import ManagerShopsView from "./ManagerShopsView";
import AdminProfileView from "./AdminProfileView";
import ComponentCard from "../components/common/ComponentCard";
import PageMeta from "../components/common/PageMeta";
import PageBreadCrumb from "../components/common/PageBreadCrumb";

const quickActions = [
  {
    href: "/shop-admin/shop",
    title: "Quản lý shop",
    section: "shop",
    color: "bg-blue-50 text-blue-600 dark:bg-blue-500/10 dark:text-blue-400",
    icon: (
      <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
      </svg>
    ),
    description: "Cập nhật thông tin, địa chỉ và cài đặt của cửa hàng",
  },
  {
    href: "/shop-admin/staff",
    title: "Quản lý nhân sự",
    section: "staff",
    color: "bg-green-50 text-green-600 dark:bg-green-500/10 dark:text-green-400",
    icon: (
      <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
      </svg>
    ),
    description: "Xem nhân viên, thêm mới, bật/ngừng quyền làm việc.",
  },
  {
    href: "/shop-admin/products",
    title: "Quản lý mặt hàng",
    section: "products",
    color: "bg-purple-50 text-purple-600 dark:bg-purple-500/10 dark:text-purple-400",
    icon: (
      <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
      </svg>
    ),
    description: "Đăng, cập nhật, xóa và ẩn các sản phẩm của cửa hàng.",
  },
  {
    href: "#",
    title: "Nhắn tin nhanh",
    section: "chat",
    color: "bg-brand-50 text-brand-600 dark:bg-brand-500/10 dark:text-brand-400",
    icon: (
      <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
      </svg>
    ),
    description: "Phản hồi tin nhắn và hỗ trợ khách hàng trực tuyến.",
  },
];

const statCards = [
  { title: "Doanh thu tháng này", value: "24.500.000 đ", trend: "+12.5%", isPositive: true },
  { title: "Đơn hàng mới", value: "156", trend: "+5.2%", isPositive: true },
  { title: "Sản phẩm đang bán", value: "48", trend: "0%", isPositive: true },
  { title: "Phản hồi chưa đọc", value: "12", trend: "-2.4%", isPositive: false },
];

export default function ManagerDashboardView({
  user,
  logout,
  busy,
  error,
  path,
}) {
  const current = quickActions.find((item) => item.href === path && item.section !== "chat");
  const profile = path === "/shop-admin/profile";
  
  return (
    <div className="w-full">
      {error && (
        <div className="mb-6 rounded-lg bg-error-50 p-4 text-sm text-error-500 dark:bg-error-500/10 dark:text-error-400">
          {error}
        </div>
      )}
      
      {!current && !profile && (
        <>
          <PageMeta title="Tổng quan Quản lý | Hệ thống" description="Bảng điều khiển dành cho Shop Manager" />
          <PageBreadCrumb pageTitle="Tổng quan cửa hàng" />
          
          <div className="mb-8">
            <h2 className="text-xl font-bold text-gray-900 dark:text-white mb-2">Xin chào, {user?.fullName || user?.email?.split('@')[0]} 👋</h2>
            <p className="text-gray-500 dark:text-gray-400">
              Dưới đây là tóm tắt hoạt động cửa hàng của bạn trong ngày hôm nay.
            </p>
          </div>

          {/* Stats Row */}
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4 mb-8">
            {statCards.map((stat, idx) => (
              <div key={idx} className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm dark:border-gray-800 dark:bg-white/3">
                <p className="text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">{stat.title}</p>
                <div className="flex items-end justify-between">
                  <h4 className="text-2xl font-bold text-gray-900 dark:text-white">{stat.value}</h4>
                  <span className={`text-sm font-medium ${stat.isPositive ? 'text-success-600 dark:text-success-500' : 'text-error-600 dark:text-error-500'}`}>
                    {stat.trend}
                  </span>
                </div>
              </div>
            ))}
          </div>
          
          <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Lối tắt chức năng</h3>
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 xl:grid-cols-4">
            {quickActions.map((item) => (
              <ComponentCard key={item.href} className="group hover:border-brand-300 transition-colors cursor-pointer">
                <Link to={item.href} className="flex flex-col gap-4 p-2">
                  <div className={`flex h-12 w-12 items-center justify-center rounded-xl ${item.color}`}>
                    {item.icon}
                  </div>
                  <div>
                    <h4 className="font-semibold text-gray-900 dark:text-white mb-2 group-hover:text-brand-500 transition-colors">{item.title}</h4>
                    <p className="text-sm text-gray-500 dark:text-gray-400 min-h-[40px]">
                      {item.description}
                    </p>
                  </div>
                  <div className="mt-2 text-sm font-medium text-brand-500 flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                    Truy cập ngay
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                    </svg>
                  </div>
                </Link>
              </ComponentCard>
            ))}
          </div>
        </>
      )}
      
      {/* /shop-admin/products chọn section=products -> ManagerShopsView -> ProductsView với manage=true. */}
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
