package com.example.flowershop.config;

import com.example.flowershop.chat.ChatService;
import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.service.TokenAuthService;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * Chat thời gian thực qua STOMP. Gửi tin đi bằng REST (đã có JWT và CSRF); WebSocket chỉ để nhận.
 * CONNECT phải mang JWT; SUBSCRIBE chỉ được vào phiên mình tham gia và hộp thư của chính mình.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String SESSION_TOPIC = "/topic/chat/";
    private static final String INBOX_TOPIC = "/topic/chat-inbox/";

    private final TokenAuthService tokens;
    private final ChatService chat;

    public WebSocketConfig(TokenAuthService tokens, ChatService chat) {
        this.tokens = tokens;
        this.chat = chat;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null || accessor.getCommand() == null) return message;
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    accessor.setUser(authenticate(accessor.getFirstNativeHeader("Authorization")));
                } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    authorize(accessor);
                } else if (StompCommand.SEND.equals(accessor.getCommand())) {
                    throw new MessagingException("Gửi tin qua WebSocket không được hỗ trợ.");
                }
                return message;
            }
        });
    }

    private UsernamePasswordAuthenticationToken authenticate(String header) {
        if (header == null || !header.startsWith("Bearer ")) throw new MessagingException("Thiếu thông tin đăng nhập.");
        try {
            CurrentUser user = tokens.authenticate(header.substring(7));
            return UsernamePasswordAuthenticationToken.authenticated(user, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.role())));
        } catch (RuntimeException e) {
            throw new MessagingException("Phiên đăng nhập không hợp lệ hoặc đã hết hạn.");
        }
    }

    private void authorize(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof UsernamePasswordAuthenticationToken auth)
                || !(auth.getPrincipal() instanceof CurrentUser user))
            throw new MessagingException("Chưa đăng nhập.");
        String destination = accessor.getDestination() == null ? "" : accessor.getDestination();
        if (destination.startsWith(INBOX_TOPIC)) {
            if (!destination.substring(INBOX_TOPIC.length()).equals(user.id())) throw new MessagingException("Không có quyền.");
        } else if (destination.startsWith(SESSION_TOPIC)) {
            String rest = destination.substring(SESSION_TOPIC.length());
            String sessionId = rest.contains("/") ? rest.substring(0, rest.indexOf('/')) : rest;
            if (!chat.canAccess(sessionId, user.id())) throw new MessagingException("Không có quyền.");
        } else {
            throw new MessagingException("Kênh không hợp lệ.");
        }
    }
}
