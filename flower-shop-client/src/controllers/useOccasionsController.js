import { useCallback, useEffect, useState } from 'react'
import { occasionService } from '../services/occasionService'

/**
 * Controller trang Dịp đặc biệt: tải danh sách và thêm, sửa, xóa dịp.
 * Mỗi hàm thay đổi trả về true khi thành công để view đóng form.
 */
export function useOccasionsController(user) {
    const [occasions, setOccasions] = useState([])
    const [loading, setLoading] = useState(false)
    const [saving, setSaving] = useState(false)
    const [error, setError] = useState('')

    const load = useCallback(async () => {
        if (!user) {
            setOccasions([])
            return
        }
        setLoading(true)
        setError('')
        try {
            const data = await occasionService.mine()
            setOccasions(Array.isArray(data) ? data : [])
        } catch (err) {
            setError(err?.message || 'Không thể tải danh sách dịp đặc biệt.')
        } finally {
            setLoading(false)
        }
    }, [user?.id])

    useEffect(() => { load() }, [load])

    const run = async action => {
        setSaving(true)
        setError('')
        try {
            await action()
            await load()
            return true
        } catch (err) {
            setError(err?.message || 'Thao tác không thành công. Vui lòng thử lại.')
            return false
        } finally {
            setSaving(false)
        }
    }

    return {
        occasions, loading, saving, error, reload: load,
        save: (id, data) => run(() => (id ? occasionService.update(id, data) : occasionService.create(data))),
        remove: id => run(() => occasionService.remove(id)),
    }
}
