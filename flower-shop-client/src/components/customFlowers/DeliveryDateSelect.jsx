import { useState } from 'react'

const pad = value => String(value).padStart(2, '0')
const daysInMonth = ({ month, year }) => month ? new Date(Number(year) || 2000, Number(month), 0).getDate() : 31

export default function DeliveryDateSelect({ onChange }) {
  const [date, setDate] = useState({ day: '', month: '', year: '' })
  const firstYear = new Date().getFullYear()
  const started = Boolean(date.day || date.month || date.year)
  function choose(part, value) {
    const next = { ...date, [part]: value }
    // Changing month/year must never silently turn an invalid date into another day.
    if (Number(next.day) > daysInMonth(next)) next.day = ''
    setDate(next)
    onChange(next.day && next.month && next.year ? `${next.year}-${pad(next.month)}-${pad(next.day)}` : '')
  }
  function clear() {
    setDate({ day: '', month: '', year: '' })
    onChange('')
  }
  return <fieldset className="custom-delivery-date">
    <legend>Ngày mong muốn nhận hoa</legend>
    <div className="grid grid-cols-3 gap-2">
      <label><span className="sr-only">Ngày nhận hoa</span><select aria-label="Ngày nhận hoa" required={started} value={date.day} onChange={e => choose('day', e.target.value)}>
        <option value="">Ngày</option>{Array.from({ length: daysInMonth(date) }, (_, i) => i + 1).map(day => <option key={day} value={day}>{pad(day)}</option>)}
      </select></label>
      <label><span className="sr-only">Tháng nhận hoa</span><select aria-label="Tháng nhận hoa" required={started} value={date.month} onChange={e => choose('month', e.target.value)}>
        <option value="">Tháng</option>{Array.from({ length: 12 }, (_, i) => i + 1).map(month => <option key={month} value={month}>{pad(month)}</option>)}
      </select></label>
      <label><span className="sr-only">Năm nhận hoa</span><select aria-label="Năm nhận hoa" required={started} value={date.year} onChange={e => choose('year', e.target.value)}>
        <option value="">Năm</option>{Array.from({ length: 11 }, (_, i) => firstYear + i).map(year => <option key={year} value={year}>{year}</option>)}
      </select></label>
    </div>
    {started && <button type="button" onClick={clear} className="mt-2 text-sm text-brand-600">Bỏ chọn ngày nhận hoa</button>}
  </fieldset>
}
