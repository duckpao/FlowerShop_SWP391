import { useEffect, useRef } from "react";
import { createPortal } from "react-dom";
import Badge from "../components/ui/badge/Badge";
import Button from "../components/ui/button/Button";
import TextArea from "../components/form/input/TextArea";

const statusLabels = {
  PENDING: "Chờ duyệt",
  APPROVED: "Đã duyệt",
  REJECTED: "Đã từ chối",
};

const statusColors = {
  PENDING: "warning",
  APPROVED: "success",
  REJECTED: "error",
};

export default function ApplicationDetailDialog({
  application: a,
  controller: c,
}) {
  const ref = useRef(null);
  
  useEffect(() => {
    const previousFocus = document.activeElement;
    const overflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    
    // Auto focus the close button for accessibility
    if (ref.current) {
      ref.current.focus();
    }
    
    return () => {
      document.body.style.overflow = overflow;
      if (previousFocus?.isConnected) previousFocus.focus();
    };
  }, []);
  
  const close = () => {
    if (!c.busy) c.setSelectedId(null);
  };
  
  return createPortal(
    <div 
      className="fixed inset-0 z-[100000] flex items-center justify-center bg-gray-900/50 p-4 backdrop-blur-sm transition-opacity"
      role="dialog"
      aria-labelledby="application-dialog-title"
      aria-modal="true"
      onClick={(e) => {
        if (e.target === e.currentTarget) close();
      }}
    >
      <div className="w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-2xl bg-white p-6 shadow-xl dark:bg-gray-800 flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-gray-100 pb-4 dark:border-white/10 mb-4">
          <div>
            <h2 id="application-dialog-title" className="text-xl font-semibold text-gray-900 dark:text-white">
              Chi tiết đơn đăng ký mở shop
            </h2>
            <p className="text-brand-500 dark:text-brand-400 mt-1 font-medium">{a.shopName}</p>
          </div>
          <button
            ref={ref}
            type="button"
            className="text-gray-400 hover:text-gray-500 focus:outline-none p-2 rounded-full hover:bg-gray-100 dark:hover:bg-white/5 transition-colors"
            disabled={c.busy}
            onClick={close}
            aria-label="Đóng chi tiết đơn"
          >
            <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto mb-6 pr-2 space-y-6">
          {c.error && (
            <div className="rounded-lg bg-error-50 p-4 text-sm text-error-500 dark:bg-error-500/10 dark:text-error-400">
              {c.error}
            </div>
          )}
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-4">
              <h4 className="font-medium text-gray-900 dark:text-white border-b border-gray-100 pb-2 dark:border-white/10">Thông tin chung</h4>
              <div className="flex justify-between">
                <span className="text-gray-500 dark:text-gray-400 text-sm">Trạng thái:</span>
                <Badge size="sm" color={statusColors[a.status] || 'primary'}>
                  {statusLabels[a.status] || a.status}
                </Badge>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500 dark:text-gray-400 text-sm">Mã đơn:</span>
                <span className="font-medium text-gray-900 dark:text-white text-sm">{a.id}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500 dark:text-gray-400 text-sm">Ngày gửi:</span>
                <span className="font-medium text-gray-900 dark:text-white text-sm">
                  {new Date(a.submittedAt).toLocaleString("vi-VN")}
                </span>
              </div>
            </div>

            <div className="space-y-4">
              <h4 className="font-medium text-gray-900 dark:text-white border-b border-gray-100 pb-2 dark:border-white/10">Người đăng ký</h4>
              <div className="flex justify-between">
                <span className="text-gray-500 dark:text-gray-400 text-sm">Họ và tên:</span>
                <span className="font-medium text-gray-900 dark:text-white text-sm">{a.fullName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500 dark:text-gray-400 text-sm">Email:</span>
                <span className="font-medium text-gray-900 dark:text-white text-sm">{a.email}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-500 dark:text-gray-400 text-sm">Số điện thoại:</span>
                <span className="font-medium text-gray-900 dark:text-white text-sm">{a.phone}</span>
              </div>
            </div>
          </div>

          <div className="space-y-4">
            <h4 className="font-medium text-gray-900 dark:text-white border-b border-gray-100 pb-2 dark:border-white/10">Thông tin cửa hàng</h4>
            <div>
              <span className="text-gray-500 dark:text-gray-400 text-sm block mb-1">Mô tả shop:</span>
              <p className="text-gray-900 dark:text-white text-sm bg-gray-50 p-3 rounded-lg dark:bg-white/5 border border-gray-100 dark:border-white/5">{a.description}</p>
            </div>
            <div>
              <span className="text-gray-500 dark:text-gray-400 text-sm block mb-1">Địa chỉ:</span>
              <p className="text-gray-900 dark:text-white text-sm">{[a.addressLine, a.ward, a.district, a.city].join(", ")}</p>
            </div>
          </div>

          {a.reviewNote && (
            <div className="space-y-2">
              <h4 className="font-medium text-gray-900 dark:text-white">Phản hồi của Admin:</h4>
              <p className="text-gray-900 dark:text-white text-sm bg-gray-50 p-3 rounded-lg dark:bg-white/5 border border-gray-100 dark:border-white/5">{a.reviewNote}</p>
            </div>
          )}
          
          {a.status === "PENDING" && (
            <div className="mt-4">
              <TextArea
                label="Ghi chú phản hồi (Tùy chọn)"
                placeholder="Nhập lý do từ chối hoặc ghi chú duyệt..."
                disabled={c.busy}
                rows={3}
                value={c.notes[a.id] || ""}
                onChange={(e) => c.setNotes({ ...c.notes, [a.id]: e.target.value })}
              />
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex justify-end gap-3 pt-4 border-t border-gray-100 dark:border-white/10 mt-auto">
          <Button size="sm" variant="outline" disabled={c.busy} onClick={close}>
            Đóng
          </Button>
          
          {a.status === "PENDING" && (
            <>
              <Button 
                size="sm" 
                variant="outline" 
                className="text-error-500 hover:bg-error-50 hover:border-error-500"
                disabled={c.busy} 
                onClick={() => c.decide(a.id, false)}
              >
                Từ chối
              </Button>
              <Button
                size="sm"
                className="bg-brand-500 hover:bg-brand-600 text-white"
                disabled={c.busy}
                onClick={() => c.decide(a.id, true)}
              >
                {c.busy ? "Đang xử lý..." : "Duyệt và tạo shop"}
              </Button>
            </>
          )}
        </div>
      </div>
    </div>,
    document.body
  );
}
