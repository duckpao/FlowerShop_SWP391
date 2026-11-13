package com.example.flowershop;

import com.example.flowershop.entity.CustomerOccasion;
import com.example.flowershop.entity.User;
import com.example.flowershop.repository.CustomerOccasionRepository;
import com.example.flowershop.repository.UserRepository;
import com.example.flowershop.service.OccasionService;
import com.example.flowershop.service.OccasionService.OccasionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Khách tạo/sửa/xóa dịp đặc biệt: các quy tắc quyết định khi nào email + thông báo nhắc được gửi. */
class OccasionServiceTests {

    private CustomerOccasionRepository occasions;
    private OccasionService service;
    private final LocalDate today = LocalDate.now(OccasionService.ZONE);

    @BeforeEach
    void setUp() {
        occasions = mock(CustomerOccasionRepository.class);
        var users = mock(UserRepository.class);
        when(users.findById("cus-1")).thenReturn(Optional.of(User.builder().id("cus-1").build()));
        when(occasions.save(any())).thenAnswer(i -> i.getArgument(0));
        service = new OccasionService(occasions, users);
    }

    private static ResponseStatusException rejected(org.junit.jupiter.api.function.Executable call) {
        return assertThrows(ResponseStatusException.class, call);
    }

    @Test
    void withoutNotifyAtTheReminderDefaultsToSevenDaysBeforeAtEight() {
        var response = service.create("cus-1", new OccasionRequest("Sinh nhật", today.plusDays(30), null, false));

        assertEquals(7, response.notifyDaysBefore());
        assertEquals(LocalTime.of(8, 0), response.notifyTime());
        assertFalse(response.customNotify());
        assertEquals(today.plusDays(23).atTime(8, 0), response.notifyAt());
        assertFalse(response.reminded());
    }

    @Test
    void customNotifyAtSetsDaysAndTimeOfDay() {
        var notifyAt = today.plusDays(28).atTime(18, 30);
        var response = service.create("cus-1", new OccasionRequest("Kỷ niệm", today.plusDays(30), notifyAt, false));

        assertEquals(2, response.notifyDaysBefore());
        assertEquals(LocalTime.of(18, 30), response.notifyTime());
        assertTrue(response.customNotify());
    }

    @Test
    void oneTimeOccasionInThePastIsRejected() {
        var e = rejected(() -> service.create("cus-1", new OccasionRequest("Cũ", today.minusDays(1), null, false)));
        assertEquals(400, e.getStatusCode().value());
        assertTrue(e.getReason().contains("quá khứ"));
    }

    @Test
    void yearlyOccasionMayStartInThePastAndRollsToTheNextOccurrence() {
        var response = service.create("cus-1", new OccasionRequest("Sinh nhật", LocalDate.of(1990, 3, 10), null, true));
        assertTrue(response.recurringYearly());
        assertFalse(response.nextOccurrence().isBefore(today));
        assertEquals(3, response.nextOccurrence().getMonthValue());
        assertEquals(10, response.nextOccurrence().getDayOfMonth());
    }

    @Test
    void reminderAfterTheOccasionOrMoreThanAYearBeforeIsRejected() {
        var date = today.plusDays(30);
        assertEquals(400, rejected(() -> service.create("cus-1",
                new OccasionRequest("Sai", date, date.plusDays(1).atTime(8, 0), false))).getStatusCode().value());
        assertEquals(400, rejected(() -> service.create("cus-1",
                new OccasionRequest("Sai", date, date.minusDays(366).atTime(8, 0), false))).getStatusCode().value());
    }

    @Test
    void reminderOnTheDayItselfIsAllowed() {
        var date = today.plusDays(30);
        var response = service.create("cus-1", new OccasionRequest("Hôm đó", date, LocalDateTime.of(date, LocalTime.of(7, 0)), false));
        assertEquals(0, response.notifyDaysBefore());
    }

    @Test
    void hundredOccasionsIsTheLimit() {
        when(occasions.countByCustomerId("cus-1")).thenReturn(100L);
        var e = rejected(() -> service.create("cus-1", new OccasionRequest("Thứ 101", today.plusDays(5), null, false)));
        assertTrue(e.getReason().contains("100"));
        verify(occasions, never()).save(any());
    }

    @Test
    void changingTheScheduleAllowsTheReminderToFireAgain() {
        var existing = CustomerOccasion.builder().id("o1").title("Sinh nhật").occasionDate(today.plusDays(30))
                .notifyDaysBefore(7).notifyTime(LocalTime.of(8, 0)).reminderSent(true).lastRemindedFor(today.plusDays(30)).build();
        when(occasions.findByIdAndCustomerId("o1", "cus-1")).thenReturn(Optional.of(existing));

        service.update("cus-1", "o1", new OccasionRequest("Sinh nhật", today.plusDays(40), null, false));

        assertFalse(existing.getReminderSent());
        assertNull(existing.getLastRemindedFor());
    }

    @Test
    void renamingOnlyKeepsTheReminderAsAlreadySent() {
        var existing = CustomerOccasion.builder().id("o1").title("Sinh nhật").occasionDate(today.plusDays(30))
                .notifyDaysBefore(7).notifyTime(LocalTime.of(8, 0)).reminderSent(true).lastRemindedFor(today.plusDays(30)).build();
        when(occasions.findByIdAndCustomerId("o1", "cus-1")).thenReturn(Optional.of(existing));

        service.update("cus-1", "o1", new OccasionRequest("  Sinh nhật mẹ  ", today.plusDays(30), null, false));

        assertEquals("Sinh nhật mẹ", existing.getTitle());
        assertTrue(existing.getReminderSent(), "no duplicate email when only the title changed");
    }

    @Test
    void anotherCustomersOccasionIsNotFoundForUpdateAndDelete() {
        when(occasions.findByIdAndCustomerId(anyString(), anyString())).thenReturn(Optional.empty());

        assertEquals(404, rejected(() -> service.update("cus-2", "o1",
                new OccasionRequest("x", today.plusDays(5), null, false))).getStatusCode().value());
        assertEquals(404, rejected(() -> service.delete("cus-2", "o1")).getStatusCode().value());
        verify(occasions, never()).delete(any());
    }
}
