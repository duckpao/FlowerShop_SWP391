import { useInvitationsController } from "../controllers/useInvitationsController";
import ComponentCard from "../components/common/ComponentCard";
import Button from "../components/ui/button/Button";
import Input from "../components/form/input/InputField";
import Label from "../components/form/Label";
import Badge from "../components/ui/badge/Badge";
import { Table, TableBody, TableCell, TableHeader, TableRow } from "../components/ui/table";

const labels = {
  PENDING: "Chờ xác nhận",
  ACCEPTED: "Đã tham gia",
  CANCELLED: "Đã hủy",
  EXPIRED: "Hết hạn",
};

export default function ShopInvitationsView({ shop }) {
  const c = useInvitationsController(shop);
  const disabled = c.busy || shop.status !== "ACTIVE";

  const getStatusColor = (status) => {
    switch (status) {
      case "PENDING": return "warning";
      case "ACCEPTED": return "success";
      case "CANCELLED": return "error";
      case "EXPIRED": return "primary";
      default: return "primary";
    }
  };

  return (
    <ComponentCard title="Thêm nhân viên mới">
      <div className="mb-6 rounded-lg bg-gray-50 p-4 dark:bg-white/5">
        <p className="text-sm text-gray-500 dark:text-gray-400">
          Nhập email người muốn mời. Nếu chưa có tài khoản, người nhận cần đăng ký
          và xác thực email, sau đó đăng nhập để chấp nhận lời mời. Chỉ khi đồng
          ý, họ mới trở thành nhân viên của shop. Mã có hiệu lực 24 giờ.
        </p>
      </div>

      {c.error && (
        <div className="mb-6 rounded-lg bg-error-50 p-4 text-sm text-error-500 dark:bg-error-500/10 dark:text-error-400">
          {c.error}
        </div>
      )}
      {c.notice && (
        <div className="mb-6 rounded-lg bg-success-50 p-4 text-sm text-success-500 dark:bg-success-500/10 dark:text-success-400">
          {c.notice}
        </div>
      )}

      <div className="mb-8 max-w-md">
        <form noValidate onSubmit={c.send}>
          <fieldset disabled={disabled} className="space-y-4">
            <div>
              <Label>Email người nhận</Label>
              <div className="flex gap-3">
                <div className="flex-1">
                  <Input
                    required
                    type="email"
                    maxLength={50}
                    aria-invalid={!!c.emailError}
                    aria-describedby="invitation-email-error"
                    value={c.email}
                    onChange={(e) => c.setEmail(e.target.value)}
                  />
                  <p id="invitation-email-error" className="text-sm text-error-500" aria-live="polite">{c.emailError}</p>
                </div>
                <Button type="submit">Gửi lời mời</Button>
              </div>
            </div>
          </fieldset>
        </form>
      </div>

      <div className="border-t border-gray-100 pt-6 dark:border-white/5">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-lg font-semibold text-gray-800 dark:text-white/90">Lịch sử lời mời</h3>
          <Button disabled={c.busy} onClick={c.retry} variant="outline" size="sm">
            Tải lại lời mời
          </Button>
        </div>

        <div className="overflow-hidden rounded-xl border border-gray-200 dark:border-white/5">
          <Table>
            <TableHeader>
              <TableRow>
                <TableCell isHeader>Email</TableCell>
                <TableCell isHeader>Trạng thái</TableCell>
                <TableCell isHeader className="text-right">Thao tác</TableCell>
              </TableRow>
            </TableHeader>
            <TableBody>
              {c.items.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={3} className="text-center py-6 text-gray-500 dark:text-gray-400">
                    Chưa có lời mời nào.
                  </TableCell>
                </TableRow>
              ) : (
                c.items.map((item) => (
                  <TableRow key={item.id}>
                    <TableCell className="font-medium">{item.email}</TableCell>
                    <TableCell>
                      <Badge color={getStatusColor(item.status)}>
                        {labels[item.status]}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-right flex justify-end gap-2">
                      {item.status !== "ACCEPTED" && (
                        <Button
                          disabled={disabled}
                          onClick={() => c.resend(item)}
                          variant="outline"
                          size="sm"
                        >
                          Gửi lại
                        </Button>
                      )}
                      {item.status === "PENDING" && (
                        <Button
                          disabled={disabled}
                          onClick={() => c.cancel(item)}
                          variant="outline"
                          size="sm"
                          className="!text-error-500 !border-error-500 hover:!bg-error-50"
                        >
                          Hủy
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>
      </div>
    </ComponentCard>
  );
}
