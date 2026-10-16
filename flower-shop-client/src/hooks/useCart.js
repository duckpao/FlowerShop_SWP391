import { useState, useCallback, useEffect, useRef } from 'react';
import cartApi from '../api/cartApi';
import { API_BASE } from '../apiBase';

// Giỏ khách nằm ở biến module (sống qua việc đổi route, mất khi F5/đóng tab) -> tránh lưu dữ liệu giỏ ở client
let inMemoryGuestItems = [];

// Dọn key localStorage của phiên bản cũ từng lưu giỏ khách
try {
  localStorage.removeItem('flower_guest_cart');
  localStorage.removeItem('flower_checkout_info');
} catch {}

/** Đọc danh sách item giỏ của khách từ biến module (không đụng localStorage). */
function getGuestCart() {
  return inMemoryGuestItems;
}

/** Ghi đè danh sách item giỏ của khách vào biến module. */
function saveGuestCart(items) {
  inMemoryGuestItems = items;
}

/** Xóa sạch giỏ khách (gọi sau khi đã merge thành công vào tài khoản). */
function clearGuestCart() {
  inMemoryGuestItems = [];
}

/**
 * Tính lại tổng từ danh sách item để có dạng dữ liệu giống response backend:
 * { items, totalItems (số dòng), totalQuantity (tổng số lượng), grandTotal (tổng tiền) }.
 * Giúp UI render giỏ khách và giỏ user bằng cùng một code.
 */
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
  // Chặn effect đồng bộ giỏ khách chạy chồng nhau (tránh cộng trùng số lượng lên server)
  const isSyncingRef = useRef(false);

  /** Ghi thông báo lỗi vào state `error` (UI hiển thị) và tắt cờ loading. */
  const handleError = (err) => {
    setError(err.message || 'Có lỗi xảy ra.');
    setLoading(false);
  };

  /**
   * Tải giỏ hàng vào state `cart`.
   * Có userId -> GET /cart/{userId} từ backend; không có -> dựng từ giỏ in-memory của khách.
   */
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

  /** Đồng bộ giỏ khách vào tài khoản user khi vừa đăng nhập (merge bằng cách addToCart từng món, BE tự cộng dồn) */
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
        // Chỉ xóa giỏ khách khi tất cả đã đẩy lên thành công; lỗi giữa chừng thì giữ lại để lần sau thử lại
        clearGuestCart();
      } catch (err) {
        console.warn('Lỗi khi đồng bộ giỏ hàng của khách vào tài khoản:', err);
      } finally {
        isSyncingRef.current = false;
        fetchCart(); // luôn tải lại để hiển thị giỏ server sau khi merge
      }
    })();
  }, [userId, fetchCart]);

  /**
   * Thêm sản phẩm vào giỏ (cộng dồn nếu đã có).
   * - User: POST /cart/{userId}/add rồi fetchCart; backend kiểm tra tồn kho và trạng thái sản phẩm.
   * - Guest: lấy thông tin sản phẩm (từ productData hoặc API public), kiểm tra tồn kho tại client,
   *   rồi cộng dồn hoặc thêm dòng mới vào giỏ in-memory.
   * productData (tùy chọn) chỉ dùng cho guest để hiển thị tên/giá/ảnh mà không phải gọi lại API.
   * Ném lỗi lên cho caller (nút Add to cart) để hiện thông báo.
   * @param {string} productId
   * @param {number} [quantity=1]
   * @param {object|null} [productData]
   */
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

    // 2. Guest: lưu in-memory, tự kiểm tra tồn kho ở client (server chưa biết giỏ này)
    try {
      let product = productData;
      // Không có dữ liệu sản phẩm từ trang gọi -> tự lấy từ API public
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
      // Không biết tồn kho thì cho phép tối đa 99; backend vẫn kiểm tra lại khi tạo đơn
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
        // Sản phẩm đã có trong giỏ -> cộng dồn số lượng, giữ nguyên giá lúc thêm lần đầu
        guestItems[existingIdx] = {
          ...currentItem,
          quantity: newQty,
          itemTotal: (currentItem.unitPrice || price) * newQty,
        };
      } else {
        if (quantity > stock) {
          throw new Error(`Số lượng yêu cầu (${quantity}) vượt quá tồn kho hiện có (${stock}).`);
        }
        // Lưu bản tóm tắt sản phẩm để CartItem render giống item từ backend
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
          id: 'guest-' + productId, // id giả, mỗi sản phẩm 1 dòng
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

  /**
   * Đặt số lượng mới (giá trị tuyệt đối, không phải delta) cho 1 dòng trong giỏ.
   * - User: PUT lên backend (backend kiểm tra tồn kho), rồi luôn fetchCart để khớp server.
   * - Guest: tự kiểm tra tồn kho ở client, cập nhật quantity + itemTotal trong mảng in-memory.
   * Lỗi chỉ được set vào `error`, không ném ra ngoài (khác addToCart).
   * @param {string} itemId id dòng giỏ ('guest-<productId>' với khách)
   * @param {number} quantity số lượng mới
   */
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
        // Dù thành công hay lỗi đều tải lại để UI khớp với số lượng thực tế trên server
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

  /**
   * Xóa 1 dòng sản phẩm khỏi giỏ.
   * - User: gọi DELETE lên backend rồi fetchCart để đồng bộ (kể cả khi lỗi).
   * - Guest: lọc item khỏi mảng in-memory và tính lại tổng.
   */
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

  // Tải giỏ khi mount/đổi user, và tải lại khi quay lại tab/cửa sổ để badge giỏ không bị cũ
  // (vd: đã thanh toán xong ở tab khác, giỏ đã bị xóa phía server)
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
