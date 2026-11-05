import React, { createContext, useContext, useMemo } from 'react';
import { useCart } from '../hooks/useCart';
import { useAuth } from './AuthContext';

const CartContext = createContext(null);

/**
 * Provider bọc app: lấy user từ AuthContext, chọn chế độ giỏ (server cho CUSTOMER, in-memory cho còn lại)
 * và chia sẻ state/hàm của useCart cùng cartCount, userId, isGuest, user cho mọi component con.
 */
export function CartProvider({ children }) {
  const auth = useAuth();
  // Server-backed carts are customer-only; other roles use the in-memory guest cart.
  const currentUserId = auth?.user?.role === 'CUSTOMER' ? auth.user.id : null;
  // userId = null -> useCart chuyển sang chế độ giỏ khách (in-memory); có userId -> gọi API backend
  const cartHook = useCart(currentUserId);

  // Tổng số lượng (không phải số dòng) - dùng cho badge trên icon giỏ ở header
  const cartCount = useMemo(() => {
    return cartHook.cart?.totalQuantity || 0;
  }, [cartHook.cart]);

  const value = {
    ...cartHook,
    cartCount,
    userId: currentUserId,
    // Chỉ true khi chưa đăng nhập; SHOP/ADMIN đăng nhập vẫn có userId = null nhưng isGuest = false
    isGuest: !auth?.user,
    user: auth?.user,
  };

  return (
    <CartContext.Provider value={value}>
      {children}
    </CartContext.Provider>
  );
}

/** Hook lấy giỏ hàng từ CartContext; ném lỗi nếu dùng ngoài CartProvider. */
export function useCartContext() {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error('useCartContext must be used within a CartProvider');
  }
  return context;
}
