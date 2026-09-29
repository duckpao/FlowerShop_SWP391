import { useAdminShopsController } from "../controllers/useAdminShopsController";
import { shopStatusLabels as labels, shopActions } from "../models/shopModel";
import PageBreadCrumb from "../components/common/PageBreadCrumb";
import PageMeta from "../components/common/PageMeta";
import { Table, TableBody, TableCell, TableHeader, TableRow } from "../components/ui/table";
import Badge from "../components/ui/badge/Badge";
import Button from "../components/ui/button/Button";
import Select from "../components/form/Select";
import Input from "../components/form/input/InputField";

const statusColors = { 
  ACTIVE: 'success', 
  INACTIVE: 'warning',
  BANNED: 'error',
  PENDING: 'warning',
  REJECTED: 'error'
};

export default function AdminShopsView() {
  const c = useAdminShopsController();
  
  return (
    <>
      <PageMeta title="Quản lý cửa hàng | Admin" description="Quản lý cửa hàng hệ thống" />
      <PageBreadCrumb pageTitle="Quản lý cửa hàng" />
      
      <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm dark:border-white/5 dark:bg-white/3 mb-6">
        <form onSubmit={c.search} className="flex flex-col sm:flex-row gap-4 items-end">
          <div className="w-full sm:w-1/2 md:w-1/3">
            <Input
              type="text"
              label="Tìm theo tên shop"
              value={c.q}
              onChange={(e) => c.setQ(e.target.value)}
              placeholder="Nhập tên shop..."
              disabled={c.busy}
            />
          </div>
          <div className="w-full sm:w-1/3 md:w-1/4">
            <Select
              label="Trạng thái"
              options={[
                { value: "", label: "Tất cả" },
                ...Object.entries(labels).map(([value, label]) => ({ value, label }))
              ]}
              value={c.status}
              onChange={(e) => c.setStatus(e.target.value)}
              disabled={c.busy}
            />
          </div>
          <div className="w-full sm:w-auto">
            <Button size="sm" type="submit" disabled={c.busy}>
              Tìm kiếm
            </Button>
          </div>
        </form>
        
        {c.busy && <div className="mt-4 text-brand-500">Đang tải...</div>}
        {c.error && (
          <div className="mt-4 flex items-center justify-between rounded-lg bg-error-50 p-4 text-sm text-error-500 dark:bg-error-500/10 dark:text-error-400">
            <p>{c.error}</p>
            <Button size="sm" variant="outline" onClick={c.retry} disabled={c.busy}>Tải lại</Button>
          </div>
        )}
        {c.notice && <div className="mt-4 rounded-lg bg-success-50 p-4 text-sm text-success-500 dark:bg-success-500/10 dark:text-success-400">{c.notice}</div>}
      </div>

      {c.data && (
        <div className="overflow-hidden rounded-xl border border-gray-200 bg-white dark:border-white/5 dark:bg-white/3">
          <div className="px-6 py-4 border-b border-gray-100 dark:border-white/5 flex justify-between items-center">
            <h3 className="font-semibold text-gray-800 dark:text-white/90">
              Danh sách ({c.data.totalElements} cửa hàng)
            </h3>
          </div>
          
          <div className="max-w-full overflow-x-auto">
            <Table>
              <TableHeader className="border-b border-gray-100 dark:border-white/5 bg-gray-50 dark:bg-white/5">
                <TableRow>
                  <TableCell isHeader className="px-5 py-3 font-medium text-gray-500 dark:text-gray-400 text-start">Tên Shop</TableCell>
                  <TableCell isHeader className="px-5 py-3 font-medium text-gray-500 dark:text-gray-400 text-start">Chủ cửa hàng</TableCell>
                  <TableCell isHeader className="px-5 py-3 font-medium text-gray-500 dark:text-gray-400 text-start">Trạng thái</TableCell>
                  <TableCell isHeader className="px-5 py-3 font-medium text-gray-500 dark:text-gray-400 text-end">Hành động</TableCell>
                </TableRow>
              </TableHeader>
              <TableBody className="divide-y divide-gray-100 dark:divide-white/5">
                {!c.data.content.length ? (
                  <TableRow>
                    <TableCell colSpan={4} className="px-5 py-6 text-center text-gray-500 dark:text-gray-400">
                      Không có cửa hàng phù hợp.
                    </TableCell>
                  </TableRow>
                ) : (
                  c.data.content.map(shop => (
                    <TableRow key={shop.id}>
                      <TableCell className="px-5 py-4 text-start font-medium text-gray-800 dark:text-white/90">
                        {shop.name}
                      </TableCell>
                      <TableCell className="px-5 py-4 text-start text-gray-500 dark:text-gray-400">
                        {shop.owner?.email || '—'}
                      </TableCell>
                      <TableCell className="px-5 py-4 text-start">
                        <Badge size="sm" color={statusColors[shop.status] || 'primary'}>
                          {labels[shop.status] || shop.status}
                        </Badge>
                      </TableCell>
                      <TableCell className="px-5 py-4 text-end">
                        <div className="flex items-center justify-end gap-2">
                          <Button size="sm" variant="outline" onClick={() => c.detail(shop.id)} disabled={c.busy}>
                            Chi tiết
                          </Button>
                          {shopActions[shop.status] && (
                            <Button 
                              size="sm" 
                              variant="outline"
                              onClick={() => c.transition(shop)} 
                              disabled={c.busy}
                            >
                              {shopActions[shop.status][1]}
                            </Button>
                          )}
                        </div>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>
          
          {/* Pagination */}
          {c.data.totalPages > 1 && (
            <div className="px-6 py-4 border-t border-gray-100 dark:border-white/5 flex items-center justify-between">
              <Button size="sm" variant="outline" disabled={c.busy || c.data.page === 0} onClick={() => c.next(-1)}>
                Trang trước
              </Button>
              <span className="text-sm text-gray-500 dark:text-gray-400">
                Trang {c.data.page + 1} / {Math.max(1, c.data.totalPages)}
              </span>
              <Button size="sm" variant="outline" disabled={c.busy || c.data.page + 1 >= c.data.totalPages} onClick={() => c.next(1)}>
                Trang sau
              </Button>
            </div>
          )}
        </div>
      )}

      {/* Detail Modal Overlay */}
      {c.selected && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-gray-900/50 p-4 backdrop-blur-sm">
          <div className="w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-2xl bg-white p-6 shadow-xl dark:bg-gray-800">
            <h3 className="mb-4 text-xl font-semibold text-gray-900 dark:text-white">{c.selected.name}</h3>
            
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
              <div className="space-y-3">
                <h4 className="font-medium text-brand-500 dark:text-brand-400 border-b border-gray-100 pb-2 dark:border-white/10">Thông tin cửa hàng</h4>
                <div className="flex justify-between">
                  <span className="text-gray-500 dark:text-gray-400">Mô tả:</span>
                  <span className="font-medium text-gray-900 dark:text-white text-right ml-4">{c.selected.description || "Chưa có mô tả"}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500 dark:text-gray-400">Trạng thái:</span>
                  <Badge size="sm" color={statusColors[c.selected.status] || 'primary'}>
                    {labels[c.selected.status] || c.selected.status}
                  </Badge>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500 dark:text-gray-400">Ngày tạo:</span>
                  <span className="font-medium text-gray-900 dark:text-white">
                    {c.selected.createdAt ? new Date(c.selected.createdAt).toLocaleString("vi-VN") : "Chưa có"}
                  </span>
                </div>
              </div>

              <div className="space-y-3">
                <h4 className="font-medium text-brand-500 dark:text-brand-400 border-b border-gray-100 pb-2 dark:border-white/10">Chủ cửa hàng</h4>
                <div className="flex justify-between">
                  <span className="text-gray-500 dark:text-gray-400">Họ tên:</span>
                  <span className="font-medium text-gray-900 dark:text-white">{c.selected.owner?.fullName || '—'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500 dark:text-gray-400">Email:</span>
                  <span className="font-medium text-gray-900 dark:text-white">{c.selected.owner?.email || '—'}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500 dark:text-gray-400">Điện thoại:</span>
                  <span className="font-medium text-gray-900 dark:text-white">{c.selected.owner?.phone || "Chưa cập nhật"}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500 dark:text-gray-400">Tài khoản:</span>
                  <span className="font-medium text-gray-900 dark:text-white">{c.selected.owner?.role} - {c.selected.owner?.status}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-500 dark:text-gray-400">Xác thực:</span>
                  <span className="font-medium text-gray-900 dark:text-white">
                    {c.selected.owner?.emailVerified ? "Đã xác thực" : "Chưa xác thực"}
                  </span>
                </div>
              </div>
            </div>
            
            <div className="flex justify-end gap-3 pt-4 border-t border-gray-100 dark:border-white/10">
              {shopActions[c.selected.status] && (
                <Button 
                  size="sm" 
                  variant="outline"
                  onClick={() => {
                    c.transition(c.selected);
                    c.setSelected(null);
                  }} 
                >
                  {shopActions[c.selected.status][1]}
                </Button>
              )}
              <Button size="sm" onClick={() => c.setSelected(null)}>
                Đóng
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
