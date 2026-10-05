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

    // Sync initial state from value when provinces are loaded
    useEffect(() => {
        if (provinces.length > 0 && value?.city && !provinceId) {
            const cityName = value.city.trim().toLowerCase();
            const prov = provinces.find(p => p.name.trim().toLowerCase() === cityName || p.name.trim().toLowerCase().includes(cityName) || cityName.includes(p.name.trim().toLowerCase()));
            if (prov) {
                setProvinceId(prov.id);
            }
        }
    }, [provinces, value?.city, provinceId])

    useEffect(() => {
        if (provinceId) {
            ghnService.districts(provinceId)
                .then(data => {
                    setDistricts(data || []);
                    if (value?.ghnDistrictId) {
                        setDistrictId(String(value.ghnDistrictId));
                    }
                })
                .catch(e => setError(e.message))
        } else {
            setDistricts([]);
        }
    }, [provinceId])

    useEffect(() => {
        if (districtId) {
            ghnService.wards(districtId)
                .then(data => {
                    setWards(data || []);
                    if (value?.ghnWardCode) {
                        setWardCode(value.ghnWardCode);
                    }
                })
                .catch(e => setError(e.message))
        } else {
            setWards([]);
        }
    }, [districtId])

    // When province changes, load districts
    const onProvinceChange = useCallback(id => {
        setProvinceId(id)
        setDistrictId('')
        setWardCode('')
        setDistricts([])
        setWards([])
        const prov = provinces.find(p => String(p.id) === String(id))
        const city = prov ? prov.name : ''
        onChange?.({ ...value, city, district: '', ward: '', ghnDistrictId: null, ghnWardCode: '' })
    }, [provinces, value, onChange])

    // When district changes, load wards
    const onDistrictChange = useCallback(id => {
        setDistrictId(id)
        setWardCode('')
        setWards([])
        const dist = districts.find(d => String(d.id) === String(id))
        const district = dist ? dist.name : ''
        onChange?.({ ...value, district, ward: '', ghnDistrictId: id ? parseInt(id) : null, ghnWardCode: '' })
    }, [districts, value, onChange])

    // When ward changes
    const onWardChange = useCallback(code => {
        setWardCode(code)
        const w = wards.find(w => String(w.id) === String(code))
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