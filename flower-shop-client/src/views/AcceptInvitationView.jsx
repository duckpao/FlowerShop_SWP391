import { useAcceptInvitationController } from '../controllers/useAcceptInvitationController'
export default function AcceptInvitationView({ email }) {
  const c = useAcceptInvitationController()
  return <section className="account-card"><h2>Lời mời nhân viên</h2>
    <p>Đăng nhập đúng email nhận thư: {email}. Dán mã lời mời trong email để đồng ý tham gia.</p>
    <p>Nếu đang là Customer, bạn sẽ chuyển sang Shop Staff và không còn dùng các chức năng riêng của Customer. Mọi phiên đăng nhập hiện tại sẽ kết thúc.</p>
    {c.error && <p role="alert" className="message error">{c.error}</p>}
    {c.done ? <p role="status">Đã tham gia. <a href="/">Đăng nhập lại</a> để dùng quyền nhân viên.</p> :
      <form onSubmit={c.accept}><fieldset disabled={c.busy}><label>Mã lời mời<input required maxLength={100} autoComplete="off" value={c.code} onChange={e => c.setCode(e.target.value)} /></label>
        <button>Đồng ý tham gia và chuyển sang quyền Staff</button></fieldset></form>}
  </section>
}
