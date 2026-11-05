package com.example.flowershop.chat;

import com.example.flowershop.chat.ChatDtos.*;
import com.example.flowershop.dto.auth.CurrentUser;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Chat phía chủ shop: /api/shop/mine/{shopId}/chat. ChatService kiểm tra người gọi là chủ shop của phiên. */
@RestController
@RequestMapping("/api/shop/mine/{shopId}/chat")
@PreAuthorize("hasRole('SHOP')")
@SecurityRequirement(name = "bearerAuth")
public class ManagerChatController {

    private final ChatService chat;

    public ManagerChatController(ChatService chat) {
        this.chat = chat;
    }

    @GetMapping("/sessions")
    public List<SessionDto> sessions(@PathVariable String shopId, @AuthenticationPrincipal CurrentUser user) {
        return chat.shopSessions(shopId, user.id());
    }

    @PostMapping("/sessions")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public SessionDto open(@PathVariable String shopId, @AuthenticationPrincipal CurrentUser user,
                           @Valid @RequestBody OpenForShop body) {
        return chat.openForShop(shopId, user.id(), body.customOrderId());
    }

    @GetMapping("/unread")
    public Map<String, Long> unread(@PathVariable String shopId, @AuthenticationPrincipal CurrentUser user) {
        return Map.of("count", chat.shopUnread(shopId, user.id()));
    }

    @GetMapping("/sessions/{id}/messages")
    public List<MessageDto> messages(@PathVariable String shopId, @AuthenticationPrincipal CurrentUser user, @PathVariable String id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime before,
            @RequestParam(defaultValue = "30") int size) {
        return chat.history(id, user.id(), before, size);
    }

    @PostMapping("/sessions/{id}/messages") @ResponseStatus(HttpStatus.CREATED)
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public MessageDto send(@PathVariable String shopId, @AuthenticationPrincipal CurrentUser user, @PathVariable String id,
                           @Valid @RequestBody SendInput body) {
        return chat.send(id, user.id(), body);
    }

    @PostMapping("/sessions/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT)
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public void read(@PathVariable String shopId, @AuthenticationPrincipal CurrentUser user, @PathVariable String id) {
        chat.markRead(id, user.id());
    }

    @PostMapping("/sessions/{id}/images")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public Map<String, String> image(@PathVariable String shopId, @AuthenticationPrincipal CurrentUser user, @PathVariable String id,
                                     @RequestParam("file") MultipartFile file) {
        return Map.of("url", chat.uploadImage(id, user.id(), file));
    }
}
