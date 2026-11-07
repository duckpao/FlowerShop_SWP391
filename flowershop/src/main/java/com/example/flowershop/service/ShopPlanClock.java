package com.example.flowershop.service;
import org.springframework.stereotype.Component;
import java.time.*;
import java.time.temporal.ChronoUnit;
@Component public class ShopPlanClock {
    public static final ZoneId ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
    public LocalDateTime now() { return LocalDateTime.now(ZONE).truncatedTo(ChronoUnit.MICROS); }
}
