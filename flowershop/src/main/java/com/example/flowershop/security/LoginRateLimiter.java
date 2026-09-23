package com.example.flowershop.security;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.HashMap;
import java.util.Map;

/** Per-instance limit; use a shared limiter when deploying multiple instances. */
@Component
public class LoginRateLimiter {
    private record Window(long start, int count) {}
    private final Map<String, Window> windows = new HashMap<>();
    public synchronized void check(String remoteAddress) {
        long now = System.currentTimeMillis();
        windows.entrySet().removeIf(entry -> now - entry.getValue().start() >= 60_000);
        Window current = windows.get(remoteAddress);
        if ((current != null && current.count() >= 20) || (current == null && windows.size() >= 10_000))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Vui lòng chờ một phút trước khi đăng nhập lại.");
        windows.put(remoteAddress, new Window(current == null ? now : current.start(),current == null ? 1 : current.count()+1));
    }
}
