import { useState } from "react";
import { useProductsController } from "../controllers/useProductsController";
import ComponentCard from "../components/common/ComponentCard";
import Button from "../components/ui/button/Button";
import Input from "../components/form/input/InputField";
import TextArea from "../components/form/input/TextArea";
import Select from "../components/form/Select";
import Label from "../components/form/Label";
import Badge from "../components/ui/badge/Badge";
import { Table, TableBody, TableCell, TableHeader, TableRow } from "../components/ui/table";

export default function ProductsView({ shop, manage = false }) {
  const c = useProductsController(shop.id, manage);
  const disabled = c.busy || shop.status !== "ACTIVE";
  const [videoTitle, setVideoTitle] = useState("");
  const [videoDescription, setVideoDescription] = useState("");

  const getStatusColor = (status) => {
    switch (status) {
      case "ACTIVE": return "success";
      case "INACTIVE": return "error";
      case "OUT_OF_STOCK": return "warning";
      default: return "primary";
    }
  };

  const getStatusLabel = (status) => {
    switch (status) {
      case "ACTIVE": return "Đăng bán";
      case "INACTIVE": return "Ẩn";
      case "OUT_OF_STOCK": return "Hết hàng";
      default: return status;
    }
  };

  return (
    <div className="space-y-6">
      <ComponentCard title={manage ? "Quản lý mặt hàng của shop" : "Sản phẩm của cửa hàng"}>
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
        
        <div className="mb-6 flex gap-3">
          <Button disabled={c.busy} onClick={c.reload} variant="outline" size="sm">
            Tải lại sản phẩm
          </Button>
          {manage && !c.editing && (
            <Button disabled={disabled} onClick={() => c.setForm({...c.form, id: undefined})} size="sm">
              Đăng sản phẩm mới
            </Button>
          )}
        </div>

        {manage && (
          <div className="mb-8 rounded-xl border border-gray-200 bg-white p-5 dark:border-white/5 dark:bg-white/3">
            <h3 className="text-lg font-semibold text-gray-800 dark:text-white/90 mb-4">
              {c.editing ? "Sửa sản phẩm" : "Đăng sản phẩm mới"}
            </h3>
            <form onSubmit={c.save}>
              <fieldset disabled={disabled} className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="md:col-span-2">
                  <Label>Tên sản phẩm</Label>
                  <Input
                    required
                    maxLength={255}
                    value={c.form.name}
                    onChange={(e) => c.setForm({ ...c.form, name: e.target.value })}
                  />
                </div>
                <div className="md:col-span-2">
                  <Label>Mô tả</Label>
                  <TextArea
                    maxLength={5000}
                    value={c.form.description}
                    onChange={(e) =>
                      c.setForm({ ...c.form, description: e.target.value })
                    }
                    rows={4}
                  />
                </div>
                <div>
                  <Label>Danh mục</Label>
                  <select
                    required
                    value={c.form.categoryId}
                    onChange={(e) =>
                      c.setForm({ ...c.form, categoryId: e.target.value })
                    }
                    className="w-full rounded-lg border border-gray-200 bg-transparent px-4 py-3 text-sm text-gray-800 outline-none transition-colors focus:border-brand-500 dark:border-white/10 dark:text-white/90 dark:focus:border-brand-500"
                  >
                    <option value="">Chọn danh mục</option>
                    {c.categories.map((x) => (
                      <option key={x.id} value={x.id}>
                        {x.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <Label>Trạng thái</Label>
                  <select
                    value={c.form.status}
                    onChange={(e) =>
                      c.setForm({ ...c.form, status: e.target.value })
                    }
                    className="w-full rounded-lg border border-gray-200 bg-transparent px-4 py-3 text-sm text-gray-800 outline-none transition-colors focus:border-brand-500 dark:border-white/10 dark:text-white/90 dark:focus:border-brand-500"
                  >
                    <option value="ACTIVE">Đăng bán</option>
                    <option value="INACTIVE">Ẩn</option>
                    <option value="OUT_OF_STOCK">Hết hàng</option>
                  </select>
                </div>
                <div>
                  <Label>Giá (VND)</Label>
                  <Input
                    required
                    type="number"
                    min="0.01"
                    max="9999999999.99"
                    step="0.01"
                    value={c.form.price}
                    onChange={(e) =>
                      c.setForm({ ...c.form, price: e.target.value })
                    }
                  />
                </div>
                <div>
                  <Label>Tồn kho</Label>
                  <Input
                    required
                    type="number"
                    min="0"
                    max="1000000"
                    step="1"
                    value={c.form.stock}
                    onChange={(e) =>
                      c.setForm({ ...c.form, stock: e.target.value })
                    }
                  />
                </div>
                
                {!c.categories.length && (
                  <p className="text-warning-500 text-sm md:col-span-2">
                    Chưa có danh mục đang hoạt động. Cần bổ sung danh mục trong dữ
                    liệu hệ thống trước khi đăng sản phẩm.
                  </p>
                )}
                
                <div className="flex gap-3 mt-2 md:col-span-2">
                  <Button type="submit" disabled={!c.categories.length}>
                    Lưu sản phẩm
                  </Button>
                  {c.editing && (
                    <Button type="button" variant="outline" onClick={c.cancel}>
                      Hủy sửa
                    </Button>
                  )}
                </div>
              </fieldset>
            </form>
          </div>
        )}

        {manage && c.editing && (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
            <div className="rounded-xl border border-gray-200 bg-white p-5 dark:border-white/5 dark:bg-white/3">
              <h3 className="text-lg font-semibold text-gray-800 dark:text-white/90 mb-4">Ảnh sản phẩm</h3>
              <div className="mb-4">
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  disabled={disabled}
                  className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-semibold file:bg-brand-50 file:text-brand-500 hover:file:bg-brand-100 dark:file:bg-white/5 dark:file:text-white/90 dark:hover:file:bg-white/10"
                  onChange={(e) => {
                    const file = e.target.files[0];
                    e.target.value = "";
                    if (file) c.uploadImage(file);
                  }}
                />
              </div>
              
              {!c.images.length ? (
                <p className="text-gray-500 dark:text-gray-400 text-sm">Chưa có ảnh nào cho sản phẩm này.</p>
              ) : (
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
                  {c.images.map((img) => (
                    <div key={img.id} className="relative group rounded-lg overflow-hidden border border-gray-200 dark:border-white/10">
                      <img
                        src={img.url}
                        alt=""
                        className="w-full h-24 object-cover"
                      />
                      {img.primary && (
                        <div className="absolute top-0 left-0 bg-brand-500 text-white text-[10px] px-2 py-1 rounded-br-lg">
                          Đại diện
                        </div>
                      )}
                      <div className="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          className="!text-error-500 !border-error-500 hover:!bg-error-500 hover:!text-white bg-white"
                          disabled={disabled}
                          onClick={() => c.deleteImage(img.id)}
                        >
                          Xoá
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
            
            <div className="rounded-xl border border-gray-200 bg-white p-5 dark:border-white/5 dark:bg-white/3">
              <h3 className="text-lg font-semibold text-gray-800 dark:text-white/90 mb-4">Video sản phẩm</h3>
              <div className="space-y-3 mb-4">
                <div>
                  <Label>Tiêu đề video (không bắt buộc)</Label>
                  <Input
                    maxLength={255}
                    value={videoTitle}
                    onChange={(e) => setVideoTitle(e.target.value)}
                  />
                </div>
                <div>
                  <Label>Mô tả video (không bắt buộc)</Label>
                  <Input
                    maxLength={1000}
                    value={videoDescription}
                    onChange={(e) => setVideoDescription(e.target.value)}
                  />
                </div>
                <div>
                  <input
                    type="file"
                    accept="video/mp4,video/webm,video/quicktime"
                    disabled={disabled}
                    className="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-semibold file:bg-brand-50 file:text-brand-500 hover:file:bg-brand-100 dark:file:bg-white/5 dark:file:text-white/90 dark:hover:file:bg-white/10"
                    onChange={(e) => {
                      const file = e.target.files[0];
                      e.target.value = "";
                      if (file) {
                        c.uploadVideo(file, videoTitle, videoDescription);
                        setVideoTitle("");
                        setVideoDescription("");
                      }
                    }}
                  />
                </div>
              </div>
              
              {!c.videos.length ? (
                <p className="text-gray-500 dark:text-gray-400 text-sm">Chưa có video nào cho sản phẩm này.</p>
              ) : (
                <div className="space-y-4">
                  {c.videos.map((v) => (
                    <div key={v.id} className="rounded-lg border border-gray-200 p-3 dark:border-white/10">
                      <video src={v.url} controls className="w-full rounded mb-2" />
                      {v.title && <p className="font-medium text-gray-800 dark:text-white/90 text-sm">{v.title}</p>}
                      {v.description && <p className="text-gray-500 dark:text-gray-400 text-sm mt-1">{v.description}</p>}
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        className="mt-3 !text-error-500 !border-error-500"
                        disabled={disabled}
                        onClick={() => c.deleteVideo(v.id)}
                      >
                        Xoá video
                      </Button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {c.data && (
          <div>
            <div className="mb-4 flex items-center justify-between">
              <p className="text-gray-500 dark:text-gray-400 font-medium">
                Tổng cộng {c.data.totalElements} sản phẩm
              </p>
            </div>
            
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6 mb-6">
              {c.data.content.map((p) => (
                <div key={p.id} className="rounded-xl border border-gray-200 bg-white overflow-hidden flex flex-col dark:border-white/5 dark:bg-white/3">
                  <div className="aspect-video bg-gray-100 dark:bg-gray-800 flex items-center justify-center relative overflow-hidden">
                    {p.images && p.images.length > 0 ? (
                      <img src={p.images[0]} alt={p.name} className="w-full h-full object-cover" />
                    ) : (
                      <svg className="w-10 h-10 text-gray-300 dark:text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                      </svg>
                    )}
                  </div>
                  <div className="p-5 flex-grow">
                    <div className="flex justify-between items-start mb-2">
                      <h3 className="font-semibold text-lg text-gray-800 dark:text-white/90 truncate pr-2">
                        {p.name}
                      </h3>
                      {manage && (
                        <Badge color={getStatusColor(p.status)}>
                          {getStatusLabel(p.status)}
                        </Badge>
                      )}
                    </div>
                    <p className="text-gray-500 dark:text-gray-400 text-sm line-clamp-2 mb-4 h-10">
                      {p.description}
                    </p>
                    <div className="space-y-1 text-sm text-gray-600 dark:text-gray-300">
                      <p className="font-semibold text-brand-500 dark:text-brand-400">
                        {Number(p.price).toLocaleString("vi-VN")} đ
                      </p>
                      <p>Danh mục: {p.categoryName}</p>
                      <p>Tồn kho: {p.stock}</p>
                    </div>
                  </div>
                  
                  {manage && (
                    <div className="border-t border-gray-100 bg-gray-50 p-4 flex gap-2 dark:border-white/5 dark:bg-white/5">
                      <Button 
                        disabled={disabled} 
                        onClick={() => c.edit(p)}
                        size="sm"
                        className="flex-1"
                      >
                        Sửa
                      </Button>
                      <Button
                        variant="outline"
                        disabled={disabled || p.status === "INACTIVE"}
                        onClick={() => c.hide(p.id)}
                        size="sm"
                        className="flex-1"
                      >
                        Ẩn
                      </Button>
                    </div>
                  )}
                </div>
              ))}
            </div>
            
            {c.data.totalPages > 1 && (
              <div className="flex justify-center gap-2">
                <Button
                  variant="outline"
                  disabled={c.busy || c.page === 0}
                  onClick={() => c.setPage((x) => x - 1)}
                >
                  Trang trước
                </Button>
                <div className="flex items-center px-4 text-sm font-medium text-gray-700 dark:text-gray-300">
                  {c.page + 1} / {c.data.totalPages}
                </div>
                <Button
                  variant="outline"
                  disabled={c.busy || c.page + 1 >= c.data.totalPages}
                  onClick={() => c.setPage((x) => x + 1)}
                >
                  Trang sau
                </Button>
              </div>
            )}
          </div>
        )}
      </ComponentCard>
    </div>
  );
}
