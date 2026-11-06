package com.example.flowershop.service;

import com.example.flowershop.entity.CustomerOccasion;
import com.example.flowershop.repository.CustomerOccasionRepository;
import com.example.flowershop.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/** Khách quản lý các dịp đặc biệt (ngày + thời điểm muốn được nhắc qua email). */
@Service
@Transactional(readOnly = true)
public class OccasionService {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int MAX_PER_CUSTOMER = 100;
    private static final long MAX_DAYS_BEFORE = 365;

    /**
     * @param notifyAt thời điểm muốn nhắc (tùy chọn); bỏ trống thì nhắc trước 7 ngày lúc 08:00.
     *                 Với dịp hằng năm, khoảng cách ngày và giờ này được áp dụng cho mỗi năm.
     */
    public record OccasionRequest(
            @NotBlank @Size(max = 255) String title,
            @NotNull LocalDate occasionDate,
            LocalDateTime notifyAt,
            Boolean recurringYearly) {
    }

    public record OccasionResponse(
            String id,
            String title,
            LocalDate occasionDate,
            boolean recurringYearly,
            LocalDate nextOccurrence,
            LocalDateTime notifyAt,
            int notifyDaysBefore,
            LocalTime notifyTime,
            boolean customNotify,
            boolean reminded) {
    }

    private final CustomerOccasionRepository occasions;
    private final UserRepository users;

    public OccasionService(CustomerOccasionRepository occasions, UserRepository users) {
        this.occasions = occasions;
        this.users = users;
    }

    public List<OccasionResponse> mine(String customerId) {
        LocalDate today = LocalDate.now(ZONE);
        return occasions.findByCustomerIdOrderByOccasionDateAsc(customerId).stream()
                .map(o -> toResponse(o, today))
                .sorted(java.util.Comparator.comparing(OccasionResponse::nextOccurrence))
                .toList();
    }

    @Transactional
    public OccasionResponse create(String customerId, OccasionRequest request) {
        if (occasions.countByCustomerId(customerId) >= MAX_PER_CUSTOMER)
            throw bad("Bạn đã đạt giới hạn " + MAX_PER_CUSTOMER + " dịp đặc biệt.");
        CustomerOccasion o = CustomerOccasion.builder()
                .id(UUID.randomUUID().toString())
                .customer(users.findById(customerId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản.")))
                .createdBy(customerId)
                .build();
        apply(o, request, customerId);
        return toResponse(occasions.save(o), LocalDate.now(ZONE));
    }

    @Transactional
    public OccasionResponse update(String customerId, String id, OccasionRequest request) {
        CustomerOccasion o = find(customerId, id);
        apply(o, request, customerId);
        return toResponse(occasions.save(o), LocalDate.now(ZONE));
    }

    @Transactional
    public void delete(String customerId, String id) {
        occasions.delete(find(customerId, id));
    }

    private CustomerOccasion find(String customerId, String id) {
        return occasions.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy dịp đặc biệt."));
    }

    private void apply(CustomerOccasion o, OccasionRequest r, String actor) {
        boolean recurring = Boolean.TRUE.equals(r.recurringYearly());
        LocalDate today = LocalDate.now(ZONE);
        if (!recurring && r.occasionDate().isBefore(today)) throw bad("Ngày diễn ra dịp không được ở quá khứ.");

        int days = OccasionSchedule.DEFAULT_DAYS_BEFORE;
        LocalTime time = OccasionSchedule.DEFAULT_TIME;
        if (r.notifyAt() != null) {
            long between = ChronoUnit.DAYS.between(r.notifyAt().toLocalDate(), r.occasionDate());
            if (between < 0 || between > MAX_DAYS_BEFORE)
                throw bad("Thời điểm nhắc phải trước hoặc đúng ngày diễn ra dịp, tối đa " + MAX_DAYS_BEFORE + " ngày.");
            days = (int) between;
            time = r.notifyAt().toLocalTime().withSecond(0).withNano(0);
        }

        // Đổi ngày/giờ nhắc thì cho phép nhắc lại
        boolean scheduleChanged = !r.occasionDate().equals(o.getOccasionDate()) || o.getNotifyDaysBefore() == null
                || days != o.getNotifyDaysBefore() || !time.equals(o.getNotifyTime()) || recurring != Boolean.TRUE.equals(o.getRecurringYearly());
        o.setTitle(r.title().trim());
        o.setOccasionDate(r.occasionDate());
        o.setRecurringYearly(recurring);
        o.setNotifyDaysBefore(days);
        o.setNotifyTime(time);
        if (scheduleChanged) {
            o.setReminderSent(false);
            o.setLastRemindedFor(null);
        }
        o.setLastModifyBy(actor);
    }

    private static OccasionResponse toResponse(CustomerOccasion o, LocalDate today) {
        LocalDate next = OccasionSchedule.nextOccurrence(o, today);
        boolean recurring = Boolean.TRUE.equals(o.getRecurringYearly());
        int days = o.getNotifyDaysBefore() == null ? OccasionSchedule.DEFAULT_DAYS_BEFORE : o.getNotifyDaysBefore();
        LocalTime time = o.getNotifyTime() == null ? OccasionSchedule.DEFAULT_TIME : o.getNotifyTime();
        boolean reminded = recurring ? next.equals(o.getLastRemindedFor()) : Boolean.TRUE.equals(o.getReminderSent());
        return new OccasionResponse(o.getId(), o.getTitle(), o.getOccasionDate(), recurring, next,
                OccasionSchedule.remindAt(o, next), days, time,
                days != OccasionSchedule.DEFAULT_DAYS_BEFORE || !time.equals(OccasionSchedule.DEFAULT_TIME), reminded);
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
