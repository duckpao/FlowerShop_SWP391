package com.example.flowershop.service;

import com.example.flowershop.entity.CustomerOccasion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;

/** Tính ngày diễn ra kế tiếp và thời điểm nhắc của một dịp đặc biệt (không phụ thuộc DB, dễ kiểm thử). */
public final class OccasionSchedule {

    public static final int DEFAULT_DAYS_BEFORE = 7;
    public static final LocalTime DEFAULT_TIME = LocalTime.of(8, 0);

    private OccasionSchedule() {
    }

    /** Ngày diễn ra kế tiếp tính từ today (kể cả hôm nay): dịp một lần là ngày đã nhập, dịp hằng năm là lần sắp tới. */
    public static LocalDate nextOccurrence(CustomerOccasion o, LocalDate today) {
        if (!Boolean.TRUE.equals(o.getRecurringYearly())) return o.getOccasionDate();
        // MonthDay.atYear đưa 29/2 về 28/2 ở năm không nhuận
        MonthDay monthDay = MonthDay.from(o.getOccasionDate());
        LocalDate thisYear = monthDay.atYear(today.getYear());
        return thisYear.isBefore(today) ? monthDay.atYear(today.getYear() + 1) : thisYear;
    }

    public static LocalDateTime remindAt(CustomerOccasion o, LocalDate occurrence) {
        int days = o.getNotifyDaysBefore() == null ? DEFAULT_DAYS_BEFORE : o.getNotifyDaysBefore();
        LocalTime time = o.getNotifyTime() == null ? DEFAULT_TIME : o.getNotifyTime();
        return occurrence.minusDays(days).atTime(time);
    }

    /**
     * Đã đến lúc nhắc: tới thời điểm nhắc, dịp chưa diễn ra xong, và lần diễn ra này chưa được nhắc.
     * Dịp được tạo muộn (thời điểm nhắc đã qua nhưng dịp chưa tới) vẫn được nhắc ngay ở lần quét kế tiếp.
     */
    public static boolean isDue(CustomerOccasion o, LocalDateTime now) {
        LocalDate occurrence = nextOccurrence(o, now.toLocalDate());
        if (occurrence.isBefore(now.toLocalDate())) return false;
        if (occurrence.equals(o.getLastRemindedFor())) return false;
        if (!Boolean.TRUE.equals(o.getRecurringYearly()) && Boolean.TRUE.equals(o.getReminderSent())) return false;
        return !now.isBefore(remindAt(o, occurrence));
    }
}
