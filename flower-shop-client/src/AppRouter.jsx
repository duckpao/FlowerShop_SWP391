import { Toaster } from 'react-hot-toast';
import { Route, BrowserRouter as Router, Routes, useLocation, useParams } from "react-router";
import AppLayout from "./layout/AppLayout";
import AuthView from "./views/AuthView";
import HomeView from "./views/HomeView";
import AdminDashboardView from "./views/AdminDashboardView";
import ManagerDashboardView from "./views/ManagerDashboardView";
import ProductDetailView from "./views/ProductDetailView";
import ProductCatalogView from "./views/ProductCatalogView";
import PublicShopView from "./views/PublicShopView";
import FavoritesView from "./views/FavoritesView";
import AcceptInvitationView from "./views/AcceptInvitationView";
import CartPage from "./pages/CartPage";
import PaymentSuccessPage from "./pages/PaymentSuccessPage";
import PaymentErrorPage from "./pages/PaymentErrorPage";
import PaymentCancelPage from "./pages/PaymentCancelPage";
import CheckoutView from "./views/CheckoutView";
import CustomerOrdersView from "./views/CustomerOrdersView";
import CustomerOrderDetailView from "./views/CustomerOrderDetailView";
import { useAuthController } from "./controllers/useAuthController";
import { AuthProvider } from "./context/AuthContext";
import { CartProvider, useCartContext } from "./context/CartContext";
import FavoriteShopsView from './views/FavoriteShopsView'

export default function AppRouter() {
  return (
    <Router>
      <AppRouterContent />
    </Router>
  );
}

function AppRouterContent() {
  const controller = useAuthController();
  const location = useLocation();

  return (
    <AuthProvider value={{ user: controller.user, busy: controller.busy, error: controller.error, logout: controller.logout }}>
      <CartProvider>
        <Toaster position="top-right" toastOptions={{ duration: 3000 }} />
        <Routes>
          <Route path="/login" element={<AuthView {...controller} />} />
          <Route path="/invitations/accept" element={<AcceptInvitationView />} />

          {/* All routes wrapped in AppLayout which handles public/admin/shop nav items dynamically */}
          <Route element={<AppLayout />}>
            <Route path="/" element={<HomeView auth={controller} />} />
            <Route path="/products" element={<ProductCatalogView auth={controller} />} />
            <Route path="/products/:id" element={<ProductDetailViewWrapper auth={controller} />} />

            <Route
              path="/shops/:id"
              element={<PublicShopView auth={controller} />}
            />

            <Route
              path="/favorites"
              element={<FavoritesView auth={controller} />}
            />

            <Route
              path="/favorite-shops"
              element={<FavoriteShopsView auth={controller} />}
            />

            <Route path="/cart" element={<CartPageWrapper />} />
            <Route path="/checkout" element={<CheckoutView />} />
            <Route path="/orders" element={<CustomerOrdersView />} />
            <Route path="/orders/:id" element={<CustomerOrderDetailView />} />
            <Route path="/payment/success" element={<PaymentSuccessPage />} />
            <Route path="/payment/error" element={<PaymentErrorPage />} />
            <Route path="/payment/cancel" element={<PaymentCancelPage />} />
            <Route path="/admin/*" element={<AdminDashboardView user={controller.user} logout={controller.logout} busy={controller.busy} error={controller.error} path={location.pathname} />} />
            <Route path="/shop-admin/*" element={<ManagerDashboardView user={controller.user} logout={controller.logout} busy={controller.busy} error={controller.error} path={location.pathname} />} />
          </Route>
        </Routes>
      </CartProvider>
    </AuthProvider>
  );
}

function ProductDetailViewWrapper({ auth }) {
  const location = useLocation();
  const match = location.pathname.match(/^\/products\/([^/]+)$/);
  return <ProductDetailView auth={auth} productId={match ? decodeURIComponent(match[1]) : ""} />;
}

function CartPageWrapper() {
  const cartHook = useCartContext();
  return (
    <CartPage
      cart={cartHook.cart}
      loading={cartHook.loading}
      error={cartHook.error}
      fetchCart={cartHook.fetchCart}
      updateItem={cartHook.updateItem}
      removeItem={cartHook.removeItem}
      userId={cartHook.userId}
      user={cartHook.user}
      isGuest={cartHook.isGuest}
    />
  );
}
