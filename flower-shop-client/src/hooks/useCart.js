import { useState, useCallback, useEffect } from 'react';
import cartApi from '../api/cartApi';

/**
 * Custom hook quản lý state giỏ hàng.
 * Dùng: const { cart, loading, error, addToCart, updateItem, removeItem, fetchCart } = useCart(userId);
 */
export function useCart(userId) {
  const [cart, setCart] = useState(null);       // CartResponse từ BE
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleError = (err) => {
    setError(err.message || 'Có lỗi xảy ra.');
    setLoading(false);
  };

  /** Fetch giỏ hàng từ server */
  const fetchCart = useCallback(async () => {
    if (!userId) return;
    setLoading(true);
    setError(null);
    try {
      const data = await cartApi.getCart(userId);
      setCart(data);
    } catch (err) {
      handleError(err);
    } finally {
      setLoading(false);
    }
  }, [userId]);

  /** Thêm sản phẩm vào giỏ */
  const addToCart = useCallback(async (productId, quantity = 1) => {
    if (!userId) return;
    setLoading(true);
    setError(null);
    try {
      await cartApi.addToCart(userId, { productId, quantity });
      await fetchCart(); // Reload giỏ hàng sau khi thêm
    } catch (err) {
      handleError(err);
    }
  }, [userId, fetchCart]);

  /** Cập nhật số lượng */
  const updateItem = useCallback(async (itemId, quantity) => {
    if (!userId) return;
    setLoading(true);
    setError(null);
    try {
      await cartApi.updateCartItem(userId, itemId, { quantity });
    } catch (err) {
      handleError(err);
    } finally {
      // Luôn đồng bộ lại giỏ hàng từ DB để tránh dữ liệu ảo/stale
      await fetchCart();
      setLoading(false);
    }
  }, [userId, fetchCart]);

  /** Xóa sản phẩm khỏi giỏ */
  const removeItem = useCallback(async (itemId) => {
    if (!userId) return;
    setLoading(true);
    setError(null);
    try {
      await cartApi.removeCartItem(userId, itemId);
    } catch (err) {
      handleError(err);
    } finally {
      await fetchCart();
      setLoading(false);
    }
  }, [userId, fetchCart]);

  // Tự động tải giỏ hàng từ database ngay khi mở web, F5, bfcache (back button) hoặc chuyển tab
  useEffect(() => {
    fetchCart();

    const handlePageShow = () => {
      fetchCart();
    };

    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        fetchCart();
      }
    };

    window.addEventListener('pageshow', handlePageShow);
    window.addEventListener('focus', handlePageShow);
    document.addEventListener('visibilitychange', handleVisibilityChange);

    return () => {
      window.removeEventListener('pageshow', handlePageShow);
      window.removeEventListener('focus', handlePageShow);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [fetchCart]);

  return {
    cart,
    loading,
    error,
    fetchCart,
    addToCart,
    updateItem,
    removeItem,
  };
}

