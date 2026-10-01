import { useManagerShopController } from "../controllers/useManagerShopController";
import { shopStatusLabels } from "../models/shopModel";
import StaffApplicationsView from "./StaffApplicationsView";
import ProductsView from "./ProductsView";
import ComponentCard from "../components/common/ComponentCard";
import Badge from "../components/ui/badge/Badge";
import Button from "../components/ui/button/Button";
import Input from "../components/form/input/InputField";
import TextArea from "../components/form/input/TextArea";
import Label from "../components/form/Label";
import { Table, TableBody, TableCell, TableHeader, TableRow } from "../components/ui/table";

export default function ManagerShopsView({ role, section = "all" }) {
  const c = useManagerShopController(role);
  const editable =
    role === "SHOP" && ["ACTIVE", "PENDING"].includes(c.selected?.status);

  const getStatusColor = (status) => {
    switch (status) {
      case "ACTIVE": return "success";
      case "PENDING": return "warning";
      case "INACTIVE": return "error";
      default: return "primary";
    }
  };

  return (
    <div className="space-y-6">
      <ComponentCard title="Cửa hàng của tôi">
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
        <div className="mb-6">
          <Button disabled={c.busy} onClick={c.retry} variant="outline" size="sm">
            {c.busy ? "Đang tải…" : "Tải lại cửa hàng"}
          </Button>
        </div>

        {!c.busy && !c.shops.length && (
          <p className="text-gray-500 dark:text-gray-400">Chưa có cửa hàng được phân công.</p>
        )}

        {role !== "SHOP" && c.shops.length > 0 && (
          <div className="mt-4 overflow-hidden rounded-xl border border-gray-200 dark:border-white/5">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableCell isHeader>Tên cửa hàng</TableCell>
                  <TableCell isHeader>Trạng thái</TableCell>
                  <TableCell isHeader className="text-right">Hành động</TableCell>
                </TableRow>
              </TableHeader>
              <TableBody>
                {c.shops.map((s) => (
                  <TableRow key={s.id}>
                    <TableCell>{s.name}</TableCell>
                    <TableCell>
                      <Badge color={getStatusColor(s.status)}>
                        {shopStatusLabels[s.status]}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-right">
                      <Button disabled={c.busy} onClick={() => c.select(s)} size="sm">
                        Xem cửa hàng
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </ComponentCard>

      {c.selected && (
        <div className="space-y-6">
          <ComponentCard title={`Chi tiết: ${c.selected.name}`}>
            <div className="mb-4">
              <Badge color={getStatusColor(c.selected.status)}>
                {shopStatusLabels[c.selected.status]}
              </Badge>
            </div>

            {!editable && (
              <p className="mb-4 text-gray-500 dark:text-gray-400">
                Cửa hàng hiện ở chế độ chỉ xem đối với tài khoản này.
              </p>
            )}

            {["all", "shop"].includes(section) && (
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <div>
                  <h3 className="text-lg font-semibold text-gray-800 dark:text-white/90 mb-4">Hồ sơ shop</h3>
                  <form onSubmit={c.save}>
                    <fieldset disabled={c.busy || !editable} className="space-y-4">
                      <div>
                        <Label>Tên shop</Label>
                        <Input
                          required
                          maxLength={255}
                          value={c.form.name}
                          onChange={(e) =>
                            c.setForm({ ...c.form, name: e.target.value })
                          }
                        />
                      </div>
                      <div>
                        <Label>Mô tả</Label>
                        <TextArea
                          maxLength={5000}
                          value={c.form.description}
                          onChange={(e) =>
                            c.setForm({ ...c.form, description: e.target.value })
                          }
                          rows={4}
                        />
                      </div>
                      <div>
                        <Label>URL logo (HTTPS)</Label>
                        <Input
                          type="url"
                          maxLength={255}
                          pattern="https://.*"
                          value={c.form.logoUrl}
                          onChange={(e) =>
                            c.setForm({ ...c.form, logoUrl: e.target.value })
                          }
                        />
                      </div>
                      {role === "SHOP" && (
                        <Button type="submit">Lưu hồ sơ cửa hàng</Button>
                      )}
                    </fieldset>
                  </form>
                </div>

                {role === "SHOP" && (
                  <div>
                    <h3 className="text-lg font-semibold text-gray-800 dark:text-white/90 mb-4">Địa chỉ cửa hàng</h3>
                    <form onSubmit={c.saveAddress}>
                      <fieldset disabled={c.busy || !editable} className="space-y-4">
                        <div>
                          <Label>Thành phố</Label>
                          <Input required readOnly value={c.address.city} />
                        </div>
                        {[
                          ["district", "Quận / Huyện", 100],
                          ["ward", "Phường / Xã", 100],
                          ["addressLine", "Số nhà, đường", 255],
                        ].map(([key, label, max]) => (
                          <div key={key}>
                            <Label>{label}</Label>
                            <Input
                              required
                              maxLength={max}
                              value={c.address[key]}
                              onChange={(e) =>
                                c.setAddress({
                                  ...c.address,
                                  [key]: e.target.value,
                                })
                              }
                            />
                          </div>
                        ))}
                        <Button type="submit">Lưu địa chỉ shop</Button>
                      </fieldset>
                    </form>
                  </div>
                )}
              </div>
            )}
          </ComponentCard>

          {role === "SHOP" && ["all", "staff"].includes(section) && (
            <div className="space-y-6">
              <StaffApplicationsView key={c.selected.id} shop={c.selected} />
              
              <ComponentCard title="Nhân viên cửa hàng">
                <p className="mb-4 text-gray-500 dark:text-gray-400">
                  Danh sách nhân viên đã được phân công vào shop. Sau khi duyệt đơn, bấm “Tải lại cửa hàng” để cập nhật danh sách.
                </p>
                {!c.members.length ? (
                  <p className="text-gray-500 dark:text-gray-400">Chưa có nhân viên tham gia shop.</p>
                ) : (
                  <div className="overflow-hidden rounded-xl border border-gray-200 dark:border-white/5">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableCell isHeader>Họ và tên</TableCell>
                          <TableCell isHeader>Email</TableCell>
                          <TableCell isHeader>Trạng thái</TableCell>
                          <TableCell isHeader className="text-right">Hành động</TableCell>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {c.members.map((m) => (
                          <TableRow key={m.userId}>
                            <TableCell>{m.fullName}</TableCell>
                            <TableCell>{m.email}</TableCell>
                            <TableCell>
                              <Badge color={m.active ? "success" : "error"}>
                                {m.active ? "Được làm việc" : "Đã ngừng quyền"}
                              </Badge>
                            </TableCell>
                            <TableCell className="text-right">
                              <Button
                                size="sm"
                                variant={m.active ? "outline" : "primary"}
                                disabled={c.busy || c.selected.status !== "ACTIVE"}
                                onClick={() => c.toggle(m)}
                              >
                                {m.active ? "Ngừng quyền" : "Bật quyền"}
                              </Button>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>
                )}
              </ComponentCard>
            </div>
          )}

          {role === "SHOP" && ["all", "products"].includes(section) && (
            <ProductsView key={`products-${c.selected.id}`} shop={c.selected} manage />
          )}

          {role === "SHOP" && ["all", "orders"].includes(section) && (
            <ManagerOrdersView key={`orders-${c.selected.id}`} shop={c.selected} onBack={() => c.setSection("all")} />
          )}
        </div>
      )}
    </div>
  );
}
