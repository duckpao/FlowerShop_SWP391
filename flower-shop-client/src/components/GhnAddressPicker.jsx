import { useEffect, useState } from 'react'
import { ghnService } from '../services/ghnService'
import Select from './form/Select'
import Label from './form/Label'

/**
 * Address picker for the two-level model (province -> ward, no district) GHN uses since 01/07/2025.
 * Reports the names for display and the GHN ids to store: ghnProvinceId and ghnWardId.
 */
export default function GhnAddressPicker({ value, onChange }) {
    const [provinces, setProvinces] = useState([])
    const [wards, setWards] = useState([])
    const [error, setError] = useState('')
    const provinceId = value?.ghnProvinceId ? String(value.ghnProvinceId) : ''
    const wardId = value?.ghnWardId ? String(value.ghnWardId) : ''

    useEffect(() => {
        let active = true
        ghnService.provinces()
            .then(list => { if (active) setProvinces(list || []) })
            .catch(e => { if (active) setError(e.message) })
        return () => { active = false }
    }, [])

    useEffect(() => {
        if (!provinceId) { setWards([]); return }
        let active = true
        ghnService.wards(provinceId)
            .then(list => { if (active) setWards(list || []) })
            .catch(e => { if (active) setError(e.message) })
        return () => { active = false }
    }, [provinceId])

    const pickProvince = id => {
        const province = provinces.find(p => String(p.id) === String(id))
        onChange({ ...value, city: province?.name || '', ghnProvinceId: id ? Number(id) : null,
            ward: '', ghnWardId: null })
    }
    const pickWard = id => {
        const ward = wards.find(w => String(w.id) === String(id))
        onChange({ ...value, ward: ward?.name || '', ghnWardId: id ? Number(id) : null })
    }

    return <div className="space-y-3">
        <div>
            <Label>Tỉnh / Thành phố</Label>
            <Select options={provinces.map(p => ({ value: p.id, label: p.name }))} placeholder="Chọn Tỉnh/Thành phố"
                defaultValue={provinceId} onChange={pickProvince} />
        </div>
        {provinceId ? <div>
            <Label>Phường / Xã</Label>
            <Select options={wards.map(w => ({ value: w.id, label: w.name }))} placeholder="Chọn Phường/Xã"
                defaultValue={wardId} onChange={pickWard} />
        </div> : null}
        {error ? <p role="alert" className="text-sm text-error-500">{error}</p> : null}
    </div>
}
