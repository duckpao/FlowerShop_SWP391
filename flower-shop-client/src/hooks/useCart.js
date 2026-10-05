import { useState, useCallback, useEffect, useRef } from 'react';
import cartApi from '../api/cartApi';
import { API_BASE } from '../apiBase';

// Quản lý giỏ hàng khách tạm thời trong bộ nhớ (In-memory), không lưu vào localStorage
let inMemoryGuestItems = [];

// Xóa key cũ trong localStorage nếu còn sót lại
try {
  localStorage.removeItem('flower_guest_cart');
  localStorage.removeItem('flower_checkout_info');
} catch {}

function getGuestCart() {
  return inMemoryGuestItems;
}

function saveGuestCart(items) {
  inMemoryGuestItems = items;
}

function clearGuestCart() {
  inMemoryGuestItems = [];
}

function calculateGuestTotals(items) {
  const totalQuantity = items.reduce((sum, item) => sum + (Number(item.quantity) || 0), 0);
  const grandTotal = items.reduce((sum, item) => sum + (Number(item.itemTotal) || 0), 0);
  return {
    items,
    totalItems: items.length,
    totalQuantity,
    grandTotal,
  };
}

/**
 * Custom hook quản lý state giỏ hàng hỗ trợ cả Guest (chưa đăng nhập) và User đã đăng nhập.
 * - Khách (Guest): giỏ hàng chỉ lưu tạm trong bộ nhớ (In-memory), KHÔNG lưu localStorage.
 * - Người dùng (User): lưu trữ và đồng bộ an toàn trên máy chủ cơ sở dữ liệu.
 */
