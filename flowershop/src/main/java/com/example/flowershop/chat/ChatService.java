package com.example.flowershop.chat;

import com.example.flowershop.chat.ChatDtos.*;
import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.ChatContextType;
import com.example.flowershop.entity.enums.SenderType;
import com.example.flowershop.entity.enums.ShopStatus;
import com.example.flowershop.entity.enums.UserRole;
import com.example.flowershop.entity.enums.OrderType;
import com.example.flowershop.service.CustomOrderWorkflowService;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.CloudinaryService;
import com.example.flowershop.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/** Chat giữa khách và chủ shop: một luồng cho mỗi cặp, tin có thể đính kèm thẻ đơn custom hoặc sản phẩm. */
@Service
@Transactional(readOnly = true)
public class ChatService {

    static final int MAX_PAGE = 50;
    private static final int PREVIEW_LENGTH = 80;
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    private final ChatSessionRepository sessions;
    private final ChatMessageRepository messages;
    private final ShopRepository shops;
    private final UserRepository users;
    private final ProductRepository products;
    private final ProductImageRepository productImages;
    private final CustomOrderRequestRepository customOrders;
    private final OrderRepository orders;
    private final CustomOrderWorkflowService customWorkflow;
    private final NotificationRepository notificationRepo;
    private final NotificationService notifications;
    private final CloudinaryService cloudinary;
    private final ApplicationEventPublisher events;
    private final ObjectMapper json = new ObjectMapper();
    @org.springframework.beans.factory.annotation.Autowired private ShopStaffRepository staff;

    public ChatService(ChatSessionRepository sessions, ChatMessageRepository messages, ShopRepository shops,
                       UserRepository users, ProductRepository products, ProductImageRepository productImages,
                       CustomOrderRequestRepository customOrders, NotificationRepository notificationRepo,
                       NotificationService notifications, CloudinaryService cloudinary, ApplicationEventPublisher events,
                       OrderRepository orders,CustomOrderWorkflowService customWorkflow) {
        this.sessions = sessions;
        this.messages = messages;
        this.shops = shops;
        this.users = users;
        this.products = products;
        this.productImages = productImages;
        this.customOrders = customOrders;
        this.orders = orders;
        this.notificationRepo = notificationRepo;
        this.notifications = notifications;
        this.cloudinary = cloudinary;
        this.events = events;
        this.customWorkflow=customWorkflow;
    }

    private static ResponseStatusException status(HttpStatus code, String message) {
        return new ResponseStatusException(code, message);
    }

    // ---- Phiên chat ----

