import { useRef, useState } from 'react'
import { useProductsController } from '../controllers/useProductsController'
import { formatPrice, productStatusLabels, productTypeLabels, ratingLabel } from '../models/productModel'
import Button from '../components/ui/button/Button'
import Badge from '../components/ui/badge/Badge'
import { Modal } from '../components/ui/modal'

const PRODUCT_PRICE_MAX = 9999999999.99
const PRODUCT_STOCK_MAX = 1000000

function validateProductForm(form) {
  const errors = {}

  if (!form.name.trim()) {
    errors.name = 'Vui lòng nhập tên sản phẩm.'
  } else if (form.name.length > 255) {
    errors.name = 'Tên sản phẩm không được vượt quá 255 ký tự.'
  }

  if (!form.categoryId) errors.categoryId = 'Vui lòng chọn danh mục.'

  const priceText = String(form.price).trim()
  const price = Number(priceText)
  if (!priceText) {
    errors.price = 'Vui lòng nhập giá sản phẩm.'
  } else if (!Number.isFinite(price) || price <= 0) {
    errors.price = 'Giá sản phẩm phải lớn hơn 0.'
  } else if (price > PRODUCT_PRICE_MAX) {
    errors.price = 'Giá sản phẩm không được vượt quá 9.999.999.999,99 VND.'
  } else if (!/^\d+(\.\d{1,2})?$/.test(priceText)) {
    errors.price = 'Giá sản phẩm chỉ được có tối đa 2 chữ số thập phân.'
  }

  if (form.type !== 'CUSTOM') {
    const stockText = String(form.stock).trim()
    const stock = Number(stockText)
    if (!stockText) {
      errors.stock = 'Vui lòng nhập số lượng tồn kho.'
    } else if (!Number.isInteger(stock)) {
      errors.stock = 'Tồn kho phải là số nguyên.'
    } else if (stock < 0) {
      errors.stock = 'Tồn kho không được nhỏ hơn 0.'
    } else if (stock > PRODUCT_STOCK_MAX) {
      errors.stock = 'Tồn kho không được vượt quá 1.000.000 sản phẩm.'
    }
  }

  if (form.description.length > 5000) {
    errors.description = 'Mô tả không được vượt quá 5.000 ký tự.'
  }

  return errors
}

function fieldClass(hasError, extra = '') {
  const borderClass = hasError
    ? 'border-error-500 focus:border-error-500 focus:ring-error-500'
    : 'border-gray-300 focus:border-brand-500 focus:ring-brand-500 dark:border-gray-700'
  return `w-full rounded-lg border px-4 py-2 text-sm focus:outline-none focus:ring-1 dark:bg-gray-900 ${borderClass} ${extra}`
}

function FieldError({ id, message }) {
  if (!message) return null
  return <p id={id} role="alert" className="mt-1 text-xs text-error-500">{message}</p>
}

