import { useSidebar } from "../context/SidebarContext";
import { useCallback, useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useLocation } from "react-router";
import { ChartColumn, Flower2, LayoutDashboard, MessagesSquare, NotebookPen, Package, ShoppingBag, Store, Users } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { managerShopService } from "../services/managerShopService";
import { managerOrderService } from "../services/managerOrderService";
import {
  BoxCubeIcon,
  ChevronDownIcon,
  GridIcon,
  HorizontaLDots,
  ListIcon,
  TableIcon,
  UserCircleIcon,
} from "../icons";
import { cn } from "../utils";
import { homePathFor } from "../utils/homePath";
import { useChatUnread } from "../controllers/useChatUnread";

type NavItem = {
  name: string;
  key?: string;
  icon: React.ReactNode;
  path?: string;
  new?: boolean;
  target?: string;
  badge?: number | string;
  subItems?: {
    name: string;
    key?: string;
    path: string;
    pro?: boolean;
    new?: boolean;
    target?: string;
  }[];
};

const adminNavItems: NavItem[] = [
  { name: 'Quản lý gói Pro', path: '/admin/shop-plans', icon: <ListIcon fontSize={24} /> },
{ name: 'Mã giảm giá của sàn', path: '/admin/vouchers', icon: <ListIcon fontSize={24} /> },
  { name: 'Tổng quan', path: '/admin', icon: <GridIcon fontSize={24} /> },
  { name: 'Quản lý người dùng', path: '/admin/users', icon: <UserCircleIcon fontSize={24} /> },
  { name: 'Quản lý các shop', path: '/admin/shops', icon: <BoxCubeIcon fontSize={24} /> },
  { name: 'Danh mục & kiểm duyệt', path: '/admin/categories', icon: <TableIcon fontSize={24} /> },
  { name: 'Đơn hàng', path: '/admin/orders', icon: <ListIcon fontSize={24} /> },
  { name: 'Hoàn tiền', path: '/admin/refunds', icon: <ListIcon fontSize={24} /> },
  { name: 'Đánh giá sản phẩm', path: '/admin/reviews', icon: <ListIcon fontSize={24} /> },
  { name: 'Xem khiếu nại', path: '#', icon: <ListIcon fontSize={24} /> }
];

// SRS 4.4: staff chỉ dùng các chức năng vận hành, không vào shop, nhân sự, gói Pro, voucher.
const staffNavPaths = ['/shop-admin', '/shop-admin/products', '/shop-admin/blogs', '/shop-admin/orders', '/shop-admin/reports', '/shop-admin/chat'];

const managerNavItems: NavItem[] = [
  { name: 'Gói đăng ký Pro', path: '/shop-admin/subscription', icon: <ListIcon fontSize={24} /> },
  { name: 'Mã giảm giá của shop', path: '/shop-admin/vouchers', icon: <ListIcon fontSize={24} /> },
  { name: 'Bài chăm sóc hoa', path: '/shop-admin/blogs', icon: <NotebookPen size={24} /> },
  { name: 'Yêu cầu đặt hoa riêng', path: '/shop-admin/custom-orders', icon: <Flower2 size={24} /> },
  { name: 'Tổng quan', path: '/shop-admin', icon: <LayoutDashboard size={24} /> },
  { name: 'Quản lý shop', path: '/shop-admin/shop', icon: <Store size={24} /> },
  { name: 'Quản lý mặt hàng', path: '/shop-admin/products', icon: <Package size={24} /> },
  { name: 'Quản lý đơn hàng', path: '/shop-admin/orders', icon: <ShoppingBag size={24} /> },
  { name: 'Báo cáo thống kê', path: '/shop-admin/reports', icon: <ChartColumn size={24} /> },
  { name: 'Quản lý nhân sự', path: '/shop-admin/staff', icon: <Users size={24} /> },
  { name: 'Tin nhắn khách hàng', path: '/shop-admin/chat', icon: <MessagesSquare size={24} /> }
];

// Phía khách không dùng sidebar nữa: menu nằm ở CategoryMenu (thanh ngang) và menu tài khoản.

