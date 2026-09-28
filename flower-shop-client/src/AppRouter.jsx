import { Route, BrowserRouter as Router, Routes, useLocation } from "react-router";
import AppLayout from "./layout/AppLayout";
import AuthView from "./views/AuthView";
import HomeView from "./views/HomeView";
import AdminDashboardView from "./views/AdminDashboardView";
import ManagerDashboardView from "./views/ManagerDashboardView";
import ProductDetailView from "./views/ProductDetailView";
import { useAuthController } from "./controllers/useAuthController";

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
    <Routes>
      <Route path="/" element={<HomeView auth={controller} />} />
      <Route path="/login" element={<AuthView {...controller} />} />
      <Route path="/products/:id" element={<ProductDetailViewWrapper />} />

      {/* Admin Dashboard */}
      <Route element={<AppLayout />}>
        <Route path="/admin/*" element={<AdminDashboardView user={controller.user} logout={controller.logout} busy={controller.busy} error={controller.error} path={location.pathname} />} />
        <Route path="/shop-admin/*" element={<ManagerDashboardView user={controller.user} logout={controller.logout} busy={controller.busy} error={controller.error} path={location.pathname} />} />
      </Route>
    </Routes>
  );
}

function ProductDetailViewWrapper() {
  const location = useLocation();
  const match = location.pathname.match(/^\/products\/([^/]+)$/);
  return <ProductDetailView id={match ? decodeURIComponent(match[1]) : ""} />;
}


