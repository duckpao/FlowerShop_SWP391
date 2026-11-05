package com.example.flowershop;

import com.example.flowershop.chat.ChatService;
import com.example.flowershop.config.WebSocketConfig;
import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.service.TokenAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketAuthTests {
    private final TokenAuthService tokens = mock(TokenAuthService.class);
    private final ChatService chat = mock(ChatService.class);
    private final MessageChannel channel = mock(MessageChannel.class);
    private ChannelInterceptor interceptor;

    @BeforeEach
    void capture() {
        var registration = mock(ChannelRegistration.class);
        var captured = org.mockito.ArgumentCaptor.forClass(ChannelInterceptor[].class);
        new WebSocketConfig(tokens, chat).configureClientInboundChannel(registration);
        org.mockito.Mockito.verify(registration).interceptors(captured.capture());
        interceptor = captured.getValue()[0];
        when(tokens.authenticate("good")).thenReturn(new CurrentUser("u1", "u1@example.test", "User", "CUSTOMER"));
        when(tokens.authenticate("bad")).thenThrow(new IllegalStateException("expired"));
    }

    private Message<byte[]> frame(StompCommand command, String destination, String authorization, Authentication user) {
        var accessor = StompHeaderAccessor.create(command);
        if (destination != null) accessor.setDestination(destination);
        if (authorization != null) accessor.addNativeHeader("Authorization", authorization);
        if (user != null) accessor.setUser(user);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Authentication connect() {
        var message = interceptor.preSend(frame(StompCommand.CONNECT, null, "Bearer good", null), channel);
        return (Authentication) StompHeaderAccessor.wrap(message).getUser();
    }

    @Test
    void connectNeedsAValidToken() {
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, null, null, null), channel))
                .isInstanceOf(MessagingException.class);
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, null, "Bearer bad", null), channel))
                .isInstanceOf(MessagingException.class);
        assertThat(connect().getName()).isNotNull();
    }

    @Test
    void subscribeIsLimitedToOwnInboxAndJoinedSessions() {
        var user = connect();
        when(chat.canAccess("mine", "u1")).thenReturn(true);
        when(chat.canAccess("theirs", "u1")).thenReturn(false);

        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/chat-inbox/u1", null, user), channel);
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/chat/mine", null, user), channel);
        interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/chat/mine/read", null, user), channel);

        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/chat-inbox/u2", null, user), channel))
                .isInstanceOf(MessagingException.class);
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/chat/theirs", null, user), channel))
                .isInstanceOf(MessagingException.class);
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/other", null, user), channel))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    void sendingThroughTheSocketIsRefused() {
        var user = connect();
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SEND, "/app/chat/x", null, user), channel))
                .isInstanceOf(MessagingException.class);
    }
}
