import { useRef, useState } from 'react'
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
  const imageFileInputRef = useRef(null)

  const openForm = () => setIsFormOpen(true)
  const openNewForm = () => {
    c.cancel()
    if (imageFileInputRef.current) imageFileInputRef.current.value = ''
    openForm()
  }
  const closeForm = () => {
    setIsFormOpen(false)
    c.cancel()
  }

  const handleEdit = async (p) => {
    openForm()
    await c.edit(p)
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
            <Button onClick={openNewForm} disabled={disabled}>Thêm sản phẩm mới</Button>
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
      <Modal isOpen={isFormOpen} onClose={closeForm} className="mx-4 max-h-[calc(100dvh-2rem)] w-[calc(100%-2rem)] max-w-3xl overflow-y-auto p-0">
        <div className="p-6 border-b border-gray-100 dark:border-gray-800">
          <h3 className="text-xl font-semibold text-gray-900 dark:text-white">
            {c.editing ? 'Sửa sản phẩm' : 'Đăng sản phẩm mới'}
          </h3>
        </div>
        
        <form onSubmit={handleSave} className="min-w-0 p-6">
          <fieldset disabled={disabled} className="min-w-0 space-y-6">
            <div className="grid min-w-0 grid-cols-1 gap-6 md:grid-cols-2">
              <div className="min-w-0 space-y-4">
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
              
              <div className="min-w-0 space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Mô tả</label>
                  <textarea maxLength={5000} rows={8} value={c.form.description} onChange={e => c.setForm({ ...c.form, description: e.target.value })} 
                    className="min-h-56 w-full rounded-lg border border-gray-300 px-4 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 md:min-h-72 dark:border-gray-700 dark:bg-gray-900" 
                  />
                </div>
                
              </div>
            </div>

            <div className="min-w-0 space-y-3 rounded-lg border border-gray-200 bg-gray-50 p-4 dark:border-gray-800 dark:bg-gray-800/50">
              <div className="flex justify-between items-center">
                <h4 className="text-sm font-medium text-gray-900 dark:text-white">Ảnh sản phẩm</h4>
                <span className="text-xs text-gray-500">Tối đa 10 ảnh</span>
              </div>
              <p className="text-xs text-gray-500">Ảnh được xem trước trên thiết bị và chỉ tải lên khi lưu sản phẩm.</p>
              <input
                ref={imageFileInputRef}
                type="file"
                accept="image/jpeg,image/png,image/webp"
                multiple
                onChange={e => c.setImageFiles(Array.from(e.target.files || []))}
                className="block w-full text-xs text-gray-600 file:mr-2 file:rounded file:border-0 file:bg-brand-500 file:px-3 file:py-1.5 file:font-medium file:text-white"
              />
              {c.imageSelectionError && (
                <div className="rounded-lg bg-error-50 p-3 text-xs text-error-500 dark:bg-error-500/10">
                  {c.imageSelectionError}
                </div>
              )}
              <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
                {c.mediaImages.map(image => (
                  <div key={image.id} className="flex items-center gap-2 bg-white dark:bg-gray-900 p-2 rounded border border-gray-200 dark:border-gray-700">
                    <img src={image.imageUrl} alt="" className="h-10 w-10 object-cover" />
                    <button
                      type="button"
                      onClick={() => c.setPrimary(c.editing, image.id)}
                      className={`rounded px-2 py-1 text-xs font-medium ${image.isPrimary ? 'bg-brand-50 text-brand-700 dark:bg-brand-500/10 dark:text-brand-400' : 'text-gray-600 hover:bg-gray-100 dark:hover:bg-gray-800'}`}
                    >
                      {image.isPrimary ? 'Ảnh chính' : 'Đặt chính'}
                    </button>
                    <button type="button" onClick={() => c.deleteImage(c.editing, image.id)} className="p-1 text-error-500 hover:bg-error-50 rounded" aria-label="Xóa ảnh">
                      <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
                    </button>
                  </div>
                ))}
                {(c.mediaImages?.length || 0) === 0 && (c.form.imageFiles?.length || 0) === 0 && (
                  <p className="text-sm text-gray-500 text-center py-4">Chưa có ảnh nào.</p>
                )}
                {c.uploadingImages && (
                  <div className="flex items-center gap-3 rounded border border-brand-200 bg-brand-50 p-3 text-sm text-brand-700 dark:border-brand-500/30 dark:bg-brand-500/10 dark:text-brand-300">
                    <span className="h-4 w-4 animate-spin rounded-full border-2 border-brand-300 border-t-brand-600 dark:border-brand-600 dark:border-t-brand-300" />
                    <span className="truncate">Đang tải ảnh: {c.uploadingImageName}</span>
                  </div>
                )}
              </div>
              {c.imagePreviews.length > 0 && (
                <div className="min-w-0 space-y-2">
                  <h4 className="text-sm font-medium text-gray-900 dark:text-white">Xem trước ảnh sản phẩm</h4>
                  <div className="flex min-w-0 max-w-full gap-2 overflow-x-auto pb-2">
                    {c.imagePreviews.map((preview, index) => (
                      <label key={`${preview.file.name}-${preview.file.lastModified}-${index}`} className="flex w-44 shrink-0 cursor-pointer items-center gap-2 rounded border border-gray-200 bg-white p-2 dark:border-gray-700 dark:bg-gray-900">
                        <img src={preview.url} alt={`Xem trước ${preview.file.name}`} className="h-12 w-12 shrink-0 rounded object-cover" />
                        <span className="min-w-0 flex-1">
                          <span className="block truncate text-xs text-gray-700 dark:text-gray-300">{preview.file.name}</span>
                          <span className="mt-1 flex items-center gap-1 text-xs text-gray-500">
                            <input
                              type="radio"
                              name="primary-product-image"
                              checked={c.form.primaryImageIndex === index}
                              onChange={() => c.setPrimaryImage(index)}
                            />
                            Ảnh chính
                          </span>
                        </span>
                      </label>
                    ))}
                  </div>
                </div>
              )}
            </div>

            <div className="flex justify-end gap-3 pt-4 border-t border-gray-100 dark:border-gray-800">
              <Button type="button" variant="outline" onClick={closeForm} disabled={c.busy}>Hủy</Button>
              <Button
                type="submit"
                disabled={!c.categories.length || c.busy || c.uploadingImages}
                startIcon={c.uploadingImages ? (
                  <span className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
                ) : null}
              >
                {c.uploadingImages ? 'Đang tải ảnh...' : 'Lưu sản phẩm'}
              </Button>
            </div>
          </fieldset>
        </form>
      </Modal>
    </div>
  )
}
