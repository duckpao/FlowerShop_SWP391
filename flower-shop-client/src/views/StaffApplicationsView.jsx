import { useStaffApplicationsController } from "../controllers/useStaffApplicationsController";
import { useState } from "react";
export default function StaffApplicationsView({ shop }) {
  const c = useStaffApplicationsController(shop.id);
  const [open, setOpen] = useState(
    new URLSearchParams(window.location.search).has("notifications"),
  );
  const pending = c.items.filter((a) => a.status === "PENDING").length;
  return (
    <section>
      <h3>Thông báo tuyển dụng</h3>
      <button aria-expanded={open} onClick={() => setOpen(!open)}>
        Thông báo đăng ký làm nhân viên ({pending} đơn chờ duyệt)
      </button>
      {c.error && <p role="alert">{c.error}</p>}
      {c.notice && <p role="status">{c.notice}</p>}
      {open && (
        <>
          <button disabled={c.busy} onClick={c.reload}>
            Tải lại đơn
          </button>
          {!c.busy && !c.items.length && <p>Chưa có đơn đăng ký.</p>}
          <ul className="address-list">
            {c.items.map((a) => (
              <li key={a.id}>
                <strong>{a.fullName}</strong>
                <p>
                  {a.email} · {a.phone}
                </p>
                <p>{a.introduction}</p>
                <p>
                  {
                    {
                      PENDING: "Chờ duyệt",
                      APPROVED: "Đã duyệt",
                      REJECTED: "Đã từ chối",
                    }[a.status]
                  }
                </p>
                {a.status === "PENDING" && (
                  <>
                    <button
                      disabled={c.busy || shop.status !== "ACTIVE"}
                      onClick={() => c.decide(a.id, true)}
                    >
                      Duyệt
                    </button>
                    <button
                      disabled={c.busy || shop.status !== "ACTIVE"}
                      onClick={() => c.decide(a.id, false)}
                    >
                      Từ chối
                    </button>
                  </>
                )}
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  );
}
