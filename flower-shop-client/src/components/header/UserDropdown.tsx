import { useClickOutside } from "@/hooks/useClickOutside";
import { cn } from "@/utils";
import { useEffect, useRef, useState } from "react";
import { Link } from "react-router";
import { Dropdown } from "../ui/dropdown/Dropdown";
import { DropdownItem } from "../ui/dropdown/DropdownItem";
import { useAuth } from "@/context/AuthContext";

export default function UserDropdown() {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const auth = useAuth();

  useClickOutside(dropdownRef, () => {
    setIsOpen(false);
  });

  const toggleDropdown = () => {
    setIsOpen((prev) => !prev);
  };

  const closeDropdown = () => {
    setIsOpen(false);
  };

  useEffect(() => {
    return () => {
      setIsOpen(false);
    };
  }, []);

  const handleLogout = (e: React.MouseEvent) => {
    e.preventDefault();
    if (auth.logout) {
      auth.logout();
    }
    closeDropdown();
  };

  if (!auth?.user) {
    return (
      <Link to="/login" className="px-4 py-2 text-sm font-medium text-white bg-brand-500 hover:bg-brand-600 rounded-lg transition-colors">
        Đăng nhập
      </Link>
    );
  }

  const displayName = auth.user.fullName || auth.user.email.split('@')[0];
  const initial = displayName.charAt(0).toUpperCase();

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={toggleDropdown}
        className="dropdown-toggle flex items-center gap-2 text-gray-700 dark:text-gray-400"
      >
        <span className="flex h-10 w-10 items-center justify-center rounded-full bg-brand-100 text-brand-600 font-bold dark:bg-brand-500/20 dark:text-brand-400">
          {initial}
        </span>

        <span className="hidden md:block text-theme-sm font-medium">{displayName}</span>
        <svg
          className={`stroke-gray-500 transition-transform duration-200 dark:stroke-gray-400 ${
            isOpen ? "rotate-180" : ""
          }`}
          width="18"
          height="20"
          viewBox="0 0 18 20"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
        >
          <path
            d="M4.3125 8.65625L9 13.3437L13.6875 8.65625"
            stroke="currentColor"
            strokeWidth="1.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </button>

      <Dropdown
        isOpen={isOpen}
        onClose={closeDropdown}
        className="absolute inset-e-0 mt-4.25 flex w-65 flex-col rounded-2xl border border-gray-200 bg-white p-3 shadow-theme-lg dark:border-gray-800 dark:bg-gray-dark"
      >
        <div>
          <span className="block text-theme-sm font-medium text-gray-700 no-underline dark:text-gray-400 truncate">
            {auth.user.fullName || "Người dùng FlowerShop"}
          </span>
          <span className="mt-0.5 block text-theme-xs text-gray-500 no-underline dark:text-gray-400 truncate">
            {auth.user.email}
          </span>
        </div>

        <ul className="flex flex-col gap-1 border-b border-gray-200 pt-4 pb-3 dark:border-gray-800">
          <li>
            <DropdownItem
              onItemClick={closeDropdown}
              tag="a"
              to="/account"
              className="group flex items-center gap-3 rounded-lg px-3 py-2 text-theme-sm font-medium text-gray-700 hover:bg-gray-100 hover:text-gray-700 dark:text-gray-400 dark:hover:bg-white/5 dark:hover:text-gray-300"
            >
              <svg
                className="fill-gray-500 group-hover:fill-gray-700 dark:fill-gray-400 dark:group-hover:fill-gray-300"
                width="24"
                height="24"
                viewBox="0 0 24 24"
                fill="none"
                xmlns="http://www.w3.org/2000/svg"
              >
                <path
                  fillRule="evenodd"
                  clipRule="evenodd"
                  d="M12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22C17.5228 22 22 17.5228 22 12C22 6.47715 17.5228 2 12 2ZM7.5 17.808C8.70617 16.5913 10.2858 15.8984 12.0022 15.8984C13.7181 15.8984 15.2974 16.5909 16.5034 17.8071C15.2981 19.1627 13.7259 20 12 20C10.2736 20 8.70114 19.1623 7.5 17.808ZM17.9255 16.4027C16.3312 14.8697 14.2494 13.8984 12.0022 13.8984C9.75442 13.8984 7.67205 14.8702 6.07727 16.4038C4.78361 15.1764 4 13.6706 4 12C4 7.58172 7.58172 4 12 4C16.4183 4 20 7.58172 20 12C20 13.6709 19.2162 15.1771 17.9255 16.4027ZM12 6C9.79086 6 8 7.79086 8 10C8 12.2091 9.79086 14 12 14C14.2091 14 16 12.2091 16 10C16 7.79086 14.2091 6 12 6ZM10 10C10 8.89543 10.8954 8 12 8C13.1046 8 14 8.89543 14 10C14 11.1046 13.1046 12 12 12C10.8954 12 10 11.1046 10 10Z"
                  fill=""
                />
              </svg>
              Hồ sơ cá nhân
            </DropdownItem>
          </li>
        </ul>
        <button
          onClick={handleLogout}
          className="group mt-3 flex w-full items-center gap-3 rounded-lg px-3 py-2 text-theme-sm font-medium text-error-600 hover:bg-error-50 dark:text-error-500 dark:hover:bg-error-500/10 transition-colors"
        >
          <svg
            className="fill-error-500"
            width="24"
            height="24"
            viewBox="0 0 24 24"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              fillRule="evenodd"
              clipRule="evenodd"
              d="M15.1007 19.247C14.6865 19.247 14.3507 18.9112 14.3507 18.497L14.3507 14.245H12.8507V18.497C12.8507 19.7396 13.8581 20.747 15.1007 20.747H18.5007C19.7434 20.747 20.7507 19.7396 20.7507 18.497L20.7507 5.49609C20.7507 4.25345 19.7433 3.24609 18.5007 3.24609H15.1007C13.8581 3.24609 12.8507 4.25345 12.8507 5.49609V9.74501L14.3507 9.74501V5.49609C14.3507 5.08188 14.6865 4.74609 15.1007 4.74609L18.5007 4.74609C18.9149 4.74609 19.2507 5.08188 19.2507 5.49609L19.2507 18.497C19.2507 18.9112 18.9149 19.247 18.5007 19.247H15.1007ZM3.25073 11.9984C3.25073 12.2144 3.34204 12.4091 3.48817 12.546L8.09483 17.1556C8.38763 17.4485 8.86251 17.4487 9.15549 17.1559C9.44848 16.8631 9.44863 16.3882 9.15583 16.0952L5.81116 12.7484L16.0007 12.7484C16.4149 12.7484 16.7507 12.4127 16.7507 11.9984C16.7507 11.5842 16.4149 11.2484 16.0007 11.2484L5.81528 11.2484L9.15585 7.90554C9.44864 7.61255 9.44847 7.13767 9.15547 6.84488C8.86248 6.55209 8.3876 6.55226 8.09481 6.84525L3.52309 11.4202C3.35673 11.5577 3.25073 11.7657 3.25073 11.9984Z"
              fill=""
            />
          </svg>
          Đăng xuất
        </button>
      </Dropdown>
    </div>
  );
}
