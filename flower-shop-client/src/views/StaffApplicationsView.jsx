import { useStaffApplicationsController } from "../controllers/useStaffApplicationsController";
import { useState } from "react";
import ComponentCard from "../components/common/ComponentCard";
import Button from "../components/ui/button/Button";
import Badge from "../components/ui/badge/Badge";

export default function StaffApplicationsView({ shop }) {
  const c = useStaffApplicationsController(shop.id);
  const [open, setOpen] = useState(
    new URLSearchParams(window.location.search).has("notifications"),
  );
  const pending = c.items.filter((a) => a.status === "PENDING").length;

  const getStatusColor = (status) => {
    switch (status) {
      case "PENDING": return "warning";
      case "APPROVED": return "success";
      case "REJECTED": return "error";
      default: return "primary";
    }
  };

  const getStatusLabel = (status) => {
    switch (status) {
      case "PENDING": return "Chờ duyệt";
      case "APPROVED": return "Đã duyệt";
      case "REJECTED": return "Đã từ chối";
      default: return status;
    }
  };

  return (
    <ComponentCard title="Thông báo tuyển dụng">
      <div className="mb-4 flex items-center justify-between">
        <Button
          variant={open ? "outline" : "primary"}
          onClick={() => setOpen(!open)}
          aria-expanded={open}
        >
          Thông báo đăng ký làm nhân viên ({pending} đơn chờ duyệt)
        </Button>
      </div>

      {c.error && (
        <div className="mb-4 rounded-lg bg-error-50 p-4 text-sm text-error-500 dark:bg-error-500/10 dark:text-error-400">
          {c.error}
        </div>
      )}
      {c.notice && (
        <div className="mb-4 rounded-lg bg-success-50 p-4 text-sm text-success-500 dark:bg-success-500/10 dark:text-success-400">
          {c.notice}
        </div>
      )}

      {open && (
        <div className="mt-6 space-y-4 border-t border-gray-100 pt-6 dark:border-white/5">
          <div className="mb-4">
            <Button disabled={c.busy} onClick={c.reload} variant="outline" size="sm">
              Tải lại đơn
            </Button>
          </div>

          {!c.busy && !c.items.length && (
            <p className="text-gray-500 dark:text-gray-400">Chưa có đơn đăng ký.</p>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {c.items.map((a) => (
              <div key={a.id} className="rounded-xl border border-gray-200 bg-white p-5 dark:border-white/5 dark:bg-white/3 flex flex-col">
                <div className="mb-3 flex justify-between items-start">
                  <h4 className="font-semibold text-gray-800 dark:text-white/90 text-lg">
                    {a.fullName}
                  </h4>
                  <Badge color={getStatusColor(a.status)}>
                    {getStatusLabel(a.status)}
                  </Badge>
                </div>
                
                <div className="mb-4 space-y-1 text-sm text-gray-500 dark:text-gray-400 flex-grow">
                  <p>Email: {a.email}</p>
                  <p>SĐT: {a.phone}</p>
                  <div className="mt-2 pt-2 border-t border-gray-100 dark:border-white/10">
                    <p className="font-medium text-gray-700 dark:text-gray-300 mb-1">Giới thiệu:</p>
                    <p className="italic text-gray-600 dark:text-gray-400">{a.introduction}</p>
                  </div>
                </div>

                {a.status === "PENDING" && (
                  <div className="mt-auto flex gap-2 pt-4">
                    <Button
                      disabled={c.busy || shop.status !== "ACTIVE"}
                      onClick={() => c.decide(a.id, true)}
                      className="flex-1 !bg-success-500 hover:!bg-success-600"
                      size="sm"
                    >
                      Duyệt
                    </Button>
                    <Button
                      disabled={c.busy || shop.status !== "ACTIVE"}
                      onClick={() => c.decide(a.id, false)}
                      variant="outline"
                      className="flex-1 !text-error-500 !border-error-500 hover:!bg-error-50 dark:hover:!bg-error-500/10"
                      size="sm"
                    >
                      Từ chối
                    </Button>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}
    </ComponentCard>
  );
}