export function useCart(userId) {
  const [cart, setCart] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const isSyncingRef = useRef(false);

  const handleError = (err) => {
    setError(err.message || 'Có lỗi xảy ra.');
    setLoading(false);
  };

  /** Fetch giỏ hàng: nếu có userId lấy từ BE, nếu không lấy từ localStorage của khách */
  const fetchCart = useCallback(async () => {
    if (!userId) {
      const guestItems = getGuestCart();
      setCart(calculateGuestTotals(guestItems));
      return;
    }

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

  /** Đồng bộ giỏ khách vào tài khoản user khi vừa đăng nhập */
  useEffect(() => {
    if (!userId || isSyncingRef.current) return;

    const guestItems = getGuestCart();
    if (guestItems.length === 0) return;

    isSyncingRef.current = true;
    (async () => {
      try {
        for (const item of guestItems) {
          const productId = item.product?.id || item.productId;
          const quantity = item.quantity || 1;
          if (productId) {
            await cartApi.addToCart(userId, { productId, quantity });
          }
        }
        clearGuestCart();
      } catch (err) {
        console.warn('Lỗi khi đồng bộ giỏ hàng của khách vào tài khoản:', err);
      } finally {
        isSyncingRef.current = false;
        fetchCart();
      }
    })();
  }, [userId, fetchCart]);

  /** Thêm sản phẩm vào giỏ */
  const addToCart = useCallback(async (productId, quantity = 1, productData = null) => {
    setLoading(true);
    setError(null);

    // 1. Nếu là User đã đăng nhập: lưu trực tiếp vào backend
    if (userId) {
      try {
        await cartApi.addToCart(userId, { productId, quantity });
        await fetchCart();
      } catch (err) {
        handleError(err);
        throw err;
      } finally {
        setLoading(false);
      }
      return;
    }

    // 2. Nếu là Guest (chưa đăng nhập): lưu vào localStorage
    try {
      let product = productData;
      if (!product) {
        try {
          const res = await fetch(`${API_BASE}/api/public/products/${productId}`);
          if (res.ok) {
            product = await res.json();
          }
        } catch {
          // ignore
        }
      }

      const guestItems = getGuestCart();
      const existingIdx = guestItems.findIndex(
        i => (i.product?.id || i.productId) === productId
      );

      const price = Number(product?.price || 0);
      const stock = product?.stock != null ? Number(product.stock) : 99;

      if (stock <= 0) {
        throw new Error('Sản phẩm này hiện đã hết hàng.');
      }

      if (existingIdx >= 0) {
        const currentItem = guestItems[existingIdx];
        const newQty = (Number(currentItem.quantity) || 0) + quantity;
        const currentStock = currentItem.product?.stock != null ? Number(currentItem.product.stock) : stock;
        if (newQty > currentStock) {
          throw new Error(`Tổng số lượng trong giỏ (${newQty}) vượt quá tồn kho hiện có (${currentStock}).`);
        }
        guestItems[existingIdx] = {
          ...currentItem,
          quantity: newQty,
          itemTotal: (currentItem.unitPrice || price) * newQty,
        };
      } else {
        if (quantity > stock) {
          throw new Error(`Số lượng yêu cầu (${quantity}) vượt quá tồn kho hiện có (${stock}).`);
        }
        const productSummary = {
          id: productId,
          name: product?.name || 'Sản phẩm',
          price: price,
          images: product?.images || (product?.image ? [product.image] : []),
          image: product?.images?.[0] || product?.image || null,
          stock: stock,
          shopId: product?.shopId || 'shop-default',
          shopName: product?.shopName || 'Cửa hàng hoa',
        };

        guestItems.push({
          id: 'guest-' + productId,
          productId,
          product: productSummary,
          quantity: quantity,
          unitPrice: price,
          itemTotal: price * quantity,
        });
      }

      saveGuestCart(guestItems);
      setCart(calculateGuestTotals(guestItems));
    } catch (err) {
      handleError(err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, [userId, fetchCart]);

  /** Cập nhật số lượng sản phẩm trong giỏ */
  const updateItem = useCallback(async (itemId, quantity) => {
    setLoading(true);
    setError(null);

    // User đã đăng nhập
    if (userId) {
      try {
        await cartApi.updateCartItem(userId, itemId, { quantity });
      } catch (err) {
        handleError(err);
      } finally {
        await fetchCart();
        setLoading(false);
      }
      return;
    }

    // Guest
    try {
      const guestItems = getGuestCart();
      const idx = guestItems.findIndex(i => i.id === itemId);
      if (idx >= 0) {
        const item = guestItems[idx];
        const stock = item.product?.stock != null ? Number(item.product.stock) : 99;
        if (quantity > stock) {
          throw new Error(`Số lượng yêu cầu (${quantity}) vượt quá tồn kho (${stock}).`);
        }
        const unitPrice = Number(item.unitPrice || item.product?.price || 0);
        guestItems[idx] = {
          ...item,
          quantity,
          itemTotal: unitPrice * quantity,
        };
        saveGuestCart(guestItems);
        setCart(calculateGuestTotals(guestItems));
      }
    } catch (err) {
      handleError(err);
    } finally {
      setLoading(false);
    }
  }, [userId, fetchCart]);

  /** Xóa sản phẩm khỏi giỏ */
  const removeItem = useCallback(async (itemId) => {
    setLoading(true);
    setError(null);

    // User đã đăng nhập
    if (userId) {
      try {
        await cartApi.removeCartItem(userId, itemId);
      } catch (err) {
        handleError(err);
      } finally {
        await fetchCart();
        setLoading(false);
      }
      return;
    }

    // Guest
    try {
      let guestItems = getGuestCart();
      guestItems = guestItems.filter(i => i.id !== itemId);
      saveGuestCart(guestItems);
      setCart(calculateGuestTotals(guestItems));
    } catch (err) {
      handleError(err);
    } finally {
      setLoading(false);
    }
  }, [userId, fetchCart]);

  // Tự động tải giỏ hàng ngay khi mở web hoặc chuyển tab
  useEffect(() => {
    fetchCart();

    const handleSync = () => {
      fetchCart();
    };

    window.addEventListener('pageshow', handleSync);
    window.addEventListener('focus', handleSync);
    document.addEventListener('visibilitychange', handleSync);

    return () => {
      window.removeEventListener('pageshow', handleSync);
      window.removeEventListener('focus', handleSync);
      document.removeEventListener('visibilitychange', handleSync);
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
