import { useState } from 'react';
import { Link } from 'react-router-dom';

export default function HomePage({ addToCart, loading, cartCount = 0 }) {
  const [toastMessage, setToastMessage] = useState(null);

  const formatPrice = (amount) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);

  const sampleProducts = [
    {
      id: 'prod-01',
      name: 'Bó Hồng Đỏ Mix Baby',
      price: 1000,
      category: 'Hoa Tình Yêu',
      shopName: 'FPTU Smart Floral',
      desc: 'Bó hoa hồng đỏ Ecuador mix hoa baby trắng tượng trưng cho tình yêu nồng nàn.',
      emoji: '🌹',
      stock: 50,
    },
    {
      id: 'prod-02',
      name: 'Lẵng Hướng Dương Ban Mai',
      price: 2000,
      category: 'Hoa Khai Trương',
      shopName: 'FPTU Smart Floral',
      desc: 'Hoa hướng dương rực rỡ tặng khai trương, tốt nghiệp, đem lại may mắn.',
      emoji: '🌻',
      stock: 20,
    },
  ];

  const handleAdd = async (product) => {
    await addToCart(product.id, 1);
    setToastMessage(`Đã thêm "${product.name}" vào giỏ hàng thành công!`);
    setTimeout(() => setToastMessage(null), 3500);
  };

  return (
    <div style={styles.container}>
      {/* Toast alert */}
      {toastMessage && (
        <div style={styles.toast}>
          <span>✅ {toastMessage}</span>
          <Link to="/cart" style={styles.toastLink}>Xem giỏ hàng →</Link>
        </div>
      )}

      {/* Hero Banner */}
      <div style={styles.hero}>
        <div style={styles.heroContent}>
          <span style={styles.heroSub}>🌸 Chào mừng đến với FlowerShop Marketplace</span>
          <h1 style={styles.heroTitle}>Trao gửi yêu thương qua từng cánh hoa</h1>
          <p style={styles.heroDesc}>
            Đặt hoa tươi thiết kế tinh tế, giao hàng hỏa tốc trong 2 giờ tại khu vực Hòa Lạc & Hà Nội.
          </p>
        </div>
      </div>

      {/* Danh sách sản phẩm */}
      <section style={styles.section}>
        <div style={styles.sectionHeader}>
          <div>
            <h2 style={styles.sectionTitle}>Mẫu hoa nổi bật trong ngày</h2>
            <p style={styles.sectionSub}>Hoa tươi mới nhập trong sáng nay, đảm bảo tươi 3-5 ngày</p>
          </div>
          <Link to="/cart" style={styles.cartBtnHeader}>
            🛒 Tới giỏ hàng của bạn {cartCount > 0 ? `(${cartCount})` : ''} →
          </Link>
        </div>

        <div style={styles.grid}>
          {sampleProducts.map((p) => (
            <div key={p.id} style={styles.card}>
              <div style={styles.cardImgWrapper}>
                <span style={styles.cardEmoji}>{p.emoji}</span>
                <span style={styles.stockBadge}>Còn {p.stock}</span>
              </div>
              <div style={styles.cardBody}>
                <span style={styles.shopTag}>🏪 {p.shopName}</span>
                <h3 style={styles.productName}>{p.name}</h3>
                <p style={styles.productDesc}>{p.desc}</p>
                <div style={styles.priceRow}>
                  <span style={styles.price}>{formatPrice(p.price)}</span>
                  <span style={styles.categoryBadge}>{p.category}</span>
                </div>
                <button
                  style={styles.addBtn}
                  onClick={() => handleAdd(p)}
                  disabled={loading}
                >
                  {loading ? 'Đang thêm...' : '+ Thêm vào giỏ hàng'}
                </button>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}

const styles = {
  container: {
    maxWidth: 1200,
    margin: '0 auto',
    padding: '24px 16px 60px',
  },
  toast: {
    position: 'fixed',
    top: 80,
    right: 24,
    zIndex: 9999,
    background: '#2b8a3e',
    color: '#fff',
    padding: '14px 22px',
    borderRadius: 10,
    boxShadow: '0 8px 24px rgba(0,0,0,0.15)',
    display: 'flex',
    alignItems: 'center',
    gap: 16,
    fontSize: 14,
    fontWeight: 600,
    animation: 'slideIn 0.3s ease',
  },
  toastLink: {
    color: '#fff',
    textDecoration: 'underline',
    fontWeight: 700,
  },
  hero: {
    background: 'linear-gradient(135deg, #fff0f0 0%, #fff8f5 100%)',
    borderRadius: 20,
    padding: '48px 40px',
    marginBottom: 40,
    border: '1px solid #ffe3dc',
  },
  heroContent: {
    maxWidth: 600,
  },
  heroSub: {
    fontSize: 14,
    fontWeight: 700,
    color: '#e8604c',
    textTransform: 'uppercase',
    letterSpacing: 1,
    display: 'block',
    marginBottom: 8,
  },
  heroTitle: {
    fontSize: 34,
    fontWeight: 800,
    color: '#1a1a1a',
    margin: '0 0 12px',
    lineHeight: 1.25,
  },
  heroDesc: {
    fontSize: 16,
    color: '#666',
    lineHeight: 1.5,
    margin: 0,
  },
  section: {
    marginBottom: 40,
  },
  sectionHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-end',
    marginBottom: 24,
  },
  sectionTitle: {
    fontSize: 22,
    fontWeight: 800,
    color: '#1a1a1a',
    margin: '0 0 6px',
  },
  sectionSub: {
    fontSize: 14,
    color: '#888',
    margin: 0,
  },
  cartBtnHeader: {
    color: '#e8604c',
    textDecoration: 'none',
    fontWeight: 700,
    fontSize: 14,
  },
  grid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
    gap: 24,
  },
  card: {
    background: '#fff',
    borderRadius: 16,
    border: '1px solid #eee',
    overflow: 'hidden',
    boxShadow: '0 4px 16px rgba(0,0,0,0.04)',
    display: 'flex',
    flexDirection: 'column',
    transition: 'transform 0.2s',
  },
  cardImgWrapper: {
    height: 180,
    background: '#fff5f3',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    position: 'relative',
  },
  cardEmoji: {
    fontSize: 72,
  },
  stockBadge: {
    position: 'absolute',
    top: 12,
    right: 12,
    background: 'rgba(0,0,0,0.6)',
    color: '#fff',
    padding: '3px 8px',
    borderRadius: 6,
    fontSize: 11,
    fontWeight: 600,
  },
  cardBody: {
    padding: '20px',
    display: 'flex',
    flexDirection: 'column',
    flex: 1,
  },
  shopTag: {
    fontSize: 12,
    color: '#999',
    fontWeight: 600,
    marginBottom: 6,
  },
  productName: {
    fontSize: 17,
    fontWeight: 700,
    color: '#222',
    margin: '0 0 8px',
  },
  productDesc: {
    fontSize: 13,
    color: '#777',
    lineHeight: 1.4,
    margin: '0 0 16px',
    flex: 1,
  },
  priceRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  price: {
    fontSize: 18,
    fontWeight: 800,
    color: '#e8604c',
  },
  categoryBadge: {
    fontSize: 12,
    background: '#f1f3f5',
    color: '#495057',
    padding: '3px 8px',
    borderRadius: 6,
  },
  addBtn: {
    width: '100%',
    padding: '11px 0',
    background: '#e8604c',
    color: '#fff',
    border: 'none',
    borderRadius: 8,
    fontSize: 14,
    fontWeight: 700,
    cursor: 'pointer',
    transition: 'background 0.2s',
  },
};

