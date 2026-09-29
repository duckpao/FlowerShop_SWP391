import { Toaster } from 'react-hot-toast';
import { Navigate, Route, BrowserRouter as Router, Routes, useLocation } from "react-router";
import AccountView from './views/AccountView';
import AppLayout from "./layout/AppLayout";
import AuthView from "./views/AuthView";
import HomeView from "./views/HomeView";
import AdminDashboardView from "./views/AdminDashboardView";
import ManagerDashboardView from "./views/ManagerDashboardView";
import ProductDetailView from "./views/ProductDetailView";
import AcceptInvitationView from "./views/AcceptInvitationView";
import { useAuthController } from "./controllers/useAuthController";
import { AuthProvider } from "./context/AuthContext";

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
      <Toaster position="top-right" toastOptions={{ duration: 3000 }} />
      <Routes>
        <Route path="/login" element={<AuthView {...controller} />} />
        <Route path="/invitations/accept" element={<AcceptInvitationView />} />

        {/* All routes wrapped in AppLayout which now handles public/admin nav items dynamically */}
        <Route element={<AppLayout />}>
          <Route path="/" element={<HomeView auth={controller} />} />
          <Route path="/account" element={controller.busy ? <p>Đang tải hồ sơ…</p> : controller.user ? <AccountView {...controller} /> : <Navigate to="/login" replace />} />
          <Route path="/products/:id" element={<ProductDetailViewWrapper />} />
          <Route path="/admin/profile" element={<Navigate to="/account" replace />} />
          <Route path="/shop-admin/profile" element={<Navigate to="/account" replace />} />
          <Route path="/admin/*" element={<AdminDashboardView user={controller.user} logout={controller.logout} busy={controller.busy} error={controller.error} path={location.pathname} />} />
          <Route path="/shop-admin/*" element={<ManagerDashboardView user={controller.user} logout={controller.logout} busy={controller.busy} error={controller.error} path={location.pathname} />} />
        </Route>
      </Routes>
    </AuthProvider>
  );
}

function ProductDetailViewWrapper() {
  const location = useLocation();
  const match = location.pathname.match(/^\/products\/([^/]+)$/);
  return <ProductDetailView id={match ? decodeURIComponent(match[1]) : ""} />;
}
