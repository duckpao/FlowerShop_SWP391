import { useEffect, useState, useCallback } from 'react'
import { ghnService } from '../services/ghnService'
import Select from './form/Select'
import Label from './form/Label'

function GhnAddressPicker({ value, onChange }) {
    const [provinces, setProvinces] = useState([])
    const [districts, setDistricts] = useState([])
    const [wards, setWards] = useState([])
    const [provinceId, setProvinceId] = useState('')
    const [districtId, setDistrictId] = useState('')
    const [wardCode, setWardCode] = useState('')
    const [error, setError] = useState('')

    // Load provinces on mount
    useEffect(() => {
        ghnService.provinces()
            .then(data => setProvinces(data || []))
            .catch(e => setError(e.message))
    }, [])

    // When province changes, load districts
    const onProvinceChange = useCallback(id => {
        setProvinceId(id)
        setDistrictId('')
        setWardCode('')
        setDistricts([])
        setWards([])
        const prov = provinces.find(p => p.id === id)
        const city = prov ? prov.name : value?.city || ''
        onChange?.({ ...value, city, ghnDistrictId: null, ghnWardCode: '' })
        if (id) {
            ghnService.districts(id)
                .then(data => setDistricts(data || []))
                .catch(e => setError(e.message))
        }
    }, [provinces, value, onChange])

    // When district changes, load wards
    const onDistrictChange = useCallback(id => {
        setDistrictId(id)
        setWardCode('')
        setWards([])
        const dist = districts.find(d => d.id === id)
        const district = dist ? dist.name : value?.district || ''
        onChange?.({ ...value, district, ghnDistrictId: id ? parseInt(id) : null, ghnWardCode: '' })
        if (id) {
            ghnService.wards(id)
                .then(data => setWards(data || []))
                .catch(e => setError(e.message))
        }
    }, [districts, value, onChange])

    // When ward changes
    const onWardChange = useCallback(code => {
        setWardCode(code)
        const w = wards.find(w => w.id === code)
        onChange?.({ ...value, ward: w ? w.name : '', ghnWardCode: code })
    }, [wards, value, onChange])

    return <div className="space-y-3">
        <div>
            <Label>Tỉnh / Thành phố</Label>
            <Select
                options={provinces.map(p => ({ value: p.id, label: p.name }))}
                placeholder="Chọn Tỉnh/Thành phố"
                defaultValue={provinceId}
                onChange={onProvinceChange}
            />
        </div>
        {provinceId ? <div>
            <Label>Quận / Huyện</Label>
            <Select
                options={districts.map(d => ({ value: d.id, label: d.name }))}
                placeholder="Chọn Quận/Huyện"
                defaultValue={districtId}
                onChange={onDistrictChange}
            />
        </div> : null}
        {districtId ? <div>
            <Label>Phường / Xã</Label>
            <Select
                options={wards.map(w => ({ value: w.id, label: w.name }))}
                placeholder="Chọn Phường/Xã"
                defaultValue={wardCode}
                onChange={onWardChange}
            />
        </div> : null}
        {error ? <p className="text-sm text-error-500">{error}</p> : null}
    </div>
}

export default GhnAddressPicker