import { useEffect, useState } from 'react'
import { ghnService } from '../services/ghnService'
import Select from './form/Select'
import Label from './form/Label'

/**
 * Address picker: province -> district -> ward, from GHN master data. Reports the names for
 * display (city, district, ward) and the GHN codes to store (ghnDistrictId, ghnWardCode).
 * Changing a level clears the levels below it, so a stale code never survives.
 */
export default function GhnAddressPicker({ value, onChange }) {
    const [provinces, setProvinces] = useState([])
    const [districts, setDistricts] = useState([])
    const [wards, setWards] = useState([])
    const [provinceId, setProvinceId] = useState('')
    const [error, setError] = useState('')
    const districtId = value?.ghnDistrictId ? String(value.ghnDistrictId) : ''
    const wardCode = value?.ghnWardCode || ''

    useEffect(() => {
        let active = true
        ghnService.provinces()
            .then(list => { if (active) setProvinces(list || []) })
            .catch(e => { if (active) setError(e.message) })
        return () => { active = false }
    }, [])

    // A saved address carries names, not the province id, so match the province by name once.
    useEffect(() => {
        if (provinceId || !provinces.length || !value?.city) return
        const name = value.city.trim().toLowerCase()
        const match = provinces.find(p => p.name.trim().toLowerCase() === name)
        if (match) setProvinceId(String(match.id))
    }, [provinces, value?.city, provinceId])

    useEffect(() => {
        if (!provinceId) { setDistricts([]); return }
        let active = true
        ghnService.districts(provinceId)
            .then(list => { if (active) setDistricts(list || []) })
            .catch(e => { if (active) setError(e.message) })
        return () => { active = false }
    }, [provinceId])

    useEffect(() => {
        if (!districtId) { setWards([]); return }
        let active = true
        ghnService.wards(districtId)
            .then(list => { if (active) setWards(list || []) })
            .catch(e => { if (active) setError(e.message) })
        return () => { active = false }
    }, [districtId])

    const pickProvince = id => {
        const province = provinces.find(p => String(p.id) === String(id))
        setProvinceId(id ? String(id) : '')
        onChange({ ...value, city: province?.name || '', district: '', ghnDistrictId: null, ward: '', ghnWardCode: '' })
    }
    const pickDistrict = id => {
        const district = districts.find(d => String(d.id) === String(id))
        onChange({ ...value, district: district?.name || '', ghnDistrictId: id ? Number(id) : null, ward: '', ghnWardCode: '' })
    }
    const pickWard = code => {
        const ward = wards.find(w => String(w.id) === String(code))
        onChange({ ...value, ward: ward?.name || '', ghnWardCode: code ? String(code) : '' })
    }

    return <div className="space-y-3">
        <div>
            <Label>Tỉnh / Thành phố</Label>
            <Select options={provinces.map(p => ({ value: p.id, label: p.name }))} placeholder="Chọn Tỉnh/Thành phố"
                defaultValue={provinceId} onChange={pickProvince} />
        </div>
        {provinceId ? <div>
            <Label>Quận / Huyện</Label>
            <Select options={districts.map(d => ({ value: d.id, label: d.name }))} placeholder="Chọn Quận/Huyện"
                defaultValue={districtId} onChange={pickDistrict} />
        </div> : null}
        {districtId ? <div>
            <Label>Phường / Xã</Label>
            <Select options={wards.map(w => ({ value: w.id, label: w.name }))} placeholder="Chọn Phường/Xã"
                defaultValue={wardCode} onChange={pickWard} />
        </div> : null}
        {error ? <p role="alert" className="text-sm text-error-500">{error}</p> : null}
    </div>
}
