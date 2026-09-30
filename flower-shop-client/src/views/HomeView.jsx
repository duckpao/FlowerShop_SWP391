import { useShopSearchController } from "../controllers/useShopSearchController";
import ManagerNotificationsView from "./ManagerNotificationsView";
import ManagerApplicationView from "./ManagerApplicationView";
import ProductsView from "./ProductsView";
import ProductCatalogView from "./ProductCatalogView";
import { Link } from "react-router";

const roles = {
  CUSTOMER: "Customer",
  SHOP_STAFF: "Staff",
  SHOP: "Manager",
  ADMIN: "Admin",
};

const statuses = {
  PENDING: "Chờ duyệt",
  APPROVED: "Đã duyệt",
  REJECTED: "Đã từ chối",
};

export default function HomeView({ auth }) {
  const c = useShopSearchController(auth.user);
  return (
    <div className="bg-gray-50 dark:bg-gray-900 flex-1">
      <main className="mx-auto w-full max-w-7xl py-8 space-y-12">
        
        {/* Hero Section */}
        <section className="relative overflow-hidden rounded-3xl bg-brand-50 dark:bg-brand-500/10 px-6 py-16 sm:px-12 sm:py-24 text-center border border-brand-100 dark:border-brand-500/20">
          <div className="mx-auto max-w-3xl relative z-10">
            <span className="text-brand-500 font-semibold tracking-wider uppercase text-sm mb-4 block">Hoa tươi giao tận nơi</span>
            <h1 className="text-4xl font-bold tracking-tight text-gray-900 dark:text-white sm:text-6xl mb-6">
              Mỗi đóa hoa, <br className="hidden sm:block"/> một lời gửi yêu thương
            </h1>
            <p className="text-lg leading-8 text-gray-600 dark:text-gray-300 mb-8">
              Khám phá hàng ngàn mẫu hoa đẹp từ các cửa hàng uy tín. Đặt hoa dễ dàng, giao hàng nhanh chóng, mang lại niềm vui cho những người thân yêu.
            </p>
            <div className="flex items-center justify-center gap-x-6">
              <a href="#shops" className="rounded-xl bg-brand-500 px-6 py-3.5 text-sm font-semibold text-white shadow-xs hover:bg-brand-600 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600 transition-colors">
                Tìm cửa hàng
              </a>
              <a href="#products" className="text-sm font-semibold leading-6 text-gray-900 dark:text-white hover:text-brand-500 dark:hover:text-brand-400 transition-colors">
                Khám phá sản phẩm <span aria-hidden="true">→</span>
              </a>
            </div>
          </div>
          
          {/* Decorative elements */}
          <div className="absolute top-0 left-0 -translate-x-1/2 -translate-y-1/2 w-64 h-64 bg-brand-200 dark:bg-brand-500/20 rounded-full blur-3xl opacity-50"></div>
          <div className="absolute bottom-0 right-0 translate-x-1/3 translate-y-1/3 w-96 h-96 bg-pink-200 dark:bg-pink-500/20 rounded-full blur-3xl opacity-50"></div>
        </section>

        {auth.error && (
          <div className="rounded-lg bg-error-50 dark:bg-error-500/10 p-4" role="alert">
            <p className="text-sm text-error-600 dark:text-error-500">{auth.error}</p>
          </div>
        )}
        
        {auth.user?.role === "SHOP" && <ManagerNotificationsView />}
        
        {auth.user && ["CUSTOMER", "SHOP"].includes(auth.user.role) && (
          <ManagerApplicationView key={auth.user.id} user={auth.user} />
        )}
        
        {c.sessionExpired && (
          <div className="rounded-lg bg-warning-50 dark:bg-warning-500/10 p-4" role="alert">
            <p className="text-sm text-warning-800 dark:text-warning-500">
              Phiên đăng nhập đã thay đổi hoặc hết hạn.{" "}
              <a href="/login" className="font-medium underline hover:text-warning-900 dark:hover:text-warning-400">
                Đăng nhập lại để xem quyền và kết quả ứng tuyển mới nhất.
              </a>
            </p>
          </div>
        )}

        {!c.selected && <ProductCatalogView />}
        
        {!c.selected && (
          <section className="space-y-6">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
              <div>
                <h2 className="text-2xl font-bold text-gray-900 dark:text-white">Tìm cửa hàng hoa</h2>
                <p className="text-gray-500 dark:text-gray-400 mt-1">
                  Hiển thị tối đa 50 shop đang hoạt động. Nhập tên để thu hẹp kết quả.
                </p>
              </div>
              
              <form onSubmit={c.search} className="flex gap-2">
                <input
                  type="search"
                  placeholder="Nhập tên shop..."
                  className="rounded-lg border border-gray-300 px-4 py-2 text-gray-900 focus:border-brand-500 focus:ring-1 focus:ring-brand-500 dark:border-white/10 dark:bg-gray-800 dark:text-white min-w-[250px]"
                  maxLength={100}
                  value={c.query}
                  onChange={(e) => c.setQuery(e.target.value)}
                />
                <button 
                  type="submit" 
                  disabled={c.busy}
                  className="px-4 py-2 bg-brand-500 hover:bg-brand-600 text-white rounded-lg transition-colors font-medium disabled:opacity-50"
                >
                  Tìm kiếm
                </button>
              </form>
            </div>

            {c.error && (
              <div className="rounded-lg bg-error-50 dark:bg-error-500/10 p-4" role="alert">
                <p className="text-sm text-error-600 dark:text-error-500">{c.error}</p>
              </div>
            )}
            {c.notice && (
              <div className="rounded-lg bg-success-50 dark:bg-success-500/10 p-4" role="status">
                <p className="text-sm text-success-600 dark:text-success-500">{c.notice}</p>
              </div>
            )}

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
              {c.shops.map((shop) => (
                <div key={shop.id} className="rounded-xl border border-gray-200 bg-white dark:border-white/5 dark:bg-white/5 p-6 flex flex-col hover:shadow-md transition-shadow">
                  <h3 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">{shop.name}</h3>
                  <p className="text-gray-600 dark:text-gray-400 text-sm flex-1 mb-6 line-clamp-3">
                    {shop.description || "Chưa có mô tả."}
                  </p>
                  <button 
                    onClick={() => c.select(shop)}
                    className="w-full py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-800 dark:bg-white/10 dark:hover:bg-white/20 dark:text-white rounded-lg font-medium transition-colors"
                  >
                    Vào shop
                  </button>
                </div>
              ))}
              {c.shops.length === 0 && !c.busy && (
                <div className="col-span-full text-center py-12 text-gray-500 dark:text-gray-400">
                  Không tìm thấy shop nào.
                </div>
              )}
            </div>
          </section>
        )}

        {c.selected && (
          <section className="rounded-xl border border-gray-200 bg-white dark:border-white/5 dark:bg-white/5 p-6 md:p-8 space-y-8">
            <div>
              <button 
                onClick={() => c.select(null)}
                className="text-sm font-medium text-brand-500 hover:text-brand-600 flex items-center gap-1 mb-6"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m15 18-6-6 6-6"/></svg>
                Quay lại danh sách shop
              </button>
              
              <div className="border-b border-gray-100 dark:border-white/5 pb-6 mb-6">
                <h2 className="text-3xl font-bold text-gray-900 dark:text-white flex items-center gap-2">
                  <span className="text-brand-500">✿</span> {c.selected.name}
                </h2>
                <p className="text-gray-600 dark:text-gray-400 mt-2 text-lg">
                  {c.selected.description || "Chào mừng bạn đến với cửa hàng."}
                </p>
              </div>
            </div>

            <div className="space-y-4">
              <h3 className="text-xl font-semibold text-gray-900 dark:text-white">Giới thiệu cửa hàng</h3>
              <p className="text-gray-600 dark:text-gray-400">
                Khám phá cửa hàng và cơ hội trở thành thành viên của đội ngũ.
              </p>
              
              <div className="mt-8">
                <ProductsView key={c.selected.id} shop={c.selected} />
              </div>
            </div>

            <div className="pt-8 border-t border-gray-100 dark:border-white/5 space-y-6">
              <div>
                <h3 className="text-xl font-semibold text-gray-900 dark:text-white">Tuyển dụng</h3>
                <p className="text-gray-600 dark:text-gray-400 mt-1">
                  Bạn yêu thích hoa và muốn làm việc tại <span className="font-medium text-gray-900 dark:text-white">{c.selected.name}</span>? Hãy gửi
                  thông tin để Manager xem xét.
                </p>
              </div>

              {c.error && (
                <div className="rounded-lg bg-error-50 dark:bg-error-500/10 p-4" role="alert">
                  <p className="text-sm text-error-600 dark:text-error-500">{c.error}</p>
                </div>
              )}
              {c.notice && (
                <div className="rounded-lg bg-success-50 dark:bg-success-500/10 p-4" role="status">
                  <p className="text-sm text-success-600 dark:text-success-500">{c.notice}</p>
                </div>
              )}

              {auth.user?.role === "CUSTOMER" ? (
                <div className="bg-gray-50 dark:bg-gray-800/50 rounded-xl p-6">
                  <button
                    aria-expanded={c.showApplication}
                    onClick={() => c.setShowApplication(!c.showApplication)}
                    className="px-4 py-2 bg-brand-500 hover:bg-brand-600 text-white rounded-lg transition-colors font-medium"
                  >
                    {c.showApplication ? "Đóng form" : "Đăng ký làm nhân viên"}
                  </button>
                  
                  {c.showApplication && (
                    <div className="mt-6 space-y-6">
                      <h3 className="text-lg font-medium text-gray-900 dark:text-white">Thông tin ứng tuyển</h3>
                      <form onSubmit={c.submit} className="space-y-4 max-w-2xl">
                        <fieldset disabled={c.busy} className="space-y-4">
                          <div>
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                              Họ và tên
                            </label>
                            <input
                              className="block w-full rounded-lg border border-gray-300 px-4 py-2.5 text-gray-900 focus:border-brand-500 focus:ring-1 focus:ring-brand-500 dark:border-white/10 dark:bg-gray-800 dark:text-white"
                              required
                              maxLength={100}
                              value={c.form.fullName}
                              onChange={(e) =>
                                c.setForm({ ...c.form, fullName: e.target.value })
                              }
                            />
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                              Số điện thoại
                            </label>
                            <input
                              className="block w-full rounded-lg border border-gray-300 px-4 py-2.5 text-gray-900 focus:border-brand-500 focus:ring-1 focus:ring-brand-500 dark:border-white/10 dark:bg-gray-800 dark:text-white"
                              required
                              pattern="[+0-9 ()\-]{8,20}"
                              maxLength={20}
                              value={c.form.phone}
                              onChange={(e) =>
                                c.setForm({ ...c.form, phone: e.target.value })
                              }
                            />
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                              Giới thiệu / kinh nghiệm
                            </label>
                            <textarea
                              className="block w-full rounded-lg border border-gray-300 px-4 py-2.5 text-gray-900 focus:border-brand-500 focus:ring-1 focus:ring-brand-500 dark:border-white/10 dark:bg-gray-800 dark:text-white"
                              required
                              rows={4}
                              maxLength={1000}
                              value={c.form.introduction}
                              onChange={(e) =>
                                c.setForm({
                                  ...c.form,
                                  introduction: e.target.value,
                                })
                              }
                            />
                          </div>
                          <div className="bg-blue-50 dark:bg-blue-900/20 p-4 rounded-lg">
                            <p className="text-sm text-blue-800 dark:text-blue-300">
                              Khi Manager duyệt, tài khoản của bạn sẽ chuyển từ
                              Customer sang Staff. Bạn cần đăng nhập lại để sử dụng
                              quyền mới.
                            </p>
                          </div>
                          <button className="px-6 py-2.5 bg-brand-500 hover:bg-brand-600 text-white rounded-lg transition-colors font-medium">
                            Gửi đơn đăng ký
                          </button>
                        </fieldset>
                      </form>
                    </div>
                  )}
                </div>
              ) : !auth.user ? (
                <div className="bg-gray-50 dark:bg-gray-800/50 rounded-xl p-6 text-center">
                  <a
                    href={`/login?next=${encodeURIComponent(window.location.pathname)}`}
                    className="inline-block px-6 py-2.5 bg-brand-500 hover:bg-brand-600 text-white rounded-lg transition-colors font-medium"
                  >
                    Đăng nhập để đăng ký làm nhân viên
                  </a>
                </div>
              ) : (
                <div className="bg-gray-50 dark:bg-gray-800/50 rounded-xl p-4">
                  <p className="text-gray-600 dark:text-gray-400">Chỉ Customer được gửi đơn xin làm nhân viên.</p>
                </div>
              )}
            </div>
          </section>
        )}

        {auth.user && c.applications.length > 0 && (
          <details className="rounded-xl border border-gray-200 bg-white dark:border-white/5 dark:bg-white/5 overflow-hidden group">
            <summary className="px-6 py-4 cursor-pointer font-medium text-gray-900 dark:text-white bg-gray-50 dark:bg-white/5 hover:bg-gray-100 dark:hover:bg-white/10 transition-colors list-none flex justify-between items-center">
              <span>Thông báo ứng tuyển của tôi ({c.applications.length})</span>
              <svg className="w-5 h-5 text-gray-500 group-open:rotate-180 transition-transform" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
              </svg>
            </summary>
            <div className="p-6 border-t border-gray-200 dark:border-white/5">
              <ul className="space-y-4">
                {c.applications.map((a) => (
                  <li key={a.id} className="p-4 rounded-lg bg-gray-50 dark:bg-gray-800/50 border border-gray-100 dark:border-white/5">
                    <div className="flex items-center gap-3 mb-2">
                      <strong className="text-gray-900 dark:text-white text-lg">{a.shopName}</strong>
                      <span className={`px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        a.status === 'APPROVED' ? 'bg-success-100 text-success-800 dark:bg-success-500/20 dark:text-success-400' :
                        a.status === 'REJECTED' ? 'bg-error-100 text-error-800 dark:bg-error-500/20 dark:text-error-400' :
                        'bg-warning-100 text-warning-800 dark:bg-warning-500/20 dark:text-warning-400'
                      }`}>
                        {statuses[a.status]}
                      </span>
                    </div>
                    <p className="text-gray-600 dark:text-gray-400 text-sm">
                      {a.status === "APPROVED"
                        ? "Chúc mừng! Bạn đã trở thành nhân viên của shop. Đăng nhập lại để sử dụng quyền Staff."
                        : a.status === "REJECTED"
                          ? "Cảm ơn bạn đã ứng tuyển. Shop chưa thể tiếp nhận bạn ở thời điểm này. Tài khoản của bạn vẫn là Customer."
                          : "Đơn đã được gửi đến Manager và đang chờ xem xét."}
                    </p>
                  </li>
                ))}
              </ul>
            </div>
          </details>
        )}
      </main>
    </div>
  );
}
