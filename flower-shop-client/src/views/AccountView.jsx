import PersonalAddressForm from '../components/form/PersonalAddressForm'
import AddressEditDialog from '../components/form/AddressEditDialog'
import AvatarUpload from '../components/UserProfile/AvatarUpload'
import { useAccountController } from '../controllers/useAccountController'
import { roleLabels } from '../models/authModel'
import Badge from "@/components/ui/badge/Badge"
import Button from "@/components/ui/button/Button"
import Input from "@/components/form/input/InputField"
import Label from "@/components/form/Label"
import ComponentCard from "@/components/common/ComponentCard"

export default function AccountView({ user, logout, checkSession, busy: authBusy, error: authError, notice: authNotice }) {
  const c = useAccountController(user)
  const busy = c.busy || authBusy

  return (
    <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8 min-h-screen bg-gray-50 dark:bg-gray-900">
      <header className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white dark:bg-gray-800 p-6 rounded-xl border border-gray-200 dark:border-white/5 shadow-sm">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Tài khoản FlowerShop</h1>
          <p className="text-brand-500 font-medium mt-1">{roleLabels[user.role]}</p>
        </div>
        <button 
          type="button" 
          disabled={busy} 
          onClick={logout}
          className="px-4 py-2 text-sm font-medium text-error-600 bg-error-50 hover:bg-error-100 dark:bg-error-500/10 dark:text-error-400 dark:hover:bg-error-500/20 rounded-lg transition-colors disabled:opacity-50"
        >
          Đăng xuất
        </button>
      </header>

      {(c.error || authError) && (
        <div className="rounded-lg bg-error-50 dark:bg-error-500/10 p-4" role="alert">
          <p className="text-sm text-error-600 dark:text-error-500">{c.error || authError}</p>
        </div>
      )}
      {(c.notice || authNotice) && (
        <div className="rounded-lg bg-success-50 dark:bg-success-500/10 p-4" role="status">
          <p className="text-sm text-success-600 dark:text-success-500">{c.notice || authNotice}</p>
        </div>
      )}

      {c.busy && (
        <div className="flex justify-center p-4">
          <p className="text-gray-500 dark:text-gray-400" role="status">Đang tải…</p>
        </div>
      )}
      
      {(!c.profile || (user.role === "CUSTOMER" && !c.addressesLoaded)) && !c.busy && (
        <div className="flex justify-center p-4">
          <Button onClick={c.retry} variant="outline">Tải lại dữ liệu tài khoản</Button>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,5fr)_minmax(0,7fr)] gap-6 lg:gap-8 items-start">
        <div className="min-w-0 space-y-8">
          {c.profile && (
            <ComponentCard title="Hồ sơ cá nhân" desc="Thông tin dùng để liên hệ khi giao hoa." className="shadow-sm">
              <div className="space-y-6">
                <AvatarUpload profile={c.profile} busy={busy} upload={c.uploadAvatar} />
                <div className="flex flex-wrap items-center gap-2 rounded-xl bg-gray-50 p-3 dark:bg-gray-800/70 text-sm text-gray-600 dark:text-gray-400">
                  <span className="min-w-0 break-all font-medium text-gray-900 dark:text-white">{c.profile.email}</span>
                  {c.profile.emailVerified && (
                    <Badge color="success">Đã xác thực</Badge>
                  )}
                </div>
                
                <form onSubmit={c.saveProfile} className="space-y-4">
                  <fieldset disabled={busy} className="space-y-4">
                    <div>
                      <Label>Họ và tên</Label>
                      <Input 
                        required 
                        maxLength={100} 
                        autoComplete="name" 
                        value={c.form.fullName}
                        onChange={e => c.setForm({ ...c.form, fullName: e.target.value })} 
                      />
                    </div>
                    <div>
                      <Label>Số điện thoại</Label>
                      <Input 
                        type="tel" 
                        maxLength={10} 
                        pattern="0[0-9]{9}" 
                        autoComplete="tel"
                        value={c.form.phone} 
                        onChange={e => c.setForm({ ...c.form, phone: e.target.value })} 
                      />
                      <p className="text-xs text-gray-500 mt-1">Có thể để trống; nếu nhập cần 10 chữ số, bắt đầu bằng 0.</p>
                      {c.phoneError && <p role="alert" className="mt-1 text-sm text-error-500">{c.phoneError}</p>}
                    </div>
                    <Button type="submit" variant="primary" className="w-full">Lưu hồ sơ</Button>
                  </fieldset>
                </form>
              </div>
            </ComponentCard>
          )}

        </div>

        <div className="min-w-0 space-y-8">
          {c.profile && user.role === 'CUSTOMER' && (
            <ComponentCard title="Địa chỉ cá nhân">
              <div className="space-y-6">

                <div className="bg-blue-50 dark:bg-blue-900/20 p-4 rounded-lg">
                  <p className="text-sm text-blue-800 dark:text-blue-300">
                    Khu vực hỗ trợ: <span className="font-semibold">Toàn quốc (qua Giao Hàng Nhanh - GHN)</span>. Tối đa 10 địa chỉ.
                  </p>
                </div>

                {!c.addresses.length ? (
                  <p className="text-gray-500 dark:text-gray-400 text-center py-4">Bạn chưa có địa chỉ cá nhân.</p>
                ) : (
                  <div className="space-y-3">
                    {c.addresses.map(item => (
                      <div key={item.id} className="rounded-xl border border-gray-200 bg-gray-50/70 p-4 dark:border-gray-700 dark:bg-gray-800/40">
                        <div className="flex flex-wrap items-start justify-between gap-2">
                          <div className="min-w-0 flex-1">
                            <p className="break-words text-sm font-semibold leading-6 text-gray-900 dark:text-white">{item.addressLine}</p>
                            <p className="mt-0.5 text-sm text-gray-500 dark:text-gray-400">
                              {[item.ward, item.district, item.city].filter(Boolean).join(', ')}
                            </p>
                            <p className="text-xs text-gray-500">{item.recipientName} · {item.recipientPhone}</p>
                          </div>
                          <div className="flex gap-1">
                            {item.isDefault ? <Badge color="primary">Mặc định</Badge> : null}
                            {item.isPickup ? <Badge color="success">Lấy hàng</Badge> : null}
                            {item.isReturn ? <Badge color="warning">Trả hàng</Badge> : null}
                          </div>
                        </div>
                        <div className="mt-3 flex flex-wrap items-center gap-2 border-t border-gray-200 pt-3 dark:border-gray-700">
                          {!item.isDefault && (
                            <button type="button" disabled={busy} onClick={() => c.makeDefault(item.id)}
                              className="rounded-lg px-3 py-1.5 text-sm font-medium text-gray-600 hover:bg-white hover:text-gray-900 dark:text-gray-300 dark:hover:bg-gray-700 disabled:opacity-50">
                              Đặt mặc định
                            </button>
                          )}
                          <button type="button" disabled={busy} onClick={() => c.edit(item)}
                            className="rounded-lg px-3 py-1.5 text-sm font-medium text-brand-600 hover:bg-brand-50 dark:text-brand-400 dark:hover:bg-brand-500/10 disabled:opacity-50">
                            Sửa
                          </button>
                          <button type="button" disabled={busy} onClick={() => c.remove(item)}
                            className="rounded-lg px-3 py-1.5 text-sm font-medium text-error-600 hover:bg-error-50 dark:text-error-400 dark:hover:bg-error-500/10 disabled:opacity-50">
                            Xóa
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}

                {!c.editing && <PersonalAddressForm controller={c} busy={busy} />}
              </div>
            </ComponentCard>
          )}
          {c.profile && user.role === 'SHOP' && (
            <ComponentCard title="Địa chỉ cửa hàng">
              <p className="mb-4 text-sm text-gray-600 dark:text-gray-400">
                Địa chỉ lấy hàng của cửa hàng được quản lý trong phần cài đặt shop.
              </p>
              <a
                href="/shop-admin/shop"
                className="inline-flex items-center rounded-lg bg-brand-500 px-4 py-2 text-sm font-medium text-white hover:bg-brand-600"
              >
                Quản lý địa chỉ cửa hàng
              </a>
            </ComponentCard>
          )}
        </div>
      </div>

      {c.editing && <AddressEditDialog key={c.editing} controller={c} busy={busy} />}

      <div className="flex justify-center pt-8 border-t border-gray-200 dark:border-white/5">
        <button 
          className="text-sm font-medium text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-300 transition-colors" 
          disabled={busy} 
          onClick={checkSession}
        >
          Kiểm tra phiên đăng nhập
        </button>
      </div>
    </main>
  )
}
