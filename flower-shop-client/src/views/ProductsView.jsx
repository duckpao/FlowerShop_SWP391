import { useState } from 'react'
import { useProductsController } from '../controllers/useProductsController'
import { formatPrice, productStatusLabels, productTypeLabels, ratingLabel } from '../models/productModel'
import ComponentCard from '../components/common/ComponentCard'
import Button from '../components/ui/button/Button'
import Badge from '../components/ui/badge/Badge'
import { Modal } from '../components/ui/modal'

export default function ProductsView({ shop, manage = false }) {
  const c = useProductsController(shop.id, manage)
  const disabled = c.busy || shop.status !== 'ACTIVE'
  const [isFormOpen, setIsFormOpen] = useState(false)

  const openForm = () => setIsFormOpen(true)
  const closeForm = () => {
    setIsFormOpen(false)
    c.cancel()
  }

  const handleEdit = (p) => {
    c.edit(p)
    openForm()
  }

  const handleSave = async (e) => {
    e.preventDefault()
    await c.save(e)
    if (!c.error) {
      setIsFormOpen(false)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <h2 className="text-2xl font-bold text-gray-900 dark:text-white">
          {manage ? 'Quản lý mặt hàng' : 'Sản phẩm của cửa hàng'}
        </h2>
        <div className="flex gap-2">
          <Button variant="outline" disabled={c.busy} onClick={c.reload}>Tải lại</Button>
          {manage && (
            <Button onClick={openForm} disabled={disabled}>Thêm sản phẩm mới</Button>
          )}
        </div>
      </div>

      {c.error && (
        <div className="rounded-lg bg-error-50 p-4 text-sm text-error-500">
          {c.error}
        </div>
      )}
      {c.notice && (
        <div className="rounded-lg bg-success-50 p-4 text-sm text-success-600">
          {c.notice}
        </div>
      )}

      {c.data && (
        <div className="space-y-4">
          <p className="text-gray-500 text-sm">Hiển thị {c.data.totalElements} sản phẩm</p>
          
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-6">
            {c.data.content.map(p => (
              <div key={p.id} className="group relative flex flex-col overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-sm transition-all hover:shadow-md dark:border-gray-800 dark:bg-gray-900">
                <div className="relative aspect-square w-full overflow-hidden bg-gray-100 dark:bg-gray-800">
                  {manage ? (
                    p.images?.length > 0 ? (
                      <img src={p.images[0]} alt={p.name} className="h-full w-full object-cover object-center transition-transform duration-300 group-hover:scale-105" loading="lazy" />
                    ) : (
                      <div className="flex h-full items-center justify-center text-sm text-gray-400">Chưa có ảnh</div>
                    )
                  ) : (
                    p.imageUrl ? (
                      <img src={p.imageUrl} alt={p.name} className="h-full w-full object-cover object-center transition-transform duration-300 group-hover:scale-105" loading="lazy" />
                    ) : (
                      <div className="flex h-full items-center justify-center text-sm text-gray-400">Chưa có ảnh</div>
                    )
                  )}
                  
                  <div className="absolute top-2 left-2 flex flex-col gap-1">
                    <Badge color={p.type === 'CUSTOM' ? 'brand' : 'gray'}>
                      {productTypeLabels[p.type] || productTypeLabels.READY_MADE}
                    </Badge>
                  </div>
                  
                  {manage && p.adminHidden && (
                    <div className="absolute top-2 right-2">
                      <Badge color="error">Đã ẩn</Badge>
                    </div>
                  )}
                </div>
                
                <div className="flex flex-1 flex-col p-4">
                  <h3 className="text-sm font-medium text-gray-900 line-clamp-2 dark:text-white">
                    {manage ? p.name : <a href={`/products/${encodeURIComponent(p.id)}`} className="hover:text-brand-500 hover:underline">{p.name}</a>}
                  </h3>
                  
                  <div className="mt-2 flex items-center justify-between">
                    <span className="text-lg font-bold text-brand-500">{formatPrice(p.price)}</span>
                  </div>
                  
                  <div className="mt-2 flex flex-col gap-1 text-xs text-gray-500">
                    <div className="flex justify-between">
                      <span>{p.categoryName}</span>
                      <span>Còn {p.stock}</span>
                    </div>
                    {!manage && <span>{ratingLabel(p)}</span>}
                    {manage && <span className="font-medium text-gray-700 dark:text-gray-300">{productStatusLabels[p.status] || p.status}</span>}
                  </div>
                  
                  {manage && (
                    <div className="mt-4 flex gap-2 border-t border-gray-100 pt-4 dark:border-gray-800">
                      <Button size="sm" variant="outline" className="flex-1" disabled={disabled} onClick={() => handleEdit(p)}>
                        Sửa
                      </Button>
                      <Button size="sm" variant="outline" className="flex-1 text-error-500 hover:bg-error-50 hover:border-error-200" disabled={disabled || p.status === 'INACTIVE'} onClick={() => c.hide(p.id)}>
                        Ẩn
                      </Button>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>

          {c.data.totalPages > 1 && (
            <div className="flex justify-between items-center mt-8 pt-4 border-t border-gray-100 dark:border-gray-800">
              <Button variant="outline" disabled={c.busy || c.page === 0} onClick={() => c.setPage(x => x - 1)}>
                Trang trước
              </Button>
              <span className="text-sm text-gray-500">
                Trang {c.page + 1} / {c.data.totalPages}
              </span>
              <Button variant="outline" disabled={c.busy || c.page + 1 >= c.data.totalPages} onClick={() => c.setPage(x => x + 1)}>
                Trang sau
              </Button>
            </div>
          )}
        </div>
      )}

      {/* Product Form Modal */}
      <Modal isOpen={isFormOpen} onClose={closeForm} className="max-w-3xl p-0">
        <div className="p-6 border-b border-gray-100 dark:border-gray-800">
          <h3 className="text-xl font-semibold text-gray-900 dark:text-white">
            {c.editing ? 'Sửa sản phẩm' : 'Đăng sản phẩm mới'}
          </h3>
        </div>
        
        <form onSubmit={handleSave} className="p-6">
          <fieldset disabled={disabled} className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Tên sản phẩm *</label>
                  <input required maxLength={255} value={c.form.name} onChange={e => c.setForm({ ...c.form, name: e.target.value })} 
                    className="w-full rounded-lg border border-gray-300 px-4 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-700 dark:bg-gray-900" 
                  />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Danh mục *</label>
                  <select required value={c.form.categoryId} onChange={e => c.setForm({ ...c.form, categoryId: e.target.value })}
                    className="w-full rounded-lg border border-gray-300 px-4 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-700 dark:bg-gray-900">
                    <option value="">Chọn danh mục</option>
                    {c.categories.map(x => <option key={x.id} value={x.id}>{x.name}</option>)}
                  </select>
                  {!c.categories.length && <p className="mt-1 text-xs text-error-500">Chưa có danh mục hoạt động.</p>}
                </div>
                
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Giá (VND) *</label>
                    <input required type="number" min="0.01" max="9999999999.99" step="0.01" value={c.form.price} onChange={e => c.setForm({ ...c.form, price: e.target.value })} 
                      className="w-full rounded-lg border border-gray-300 px-4 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-700 dark:bg-gray-900" 
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Tồn kho *</label>
                    <input required type="number" min="0" max="1000000" step="1" value={c.form.stock} onChange={e => c.setForm({ ...c.form, stock: e.target.value })} 
                      className="w-full rounded-lg border border-gray-300 px-4 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-700 dark:bg-gray-900" 
                    />
                  </div>
                </div>
                
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Trạng thái</label>
                    <select value={c.form.status} onChange={e => c.setForm({ ...c.form, status: e.target.value })}
                      className="w-full rounded-lg border border-gray-300 px-4 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-700 dark:bg-gray-900">
                      {Object.entries(productStatusLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Loại</label>
                    <select value={c.form.type} onChange={e => c.setForm({ ...c.form, type: e.target.value })}
                      className="w-full rounded-lg border border-gray-300 px-4 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-700 dark:bg-gray-900">
                      {Object.entries(productTypeLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                    </select>
                  </div>
                </div>
              </div>
              
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Mô tả</label>
                  <textarea maxLength={5000} rows={5} value={c.form.description} onChange={e => c.setForm({ ...c.form, description: e.target.value })} 
                    className="w-full rounded-lg border border-gray-300 px-4 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-700 dark:bg-gray-900" 
                  />
                </div>
                
                <div className="border border-gray-200 dark:border-gray-800 rounded-lg p-4 bg-gray-50 dark:bg-gray-800/50">
                  <div className="flex justify-between items-center mb-2">
                    <h4 className="text-sm font-medium text-gray-900 dark:text-white">Ảnh sản phẩm</h4>
                    {c.form.images.length < 10 && (
                      <button type="button" onClick={c.addImage} className="text-xs text-brand-500 font-medium hover:underline">
                        + Thêm ảnh
                      </button>
                    )}
                  </div>
                  <p className="text-xs text-gray-500 mb-3">Dán URL ảnh HTTPS, tối đa 10 ảnh. Ảnh chính hiển thị ở trang danh sách.</p>
                  
                  <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
                    {c.form.images.map((image, index) => (
                      <div key={index} className="flex items-center gap-2 bg-white dark:bg-gray-900 p-2 rounded border border-gray-200 dark:border-gray-700">
                        <input type="url" maxLength={500} pattern="https://.*" placeholder="https://..." value={image.url}
                          onChange={e => c.setImageUrl(index, e.target.value)} 
                          className="flex-1 rounded border border-gray-300 px-2 py-1 text-xs focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800"
                        />
                        <label className="flex items-center gap-1 text-xs cursor-pointer">
                          <input type="radio" name="primary-image" checked={image.primary} onChange={() => c.setPrimaryImage(index)} className="text-brand-500 focus:ring-brand-500" />
                          <span className="hidden sm:inline">Ảnh chính</span>
                        </label>
                        <button type="button" onClick={() => c.removeImage(index)} className="p-1 text-error-500 hover:bg-error-50 rounded">
                          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
                        </button>
                      </div>
                    ))}
                    {c.form.images.length === 0 && (
                      <p className="text-sm text-gray-500 text-center py-4">Chưa có ảnh nào.</p>
                    )}
                  </div>
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-3 pt-4 border-t border-gray-100 dark:border-gray-800">
              <Button type="button" variant="outline" onClick={closeForm}>Hủy</Button>
              <Button type="submit" disabled={!c.categories.length || c.busy}>Lưu sản phẩm</Button>
            </div>
          </fieldset>
        </form>
      </Modal>
    </div>
  )
}
