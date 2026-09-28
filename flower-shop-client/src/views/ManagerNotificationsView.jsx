import { useEffect, useState } from "react";
import { managerShopService } from "../services/managerShopService";
import { staffApplicationService } from "../services/staffApplicationService";

export default function ManagerNotificationsView() {
  const [count, setCount] = useState(0);
  const [error, setError] = useState("");
  useEffect(() => {
    let active = true;
    let timer;
    async function load() {
      try {
        const shops = await managerShopService.list("SHOP");
        const lists = await Promise.all(
          shops.map((s) => staffApplicationService.list(s.id)),
        );
        if (active) {
          setCount(lists.flat().filter((a) => a.status === "PENDING").length);
          setError("");
        }
      } catch (e) {
        if (active) setError(e.message);
      }
      if (active) timer = setTimeout(load, 15000);
    }
    load();
    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, []);
  
  if (count === 0 && !error) return null;

  return (
    <div className="mb-6">
      {count > 0 && (
        <a 
          href="/shop-admin?notifications=1"
          className="flex items-center gap-3 rounded-lg bg-warning-50 px-4 py-3 text-warning-700 hover:bg-warning-100 transition-colors dark:bg-warning-500/10 dark:text-warning-400 dark:hover:bg-warning-500/20"
        >
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-warning-100 dark:bg-warning-500/20">
            <svg className="w-5 h-5 text-warning-600 dark:text-warning-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
            </svg>
          </div>
          <span className="font-medium text-sm">
            Bạn có thông báo mới: Đăng ký làm nhân viên ({count} đơn chờ duyệt)
          </span>
        </a>
      )}
      {error && (
        <div className="rounded-lg bg-error-50 p-4 text-sm text-error-500 dark:bg-error-500/10 dark:text-error-400 mt-2">
          {error}
        </div>
      )}
    </div>
  );
}
