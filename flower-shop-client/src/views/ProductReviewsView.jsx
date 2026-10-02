import { useProductReviewsController } from '../controllers/useProductReviewsController'
import '../styles/catalog.css'

// Phần review của Product Details: hook tải danh sách + review của mình, nhận thao tác gửi/sửa/xóa/phản hồi.
// ownsShop bên dưới chỉ kiểm tra role SHOP để hiện form; BE mới kiểm tra có thật sự sở hữu shop này hay không.
export default function ProductReviewsView({ product, auth }) {
  const c = useProductReviewsController(product.id, auth?.user)
  const user = auth?.user
  const isCustomer = user?.role === 'CUSTOMER'
  const ownsShop = user?.role === 'SHOP'

  return <section className="catalog-panel">
    <h3>Đánh giá sản phẩm</h3>
    {c.error && <p role="alert" className="message error">{c.error}</p>}
    {c.notice && <p role="status" className="message success">{c.notice}</p>}
    <button disabled={c.busy} onClick={c.reload}>Tải lại đánh giá</button>

    {isCustomer && <form onSubmit={c.submit}>
      <h4>{c.own ? 'Sửa đánh giá của bạn' : 'Viết đánh giá'}</h4>
      <fieldset disabled={c.busy}>
        <label>Số sao
          <select value={c.draft.rating} onChange={e => c.setDraft({ ...c.draft, rating: e.target.value })}>
            {[5, 4, 3, 2, 1].map(n => <option key={n} value={n}>{'★'.repeat(n)} ({n})</option>)}
          </select>
        </label>
        <label>Nhận xét
          <textarea maxLength={2000} value={c.draft.comment}
            onChange={e => c.setDraft({ ...c.draft, comment: e.target.value })} />
        </label>
        <button className="primary">{c.own ? 'Cập nhật đánh giá' : 'Gửi đánh giá'}</button>
        {c.own && <button type="button" onClick={c.remove}>Xóa đánh giá</button>}
      </fieldset>
    </form>}

    {!user && <p><a href={`/login?next=${encodeURIComponent(`/products/${product.id}`)}`}>Đăng nhập</a> để viết đánh giá.</p>}
    {user && !isCustomer && !ownsShop && <p>Chỉ tài khoản Khách hàng mới viết được đánh giá.</p>}

    {c.busy && <p role="status">Đang tải đánh giá…</p>}
    {c.data && <>
      <p>{c.data.totalElements} đánh giá</p>
      {!c.data.content.length && <p>Sản phẩm chưa có đánh giá nào.</p>}
      <ul className="catalog-list">{c.data.content.map(r => <li key={r.id}>
        <strong>{'★'.repeat(r.rating)} · {r.reviewerName}</strong>
        <p className="muted">{new Date(r.createdAt).toLocaleString('vi-VN')}</p>
        {r.comment && <p>{r.comment}</p>}
        {r.shopReply && <p><em>Shop phản hồi: {r.shopReply}</em></p>}
        {ownsShop && <div>
          <label>Phản hồi của shop
            <textarea maxLength={2000} value={c.replies[r.id] ?? r.shopReply ?? ''}
              onChange={e => c.setReplies({ ...c.replies, [r.id]: e.target.value })} />
          </label>
          <button disabled={c.busy} onClick={() => c.reply(product.shopId, r.id)}>Gửi phản hồi</button>
        </div>}
      </li>)}</ul>

      <div className="catalog-actions">
        <button disabled={c.busy || c.page === 0} onClick={() => c.setPage(x => x - 1)}>Trang trước</button>
        <button disabled={c.busy || c.page + 1 >= c.data.totalPages} onClick={() => c.setPage(x => x + 1)}>Trang sau</button>
      </div>
    </>}
  </section>
}
