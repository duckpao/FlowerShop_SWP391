/**
 * CartItem Component — Hiển thị 1 dòng sản phẩm trong giỏ hàng
 *
 * Props:
 *  - item: CartItemResponse
 *  - onUpdate: (itemId, quantity) => void
 *  - onRemove: (itemId) => void
 */
export default function CartItem({ item, onUpdate, onRemove }) {
  const { id, product, quantity, itemTotal } = item;

  const handleDecrease = () => {
    if (quantity > 1) onUpdate(id, quantity - 1);
  };

  const handleIncrease = () => {
    onUpdate(id, quantity + 1);
  };

  const formatPrice = (amount) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);

  return (
    <div style={styles.container}>
      {/* Hình ảnh sản phẩm */}
      <div style={styles.imageBox}>
        {product.images ? (
          <img
            src={JSON.parse(product.images)[0]}
            alt={product.name}
            style={styles.image}
            onError={(e) => { e.target.src = 'https://via.placeholder.com/80x80?text=🌸'; }}
          />
        ) : (
          <div style={styles.imagePlaceholder}>🌸</div>
        )}
      </div>

      {/* Thông tin sản phẩm */}
      <div style={styles.info}>
        <p style={styles.shopName}>{product.shopName}</p>
        <p style={styles.productName}>{product.name}</p>
        <p style={styles.price}>{formatPrice(product.price)}</p>
      </div>

      {/* Điều chỉnh số lượng */}
      <div style={styles.quantityControl}>
        <button style={styles.qtyBtn} onClick={handleDecrease}>−</button>
        <span style={styles.qtyDisplay}>{quantity}</span>
        <button
          style={styles.qtyBtn}
          onClick={handleIncrease}
          disabled={quantity >= product.stock}
        >
          +
        </button>
      </div>

      {/* Tổng tiền item */}
      <div style={styles.itemTotal}>
        <span style={styles.totalAmount}>{formatPrice(itemTotal)}</span>
      </div>

      {/* Nút xóa */}
      <button style={styles.removeBtn} onClick={() => onRemove(id)} title="Xóa khỏi giỏ">
        ✕
      </button>
    </div>
  );
}

const styles = {
  container: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    padding: '12px 0',
    borderBottom: '1px solid #f0f0f0',
  },
  imageBox: {
    flexShrink: 0,
    width: 72,
    height: 72,
    borderRadius: 8,
    overflow: 'hidden',
    background: '#fdf6f0',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  image: { width: '100%', height: '100%', objectFit: 'cover' },
  imagePlaceholder: { fontSize: 36 },
  info: { flex: 1, minWidth: 0 },
  shopName: { margin: 0, fontSize: 11, color: '#999' },
  productName: {
    margin: '2px 0 4px',
    fontSize: 14,
    fontWeight: 600,
    color: '#222',
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
  },
  price: { margin: 0, fontSize: 13, color: '#888' },
  quantityControl: { display: 'flex', alignItems: 'center', gap: 8 },
  qtyBtn: {
    width: 28,
    height: 28,
    border: '1px solid #ddd',
    borderRadius: 6,
    background: '#fff',
    cursor: 'pointer',
    fontSize: 16,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  qtyDisplay: { fontSize: 14, fontWeight: 600, minWidth: 24, textAlign: 'center' },
  itemTotal: { minWidth: 90, textAlign: 'right' },
  totalAmount: { fontSize: 14, fontWeight: 700, color: '#e8604c' },
  removeBtn: {
    background: 'none',
    border: 'none',
    cursor: 'pointer',
    color: '#bbb',
    fontSize: 16,
    padding: '4px 6px',
  },
};