export default function ProductsView({ shop, manage = false }) {
  const c = useProductsController(shop.id, manage)
  const disabled = c.busy || shop.status !== 'ACTIVE'
  const [isFormOpen, setIsFormOpen] = useState(false)
  const [fieldErrors, setFieldErrors] = useState({})
  const imageFileInputRef = useRef(null)
  const videoFileInputRef = useRef(null)

  const openForm = () => setIsFormOpen(true)
  const openNewForm = () => {
    c.cancel()
    setFieldErrors({})
    if (imageFileInputRef.current) imageFileInputRef.current.value = ''
    if (videoFileInputRef.current) videoFileInputRef.current.value = ''
    openForm()
  }
  const closeForm = () => {
    setIsFormOpen(false)
    setFieldErrors({})
    if (imageFileInputRef.current) imageFileInputRef.current.value = ''
    if (videoFileInputRef.current) videoFileInputRef.current.value = ''
    c.cancel()
  }

  const handleEdit = async (p) => {
    setFieldErrors({})
    openForm()
    await c.edit(p)
  }

  const handleSave = async (e) => {
    e.preventDefault()
    const errors = validateProductForm(c.form)
    setFieldErrors(errors)
    const firstInvalidField = Object.keys(errors)[0]
    if (firstInvalidField) {
      e.currentTarget.elements.namedItem(firstInvalidField)?.focus()
      return
    }
    const saved = await c.save(e)
    if (saved) setIsFormOpen(false)
  }

  const updateFormField = (field, value) => {
    const nextForm = { ...c.form, [field]: value }
    c.setForm(nextForm)
    setFieldErrors(current => {
      if (!current[field] && !(field === 'type' && current.stock)) return current
      const nextErrors = validateProductForm(nextForm)
      return { ...current, [field]: nextErrors[field], ...(field === 'type' ? { stock: nextErrors.stock } : {}) }
    })
  }

  const validateField = field => {
    const message = validateProductForm(c.form)[field]
    setFieldErrors(current => ({ ...current, [field]: message }))
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
                  
                  <div className="absolute top-2 left-2 flex flex-col gap-1 z-10 pointer-events-none">
                    {p.type === 'CUSTOM' ? (
                      <span className="inline-flex items-center rounded-lg bg-amber-500 text-white px-2.5 py-0.5 text-xs font-bold shadow-xs">
                        Hàng đặt trước
                      </span>
                    ) : (
                      <span className="inline-flex items-center rounded-lg bg-emerald-600 text-white px-2.5 py-0.5 text-xs font-bold shadow-xs">
                        Có sẵn
                      </span>
                    )}
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
                    <div className="flex justify-between items-center">
                      <span>{p.categoryName}</span>
                      {p.type === 'CUSTOM' ? (
                        <span className="font-semibold text-amber-600 dark:text-amber-400">Hàng đặt trước</span>
                      ) : (
                        <span>Còn {p.stock}</span>
                      )}
                    </div>
                    {!manage && <span>{ratingLabel(p)}</span>}
                    {manage && <span className="font-medium text-gray-700 dark:text-gray-300">{productStatusLabels[p.status] || p.status}</span>}
                  </div>
                  
                  {manage && (
                    <div className="mt-4 flex gap-2 border-t border-gray-100 pt-4 dark:border-gray-800">
                      <Button size="sm" variant="outline" className="flex-1" disabled={disabled} onClick={() => handleEdit(p)}>
                        Sửa
                      </Button>
                      {p.status === 'INACTIVE' ? (
                        <Button
                          size="sm"
                          variant="outline"
                          className="flex-1 text-emerald-600 hover:bg-emerald-50 hover:border-emerald-200 dark:text-emerald-400 dark:hover:bg-emerald-500/10"
                          disabled={disabled}
                          onClick={() => c.unhide(p.id)}
                        >
                          Bỏ ẩn
                        </Button>
                      ) : (
                        <Button
                          size="sm"
                          variant="outline"
                          className="flex-1 text-error-500 hover:bg-error-50 hover:border-error-200"
                          disabled={disabled}
                          onClick={() => c.hide(p.id)}
                        >
                          Ẩn
                        </Button>
                      )}
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
        
        <form noValidate onSubmit={handleSave} className="min-w-0 p-6">
          <fieldset disabled={disabled} className="min-w-0 space-y-6">
            {c.error && (
              <div role="alert" className="rounded-lg bg-error-50 p-3 text-sm text-error-500 dark:bg-error-500/10">
                {c.error}
              </div>
            )}
            {Object.keys(fieldErrors).some(field => fieldErrors[field]) && (
              <div role="alert" className="rounded-lg border border-error-200 bg-error-50 p-3 text-sm text-error-600 dark:border-error-500/30 dark:bg-error-500/10">
                Vui lòng kiểm tra các trường được đánh dấu bên dưới.
              </div>
            )}
            <div className="grid min-w-0 grid-cols-1 gap-6 md:grid-cols-2">
              <div className="min-w-0 space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Tên sản phẩm *</label>
                  <input name="name" required maxLength={255} value={c.form.name} onChange={e => updateFormField('name', e.target.value)} onBlur={() => validateField('name')}
                    aria-invalid={Boolean(fieldErrors.name)} aria-describedby={fieldErrors.name ? 'product-name-error' : undefined}
                    className={fieldClass(Boolean(fieldErrors.name))}
                  />
                  <FieldError id="product-name-error" message={fieldErrors.name} />
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Danh mục *</label>
                  <select name="categoryId" required value={c.form.categoryId} onChange={e => updateFormField('categoryId', e.target.value)} onBlur={() => validateField('categoryId')}
                    aria-invalid={Boolean(fieldErrors.categoryId)} aria-describedby={fieldErrors.categoryId ? 'product-category-error' : undefined}
                    className={fieldClass(Boolean(fieldErrors.categoryId))}>
                    <option value="">Chọn danh mục</option>
                    {c.categories.map(x => <option key={x.id} value={x.id}>{x.name}</option>)}
                  </select>
                  <FieldError id="product-category-error" message={fieldErrors.categoryId} />
                  {!c.categories.length && <p className="mt-1 text-xs text-error-500">Chưa có danh mục hoạt động.</p>}
                </div>
                
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Giá (VND) *</label>
                    <input name="price" required type="number" min="0.01" max="9999999999.99" step="0.01" value={c.form.price} onChange={e => updateFormField('price', e.target.value)} onBlur={() => validateField('price')}
                      aria-invalid={Boolean(fieldErrors.price)} aria-describedby={fieldErrors.price ? 'product-price-error' : 'product-price-help'}
                      className={fieldClass(Boolean(fieldErrors.price))}
                    />
                    <FieldError id="product-price-error" message={fieldErrors.price} />
                    {!fieldErrors.price && <p id="product-price-help" className="mt-1 text-xs text-gray-500">Tối đa 9.999.999.999,99 VND.</p>}
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                      {c.form.type === 'CUSTOM' ? 'Tồn kho (Hàng đặt trước)' : 'Tồn kho *'}
                    </label>
                    <input
                      name="stock"
                      required={c.form.type !== 'CUSTOM'}
                      disabled={c.form.type === 'CUSTOM'}
                      type="number"
                      min="0"
                      max="1000000"
                      step="1"
                      value={c.form.type === 'CUSTOM' ? 0 : c.form.stock}
                      onChange={e => updateFormField('stock', e.target.value)}
                      onBlur={() => validateField('stock')}
                      aria-invalid={Boolean(fieldErrors.stock)}
                      aria-describedby={fieldErrors.stock ? 'product-stock-error' : 'product-stock-help'}
                      className={fieldClass(Boolean(fieldErrors.stock), 'disabled:bg-gray-100 disabled:text-gray-400 dark:disabled:bg-gray-800')}
                    />
                    <FieldError id="product-stock-error" message={fieldErrors.stock} />
                    {!fieldErrors.stock && c.form.type !== 'CUSTOM' && <p id="product-stock-help" className="mt-1 text-xs text-gray-500">Từ 0 đến 1.000.000 sản phẩm.</p>}
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
                    <select value={c.form.type} onChange={e => updateFormField('type', e.target.value)}
                      className="w-full rounded-lg border border-gray-300 px-4 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500 dark:border-gray-700 dark:bg-gray-900">
                      {Object.entries(productTypeLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                    </select>
                  </div>
                </div>
              </div>
              
              <div className="min-w-0 space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Mô tả</label>
                  <textarea name="description" maxLength={5000} rows={8} value={c.form.description} onChange={e => updateFormField('description', e.target.value)} onBlur={() => validateField('description')}
                    aria-invalid={Boolean(fieldErrors.description)} aria-describedby={fieldErrors.description ? 'product-description-error' : undefined}
                    className={fieldClass(Boolean(fieldErrors.description), 'min-h-56 md:min-h-72')}
                  />
                  <FieldError id="product-description-error" message={fieldErrors.description} />
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
                className="block w-full text-xs text-gray-600 file:mr-2 file:rounded file:border-0 file:bg-brand-500 file:px-3 file:py-1.5 file:font-medium file:text-white cursor-pointer"
              />
              {c.imageSelectionError && (
                <div className="rounded-lg bg-error-50 p-3 text-xs text-error-500 dark:bg-error-500/10">
                  {c.imageSelectionError}
                </div>
              )}
              <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
                {c.mediaImages.map(image => (
                  <div key={image.id} className="flex items-center gap-2 bg-white dark:bg-gray-900 p-2 rounded border border-gray-200 dark:border-gray-700">
                    <img src={image.imageUrl} alt="" className="h-10 w-10 object-cover rounded" />
                    <label className="flex items-center gap-1 text-xs text-gray-600 dark:text-gray-300 cursor-pointer">
                      <input
                        type="radio"
                        name="primary-product-image"
                        checked={c.form.primaryImageId === image.id}
                        onChange={() => c.setPrimaryExistingImage(image.id)}
                      />
                      Ảnh chính
                    </label>
                    <button type="button" onClick={() => c.deleteImage(c.editing, image.id)} className="p-1 text-error-500 hover:bg-error-50 rounded ml-auto cursor-pointer" aria-label="Xóa ảnh">
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
                  <h4 className="text-sm font-medium text-gray-900 dark:text-white">Xem trước ảnh mới</h4>
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
                              checked={c.form.primaryImageId == null && c.form.primaryImageIndex === index}
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

            {/* Video Management Section */}
            <div className="min-w-0 space-y-3 rounded-lg border border-gray-200 bg-gray-50 p-4 dark:border-gray-800 dark:bg-gray-800/50">
              <div className="flex justify-between items-center">
                <h4 className="text-sm font-medium text-gray-900 dark:text-white">Video sản phẩm</h4>
                <span className="text-xs text-gray-500">Tối đa 5 video (MP4, WebM, MOV - tối đa 50MB/video)</span>
              </div>
              <p className="text-xs text-gray-500">Video được xem trước trên thiết bị và chỉ tải lên khi lưu sản phẩm.</p>
              <input
                ref={videoFileInputRef}
                type="file"
                accept="video/mp4,video/webm,video/quicktime"
                multiple
                onChange={e => c.setVideoFiles(Array.from(e.target.files || []))}
                className="block w-full text-xs text-gray-600 file:mr-2 file:rounded file:border-0 file:bg-brand-500 file:px-3 file:py-1.5 file:font-medium file:text-white cursor-pointer"
              />
              {c.videoSelectionError && (
                <div className="rounded-lg bg-error-50 p-3 text-xs text-error-500 dark:bg-error-500/10">
                  {c.videoSelectionError}
                </div>
              )}
              <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
                {c.mediaVideos.map(video => (
                  <div key={video.id} className="flex items-center justify-between gap-2 bg-white dark:bg-gray-900 p-2 rounded border border-gray-200 dark:border-gray-700">
                    <div className="flex items-center gap-2 min-w-0">
                      <video src={video.videoUrl} className="h-10 w-16 object-cover rounded bg-black shrink-0" preload="metadata" />
                      <span className="truncate text-xs text-gray-700 dark:text-gray-300">
                        {video.title || 'Video sản phẩm'}
                      </span>
                    </div>
                    <button type="button" onClick={() => c.deleteVideo(c.editing, video.id)} className="p-1 text-error-500 hover:bg-error-50 rounded shrink-0 cursor-pointer" aria-label="Xóa video">
                      <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
                    </button>
                  </div>
                ))}
                {(c.mediaVideos?.length || 0) === 0 && (c.form.videoFiles?.length || 0) === 0 && (
                  <p className="text-sm text-gray-500 text-center py-4">Chưa có video nào.</p>
                )}
                {c.uploadingVideos && (
                  <div className="flex items-center gap-3 rounded border border-brand-200 bg-brand-50 p-3 text-sm text-brand-700 dark:border-brand-500/30 dark:bg-brand-500/10 dark:text-brand-300">
                    <span className="h-4 w-4 animate-spin rounded-full border-2 border-brand-300 border-t-brand-600 dark:border-brand-600 dark:border-t-brand-300" />
                    <span className="truncate">Đang tải video: {c.uploadingVideoName}</span>
                  </div>
                )}
              </div>
              {c.videoPreviews.length > 0 && (
                <div className="min-w-0 space-y-2">
                  <h4 className="text-sm font-medium text-gray-900 dark:text-white">Xem trước video mới</h4>
                  <div className="flex min-w-0 max-w-full gap-2 overflow-x-auto pb-2">
                    {c.videoPreviews.map((preview, index) => (
                      <div key={`${preview.file.name}-${preview.file.lastModified}-${index}`} className="flex w-52 shrink-0 items-center gap-2 rounded border border-gray-200 bg-white p-2 dark:border-gray-700 dark:bg-gray-900">
                        <video src={preview.url} className="h-12 w-16 shrink-0 rounded object-cover bg-black" preload="metadata" />
                        <span className="min-w-0 flex-1">
                          <span className="block truncate text-xs text-gray-700 dark:text-gray-300">{preview.file.name}</span>
                          <span className="text-[10px] text-gray-400">{(preview.file.size / (1024 * 1024)).toFixed(1)} MB</span>
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>

            <div className="flex justify-end gap-3 pt-4 border-t border-gray-100 dark:border-gray-800">
              <Button type="button" variant="outline" onClick={closeForm} disabled={c.busy}>Hủy</Button>
              <Button
                type="submit"
                disabled={!c.categories.length || c.busy || c.uploadingImages || c.uploadingVideos}
                startIcon={(c.uploadingImages || c.uploadingVideos) ? (
                  <span className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
                ) : null}
              >
                {c.uploadingImages ? 'Đang tải ảnh...' : c.uploadingVideos ? 'Đang tải video...' : 'Lưu sản phẩm'}
              </Button>
            </div>
          </fieldset>
        </form>
      </Modal>
    </div>
  )
}
