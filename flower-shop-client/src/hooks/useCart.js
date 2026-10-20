import { useState, useCallback, useEffect, useRef } from 'react';
import cartApi from '../api/cartApi';
import { API_BASE } from '../apiBase';

const GUEST_KEY = 'flower_guest_cart';
const GUEST_TTL_MS = 7 * 24 * 60 * 60 * 1000; // giỏ khách quá 7 ngày thì bỏ

function loadGuestItems() {
  try {
    const saved = JSON.parse(localStorage.getItem(GUEST_KEY));
    if (saved && Array.isArray(saved.items) && Date.now() - saved.savedAt < GUEST_TTL_MS) return saved.items;
  } catch { /* dữ liệu hỏng hoặc không có localStorage thì coi như giỏ rỗng */ }
  return [];
}

function saveGuestItems(items) {
  try {
    if (items.length) localStorage.setItem(GUEST_KEY, JSON.stringify({ savedAt: Date.now(), items }));
    else localStorage.removeItem(GUEST_KEY);
  } catch { /* localStorage đầy/bị chặn: giỏ vẫn chạy trong RAM */ }
}

// Giỏ khách: biến module (sống qua đổi route) + localStorage (sống qua F5)
let guestItems = loadGuestItems();
let guestRefreshed = false; // mỗi lần tải trang chỉ đối chiếu giá/kho với server một lần

// Giá/kho lưu từ lần trước có thể đã cũ: hỏi lại server, sản phẩm không còn thì bỏ khỏi giỏ
async function refreshGuestItems() {
  const refreshed = [];
  for (const item of guestItems) {
    const productId = item.product?.id || item.productId;
    try {
      const res = await fetch(`${API_BASE}/api/public/products/${productId}`);
      if (res.status === 404) continue;
      if (res.ok) {
        const fresh = await res.json();
        const price = Number(fresh.price || 0);
        const stock = fresh.stock != null ? Number(fresh.stock) : item.product?.stock;
        if (stock != null && stock <= 0) continue;
        const quantity = stock != null ? Math.min(item.quantity, stock) : item.quantity;
        refreshed.push({ ...item, quantity, unitPrice: price, itemTotal: price * quantity,
          product: { ...item.product, price, stock } });
        continue;
      }
    } catch { /* không hỏi được thì giữ nguyên món này */ }
    refreshed.push(item);
  }
  return refreshed;
}

// Dựng dữ liệu cùng dạng response backend để UI render giỏ khách và giỏ user bằng một code
function toCart(items) {
  return {
    items,
    totalItems: items.length,
    totalQuantity: items.reduce((sum, i) => sum + (Number(i.quantity) || 0), 0),
    grandTotal: items.reduce((sum, i) => sum + (Number(i.itemTotal) || 0), 0),
  };
}

/**
 * Hook quản lý giỏ hàng cho cả khách (userId = null, lưu in-memory) và user đã đăng nhập (lưu trên server).
 */
