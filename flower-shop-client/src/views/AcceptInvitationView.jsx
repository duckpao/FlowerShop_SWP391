import { useAcceptInvitationController } from '../controllers/useAcceptInvitationController'
import { Link } from 'react-router'
import ComponentCard from '../components/common/ComponentCard'
import Button from '../components/ui/button/Button'
import Input from '../components/form/input/InputField'
import Label from '../components/form/Label'
import { useAuth } from '../context/AuthContext'

export default function AcceptInvitationView({ email }) {
  const c = useAcceptInvitationController()
  const { user } = useAuth()
  const invitedEmail = new URLSearchParams(window.location.search).get('email') || email || ''
  const next = encodeURIComponent(window.location.pathname + window.location.search)
  const loginUrl = `/login?next=${next}&email=${encodeURIComponent(invitedEmail)}`
  
  return (
    <div className="w-full max-w-md mx-auto p-4 sm:p-6 mt-10">
      <ComponentCard title="Lời mời nhân viên">
        {!user && !c.done && (
          <div className="mb-6 space-y-3 rounded-lg bg-brand-50 p-4">
            <p>Bạn được mời làm nhân viên cửa hàng bằng email {invitedEmail}. Hãy tạo tài khoản và xác thực email, hoặc đăng nhập nếu đã có tài khoản.</p>
            <div className="flex gap-4">
              <Link to={`${loginUrl}&register=1`}>Tạo tài khoản để tham gia</Link>
              <Link to={loginUrl}>Đã có tài khoản? Đăng nhập</Link>
            </div>
          </div>
        )}
        <div className="space-y-4 mb-6">
          <div className="rounded-lg bg-gray-50 p-4 dark:bg-white/5">
            <p className="text-sm text-gray-600 dark:text-gray-300">
              Đăng nhập đúng email nhận thư: <strong className="text-gray-900 dark:text-white">{invitedEmail || user?.email}</strong>. Mã được điền sẵn khi bạn mở liên kết trong email; xác nhận để tham gia shop.
            </p>
          </div>
          
          <div className="rounded-lg bg-warning-50 p-4 border border-warning-100 dark:bg-warning-500/10 dark:border-warning-500/20">
            <div className="flex gap-3">
              <svg className="w-5 h-5 text-warning-600 mt-0.5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
              <p className="text-sm text-warning-800 dark:text-warning-400">
                Nếu đang là Customer, bạn sẽ chuyển sang Shop Staff và không còn dùng các chức năng riêng của Customer. Mọi phiên đăng nhập hiện tại sẽ kết thúc.
              </p>
            </div>
          </div>
        </div>

        {c.error && (
          <div className="mb-6 rounded-lg bg-error-50 p-4 text-sm text-error-500 dark:bg-error-500/10 dark:text-error-400">
            {c.error}
          </div>
        )}
        
        {c.done ? (
          <div className="rounded-lg border border-success-200 bg-success-50 p-6 text-center dark:border-success-500/20 dark:bg-success-500/10">
            <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-success-100 dark:bg-success-500/20">
              <svg className="h-6 w-6 text-success-600 dark:text-success-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
              </svg>
            </div>
            <h3 className="mb-2 text-lg font-medium text-gray-900 dark:text-white">Đã tham gia thành công</h3>
            <p className="mb-4 text-sm text-gray-600 dark:text-gray-400">
              Bạn đã trở thành nhân viên cửa hàng. Vui lòng đăng nhập lại để sử dụng quyền nhân viên.
            </p>
            <a href="/login" className="inline-flex items-center justify-center rounded-lg bg-brand-500 px-5 py-2.5 text-sm font-medium text-white hover:bg-brand-600 transition-colors">
              Đăng nhập lại
            </a>
          </div>
        ) : (
          <form onSubmit={c.accept}>
            <fieldset disabled={c.busy || !user} className="space-y-5">
              <div>
                <Label>Mã lời mời</Label>
                <Input 
                  required 
                  maxLength={100} 
                  autoComplete="off" 
                  value={c.code} 
                  onChange={e => c.setCode(e.target.value)} 
                  placeholder="Nhập mã từ email..."
                />
              </div>
              <Button type="submit" className="w-full">
                Đồng ý tham gia và chuyển sang quyền Staff
              </Button>
            </fieldset>
          </form>
        )}
      </ComponentCard>
    </div>
  )
}
