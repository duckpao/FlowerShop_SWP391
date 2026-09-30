import { useManagerApplicationController } from "../controllers/useManagerApplicationController";
import ComponentCard from "../components/common/ComponentCard";
import Button from "../components/ui/button/Button";
import Input from "../components/form/input/InputField";
import TextArea from "../components/form/input/TextArea";
import Label from "../components/form/Label";
import Badge from "../components/ui/badge/Badge";

export default function ManagerApplicationView({ user }) {
  const c = useManagerApplicationController(user);
  const canApply =
    user.role === "CUSTOMER" &&
    (!c.items.length || c.items[0].status === "REJECTED");

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
      case "PENDING": return "Chờ Admin duyệt";
      case "APPROVED": return "Đã được duyệt";
      case "REJECTED": return "Đã từ chối";
      default: return status;
    }
  };

  return (
    <div className="space-y-6">
      <ComponentCard title="Đăng ký mở shop / trở thành Manager">
        {c.error && (
          <div className="mb-6 rounded-lg bg-error-50 p-4 text-sm text-error-500 dark:bg-error-500/10 dark:text-error-400">
            {c.error} <a href="/login" className="font-semibold underline">Đăng nhập lại</a>
          </div>
        )}
        {c.notice && (
          <div className="mb-6 rounded-lg bg-success-50 p-4 text-sm text-success-500 dark:bg-success-500/10 dark:text-success-400">
            {c.notice}
          </div>
        )}

        <div className="mb-6 flex gap-3">
          <Button disabled={c.busy} onClick={c.reload} variant="outline">
            Cập nhật trạng thái đơn
          </Button>
          {canApply && (
            <Button
              disabled={c.busy}
              aria-expanded={c.open}
              onClick={() => c.setOpen(!c.open)}
            >
              {c.open ? "Đóng form" : "Đăng ký làm Manager shop"}
            </Button>
          )}
        </div>

        {c.items.length > 0 && (
          <div className="mb-8 space-y-4">
            <h3 className="text-lg font-semibold text-gray-800 dark:text-white/90 mb-3">Lịch sử đăng ký</h3>
            {c.items.map((a) => (
              <div key={a.id} className="rounded-xl border border-gray-200 bg-gray-50 p-5 dark:border-white/5 dark:bg-white/5">
                <div className="flex justify-between items-start mb-3">
                  <h4 className="font-semibold text-lg text-gray-800 dark:text-white/90">
                    {a.shopName}
                  </h4>
                  <Badge color={getStatusColor(a.status)}>
                    {getStatusLabel(a.status)}
                  </Badge>
                </div>
                
                {a.reviewNote && (
                  <div className="mt-3 p-3 bg-white dark:bg-white/5 rounded-lg border border-gray-100 dark:border-white/5">
                    <p className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Ghi chú từ Admin:</p>
                    <p className="text-sm text-gray-600 dark:text-gray-400">{a.reviewNote}</p>
                  </div>
                )}
                
                {a.status === "APPROVED" && (
                  <p className="mt-4 text-sm font-medium text-success-600 dark:text-success-400">
                    Bạn đã được cấp quyền Manager. Đăng nhập lại và chọn Vào trang quản trị shop để đăng sản phẩm.
                  </p>
                )}
              </div>
            ))}
          </div>
        )}

        {c.open && canApply && (
          <div className="rounded-xl border border-gray-200 p-6 dark:border-white/5">
            <h3 className="text-lg font-semibold text-gray-800 dark:text-white/90 mb-5">Đơn đăng ký mới</h3>
            <form onSubmit={c.submit}>
              <fieldset disabled={c.busy} className="space-y-5">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                  {[
                    ["fullName", "Họ và tên", 100],
                    ["phone", "Số điện thoại", 16],
                    ["shopName", "Tên shop", 255],
                    ["addressLine", "Địa chỉ cửa hàng", 255],
                    ["district", "Quận / Huyện", 100],
                    ["ward", "Phường / Xã", 100],
                  ].map(([key, label, max]) => (
                    <div key={key} className={key === "shopName" || key === "addressLine" ? "md:col-span-2" : ""}>
                      <Label>{label}</Label>
                      <Input
                        required
                        maxLength={max}
                        pattern={key === "phone" ? "[+]?[0-9]{9,15}" : undefined}
                        value={c.form[key]}
                        onChange={(e) =>
                          c.setForm({ ...c.form, [key]: e.target.value })
                        }
                      />
                    </div>
                  ))}
                  <div className="md:col-span-2">
                    <Label>Thành phố</Label>
                    <Input required readOnly value={c.form.city} />
                  </div>
                  <div className="md:col-span-2">
                    <Label>Giới thiệu shop</Label>
                    <TextArea
                      required
                      maxLength={5000}
                      value={c.form.description}
                      onChange={(e) =>
                        c.setForm({ ...c.form, description: e.target.value })
                      }
                      rows={5}
                    />
                  </div>
                </div>

                <div className="rounded-lg bg-brand-50 p-4 dark:bg-brand-500/10 mt-6">
                  <p className="text-sm text-brand-600 dark:text-brand-400">
                    Sau khi Admin duyệt, tài khoản chuyển từ Customer sang Manager và
                    chỉ quản lý shop được tạo từ đơn này.
                  </p>
                </div>
                
                <div className="pt-2">
                  <Button type="submit" className="w-full sm:w-auto">Gửi đơn chờ Admin duyệt</Button>
                </div>
              </fieldset>
            </form>
          </div>
        )}
      </ComponentCard>
    </div>
  );
}