export function useCart(userId) {
  const [cart, setCart] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const isSyncingRef = useRef(false); // chặn merge giỏ khách chạy chồng nhau

  // Ghi giỏ khách và cập nhật UI
  const setGuestItems = (items) => {
    guestItems = items;
    saveGuestItems(items);
    setCart(toCart(items));
  };

  // Chạy 1 thao tác với loading/error. rethrow = true thì ném lỗi tiếp cho nơi gọi (nút Add to cart)
  const run = async (task, rethrow = false) => {
    setLoading(true);
    setError(null);
    try {
      await task();
    } catch (err) {
      setError(err.message || 'Có lỗi xảy ra.');
      if (rethrow) throw err;
    } finally {
      setLoading(false);
    }
  };

  /** Tải giỏ: user -> GET từ backend; khách -> dựng từ giỏ in-memory. */
  const fetchCart = useCallback(async () => {
    if (!userId) {
      if (!guestRefreshed && guestItems.length) {
        guestRefreshed = true;
        guestItems = await refreshGuestItems();
        saveGuestItems(guestItems);
      }
      setCart(toCart(guestItems));
      return;
    }
    setLoading(true);
    setError(null);
    try {
      setCart(await cartApi.getCart(userId));
    } catch (err) {
      setError(err.message || 'Có lỗi xảy ra.');
    } finally {
      setLoading(false);
    }
  }, [userId]);

  /** Vừa đăng nhập: đẩy từng món của giỏ khách lên server (BE tự cộng dồn), xong mới xóa giỏ khách. */
  useEffect(() => {
    if (!userId || isSyncingRef.current || guestItems.length === 0) return;

    isSyncingRef.current = true;
    (async () => {
      try {
        for (const item of guestItems) {
          const productId = item.product?.id || item.productId;
          if (productId) {
            await cartApi.addToCart(userId, { productId, quantity: item.quantity || 1 });
          }
        }
        guestItems = []; // lỗi giữa chừng thì giữ lại để lần sau thử lại
        saveGuestItems(guestItems);
      } catch (err) {
        console.warn('Lỗi khi đồng bộ giỏ hàng của khách vào tài khoản:', err);
      } finally {
        isSyncingRef.current = false;
        fetchCart();
      }
    })();
  }, [userId, fetchCart]);

  /**
   * Thêm sản phẩm vào giỏ (cộng dồn nếu đã có). Ném lỗi lên cho nơi gọi.
   * productData (tùy chọn) chỉ dùng cho khách để có sẵn tên/giá/ảnh/tồn kho.
   */
  const addToCart = useCallback((productId, quantity = 1, productData = null) => run(async () => {
    if (userId) {
      await cartApi.addToCart(userId, { productId, quantity });
      await fetchCart();
      return;
    }

    // Khách: server chưa biết giỏ này nên tự kiểm tra tồn kho ở client
    let product = productData;
    if (!product) {
      try {
        const res = await fetch(`${API_BASE}/api/public/products/${productId}`);
        if (res.ok) product = await res.json();
      } catch {
        // không lấy được thì dùng giá trị mặc định bên dưới
      }
    }

    const price = Number(product?.price || 0);
    const stock = product?.stock != null ? Number(product.stock) : 99; // không biết tồn kho thì tối đa 99, BE kiểm tra lại khi tạo đơn
    if (stock <= 0) throw new Error('Sản phẩm này hiện đã hết hàng.');

    const existing = guestItems.find((i) => (i.product?.id || i.productId) === productId);
    if (existing) {
      const newQty = (Number(existing.quantity) || 0) + quantity;
      const maxQty = existing.product?.stock != null ? Number(existing.product.stock) : stock;
      if (newQty > maxQty) {
        throw new Error(`Tổng số lượng trong giỏ (${newQty}) vượt quá tồn kho hiện có (${maxQty}).`);
      }
      // Giữ nguyên giá lúc thêm lần đầu
      setGuestItems(guestItems.map((i) => i === existing
        ? { ...i, quantity: newQty, itemTotal: (i.unitPrice || price) * newQty }
        : i));
      return;
    }

    if (quantity > stock) {
      throw new Error(`Số lượng yêu cầu (${quantity}) vượt quá tồn kho hiện có (${stock}).`);
    }
    setGuestItems([...guestItems, {
      id: 'guest-' + productId, // id giả, mỗi sản phẩm 1 dòng
      productId,
      product: {
        id: productId,
        name: product?.name || 'Sản phẩm',
        price,
        images: product?.images || (product?.image ? [product.image] : []),
        image: product?.images?.[0] || product?.image || null,
        stock,
        shopId: product?.shopId || 'shop-default',
        shopName: product?.shopName || 'Cửa hàng hoa',
      },
      quantity,
      unitPrice: price,
      itemTotal: price * quantity,
    }]);
  }, true), [userId, fetchCart]);

  /** Đặt số lượng mới (giá trị tuyệt đối) cho 1 dòng. Lỗi chỉ set vào `error`, không ném ra ngoài. */
  const updateItem = useCallback((itemId, quantity) => run(async () => {
    if (userId) {
      try {
        await cartApi.updateCartItem(userId, itemId, { quantity });
      } finally {
        await fetchCart(); // dù lỗi cũng tải lại để UI khớp server
      }
      return;
    }

    const item = guestItems.find((i) => i.id === itemId);
    if (!item) return;
    const stock = item.product?.stock != null ? Number(item.product.stock) : 99;
    if (quantity > stock) throw new Error(`Số lượng yêu cầu (${quantity}) vượt quá tồn kho (${stock}).`);
    const unitPrice = Number(item.unitPrice || item.product?.price || 0);
    setGuestItems(guestItems.map((i) => i === item ? { ...i, quantity, itemTotal: unitPrice * quantity } : i));
  }), [userId, fetchCart]);

  /** Xóa 1 dòng khỏi giỏ. */
  const removeItem = useCallback((itemId) => run(async () => {
    if (userId) {
      try {
        await cartApi.removeCartItem(userId, itemId);
      } finally {
        await fetchCart();
      }
      return;
    }
    setGuestItems(guestItems.filter((i) => i.id !== itemId));
  }), [userId, fetchCart]);

  // Tải giỏ khi mount/đổi user và khi quay lại tab/cửa sổ (vd: đã thanh toán ở tab khác nên giỏ server đã bị xóa)
  useEffect(() => {
    fetchCart();
    window.addEventListener('pageshow', fetchCart);
    window.addEventListener('focus', fetchCart);
    document.addEventListener('visibilitychange', fetchCart);
    return () => {
      window.removeEventListener('pageshow', fetchCart);
      window.removeEventListener('focus', fetchCart);
      document.removeEventListener('visibilitychange', fetchCart);
    };
  }, [fetchCart]);

  return { cart, loading, error, fetchCart, addToCart, updateItem, removeItem };
}
