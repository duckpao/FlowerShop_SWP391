import { SidebarProvider, useSidebar } from "@/context/SidebarContext";
import { cn } from "@/utils";
import { Outlet, useLocation } from "react-router";
import AppHeader from "./AppHeader";
import AppSidebar from "./AppSidebar";
import Backdrop from "./Backdrop";
import PublicFooter from "./PublicFooter";
import CategoryMenu from "@/components/common/CategoryMenu";

const LayoutContent: React.FC = () => {
  const { isExpanded, isHovered, isMobileOpen } = useSidebar();
  const { pathname } = useLocation();
  // Trang quản trị dùng sidebar; phía khách điều hướng bằng thanh ngang dưới header.
  const isBackOffice = pathname.startsWith("/admin") || pathname.startsWith("/shop-admin");

  return (
    <div className="min-h-screen xl:flex">
      {isBackOffice && <AppSidebar />}
      {isBackOffice && <Backdrop />}

      <div
        className={cn(
          "flex-1 flex flex-col min-h-screen transition-[margin] duration-300 ease-in-out",
          isBackOffice && (isExpanded || isHovered ? "xl:ms-72.5" : "xl:ms-22.5"),
          isMobileOpen ? "ms-0" : "",
        )}
      >
        <AppHeader />
        {!isBackOffice && <CategoryMenu />}
        <main className="flex-grow mx-auto w-full max-w-(--breakpoint-2xl) p-4 md:p-6">
          <Outlet />
        </main>
        <PublicFooter />
      </div>
    </div>
  );
};

const AppLayout: React.FC = () => {
  return (
    <SidebarProvider>
      <LayoutContent />
    </SidebarProvider>
  );
};

export default AppLayout;
