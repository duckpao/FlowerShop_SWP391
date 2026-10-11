import LocationFields from '../components/form/LocationFields'
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
    <main className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8 min-h-screen bg-gray-50 dark:bg-gray-900">
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
      
      {(!c.profile || !c.addressesLoaded) && !c.busy && (
        <div className="flex justify-center p-4">
          <Button onClick={c.retry} variant="outline">Tải lại dữ liệu tài khoản</Button>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,5fr)_minmax(0,7fr)] gap-6 lg:gap-8 items-start">
        <div className="min-w-0">
          {c.profile && (
            <ComponentCard title="Hồ sơ cá nhân" desc="Thông tin dùng để liên hệ khi giao hoa." className="shadow-sm">
              <div className="space-y-6">
                <div className="flex flex-wrap items-center gap-2 rounded-xl bg-gray-50 p-3 dark:bg-gray-800/70 text-sm text-gray-600 dark:text-gray-400">
                  <span className="min-w-0 break-all font-medium text-gray-900 dark:text-white">{c.profile.email}</span>
                  {c.profile.emailVerified && (
                    <Badge color="success">Đã xác thực</Badge>
                  )}
                </div>
                
                <form onSubmit={c.saveProfile}>
                  <fieldset disabled={busy} className="space-y-5">
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
                      <p className="text-xs leading-5 text-gray-500 dark:text-gray-400 mt-2">Có thể để trống. Nếu nhập, dùng 10 chữ số bắt đầu bằng 0.</p>
                    </div>
                    <div className="pt-1">
                      <Button type="submit" variant="primary" className="w-full justify-center">Lưu hồ sơ</Button>
                    </div>
                  </fieldset>
                </form>
              </div>
            </ComponentCard>
          )}

        </div>

        <div className="min-w-0">
          {c.addressesLoaded && (
            <ComponentCard title="Địa chỉ nhận hoa" desc="Chọn địa chỉ mặc định khi đặt hàng hoặc lưu thêm địa chỉ mới." className="shadow-sm">
              <div className="space-y-7">
                {!c.addresses.length ? (
                  <div className="rounded-xl border border-dashed border-gray-300 bg-gray-50 px-5 py-7 text-center dark:border-gray-700 dark:bg-gray-800/40">
                    <p className="text-sm font-medium text-gray-700 dark:text-gray-200">Bạn chưa có địa chỉ nhận hoa.</p>
                    <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">Thêm địa chỉ bên dưới để sử dụng khi thanh toán.</p>
                  </div>
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
                          </div>
                          {item.isDefault && <Badge color="primary">Mặc định</Badge>}
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

                <div className="pt-7 border-t border-gray-100 dark:border-white/5">
                  <h3 className="text-base font-semibold text-gray-900 dark:text-white mb-5">
                    {c.editing ? 'Sửa địa chỉ' : 'Thêm địa chỉ'}
                  </h3>
                  <form onSubmit={c.saveAddress}>
                    <fieldset disabled={busy} className="grid grid-cols-1 sm:grid-cols-2 gap-x-4 gap-y-5">
                      <LocationFields address={c.address} setAddress={c.setAddress} />
                      <div className="sm:col-span-2">
                        <Label>Số nhà, đường, tòa nhà</Label>
                        <Input 
                          required 
                          maxLength={255} 
                          autoComplete="street-address" 
                          value={c.address.addressLine}
                          onChange={e => c.setAddress({ ...c.address, addressLine: e.target.value })} 
                        />
                      </div>
                      <div className="sm:col-span-2 flex items-center">
                        <label className="flex items-center gap-2 cursor-pointer">
                          <input 
                            type="checkbox" 
                            className="w-4 h-4 text-brand-500 border-gray-300 rounded focus:ring-brand-500"
                            checked={c.address.isDefault}
                            onChange={e => c.setAddress({ ...c.address, isDefault: e.target.checked })} 
                          />
                          <span className="text-sm font-medium text-gray-700 dark:text-gray-300">Đặt làm địa chỉ mặc định</span>
                        </label>
                      </div>
                      <div className="sm:col-span-2 flex flex-wrap gap-3 pt-1">
                        <Button type="submit" variant="primary" className="min-w-36 justify-center">
                          {c.editing ? 'Lưu thay đổi' : 'Thêm địa chỉ'}
                        </Button>
                        {c.editing && (
                          <Button type="button" variant="outline" onClick={c.cancel}>
                            Hủy chỉnh sửa
                          </Button>
                        )}
                      </div>
                    </fieldset>
                  </form>
                </div>
              </div>
            </ComponentCard>
          )}
        </div>
      </div>

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
