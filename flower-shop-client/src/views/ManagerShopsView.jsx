import { useState, useEffect } from "react";
import { useManagerShopController } from "../controllers/useManagerShopController";
import { shopStatusLabels } from "../models/shopModel";
import StaffApplicationsView from "./StaffApplicationsView";
import ShopInvitationsView from "./ShopInvitationsView";
import ProductsView from "./ProductsView";
import OrderQueueView from "./OrderQueueView";
import ComponentCard from "../components/common/ComponentCard";
import Badge from "../components/ui/badge/Badge";
import Button from "../components/ui/button/Button";
import Input from "../components/form/input/InputField";
import TextArea from "../components/form/input/TextArea";
import Label from "../components/form/Label";
import { Table, TableBody, TableCell, TableHeader, TableRow } from "../components/ui/table";
import GhnAddressPicker from "../components/GhnAddressPicker";

/**
 * View Quản lý Cửa hàng (ManagerShopsView).
 * Dành cho chủ cửa hàng (role === 'SHOP') hoặc nhân viên để xem và quản lý:
 * 1. Thông tin hồ sơ shop (Tên, mô tả, logo).
 * 2. Cấu hình địa chỉ kho lấy hàng chuẩn 3 cấp theo API Giao Hàng Nhanh (GHN).
 * 3. Quản lý danh sách nhân viên của cửa hàng.
 * 4. Tích hợp các module con: Duyệt đơn ứng tuyển (StaffApplicationsView), Quản lý sản phẩm (ProductsView).
 * 
 * @param {string} role - Quyền người dùng ('SHOP' hoặc 'SHOP_STAFF')
 * @param {string} section - Mục đang hiển thị ('all', 'shop', 'staff', 'products', 'orders')
 */
export default function ManagerShopsView({ role, section = "all" }) {
  // Hook điều khiển logic cửa hàng (load thông tin, cập nhật hồ sơ, đổi địa chỉ, bật/tắt nhân viên)
  const c = useManagerShopController(role);
  
  // Chỉ cho phép chỉnh sửa nếu là chủ shop và shop đang ACTIVE hoặc PENDING
  const editable =
    role === "SHOP" && ["ACTIVE", "PENDING"].includes(c.selected?.status);

  // Xác định màu sắc badge theo trạng thái cửa hàng
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
          <ComponentCard title={`Xem chi tiết: ${c.selected.name}`}>
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
                        <Label>Logo shop</Label>
                        <div className="flex items-center gap-4">
                          {c.selected?.logoUrl
                            ? <img src={c.selected.logoUrl} alt="Logo shop" className="h-16 w-16 rounded-lg border border-gray-200 object-cover" />
                            : <div className="flex h-16 w-16 items-center justify-center rounded-lg bg-gray-100 text-xs text-gray-400">Chưa có</div>}
                          {role === "SHOP" && (
                            <label className={`inline-flex items-center gap-2 rounded-lg bg-brand-500 px-4 py-2.5 text-sm font-medium text-white shadow-theme-xs transition-colors focus-within:outline-2 focus-within:outline-offset-2 focus-within:outline-brand-500 ${c.busy ? 'cursor-not-allowed opacity-50' : 'cursor-pointer hover:bg-brand-600'}`}>
                              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} aria-hidden="true">
                                <path strokeLinecap="round" strokeLinejoin="round" d="M4 16v2a2 2 0 002 2h12a2 2 0 002-2v-2M16 8l-4-4m0 0L8 8m4-4v12" />
                              </svg>
                              {c.busy ? 'Đang tải ảnh…' : 'Chọn ảnh logo'}
                              <input
                                type="file"
                                accept="image/jpeg,image/png,image/webp"
                                className="sr-only"
                                disabled={c.busy}
                                onChange={(e) => {
                                  const file = e.target.files?.[0]
                                  e.target.value = ''
                                  if (file) c.uploadLogo(file)
                                }}
                              />
                            </label>
                          )}
                          {role === "SHOP" && c.selected?.logoUrl && (
                            <button type="button" disabled={c.busy} onClick={c.removeLogo}
                              className="rounded-lg border border-error-300 px-4 py-2.5 text-sm font-medium text-error-600 transition-colors hover:bg-error-50 disabled:cursor-not-allowed disabled:opacity-50">
                              Xóa logo
                            </button>
                          )}
                        </div>
                        <p className="mt-1 text-xs text-gray-500">JPEG, PNG hoặc WebP, tối đa 5MB. Ảnh được tải lên Cloudinary ngay khi chọn.</p>
                        {c.logoError && <p role="alert" className="mt-2 rounded-lg bg-error-50 px-3 py-2 text-sm text-error-500 dark:bg-error-500/10 dark:text-error-400">{c.logoError}</p>}
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
                        <GhnAddressPicker value={c.address} onChange={c.setAddress} />

                        <div>
                          <Label>Số nhà, đường</Label>
                          <Input
                            required
                            maxLength={255}
                            value={c.address.addressLine || ''}
                            onChange={(e) => c.setAddress({ ...c.address, addressLine: e.target.value })}
                          />
                        </div>
                        
                        <Button type="submit" disabled={!(c.address.ghnDistrictId && c.address.ghnWardCode)}>Lưu địa chỉ shop</Button>
                      </fieldset>
                    </form>
                  </div>
                )}
              </div>
            )}
          </ComponentCard>

          {role === "SHOP" && ["all", "staff"].includes(section) && (
            <div className="space-y-6">
              <ShopInvitationsView key={`invitations-${c.selected.id}`} shop={c.selected} />
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

          {["all", "orders"].includes(section) && (
            <OrderQueueView key={`orders-${c.selected.id}`} shop={c.selected} />
          )}
        </div>
      )}
    </div>
  );
}