    /** Khách mở chat với một shop; tạo phiên nếu chưa có. */
    @Transactional
    public SessionDto openForCustomer(String customerId, String shopId) {
        Shop shop = shops.findById(shopId).orElseThrow(() -> status(HttpStatus.NOT_FOUND, "Không tìm thấy cửa hàng."));
        if (shop.getStatus() != ShopStatus.ACTIVE) throw status(HttpStatus.BAD_REQUEST, "Cửa hàng hiện không hoạt động.");
        User customer = users.findById(customerId).orElseThrow(() -> status(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
        return toSession(findOrCreate(customer, shop), SenderType.CUSTOMER);
    }

    /** Chủ shop mở chat với khách đã gửi một yêu cầu custom. */
    @Transactional
    public SessionDto openForShop(String shopId, String ownerId, String customOrderId) {
        Shop shop = requireOwner(shopId, ownerId);
        if (customOrderId == null) throw status(HttpStatus.BAD_REQUEST, "Thiếu yêu cầu custom hoặc đơn hàng.");
        var order=orders.findById(customOrderId);
        if(order.isPresent()) {
            var o=order.get();
            if(o.getOrderType()!=OrderType.CUSTOM || !o.getShop().getId().equals(shopId))
                throw status(HttpStatus.NOT_FOUND,"Không tìm thấy đơn hoa riêng của cửa hàng.");
            return toSession(findOrCreate(o.getCustomer(),shop),SenderType.SHOP);
        }
        CustomOrderRequest request = customOrders.findById(customOrderId)
                .filter(r -> r.getShop().getId().equals(shopId))
                .orElseThrow(() -> status(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu custom."));
        return toSession(findOrCreate(request.getCustomer(), shop), SenderType.SHOP);
    }

    /** Chủ shop mở chat với khách của một đơn hàng thuộc shop mình. */
    @Transactional
    public SessionDto openForShopByOrder(String shopId, String ownerId, String orderId) {
        Shop shop = requireOwner(shopId, ownerId);
        Order order = orders.findById(orderId).filter(o -> o.getShop().getId().equals(shopId))
                .orElseThrow(() -> status(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng."));
        return toSession(findOrCreate(order.getCustomer(), shop), SenderType.SHOP);
    }

    public List<SessionDto> customerSessions(String customerId) {
        return sessions.forCustomer(customerId).stream().map(s -> toSession(s, SenderType.CUSTOMER)).toList();
    }

    public List<SessionDto> shopSessions(String shopId, String ownerId) {
        requireOwner(shopId, ownerId);
        return sessions.forShop(shopId).stream().map(s -> toSession(s, SenderType.SHOP)).toList();
    }

    public long customerUnread(String customerId) {
        return sessions.customerUnreadTotal(customerId);
    }

    public long shopUnread(String shopId, String ownerId) {
        requireOwner(shopId, ownerId);
        return sessions.shopUnreadTotal(shopId);
    }

    private ChatSession findOrCreate(User customer, Shop shop) {
        return sessions.findByCustomerIdAndShopId(customer.getId(), shop.getId()).orElseGet(() -> sessions.saveAndFlush(
                ChatSession.builder().id(UUID.randomUUID().toString()).customer(customer).shop(shop)
                        .createdBy(customer.getId()).build()));
    }

    /** Chủ shop hoặc nhân viên đang làm tại shop mới được trả lời chat. */
    private Shop requireOwner(String shopId, String ownerId) {
        Shop shop = shops.findById(shopId).orElseThrow(() -> status(HttpStatus.NOT_FOUND, "Không tìm thấy cửa hàng."));
        if (!worksAt(shop, ownerId)) throw status(HttpStatus.FORBIDDEN, "Không có quyền truy cập cửa hàng này.");
        return shop;
    }

    private boolean worksAt(Shop shop, String userId) {
        return shop.getOwner().getId().equals(userId)
                || staff.findByShopIdAndUserId(shop.getId(), userId).map(ShopStaff::isActive).orElse(false);
    }

    /** Xác định người đang xem là khách hay phía shop (chủ hoặc staff) của phiên; người ngoài bị từ chối. */
    public SenderType sideOf(ChatSession session, String userId) {
        if (session.getCustomer().getId().equals(userId)) return SenderType.CUSTOMER;
        if (worksAt(session.getShop(), userId)) return SenderType.SHOP;
        throw status(HttpStatus.FORBIDDEN, "Không có quyền truy cập cuộc trò chuyện này.");
    }

    /** Dùng khi đăng ký nhận tin qua WebSocket. */
    public boolean canAccess(String sessionId, String userId) {
        return sessions.findById(sessionId).map(s -> {
            try {
                sideOf(s, userId);
                return true;
            } catch (ResponseStatusException e) {
                return false;
            }
        }).orElse(false);
    }

    private ChatSession session(String sessionId) {
        return sessions.findById(sessionId).orElseThrow(() -> status(HttpStatus.NOT_FOUND, "Không tìm thấy cuộc trò chuyện."));
    }

    // ---- Tin nhắn ----

    /** @param before lấy các tin cũ hơn mốc này (null: trang mới nhất); kết quả theo thứ tự cũ đến mới. */
    public List<MessageDto> history(String sessionId, String userId, LocalDateTime before, int size) {
        sideOf(session(sessionId), userId);
        var page = PageRequest.of(0, Math.max(1, Math.min(size, MAX_PAGE)));
        List<ChatMessage> found = before == null ? messages.latest(sessionId, page) : messages.olderThan(sessionId, before, page);
        List<MessageDto> result = new ArrayList<>(found.stream().map(this::toMessage).toList());
        Collections.reverse(result);
        return result;
    }

    @Transactional
    public MessageDto send(String sessionId, String userId, SendInput input) {
        ChatSession session = session(sessionId);
        SenderType side = sideOf(session, userId);
        String text = input.message() == null ? "" : input.message().strip();
        List<String> images = input.images() == null ? List.of() : input.images().stream().filter(u -> u != null && !u.isBlank()).toList();
        if (text.isEmpty() && images.isEmpty()) throw status(HttpStatus.BAD_REQUEST, "Tin nhắn không được để trống.");
        if (images.stream().anyMatch(u -> !u.startsWith("https://"))) throw status(HttpStatus.BAD_REQUEST, "Ảnh đính kèm không hợp lệ.");

        ChatContextType type = ChatContextType.NONE;
        String contextId = null;
        String snapshot = null;
        if (input.context() != null && input.context().type() != ChatContextType.NONE) {
            type = input.context().type();
            contextId = input.context().id();
            snapshot = write(snapshotOf(session, type, contextId));
        }

        User sender = users.getReferenceById(userId);
        ChatMessage saved = messages.saveAndFlush(ChatMessage.builder().id(UUID.randomUUID().toString()).session(session)
                .sender(sender).senderType(side).message(text.isEmpty() ? null : text)
                .media(images.isEmpty() ? null : write(images)).contextType(type).contextId(contextId)
                .contextSnapshot(snapshot).createdBy(userId).build());

        String preview = !text.isEmpty() ? text : "[Hình ảnh]";
        session.setLastMessageAt(saved.getCreatedDate());
        session.setLastMessagePreview(preview.length() > PREVIEW_LENGTH ? preview.substring(0, PREVIEW_LENGTH) : preview);
        if (side == SenderType.CUSTOMER) session.setShopUnreadCount(session.getShopUnreadCount() + 1);
        else session.setCustomerUnreadCount(session.getCustomerUnreadCount() + 1);
        sessions.saveAndFlush(session);

        notifyRecipient(session, side);
        MessageDto dto = toMessage(saved);
        events.publishEvent(new MessageEvent(dto, session.getCustomer().getId(), session.getShop().getOwner().getId(),
                session.getCustomerUnreadCount(), session.getShopUnreadCount(),
                sessions.customerUnreadTotal(session.getCustomer().getId()),
                sessions.shopUnreadTotal(session.getShop().getId())));
        return dto;
    }

    @Transactional
    public void markRead(String sessionId, String userId) {
        ChatSession session = session(sessionId);
        SenderType side = sideOf(session, userId);
        messages.markRead(sessionId, side == SenderType.CUSTOMER ? SenderType.SHOP : SenderType.CUSTOMER);
        if (side == SenderType.CUSTOMER) session.setCustomerUnreadCount(0);
        else session.setShopUnreadCount(0);
        // markRead clears the persistence context. Continue with merge's managed
        // result, otherwise the old session's lazy Shop proxy is detached.
        session = sessions.saveAndFlush(session);
        events.publishEvent(new ReadEvent(sessionId, session.getCustomer().getId(), session.getShop().getOwner().getId(),
                side == SenderType.CUSTOMER, sessions.customerUnreadTotal(session.getCustomer().getId()),
                sessions.shopUnreadTotal(session.getShop().getId())));
    }

    /** Ảnh được giải mã và mã hóa lại thành PNG trước khi lên Cloudinary để loại tệp không phải ảnh. */
    public String uploadImage(String sessionId, String userId, MultipartFile file) {
        sideOf(session(sessionId), userId);
        if (file.isEmpty() || file.getSize() > MAX_IMAGE_BYTES)
            throw status(HttpStatus.BAD_REQUEST, "Ảnh phải có dung lượng từ 1 byte đến 5 MB.");
        try {
            var image = javax.imageio.ImageIO.read(file.getInputStream());
            if (image == null) throw status(HttpStatus.BAD_REQUEST, "Tệp tải lên không phải ảnh hợp lệ.");
            if (image.getWidth() > 4096 || image.getHeight() > 4096)
                throw status(HttpStatus.BAD_REQUEST, "Kích thước ảnh tối đa 4096 × 4096 pixel.");
            var out = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", out);
            return cloudinary.upload(out.toByteArray(), "chat-" + UUID.randomUUID() + ".png", "image/png");
        } catch (IOException e) {
            throw status(HttpStatus.BAD_REQUEST, "Không thể đọc ảnh. Vui lòng chọn ảnh hợp lệ.");
        }
    }

    // ---- Thẻ ngữ cảnh ----

    private Map<String, Object> snapshotOf(ChatSession session, ChatContextType type, String id) {
        if (id == null || id.isBlank()) throw status(HttpStatus.BAD_REQUEST, "Thiếu mã đối tượng đính kèm.");
        Map<String, Object> snap = new LinkedHashMap<>();
        if (type == ChatContextType.PRODUCT) {
            Product p = products.findById(id).filter(x -> x.getShop().getId().equals(session.getShop().getId()))
                    .orElseThrow(() -> status(HttpStatus.BAD_REQUEST, "Sản phẩm không thuộc cửa hàng này."));
            snap.put("name", p.getName());
            snap.put("price", p.getPrice());
            productImages.findByProductIdOrderByDisplayOrderAscIdAsc(p.getId()).stream().findFirst()
                    .ifPresent(img -> snap.put("imageUrl", img.getImageUrl()));
        } else {
            var order=orders.findById(id);
            if(order.isPresent()) {
                var o=order.get();
                if(o.getOrderType()!=OrderType.CUSTOM || !o.getShop().getId().equals(session.getShop().getId())
                        || !o.getCustomer().getId().equals(session.getCustomer().getId()))
                    throw status(HttpStatus.BAD_REQUEST,"Đơn hoa riêng không thuộc cuộc trò chuyện này.");
                snap.put("orderId",o.getId());
                snap.put("name","Đơn hoa riêng #"+o.getId().replace("-","").substring(0,8).toUpperCase());
                snap.put("price",o.getSubTotal()!=null&&o.getSubTotal().signum()>0?o.getSubTotal():null);
                snap.put("status",o.getStatus().name());
                return snap;
            }
            CustomOrderRequest r = customOrders.findById(id)
                    .filter(x -> x.getShop().getId().equals(session.getShop().getId())
                            && x.getCustomer().getId().equals(session.getCustomer().getId()))
                    .orElseThrow(() -> status(HttpStatus.BAD_REQUEST, "Yêu cầu custom không thuộc cuộc trò chuyện này."));
            snap.put("name", "Yêu cầu hoa custom #" + r.getId().replace("-", "").substring(0, 8).toUpperCase());
            var designSnapshot=readMap(r.getDesiredComponents());
            snap.put("price", designSnapshot==null?null:designSnapshot.get("quotedPrice"));
            snap.put("status", r.getStatus().name());
            orders.findLinkedCustomOrder(r.getId(),r.getCustomer().getId(),r.getShop().getId()).ifPresent(o -> snap.put("orderId",o.getId()));
        }
        return snap;
    }

    // ---- Chuông thông báo ----

    /** Gộp thông báo: nếu chuông chat gần nhất của phiên chưa đọc thì không tạo thêm. */
    private void notifyRecipient(ChatSession session, SenderType from) {
        boolean toShop = from == SenderType.CUSTOMER;
        User target = toShop ? session.getShop().getOwner() : session.getCustomer();
        String link = (toShop ? "/shop-admin/chat?session=" : "/chat?session=") + session.getId();
        boolean pending = notificationRepo.findFirstByUserIdAndLinkOrderByCreatedDateDesc(target.getId(), link)
                .map(n -> !n.isRead()).orElse(false);
        if (pending) return;
        String who = toShop ? session.getCustomer().getFullName() : session.getShop().getName();
        notifications.notifyUser(target, "Tin nhắn mới", who + " vừa gửi tin nhắn cho bạn.", link);
    }

    // ---- Chuyển đổi ----

    private SessionDto toSession(ChatSession s, SenderType viewer) {
        return new SessionDto(s.getId(), s.getShop().getId(), s.getShop().getName(), s.getShop().getLogoUrl(),
                s.getShop().getOwner().getAvatarUrl(), s.getCustomer().getId(), s.getCustomer().getFullName(),
                s.getCustomer().getAvatarUrl(), s.getLastMessageAt(), s.getLastMessagePreview(),
                viewer == SenderType.CUSTOMER ? s.getCustomerUnreadCount() : s.getShopUnreadCount());
    }

    private MessageDto toMessage(ChatMessage m) {
        return new MessageDto(m.getId(), m.getSession().getId(), m.getSenderType(), m.getSender().getId(),
                m.getSender().getFullName(), m.getMessage(), readImages(m.getMedia()), m.getContextType(), m.getContextId(),
                readMap(m.getContextSnapshot()), m.getCreatedDate(), Boolean.TRUE.equals(m.getIsRead()));
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw status(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu tin nhắn.");
        }
    }

    private String jsonDocument(String value) throws JsonProcessingException {
        var tree = json.readTree(value);
        return tree != null && tree.isTextual() ? tree.textValue() : value;
    }

    private List<String> readImages(String value) {
        if (value == null || value.isBlank()) return List.of();
        try {
            return json.readValue(jsonDocument(value), new TypeReference<List<String>>() { });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private Map<String, Object> readMap(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return json.readValue(jsonDocument(value), new TypeReference<Map<String, Object>>() { });
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
