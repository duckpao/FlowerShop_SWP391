import '../styles/table.css'

export default function DataTable({ title, columns, rows, rowKey = 'id', empty = 'Chưa có dữ liệu.' }) {
  return <div className="data-table-scroll" role="region" aria-label={title} tabIndex={0}>
    <table className="data-table">
      <caption>{title}</caption>
      <thead><tr>{columns.map(column => <th key={column.key} scope="col">{column.label}</th>)}</tr></thead>
      <tbody>{rows.length ? rows.map(row => <tr key={row[rowKey]}>
        {columns.map(column => <td key={column.key}>{column.render ? column.render(row) : (row[column.key] ?? '—')}</td>)}
      </tr>) : <tr><td colSpan={columns.length} className="table-empty">{empty}</td></tr>}</tbody>
    </table>
  </div>
}
