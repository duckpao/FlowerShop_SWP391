import { useProductCatalogController } from "../controllers/useProductCatalogController";
import { Link } from "react-router";
import PageMeta from "../components/common/PageMeta";
import PageBreadCrumb from "../components/common/PageBreadCrumb";
import Button from "../components/ui/button/Button";
import Input from "../components/form/input/InputField";

export default function ProductCatalogView() {
  const c = useProductCatalogController();

  return (
    <div className="p-4 sm:p-6 lg:p-8 w-full max-w-screen-2xl mx-auto">
      <PageMeta title="Danh mục sản phẩm | Hệ thống" description="Danh mục sản phẩm toàn hệ thống" />
      <PageBreadCrumb pageTitle="Danh mục sản phẩm" />

      <div className="mb-8 rounded-xl border border-gray-200 bg-white p-5 shadow-sm dark:border-white/5 dark:bg-white/3">
        <form onSubmit={c.search} className="flex flex-col md:flex-row gap-4 items-end">
          <div className="flex-1 w-full">
            <label className="mb-1.5 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Tìm sản phẩm
            </label>
            <Input
              maxLength={100}
              value={c.q}
              onChange={(e) => c.setQ(e.target.value)}
              placeholder="Nhập tên sản phẩm..."
            />
          </div>
          <div className="flex-1 w-full md:max-w-xs">
            <label className="mb-1.5 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Danh mục
            </label>
            <select
              value={c.categoryId}
              onChange={(e) => c.setCategoryId(e.target.value)}
              className="w-full rounded-lg border border-gray-200 bg-transparent px-4 py-3 text-sm text-gray-800 outline-none transition-colors focus:border-brand-500 dark:border-white/10 dark:text-white/90 dark:focus:border-brand-500"
            >
              <option value="">Tất cả danh mục</option>
              {c.categories.map((x) => (
                <option key={x.id} value={x.id}>
                  {x.name}
                </option>
              ))}
            </select>
          </div>
          <Button disabled={c.busy} type="submit" className="w-full md:w-auto px-8">
            Tìm kiếm
          </Button>
        </form>
      </div>

      {c.error && (
        <div className="mb-8 rounded-lg bg-error-50 p-4 text-sm text-error-500 dark:bg-error-500/10 dark:text-error-400">
          {c.error}
        </div>
      )}

      {c.data && (
        <div>
          <div className="mb-6 flex items-center justify-between">
            <p className="text-gray-600 dark:text-gray-400 font-medium">
              Tìm thấy <span className="font-bold text-gray-900 dark:text-white">{c.data.totalElements}</span> sản phẩm
            </p>
          </div>

          {c.data.content.length === 0 ? (
            <div className="rounded-xl border border-gray-200 bg-white p-12 text-center dark:border-white/5 dark:bg-white/3">
              <svg className="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
              </svg>
              <h3 className="mt-4 text-lg font-medium text-gray-900 dark:text-white">Không có sản phẩm</h3>
              <p className="mt-2 text-gray-500 dark:text-gray-400">Không tìm thấy sản phẩm nào phù hợp với điều kiện tìm kiếm.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 mb-8">
              {c.data.content.map((p) => (
                <Link
                  key={p.id}
                  to={`/products/${encodeURIComponent(p.id)}`}
                  className="group rounded-xl border border-gray-200 bg-white overflow-hidden hover:shadow-md transition-all duration-300 hover:border-brand-300 flex flex-col dark:border-white/5 dark:bg-white/3 dark:hover:border-brand-500/50"
                >
                  <div className="aspect-[4/3] bg-gray-100 dark:bg-gray-800 flex items-center justify-center relative overflow-hidden">
                    {p.images && p.images.length > 0 ? (
                      <img src={p.images[0]} alt={p.name} className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105" />
                    ) : (
                      <svg className="w-12 h-12 text-gray-300 dark:text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                      </svg>
                    )}
                  </div>
                  
                  <div className="p-5 flex-grow flex flex-col">
                    <h3 className="font-semibold text-lg text-gray-900 dark:text-white line-clamp-1 mb-1 group-hover:text-brand-500 transition-colors">
                      {p.name}
                    </h3>
                    
                    <p className="text-sm text-gray-500 dark:text-gray-400 mb-3 flex items-center gap-1">
                      <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                      </svg>
                      <span className="truncate">{p.shopName}</span>
                    </p>
                    
                    <p className="text-sm text-gray-600 dark:text-gray-300 line-clamp-2 mb-4 flex-grow">
                      {p.description}
                    </p>
                    
                    <div className="pt-4 border-t border-gray-100 dark:border-white/5 flex items-end justify-between mt-auto">
                      <div>
                        <p className="text-xs text-gray-500 dark:text-gray-400 mb-0.5">{p.categoryName}</p>
                        <p className="font-bold text-lg text-brand-500">
                          {Number(p.price).toLocaleString("vi-VN")} đ
                        </p>
                      </div>
                      <p className="text-xs font-medium text-gray-500 dark:text-gray-400 bg-gray-100 dark:bg-white/10 px-2 py-1 rounded">
                        Còn {p.stock}
                      </p>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          )}

          {c.data.totalPages > 1 && (
            <div className="flex justify-center items-center gap-4">
              <Button
                variant="outline"
                disabled={c.busy || c.page === 0}
                onClick={() => c.setPage((x) => x - 1)}
              >
                Trang trước
              </Button>
              <div className="text-sm font-medium text-gray-700 dark:text-gray-300">
                Trang {c.page + 1} / {c.data.totalPages}
              </div>
              <Button
                variant="outline"
                disabled={c.busy || c.page + 1 >= c.data.totalPages}
                onClick={() => c.setPage((x) => x + 1)}
              >
                Trang sau
              </Button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
