import React, { createContext, useContext, useMemo } from 'react';
import { useCart } from '../hooks/useCart';
import { useAuth } from './AuthContext';

const CartContext = createContext(null);

export function CartProvider({ children }) {
  const auth = useAuth();
  // Nếu đã đăng nhập: dùng ID của user. Nếu chưa: null (chế độ Guest)
  const currentUserId = auth?.user?.id || null;
  const cartHook = useCart(currentUserId);

  const cartCount = useMemo(() => {
    return cartHook.cart?.totalQuantity || 0;
  }, [cartHook.cart]);

  const value = {
    ...cartHook,
    cartCount,
    userId: currentUserId,
    isGuest: !auth?.user,
    user: auth?.user,
  };

  return (
    <CartContext.Provider value={value}>
      {children}
    </CartContext.Provider>
  );
}

export function useCartContext() {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error('useCartContext must be used within a CartProvider');
  }
  return context;
}
