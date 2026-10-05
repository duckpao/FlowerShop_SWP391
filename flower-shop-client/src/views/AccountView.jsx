import { useAccountController } from '../controllers/useAccountController'
import { roleLabels } from '../models/authModel'
import AdminCustomersView from './AdminCustomersView'
import AdminShopsView from './AdminShopsView'
import AdminManagerApplicationsView from './AdminManagerApplicationsView'
import ManagerShopsView from './ManagerShopsView'
import { Table, TableBody, TableCell, TableHeader, TableRow } from "@/components/ui/table"
import Badge from "@/components/ui/badge/Badge"
import Button from "@/components/ui/button/Button"
import Input from "@/components/form/input/InputField"
import Select from "@/components/form/Select"
import Label from "@/components/form/Label"
import ComponentCard from "@/components/common/ComponentCard"
import GhnAddressPicker from "@/components/GhnAddressPicker"

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
      
      {!c.profile && !c.busy && (
        <div className="flex justify-center p-4">
          <Button onClick={c.retry} variant="outline">Tải lại hồ sơ</Button>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-1 space-y-8">
          {c.profile && (
            <ComponentCard title="Hồ sơ cá nhân">
              <div className="space-y-6">
                <div className="flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
                  <span className="font-medium text-gray-900 dark:text-white">{c.profile.email}</span>
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
                        maxLength={16} 
                        pattern="[+]?[0-9]{9,15}" 
                        autoComplete="tel"
                        value={c.form.phone} 
                        onChange={e => c.setForm({ ...c.form, phone: e.target.value })} 
                      />
                      <p className="text-xs text-gray-500 mt-1">Có thể để trống; nếu nhập cần 9–15 chữ số, có thể bắt đầu bằng +.</p>
                    </div>
                    <Button type="submit" variant="primary" className="w-full">Lưu hồ sơ</Button>
                  </fieldset>
                </form>
              </div>
            </ComponentCard>
          )}

          <div className="space-y-4">
            {['SHOP', 'SHOP_STAFF'].includes(user.role) && <ManagerShopsView role={user.role} />}
            {user.role === 'ADMIN' && <AdminCustomersView />}
            {user.role === 'ADMIN' && (
              <div className="rounded-xl border border-gray-200 bg-white dark:border-white/5 dark:bg-white/5 p-4">
                <a href="/admin/approvals" className="text-brand-500 hover:text-brand-600 font-medium">
                  Mở màn Duyệt yêu cầu đăng ký Manager
                </a>
              </div>
            )}
            {user.role === 'ADMIN' && <AdminShopsView />}
            {user.role === 'ADMIN' && <AdminManagerApplicationsView />}
          </div>
        </div>

        <div className="lg:col-span-2 space-y-8">
          {c.profile && user.role === 'CUSTOMER' && (
            <ComponentCard title="Địa chỉ giao hàng">
              <div className="space-y-6">
                <div className="bg-blue-50 dark:bg-blue-900/20 p-4 rounded-lg">
                  <p className="text-sm text-blue-800 dark:text-blue-300">
                    Khu vực hỗ trợ: <span className="font-semibold">Toàn quốc (qua Giao Hàng Nhanh - GHN)</span>. Tối đa 10 địa chỉ.
                  </p>
                </div>

                {!c.addresses.length ? (
                  <p className="text-gray-500 dark:text-gray-400 text-center py-4">Bạn chưa có địa chỉ giao hàng.</p>
                ) : (
                  <div className="overflow-x-auto rounded-lg border border-gray-200 dark:border-white/10">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableCell isHeader>Địa chỉ</TableCell>
                          <TableCell isHeader>Khu vực</TableCell>
                          <TableCell isHeader>Trạng thái</TableCell>
                          <TableCell isHeader className="text-right">Thao tác</TableCell>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {c.addresses.map(item => (
                          <TableRow key={item.id}>
                            <TableCell>
                              <span className="font-medium text-gray-900 dark:text-white">{item.addressLine}</span>
                            </TableCell>
                            <TableCell>
                              {[item.ward, item.district, item.city].filter(Boolean).join(', ')}
                            </TableCell>
                            <TableCell>
                              {item.isDefault ? <Badge color="primary">Mặc định</Badge> : null}
                            </TableCell>
                            <TableCell className="text-right">
                              <div className="flex justify-end gap-2">
                                <button 
                                  disabled={busy} 
                                  onClick={() => c.edit(item)}
                                  className="text-sm font-medium text-brand-500 hover:text-brand-600 disabled:opacity-50"
                                >
                                  Sửa
                                </button>
                                {!item.isDefault && (
                                  <button 
                                    disabled={busy} 
                                    onClick={() => c.makeDefault(item.id)}
                                    className="text-sm font-medium text-gray-600 hover:text-gray-900 dark:text-gray-400 dark:hover:text-white disabled:opacity-50"
                                  >
                                    Đặt mặc định
                                  </button>
                                )}
                                <button 
                                  disabled={busy} 
                                  onClick={() => c.remove(item)}
                                  className="text-sm font-medium text-error-500 hover:text-error-600 disabled:opacity-50"
                                >
                                  Xóa
                                </button>
                              </div>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>
                )}

                <div className="pt-6 border-t border-gray-100 dark:border-white/5">
                  <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">
                    {c.editing ? 'Sửa địa chỉ' : 'Thêm địa chỉ'}
                  </h3>
                  <form onSubmit={c.saveAddress}>
                    <fieldset disabled={busy} className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div className="md:col-span-2">
                        <GhnAddressPicker value={c.address} onChange={c.setAddress} />
                      </div>
                      <div className="md:col-span-2">
                        <Label>Số nhà, đường, tòa nhà</Label>
                        <Input 
                          required 
                          maxLength={255} 
                          autoComplete="street-address" 
                          value={c.address.addressLine}
                          onChange={e => c.setAddress({ ...c.address, addressLine: e.target.value })} 
                        />
                      </div>
                      <div className="md:col-span-2 flex items-center mt-2">
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
                      <div className="md:col-span-2 flex gap-3 mt-4">
                        <Button type="submit" variant="primary">
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
