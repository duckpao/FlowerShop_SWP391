import { useAccountController } from "../controllers/useAccountController";
import ComponentCard from "../components/common/ComponentCard";
import PageMeta from "../components/common/PageMeta";
import PageBreadCrumb from "../components/common/PageBreadCrumb";
import Input from "../components/form/input/InputField";
import Button from "../components/ui/button/Button";
import Badge from "../components/ui/badge/Badge";

export default function AdminProfileView({ user, roleLabel = "Admin" }) {
  const c = useAccountController(user);
  
  return (
    <>
      <PageMeta title={`Hồ sơ ${roleLabel} | Hệ thống`} description={`Thông tin tài khoản ${roleLabel}`} />
      <PageBreadCrumb pageTitle={`Thông tin tài khoản ${roleLabel}`} />
      
      <ComponentCard title="Hồ sơ của bạn">
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
        {c.busy && <div className="mb-4 text-brand-500">Đang xử lý…</div>}
        
        {!c.profile && !c.busy && (
          <Button onClick={c.retry} variant="outline" size="sm">
            Tải lại hồ sơ
          </Button>
        )}
        
        {c.profile && (
          <div className="space-y-6">
            <div className="flex flex-col gap-2 rounded-lg bg-gray-50 p-4 dark:bg-white/5">
              <div className="flex items-center gap-2">
                <span className="text-gray-500 dark:text-gray-400">Email:</span>
                <span className="font-medium text-gray-900 dark:text-white">{c.profile.email}</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="text-gray-500 dark:text-gray-400">Vai trò:</span>
                <Badge size="sm" color="primary">{roleLabel}</Badge>
              </div>
              <div className="flex items-center gap-2">
                <span className="text-gray-500 dark:text-gray-400">Trạng thái:</span>
                <Badge size="sm" color={c.profile.emailVerified ? "success" : "warning"}>
                  {c.profile.emailVerified ? "Đã xác thực email" : "Chưa xác thực email"}
                </Badge>
              </div>
            </div>

            <form onSubmit={c.saveProfile} className="space-y-4">
              <fieldset disabled={c.busy} className="space-y-4">
                <Input
                  label="Họ và tên"
                  required
                  maxLength={100}
                  autoComplete="name"
                  value={c.form.fullName}
                  onChange={(e) => c.setForm({ ...c.form, fullName: e.target.value })}
                  placeholder="Nhập họ và tên..."
                />
                
                <Input
                  label="Số điện thoại"
                  type="tel"
                  maxLength={16}
                  pattern="[+]?[0-9]{9,15}"
                  autoComplete="tel"
                  value={c.form.phone}
                  onChange={(e) => c.setForm({ ...c.form, phone: e.target.value })}
                  placeholder="Nhập số điện thoại..."
                />
                
                <div className="pt-2">
                  <Button type="submit" disabled={c.busy}>
                    Lưu thông tin tài khoản
                  </Button>
                </div>
              </fieldset>
            </form>
          </div>
        )}
      </ComponentCard>
    </>
  );
}
