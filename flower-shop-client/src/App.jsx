import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { useCart } from './hooks/useCart';
import Navbar from './components/common/Navbar';
import HomePage from './pages/HomePage';
import CartPage from './pages/CartPage';
import PaymentSuccessPage from './pages/PaymentSuccessPage';
import PaymentErrorPage from './pages/PaymentErrorPage';
import PaymentCancelPage from './pages/PaymentCancelPage';

// User ID mẫu từ database.sql
const DEMO_USER_ID = 'user-customer-01';

export default function App() {
  const { cart, loading, error, fetchCart, addToCart, updateItem, removeItem } =
    useCart(DEMO_USER_ID);

  return (
    <BrowserRouter>
      <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', backgroundColor: '#f8f9fa' }}>
        <Navbar cartCount={cart?.totalQuantity || 0} userId={DEMO_USER_ID} />

        <main style={{ flex: 1 }}>
          <Routes>
            <Route
              path="/"
              element={
                <HomePage
                  addToCart={addToCart}
                  loading={loading}
                />
              }
            />
            <Route
              path="/cart"
              element={
                <CartPage
                  cart={cart}
                  loading={loading}
                  error={error}
                  fetchCart={fetchCart}
                  updateItem={updateItem}
                  removeItem={removeItem}
                  userId={DEMO_USER_ID}
                />
              }
            />
            {/* Các routes callback từ SePay Payment Gateway */}
            <Route path="/payment/success" element={<PaymentSuccessPage />} />
            <Route path="/payment/error" element={<PaymentErrorPage />} />
            <Route path="/payment/cancel" element={<PaymentCancelPage />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}