const AppSidebar: React.FC = () => {
  const { isExpanded, isMobileOpen, isHovered, setIsHovered, setIsMobileOpen } =
    useSidebar();
  const { t } = useTranslation();
  const location = useLocation();
  const { user } = useAuth();
  const [openSubmenu, setOpenSubmenu] = useState<{
    type: "main" | "others";
    index: number;
  } | null>(null);
  const [subMenuHeight, setSubMenuHeight] = useState<Record<string, number>>(
    {},
  );
  const subMenuRefs = useRef<Record<string, HTMLDivElement | null>>({});
  const [queueCount, setQueueCount] = useState<number>(0);
  const [managerShopId, setManagerShopId] = useState<string | undefined>(undefined);
  const isAdmin = location.pathname.startsWith('/admin') && !location.pathname.startsWith('/shop-admin');
  const isManager = location.pathname.startsWith('/shop-admin');
  const chatScope = isManager ? { kind: 'shop', shopId: managerShopId } : null;
  const chatUnread = useChatUnread(chatScope, user?.role === 'SHOP' || user?.role === 'SHOP_STAFF' ? user?.id : undefined);
  const navItems = isAdmin
    ? adminNavItems
    : [...managerNavItems.filter(item => user?.role !== 'SHOP_STAFF' || staffNavPaths.includes(item.path)).map(item => item.path === '/shop-admin/orders' ? { ...item, badge: queueCount }
         : item.path === '/shop-admin/chat' ? { ...item, badge: chatUnread } : item),
       ...(user?.role === 'SHOP_STAFF' || user?.role === 'SHOP' ? [{ name: 'Đơn hủy cần hoàn tiền', path: '/shop-admin/refunds', icon: <ListIcon fontSize={24} /> } as NavItem] : [])];

  useEffect(() => {
    if (location.pathname.startsWith('/shop-admin')) {
      let active = true;
      managerShopService.list(user?.role === "SHOP_STAFF" ? "SHOP_STAFF" : "SHOP").then(shops => {
        if (active && shops.length > 0) {
          setManagerShopId(shops[0].id);
          managerOrderService.getQueueCount(shops[0].id).then(res => {
            if (active) setQueueCount(res.pendingCount);
          }).catch(() => {});
        }
      }).catch(() => {});
      return () => { active = false; };
    }
  }, [location.pathname, user?.role]);

  // Auto-close sidebar on mobile after route change
  useEffect(() => {
    if (isMobileOpen) {
      setIsMobileOpen(false);
    }
  }, [location.pathname, setIsMobileOpen]);

  const isActive = useCallback(
    (path: string) => {
      if (path.includes('#')) {
        const [pathname, hash] = path.split('#');
        return location.pathname === (pathname || '/') && location.hash === `#${hash}`;
      }
      if (path === '/orders') return location.pathname === path || location.pathname.startsWith(`${path}/`);
      if (path === '/') return location.pathname === '/' && !location.hash;
      return location.pathname === path;
    },
    [location],
  );

  useEffect(() => {
    let submenuToOpen: { type: "main" | "others"; index: number } | null = null;

    navItems.forEach((nav, index) => {
      if (nav.subItems) {
        nav.subItems.forEach((subItem) => {
          if (isActive(subItem.path)) {
            submenuToOpen = { type: "main", index };
          }
        });
      }
    });

    if (submenuToOpen) {
      setOpenSubmenu(submenuToOpen);
    }
  }, [location, isActive, navItems]);

  useEffect(() => {
    if (openSubmenu !== null) {
      const key = `${openSubmenu.type}-${openSubmenu.index}`;
      if (subMenuRefs.current[key]) {
        setSubMenuHeight((prevHeights) => ({
          ...prevHeights,
          [key]: subMenuRefs.current[key]?.scrollHeight || 0,
        }));
      }
    }
  }, [openSubmenu]);

  const handleSubmenuToggle = (index: number, menuType: "main" | "others") => {
    setOpenSubmenu((prevOpenSubmenu) => {
      if (
        prevOpenSubmenu &&
        prevOpenSubmenu.type === menuType &&
        prevOpenSubmenu.index === index
      ) {
        return null;
      }
      return { type: menuType, index };
    });
  };

  const renderMenuItems = (items: NavItem[], menuType: "main" | "others") => (
    <ul className="flex flex-col gap-1">
      {items.map((nav, index) => (
        <li key={nav.name}>
          {nav.subItems ? (
            <button
              onClick={() => handleSubmenuToggle(index, menuType)}
              className={`group menu-item ${
                openSubmenu?.type === menuType && openSubmenu?.index === index
                  ? "menu-item-active"
                  : "menu-item-inactive"
              } cursor-pointer ${
                !isExpanded && !isHovered
                  ? "xl:justify-center"
                  : "xl:justify-start"
              }`}
            >
              <span
                className={`menu-item-icon-size ${
                  openSubmenu?.type === menuType && openSubmenu?.index === index
                    ? "menu-item-icon-active"
                    : "menu-item-icon-inactive"
                }`}
              >
                {nav.icon}
              </span>

              {(isExpanded || isHovered || isMobileOpen) && (
                <span className="menu-item-text">
                  {nav.key ? t(`sidebar.items.${nav.key}`) : nav.name}
                </span>
              )}
              {nav.new && (isExpanded || isHovered || isMobileOpen) && (
                <span
                  className={`absolute inset-e-10 ms-auto ${
                    openSubmenu?.type === menuType &&
                    openSubmenu?.index === index
                      ? "menu-dropdown-badge-active"
                      : "menu-dropdown-badge-inactive"
                  } menu-dropdown-badge`}
                >
                  {t("sidebar.badges.new")}
                </span>
              )}
              {(isExpanded || isHovered || isMobileOpen) && (
                <ChevronDownIcon
                  className={`ms-auto h-5 w-5 transition-transform duration-200 ${
                    openSubmenu?.type === menuType &&
                    openSubmenu?.index === index
                      ? "rotate-180 text-brand-500"
                      : ""
                  }`}
                />
              )}
            </button>
          ) : (
            nav.path && (
              <a
                href={nav.path}
                target={nav.target}
                className={`group menu-item ${
                  isActive(nav.path) ? "menu-item-active" : "menu-item-inactive"
                }`}
              >
                <span
                  className={`menu-item-icon-size ${
                    isActive(nav.path)
                      ? "menu-item-icon-active"
                      : "menu-item-icon-inactive"
                  }`}
                >
                  {nav.icon}
                </span>
                {(isExpanded || isHovered || isMobileOpen) && (
                  <span className="menu-item-text">
                    {nav.key ? t(`sidebar.items.${nav.key}`) : nav.name}
                  </span>
                )}
                {nav.badge !== undefined && nav.badge > 0 && (isExpanded || isHovered || isMobileOpen) && (
                  <span className="absolute inset-e-6 ms-auto flex items-center justify-center rounded-full bg-error-500 px-2 py-0.5 text-xs font-medium text-white">
                    {nav.badge}
                  </span>
                )}
              </a>
            )
          )}
          {nav.subItems && (isExpanded || isHovered || isMobileOpen) && (
            <div
              ref={(el) => {
                subMenuRefs.current[`${menuType}-${index}`] = el;
              }}
              className="overflow-hidden transition-all duration-300"
              style={{
                height:
                  openSubmenu?.type === menuType && openSubmenu?.index === index
                    ? `${subMenuHeight[`${menuType}-${index}`]}px`
                    : "0px",
              }}
            >
              <ul className="ms-9 mt-2 space-y-1">
                {nav.subItems.map((subItem) => (
                  <li key={subItem.name}>
                    <Link
                      to={subItem.path}
                      target={subItem.target}
                      className={`menu-dropdown-item ${
                        isActive(subItem.path)
                          ? "menu-dropdown-item-active"
                          : "menu-dropdown-item-inactive"
                      }`}
                    >
                      {subItem.key
                        ? t(`sidebar.items.${subItem.key}`)
                        : subItem.name}
                      <span className="ms-auto flex items-center gap-1">
                        {subItem.new && (
                          <span
                            className={`ms-auto ${
                              isActive(subItem.path)
                                ? "menu-dropdown-badge-active"
                                : "menu-dropdown-badge-inactive"
                            } menu-dropdown-badge`}
                          >
                            {t("sidebar.badges.new")}
                          </span>
                        )}
                        {subItem.pro && (
                          <span
                            className={`ms-auto ${
                              isActive(subItem.path)
                                ? "menu-dropdown-badge-pro-active"
                                : "menu-dropdown-badge-pro-inactive"
                            } menu-dropdown-badge-pro`}
                          >
                            {t("sidebar.badges.pro")}
                          </span>
                        )}
                      </span>
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </li>
      ))}
    </ul>
  );

  const sectionTitle = 'QUẢN LÝ';

  return (
    <aside
      className={cn(
        "fixed inset-s-0 top-0 z-50 flex h-screen flex-col border-e border-gray-200 bg-white px-5 text-gray-900 transition-all duration-300 ease-in-out xl:translate-x-0 xl:rtl:translate-x-0 dark:border-gray-800 dark:bg-gray-900",
        isExpanded || isMobileOpen ? "w-72.5" : isHovered ? "w-72.5" : "w-22.5",
        isMobileOpen
          ? "translate-x-0"
          : "-translate-x-full rtl:translate-x-full",
      )}
      onMouseEnter={() => !isExpanded && setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      <div
        className={cn(
          "flex py-8",
          !isExpanded && !isHovered ? "xl:justify-center" : "justify-start",
        )}
      >
        <Link to={homePathFor(user?.role)} className="flex items-center gap-2">
          <span className="text-2xl">✿</span>
          {(isExpanded || isHovered || isMobileOpen) && (
            <span className="text-xl font-bold text-brand-500 truncate">
              FlowerShop
            </span>
          )}
        </Link>
      </div>

      <div className="no-scrollbar flex flex-col overflow-y-auto duration-300 ease-linear">
        <nav className="mb-6">
          <div className="flex flex-col gap-4">
            <div>
              <h2
                className={`mb-4 flex text-xs leading-5 text-gray-400 uppercase ${
                  !isExpanded && !isHovered
                    ? "xl:justify-center"
                    : "justify-start"
                }`}
              >
                {isExpanded || isHovered || isMobileOpen ? (
                  sectionTitle
                ) : (
                  <HorizontaLDots className="size-6" />
                )}
              </h2>
              {renderMenuItems(navItems, "main")}
            </div>
          </div>
        </nav>

      </div>
    </aside>
  );
};

export default AppSidebar;
