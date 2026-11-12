package com.example.flowershop;

import com.example.flowershop.entity.CustomerOccasion;
import com.example.flowershop.entity.User;
import com.example.flowershop.repository.CustomerOccasionRepository;
import com.example.flowershop.service.HtmlMailSender;
import com.example.flowershop.service.NotificationService;
import com.example.flowershop.service.OccasionReminderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Luồng nhắc dịp đặc biệt: email + thông báo chuông, đánh dấu đã nhắc, thử lại khi mail lỗi. */
class OccasionReminderServiceTests {

    private static final LocalDateTime NOW = LocalDateTime.parse("2026-12-13T08:05");

    private CustomerOccasionRepository occasions;
    private HtmlMailSender mail;
    private NotificationService notifications;
    private OccasionReminderService service;

    @BeforeEach
    void setUp() {
        occasions = mock(CustomerOccasionRepository.class);
        mail = mock(HtmlMailSender.class);
        notifications = mock(NotificationService.class);
        when(mail.url(anyString())).thenAnswer(i -> "http://app.test" + i.getArgument(0));
        service = new OccasionReminderService(occasions, mail, notifications);
    }

    private static User customer(boolean verified) {
        return User.builder().id("cus-1").email("khach@example.test").fullName("Trần Thị B").isEmailVerified(verified).build();
    }

    private static CustomerOccasion occasion(User customer, String date, boolean recurring) {
        return CustomerOccasion.builder().id("occ-" + date).customer(customer).title("Sinh nhật mẹ <3")
                .occasionDate(LocalDate.parse(date)).recurringYearly(recurring).build();
    }

    private void candidates(CustomerOccasion... list) {
        when(occasions.findReminderCandidates(any())).thenReturn(List.of(list));
    }

    @Test
    void dueOccasionSendsEmailAndBellNotificationAndIsMarkedSent() {
        var o = occasion(customer(true), "2026-12-20", false);
        candidates(o);
        when(mail.send(anyString(), anyString(), anyString())).thenReturn(true);

        assertEquals(1, service.sendDueReminders(NOW));

        var html = ArgumentCaptor.forClass(String.class);
        verify(mail).send(eq("khach@example.test"), eq("FlowerShop - Sắp đến Sinh nhật mẹ <3"), html.capture());
        // Thân thư được HTML-escape (chữ có dấu thành thực thể HTML hợp lệ), nên so với kết quả của esc()
        assertTrue(html.getValue().contains(HtmlMailSender.esc("Trần Thị B")));
        assertTrue(html.getValue().contains(HtmlMailSender.esc("sau 7 ngày nữa (20/12/2026)")));
        assertTrue(html.getValue().contains(HtmlMailSender.esc("Sinh nhật mẹ <3")));
        assertFalse(html.getValue().contains("mẹ <3"), "raw '<' from the title must never reach the HTML");
        assertTrue(html.getValue().contains("http://app.test/occasions"));

        var message = ArgumentCaptor.forClass(String.class);
        verify(notifications).notifyUser(eq(o.getCustomer()), eq("Sắp đến dịp: Sinh nhật mẹ <3"), message.capture(), eq("/shops"));
        assertTrue(message.getValue().contains("sau 7 ngày nữa (20/12/2026)"));

        assertTrue(o.getReminderSent(), "one-time occasion must not be reminded again");
        assertEquals(LocalDate.parse("2026-12-20"), o.getLastRemindedFor());
        verify(occasions).save(o);
    }

    @Test
    void recurringOccasionStaysActiveAndRemembersTheYearItWasReminded() {
        var o = occasion(customer(true), "1990-12-20", true);
        candidates(o);
        when(mail.send(anyString(), anyString(), anyString())).thenReturn(true);

        assertEquals(1, service.sendDueReminders(NOW));

        assertFalse(o.getReminderSent(), "yearly occasion must keep reminding next year");
        assertEquals(LocalDate.parse("2026-12-20"), o.getLastRemindedFor());
    }

    @Test
    void whenTheOccasionIsTodayOrTomorrowTheWordingSaysSo() {
        var today = occasion(customer(true), "2026-12-13", false);
        var tomorrow = occasion(customer(true), "2026-12-14", false);
        candidates(today, tomorrow);
        when(mail.send(anyString(), anyString(), anyString())).thenReturn(true);

        assertEquals(2, service.sendDueReminders(NOW));

        var message = ArgumentCaptor.forClass(String.class);
        verify(notifications, times(2)).notifyUser(any(), anyString(), message.capture(), anyString());
        assertTrue(message.getAllValues().get(0).contains("diễn ra hôm nay"));
        assertTrue(message.getAllValues().get(1).contains("diễn ra ngày mai"));
    }

    @Test
    void failedSmtpLeavesTheOccasionUnsentSoTheNextScanRetries() {
        var o = occasion(customer(true), "2026-12-20", false);
        candidates(o);
        when(mail.send(anyString(), anyString(), anyString())).thenReturn(false);

        assertEquals(0, service.sendDueReminders(NOW));

        assertFalse(o.getReminderSent());
        assertNull(o.getLastRemindedFor());
        verify(notifications, never()).notifyUser(any(), anyString(), anyString(), anyString());
        verify(occasions, never()).save(any());
    }

    @Test
    void unverifiedEmailSkipsTheMailButStillShowsTheBellNotification() {
        var o = occasion(customer(false), "2026-12-20", false);
        candidates(o);

        assertEquals(1, service.sendDueReminders(NOW));

        verify(mail, never()).send(anyString(), anyString(), anyString());
        verify(notifications).notifyUser(eq(o.getCustomer()), anyString(), anyString(), eq("/shops"));
        assertTrue(o.getReminderSent());
    }

    @Test
    void occasionBeforeItsReminderTimeIsNotSent() {
        var o = occasion(customer(true), "2026-12-25", false); // remind at 2026-12-18 08:00
        candidates(o);

        assertEquals(0, service.sendDueReminders(NOW));

        verify(mail, never()).send(anyString(), anyString(), anyString());
        verify(notifications, never()).notifyUser(any(), anyString(), anyString(), anyString());
    }

    @Test
    void alreadyRemindedOccasionIsNotSentTwice() {
        var o = occasion(customer(true), "2026-12-20", false);
        o.setReminderSent(true);
        candidates(o);

        assertEquals(0, service.sendDueReminders(NOW));
        verify(mail, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void oneBrokenOccasionDoesNotStopTheOthers() {
        var broken = occasion(customer(true), "2026-12-20", false);
        var fine = occasion(customer(true), "2026-12-19", false);
        candidates(broken, fine);
        when(mail.send(anyString(), anyString(), anyString())).thenReturn(true);
        doThrow(new IllegalStateException("db down")).when(occasions).save(broken);

        assertEquals(1, service.sendDueReminders(NOW));
        verify(occasions).save(fine);
    }
}
