import { Link, useLocation } from 'react-router-dom';

export default function Navbar({ cartCount, userId }) {
  const location = useLocation();

  return (
    <header style={styles.header}>
      <div style={styles.container}>
        {/* Logo */}
        <Link to="/" style={styles.logo}>
          <span style={styles.logoIcon}>🌻</span>
          <span style={styles.logoText}>FlowerShop</span>
        </Link>

        {/* Navigation */}
        <nav style={styles.nav}>
          <Link
            to="/"
            style={{
              ...styles.navLink,
              color: location.pathname === '/' ? '#e8604c' : '#555',
              fontWeight: location.pathname === '/' ? 700 : 500,
            }}
          >
            Trang chủ
          </Link>

          <Link
            to="/cart"
            style={{
              ...styles.cartButton,
              backgroundColor: location.pathname === '/cart' ? '#d04f3c' : '#e8604c',
            }}
          >
            <span>🛒 Giỏ hàng</span>
            {cartCount > 0 && <span style={styles.badge}>{cartCount}</span>}
          </Link>
        </nav>
      </div>
    </header>
  );
}

const styles = {
  header: {
    background: '#fff',
    borderBottom: '1px solid #eee',
    position: 'sticky',
    top: 0,
    zIndex: 1000,
    boxShadow: '0 2px 10px rgba(0,0,0,0.04)',
  },
  container: {
    maxWidth: 1200,
    margin: '0 auto',
    padding: '0 16px',
    height: 64,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  logo: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    textDecoration: 'none',
  },
  logoIcon: { fontSize: 26 },
  logoText: {
    fontSize: 22,
    fontWeight: 900,
    color: '#e8604c',
    letterSpacing: -0.5,
  },
  nav: {
    display: 'flex',
    alignItems: 'center',
    gap: 20,
  },
  navLink: {
    textDecoration: 'none',
    fontSize: 15,
    padding: '8px 12px',
    borderRadius: 6,
    transition: 'color 0.2s',
  },
  cartButton: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    padding: '8px 18px',
    color: '#fff',
    borderRadius: 8,
    textDecoration: 'none',
    fontWeight: 700,
    fontSize: 14,
    transition: 'background 0.2s',
  },
  badge: {
    background: '#fff',
    color: '#e8604c',
    padding: '2px 7px',
    borderRadius: 12,
    fontSize: 12,
    fontWeight: 800,
  },
};

