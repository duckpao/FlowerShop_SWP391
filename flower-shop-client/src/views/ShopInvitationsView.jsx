import { useInvitationsController } from "../controllers/useInvitationsController";
const labels = {
  PENDING: "Chờ xác nhận",
  ACCEPTED: "Đã tham gia",
  CANCELLED: "Đã hủy",
  EXPIRED: "Hết hạn",
};
export default function ShopInvitationsView({ shop }) {
  const c = useInvitationsController(shop);
  const disabled = c.busy || shop.status !== "ACTIVE";
  return (
    <section>
      <h3>Thêm nhân viên mới</h3>
      <p>
        Nhập email người muốn mời. Nếu chưa có tài khoản, người nhận cần đăng ký
        và xác thực email, sau đó đăng nhập để chấp nhận lời mời. Chỉ khi đồng
        ý, họ mới trở thành nhân viên của shop. Mã có hiệu lực 24 giờ.
      </p>
      {c.error && (
        <p role="alert" className="message error">
          {c.error}
        </p>
      )}
      {c.notice && (
        <p role="status" className="message success">
          {c.notice}
        </p>
      )}
      <form onSubmit={c.send}>
        <fieldset disabled={disabled}>
          <label>
            Email người nhận
            <input
              required
              type="email"
              maxLength={50}
              value={c.email}
              onChange={(e) => c.setEmail(e.target.value)}
            />
          </label>
          <button>Gửi lời mời</button>
        </fieldset>
      </form>
      <button disabled={c.busy} onClick={c.retry}>
        Tải lại lời mời
      </button>
      <ul className="address-list">
        {c.items.map((item) => (
          <li key={item.id}>
            {item.email} · {labels[item.status]}
            {item.status !== "ACCEPTED" && (
              <button disabled={disabled} onClick={() => c.resend(item)}>
                Gửi lại
              </button>
            )}
            {item.status === "PENDING" && (
              <button disabled={disabled} onClick={() => c.cancel(item)}>
                Hủy lời mời
              </button>
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}
