import { useAdminManagerApplicationsController } from "../controllers/useAdminManagerApplicationsController";
import ApplicationDetailDialog from "./ApplicationDetailDialog";
import { Table, TableBody, TableCell, TableHeader, TableRow } from "../components/ui/table";
import Badge from "../components/ui/badge/Badge";
import Button from "../components/ui/button/Button";
import Select from "../components/form/Select";

const labels = {
  PENDING: "Chờ duyệt",
  APPROVED: "Đã duyệt",
  REJECTED: "Đã từ chối",
};

const statusColors = {
  PENDING: "warning",
  APPROVED: "success",
  REJECTED: "error",
};

export default function AdminManagerApplicationsView() {
  const c = useAdminManagerApplicationsController();
  const selected = c.data?.content.find((a) => a.id === c.selectedId);
  
  return (
    <div className="space-y-6">
      <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm dark:border-white/5 dark:bg-white/3">
        <div className="flex flex-col sm:flex-row gap-4 items-end justify-between">
          <div className="w-full sm:w-1/3 md:w-1/4">
            <Select
              label="Trạng thái"
              options={[
                { value: "PENDING", label: "Chờ duyệt" },
                { value: "APPROVED", label: "Đã duyệt" },
                { value: "REJECTED", label: "Đã từ chối" }
              ]}
              value={c.status}
              onChange={(e) => c.filter(e.target.value)}
              disabled={c.busy}
            />
          </div>
          <div className="w-full sm:w-auto">
            <Button size="sm" variant="outline" onClick={c.reload} disabled={c.busy}>
              <svg className="mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
              Tải lại
            </Button>
          </div>
        </div>
        
        {c.busy && <div className="mt-4 text-brand-500">Đang tải hoặc xử lý...</div>}
        {c.error && (
          <div className="mt-4 rounded-lg bg-error-50 p-4 text-sm text-error-500 dark:bg-error-500/10 dark:text-error-400">
            {c.error}
          </div>
        )}
        {c.notice && (
          <div className="mt-4 rounded-lg bg-success-50 p-4 text-sm text-success-500 dark:bg-success-500/10 dark:text-success-400">
            {c.notice}
          </div>
        )}
      </div>

      {c.data && (
        <div className="overflow-hidden rounded-xl border border-gray-200 bg-white dark:border-white/5 dark:bg-white/3">
          <div className="px-6 py-4 border-b border-gray-100 dark:border-white/5 flex justify-between items-center">
            <h3 className="font-semibold text-gray-800 dark:text-white/90">
              Danh sách ({c.data.totalElements} đơn {labels[c.status].toLowerCase()})
            </h3>
          </div>
          
          <div className="max-w-full overflow-x-auto">
            <Table>
              <TableHeader className="border-b border-gray-100 dark:border-white/5 bg-gray-50 dark:bg-white/5">
                <TableRow>
                  <TableCell isHeader className="px-5 py-3 font-medium text-gray-500 dark:text-gray-400 text-start">Tên Shop Đăng ký</TableCell>
                  <TableCell isHeader className="px-5 py-3 font-medium text-gray-500 dark:text-gray-400 text-start">Người đăng ký</TableCell>
                  <TableCell isHeader className="px-5 py-3 font-medium text-gray-500 dark:text-gray-400 text-start">Ngày gửi</TableCell>
                  <TableCell isHeader className="px-5 py-3 font-medium text-gray-500 dark:text-gray-400 text-start">Trạng thái</TableCell>
                  <TableCell isHeader className="px-5 py-3 font-medium text-gray-500 dark:text-gray-400 text-end">Hành động</TableCell>
                </TableRow>
              </TableHeader>
              <TableBody className="divide-y divide-gray-100 dark:divide-white/5">
                {!c.data.content.length ? (
                  <TableRow>
                    <TableCell colSpan={5} className="px-5 py-8 text-center text-gray-500 dark:text-gray-400">
                      Không có đơn trong trạng thái này.
                    </TableCell>
                  </TableRow>
                ) : (
                  c.data.content.map(a => (
                    <TableRow key={a.id}>
                      <TableCell className="px-5 py-4 text-start font-medium text-gray-800 dark:text-white/90">
                        {a.shopName}
                      </TableCell>
                      <TableCell className="px-5 py-4 text-start text-gray-500 dark:text-gray-400">
                        {a.fullName}
                      </TableCell>
                      <TableCell className="px-5 py-4 text-start text-gray-500 dark:text-gray-400">
                        {new Date(a.submittedAt).toLocaleString("vi-VN")}
                      </TableCell>
                      <TableCell className="px-5 py-4 text-start">
                        <Badge size="sm" color={statusColors[a.status] || 'primary'}>
                          {labels[a.status] || a.status}
                        </Badge>
                      </TableCell>
                      <TableCell className="px-5 py-4 text-end">
                        <Button size="sm" variant="outline" onClick={() => c.setSelectedId(a.id)} disabled={c.busy}>
                          Chi tiết
                        </Button>
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
              <Button size="sm" variant="outline" disabled={c.busy || c.data.page === 0} onClick={() => c.setPage(p => p - 1)}>
                Trang trước
              </Button>
              <span className="text-sm text-gray-500 dark:text-gray-400">
                Trang {c.data.page + 1} / {Math.max(1, c.data.totalPages)}
              </span>
              <Button size="sm" variant="outline" disabled={c.busy || c.data.page + 1 >= c.data.totalPages} onClick={() => c.setPage(p => p + 1)}>
                Trang sau
              </Button>
            </div>
          )}
        </div>
      )}

      {selected && (
        <ApplicationDetailDialog
          key={selected.id}
          application={selected}
          controller={c}
        />
      )}
    </div>
  );
}
