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

/** Chat phía khách: /api/customer/chat. */
@RestController
@RequestMapping("/api/customer/chat")
@PreAuthorize("hasRole('CUSTOMER')")
@SecurityRequirement(name = "bearerAuth")
public class CustomerChatController {

    private final ChatService chat;

    public CustomerChatController(ChatService chat) {
        this.chat = chat;
    }

    @GetMapping("/sessions")
    public List<SessionDto> sessions(@AuthenticationPrincipal CurrentUser user) {
        return chat.customerSessions(user.id());
    }

    @PostMapping("/sessions")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public SessionDto open(@AuthenticationPrincipal CurrentUser user, @Valid @RequestBody OpenForCustomer body) {
        return chat.openForCustomer(user.id(), body.shopId());
    }

    @GetMapping("/unread")
    public Map<String, Long> unread(@AuthenticationPrincipal CurrentUser user) {
        return Map.of("count", chat.customerUnread(user.id()));
    }

    @GetMapping("/sessions/{id}/messages")
    public List<MessageDto> messages(@AuthenticationPrincipal CurrentUser user, @PathVariable String id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime before,
            @RequestParam(defaultValue = "30") int size) {
        return chat.history(id, user.id(), before, size);
    }

    @PostMapping("/sessions/{id}/messages") @ResponseStatus(HttpStatus.CREATED)
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public MessageDto send(@AuthenticationPrincipal CurrentUser user, @PathVariable String id, @Valid @RequestBody SendInput body) {
        return chat.send(id, user.id(), body);
    }

    @PostMapping("/sessions/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT)
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public void read(@AuthenticationPrincipal CurrentUser user, @PathVariable String id) {
        chat.markRead(id, user.id());
    }

    @PostMapping("/sessions/{id}/images")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public Map<String, String> image(@AuthenticationPrincipal CurrentUser user, @PathVariable String id,
                                     @RequestParam("file") MultipartFile file) {
        return Map.of("url", chat.uploadImage(id, user.id(), file));
    }
}
