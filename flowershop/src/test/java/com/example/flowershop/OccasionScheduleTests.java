package com.example.flowershop;

import com.example.flowershop.entity.CustomerOccasion;
import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.enums.OrderStatus;
import com.example.flowershop.event.OrderStatusChangedEvent;
import com.example.flowershop.event.OrderStatusListener;
import com.example.flowershop.service.OccasionSchedule;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OccasionScheduleTests {

    private static CustomerOccasion occasion(String date, boolean recurring) {
        return CustomerOccasion.builder().id("o1").title("Sinh nhật")
                .occasionDate(LocalDate.parse(date)).recurringYearly(recurring).build();
    }

    @Test
    void defaultsToSevenDaysBeforeAtEightAm() {
        var o = occasion("2026-12-20", false);
        assertEquals(LocalDateTime.parse("2026-12-13T08:00"), OccasionSchedule.remindAt(o, o.getOccasionDate()));
        assertFalse(OccasionSchedule.isDue(o, LocalDateTime.parse("2026-12-13T07:59")));
        assertTrue(OccasionSchedule.isDue(o, LocalDateTime.parse("2026-12-13T08:00")));
    }

    @Test
    void customDaysAndTimeAreRespected() {
        var o = occasion("2026-12-20", false);
        o.setNotifyDaysBefore(2);
        o.setNotifyTime(LocalTime.of(18, 30));
        assertFalse(OccasionSchedule.isDue(o, LocalDateTime.parse("2026-12-18T18:29")));
        assertTrue(OccasionSchedule.isDue(o, LocalDateTime.parse("2026-12-18T18:30")));
    }

    @Test
    void lateCreatedOccasionIsStillDueUntilItsDayPasses() {
        var o = occasion("2026-12-20", false);
        assertTrue(OccasionSchedule.isDue(o, LocalDateTime.parse("2026-12-19T10:00")));
        assertTrue(OccasionSchedule.isDue(o, LocalDateTime.parse("2026-12-20T23:00")));
        assertFalse(OccasionSchedule.isDue(o, LocalDateTime.parse("2026-12-21T00:01")));
    }

    @Test
    void oneTimeOccasionIsRemindedOnlyOnce() {
        var o = occasion("2026-12-20", false);
        o.setReminderSent(true);
        assertFalse(OccasionSchedule.isDue(o, LocalDateTime.parse("2026-12-15T09:00")));
    }

    @Test
    void recurringOccasionRollsToNextYearAfterItPasses() {
        var o = occasion("1990-03-10", true);
        assertEquals(LocalDate.parse("2026-03-10"), OccasionSchedule.nextOccurrence(o, LocalDate.parse("2026-03-10")));
        assertEquals(LocalDate.parse("2027-03-10"), OccasionSchedule.nextOccurrence(o, LocalDate.parse("2026-03-11")));
        o.setLastRemindedFor(LocalDate.parse("2026-03-10"));
        assertFalse(OccasionSchedule.isDue(o, LocalDateTime.parse("2026-03-10T12:00")));
        assertFalse(OccasionSchedule.isDue(o, LocalDateTime.parse("2026-03-11T12:00")));
        assertTrue(OccasionSchedule.isDue(o, LocalDateTime.parse("2027-03-03T08:00")));
    }

    @Test
    void leapDayFallsBackToFebruary28() {
        var o = occasion("2024-02-29", true);
        assertEquals(LocalDate.parse("2027-02-28"), OccasionSchedule.nextOccurrence(o, LocalDate.parse("2027-01-01")));
        assertEquals(LocalDate.parse("2028-02-29"), OccasionSchedule.nextOccurrence(o, LocalDate.parse("2028-01-01")));
    }

    @Test
    void orderStatusListenerPublishesOnlyWhenStatusChangedAfterLoad() {
        var publisher = mock(ApplicationEventPublisher.class);
        var listener = new OrderStatusListener(publisher);
        Order order = Order.builder().id("order-1").status(OrderStatus.PENDING).build();

        listener.publishIfChanged(order); // đơn mới tạo, chưa có ảnh chụp: không phát
        verifyNoInteractions(publisher);

        order.setStatusSnapshot(OrderStatus.PENDING);
        order.setStatus(OrderStatus.PROCESSING);
        listener.publishIfChanged(order);
        var captor = ArgumentCaptor.forClass(OrderStatusChangedEvent.class);
        verify(publisher).publishEvent(captor.capture());
        assertEquals(OrderStatus.PENDING, captor.getValue().from());
        assertEquals(OrderStatus.PROCESSING, captor.getValue().to());

        listener.publishIfChanged(order); // cập nhật khác, trạng thái giữ nguyên: không phát thêm
        verifyNoMoreInteractions(publisher);
    }
}
