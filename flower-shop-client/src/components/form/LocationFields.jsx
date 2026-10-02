import { useEffect, useState } from 'react';

const cache = new Map();
async function load(path) {
  if (cache.has(path)) return cache.get(path);
  const response = await fetch(`https://provinces.open-api.vn/api/v2${path}`, { credentials: 'omit', signal: AbortSignal.timeout(10000) });
  if (!response.ok) throw new Error('Không tải được dữ liệu tỉnh/xã.');
  const data = await response.json();
  if (!Array.isArray(data)) throw new Error('Dữ liệu không hợp lệ.');
  cache.set(path, data);
  return data;
}

export default function LocationFields({ address, setAddress }) {
  const [provinces, setProvinces] = useState([]);
  const [wards, setWards] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [retry, setRetry] = useState(0);
  const province = provinces.find(p => p.name === address.city);
  useEffect(() => {
    let active = true;
    setLoading(true); setError(''); setWards([]);
    (async () => {
      const list = await load('/p/');
      if (!active) return;
      setProvinces(list);
      const selected = list.find(p => p.name === address.city);
      const children = selected ? await load(`/w/?province=${selected.code}`) : [];
      if (active) setWards(children);
    })().catch(() => { if (active) setError('Không tải được tỉnh/xã. Vui lòng thử lại.'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [address.city, retry]);
  const style = 'h-11 w-full rounded-lg border border-gray-300 px-3 dark:bg-gray-900 dark:text-white';
  return <>
    <div>
      <label htmlFor="address-province">Tỉnh / Thành phố <span className="text-error-500">*</span></label>
      <select id="address-province" className={style} required value={address.city} ref={node => { if (node) node.setCustomValidity(province ? '' : 'Vui lòng chọn tỉnh trong danh sách.'); }} onChange={e => setAddress(current => ({...current, city: e.target.value, ward: ''}))}>
        <option value="">Chọn tỉnh / thành phố</option>
        {address.city && !province && <option disabled value={address.city}>{address.city} — chọn lại</option>}
        {provinces.map(p => <option key={p.code} value={p.name}>{p.name}</option>)}
      </select>
    </div>
    <div>
      <label htmlFor="address-ward">Phường / Xã <span className="text-error-500">*</span></label>
      <select id="address-ward" className={style} required value={address.ward} ref={node => { if (node) node.setCustomValidity(!loading && !error && wards.some(w => w.name === address.ward) ? '' : 'Vui lòng chọn phường/xã trong danh sách.'); }} onChange={e => setAddress(current => ({...current, ward: e.target.value}))}>
        <option value="">{loading ? 'Đang tải…' : 'Chọn phường / xã'}</option>
        {address.ward && !wards.some(w => w.name === address.ward) && <option disabled value={address.ward}>{address.ward} — chọn lại</option>}
        {!loading && wards.map(w => <option key={w.code} value={w.name}>{w.name}</option>)}
      </select>
    </div>
    {error && <div role="alert" className="md:col-span-2 text-error-500">{error} <button type="button" className="font-bold underline" onClick={() => setRetry(n => n + 1)}>Thử lại</button></div>}
  </>;
}
