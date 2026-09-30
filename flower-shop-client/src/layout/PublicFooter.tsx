import React from 'react';
import { Link } from 'react-router';

const PublicFooter: React.FC = () => {
  return (
    <footer className="w-full bg-white border-t border-gray-200 dark:bg-gray-900 dark:border-gray-800 transition-colors duration-300 mt-auto">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          <div className="md:col-span-1">
            <Link to="/" className="flex items-center gap-2 mb-4">
              <span className="text-2xl font-bold text-brand-500 dark:text-brand-400">✿ FlowerShop</span>
            </Link>
            <p className="text-sm text-gray-500 dark:text-gray-400">
              Mỗi đóa hoa, một lời gửi.<br/>
              Gửi yêu thương, đón những điều đẹp.
            </p>
          </div>
          
          <div>
            <h4 className="text-sm font-semibold text-gray-900 dark:text-white uppercase tracking-wider mb-4">Khám phá</h4>
            <ul className="space-y-3">
              <li>
                <a href="#shops" className="text-sm text-gray-500 hover:text-brand-500 dark:text-gray-400 dark:hover:text-brand-400">Danh sách Cửa hàng</a>
              </li>
              <li>
                <a href="#products" className="text-sm text-gray-500 hover:text-brand-500 dark:text-gray-400 dark:hover:text-brand-400">Sản phẩm mới nhất</a>
              </li>
              <li>
                <Link to="/login" className="text-sm text-gray-500 hover:text-brand-500 dark:text-gray-400 dark:hover:text-brand-400">Đăng nhập / Đăng ký</Link>
              </li>
            </ul>
          </div>
          
          <div>
            <h4 className="text-sm font-semibold text-gray-900 dark:text-white uppercase tracking-wider mb-4">Hỗ trợ</h4>
            <ul className="space-y-3">
              <li>
                <a href="#" className="text-sm text-gray-500 hover:text-brand-500 dark:text-gray-400 dark:hover:text-brand-400">Trung tâm trợ giúp</a>
              </li>
              <li>
                <a href="#" className="text-sm text-gray-500 hover:text-brand-500 dark:text-gray-400 dark:hover:text-brand-400">Chính sách bảo mật</a>
              </li>
              <li>
                <a href="#" className="text-sm text-gray-500 hover:text-brand-500 dark:text-gray-400 dark:hover:text-brand-400">Điều khoản dịch vụ</a>
              </li>
            </ul>
          </div>
          
          <div>
            <h4 className="text-sm font-semibold text-gray-900 dark:text-white uppercase tracking-wider mb-4">Liên hệ</h4>
            <ul className="space-y-3">
              <li className="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400">
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                </svg>
                support@flowershop.com
              </li>
              <li className="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400">
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" />
                </svg>
                1900 1234
              </li>
            </ul>
          </div>
        </div>
        
        <div className="mt-12 pt-8 border-t border-gray-200 dark:border-gray-800 text-center">
          <p className="text-sm text-gray-500 dark:text-gray-400">
            &copy; {new Date().getFullYear()} FlowerShop. Mọi bản quyền được bảo lưu. (Thiết kế dựa trên TailAdmin)
          </p>
        </div>
      </div>
    </footer>
  );
};

export default PublicFooter;
