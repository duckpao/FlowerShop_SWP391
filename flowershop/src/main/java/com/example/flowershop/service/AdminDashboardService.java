package com.example.flowershop.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;

/**
 * Báo cáo tổng quan toàn hệ thống cho Admin: người dùng mới, đơn hàng theo nhóm trạng thái và doanh thu,
 * gom theo ngày (30 ngày gần nhất), tháng (12 tháng gần nhất) hoặc năm (5 năm gần nhất).
 * Doanh thu = tổng total_amount của đơn COMPLETED, tính theo ngày tạo đơn.
 */
@Service
@Transactional(readOnly = true)
public class AdminDashboardService {
    public enum Granularity { DAY, MONTH, YEAR }

    public record Counts(long newCustomers, long newShops, long ordersPlacing, long ordersDelivering,
                         long ordersCompleted, long ordersClosed, BigDecimal revenue) {}
    public record Point(String key, String label, Counts counts) {}
    public record Totals(long customers, long shops, long activeShops, long pendingShops, long bannedShops,
                         long pendingApplications, Counts orders) {}
    public record Dashboard(String granularity, LocalDate from, LocalDate to, Totals totals, Counts summary,
                            List<Point> series) {}

    private static final class Bucket {
        long customers, shops, placing, delivering, completed, closed;
        BigDecimal revenue = BigDecimal.ZERO;
        Counts toCounts() { return new Counts(customers, shops, placing, delivering, completed, closed, revenue); }
        void add(Bucket o) {
            customers += o.customers; shops += o.shops; placing += o.placing; delivering += o.delivering;
            completed += o.completed; closed += o.closed; revenue = revenue.add(o.revenue);
        }
        void order(String status, long count, BigDecimal total) {
            switch (status) {
                case "PENDING", "AWAITING_DEPOSIT", "DEPOSIT_PAID", "PROCESSING" -> placing += count;
                case "DELIVERING" -> delivering += count;
                case "COMPLETED" -> { completed += count; revenue = revenue.add(total); }
                case "CANCELLED", "REFUNDED" -> closed += count;
                default -> { }
            }
        }
    }

    private final JdbcTemplate jdbc;
    public AdminDashboardService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static final int MAX_DAYS = 366, MAX_MONTHS = 60, MAX_YEARS = 20;

    /** Không truyền from/to: dùng khoảng mặc định. Có from/to: báo cáo theo khoảng tuỳ chọn (gồm cả hai đầu mút). */
    public Dashboard dashboard(Granularity granularity, LocalDate customFrom, LocalDate customTo) {
        LocalDate today = LocalDate.now();
        LocalDate start, endExclusive;
        String format = switch (granularity) { case DAY -> "%Y-%m-%d"; case MONTH -> "%Y-%m"; case YEAR -> "%Y"; };
        if (customFrom == null && customTo == null) {
            switch (granularity) {
                case DAY -> { start = today.minusDays(29); endExclusive = today.plusDays(1); }
                case MONTH -> { start = today.withDayOfMonth(1).minusMonths(11); endExclusive = today.withDayOfMonth(1).plusMonths(1); }
                default -> { start = LocalDate.of(today.getYear() - 4, 1, 1); endExclusive = LocalDate.of(today.getYear() + 1, 1, 1); }
            }
        } else {
            if (customFrom == null || customTo == null)
                throw new IllegalArgumentException("Vui lòng chọn cả ngày bắt đầu và ngày kết thúc.");
            if (customFrom.isAfter(customTo))
                throw new IllegalArgumentException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
            switch (granularity) {
                case DAY -> { start = customFrom; endExclusive = customTo.plusDays(1); }
                case MONTH -> { start = customFrom.withDayOfMonth(1); endExclusive = customTo.withDayOfMonth(1).plusMonths(1); }
                default -> { start = LocalDate.of(customFrom.getYear(), 1, 1); endExclusive = LocalDate.of(customTo.getYear() + 1, 1, 1); }
            }
            long span = switch (granularity) {
                case DAY -> java.time.temporal.ChronoUnit.DAYS.between(start, endExclusive);
                case MONTH -> java.time.temporal.ChronoUnit.MONTHS.between(start, endExclusive);
                case YEAR -> java.time.temporal.ChronoUnit.YEARS.between(start, endExclusive);
            };
            long max = switch (granularity) { case DAY -> MAX_DAYS; case MONTH -> MAX_MONTHS; case YEAR -> MAX_YEARS; };
            if (span > max) throw new IllegalArgumentException("Khoảng thời gian quá dài: tối đa " + max + switch (granularity) {
                case DAY -> " ngày"; case MONTH -> " tháng"; case YEAR -> " năm"; } + " cho kiểu báo cáo này.");
        }

        Map<String, Bucket> buckets = new LinkedHashMap<>();
        for (LocalDate d = start; d.isBefore(endExclusive); d = next(d, granularity)) buckets.put(key(d, granularity), new Bucket());

        Timestamp from = Timestamp.valueOf(start.atStartOfDay());
        Timestamp to = Timestamp.valueOf(endExclusive.atStartOfDay());

        jdbc.query("SELECT DATE_FORMAT(created_date, '" + format + "') k, role, COUNT(*) c FROM Users "
                + "WHERE role IN ('CUSTOMER','SHOP') AND created_date >= ? AND created_date < ? GROUP BY k, role",
                rs -> {
                    Bucket b = buckets.get(rs.getString("k"));
                    if (b == null) return;
                    if ("CUSTOMER".equals(rs.getString("role"))) b.customers += rs.getLong("c");
                    else b.shops += rs.getLong("c");
                }, from, to);

        jdbc.query("SELECT DATE_FORMAT(created_date, '" + format + "') k, status, COUNT(*) c, COALESCE(SUM(total_amount),0) s FROM Orders "
                + "WHERE created_date >= ? AND created_date < ? GROUP BY k, status",
                rs -> {
                    Bucket b = buckets.get(rs.getString("k"));
                    if (b != null) b.order(rs.getString("status"), rs.getLong("c"), rs.getBigDecimal("s"));
                }, from, to);

        Bucket summary = new Bucket();
        List<Point> series = new ArrayList<>();
        buckets.forEach((k, b) -> { summary.add(b); series.add(new Point(k, label(k, granularity), b.toCounts())); });

        return new Dashboard(granularity.name(), start, endExclusive.minusDays(1), totals(), summary.toCounts(), series);
    }

    private Totals totals() {
        long[] users = new long[2]; // customers, shops
        jdbc.query("SELECT role, COUNT(*) c FROM Users WHERE role IN ('CUSTOMER','SHOP') GROUP BY role", rs -> {
            users["CUSTOMER".equals(rs.getString("role")) ? 0 : 1] = rs.getLong("c");
        });
        long[] shops = new long[3]; // active, pending, banned
        jdbc.query("SELECT status, COUNT(*) c FROM Shops GROUP BY status", rs -> {
            switch (rs.getString("status")) {
                case "ACTIVE" -> shops[0] = rs.getLong("c");
                case "PENDING" -> shops[1] = rs.getLong("c");
                case "BANNED" -> shops[2] = rs.getLong("c");
                default -> { }
            }
        });
        Long applications = jdbc.queryForObject("SELECT COUNT(*) FROM Manager_Applications WHERE status = 'PENDING'", Long.class);
        Bucket orders = new Bucket();
        jdbc.query("SELECT status, COUNT(*) c, COALESCE(SUM(total_amount),0) s FROM Orders GROUP BY status",
                rs -> { orders.order(rs.getString("status"), rs.getLong("c"), rs.getBigDecimal("s")); });
        return new Totals(users[0], users[1], shops[0], shops[1], shops[2], applications == null ? 0 : applications, orders.toCounts());
    }

    private static LocalDate next(LocalDate d, Granularity g) {
        return switch (g) { case DAY -> d.plusDays(1); case MONTH -> d.plusMonths(1); case YEAR -> d.plusYears(1); };
    }

    private static String key(LocalDate d, Granularity g) {
        return switch (g) {
            case DAY -> d.toString();
            case MONTH -> String.format("%04d-%02d", d.getYear(), d.getMonthValue());
            case YEAR -> String.valueOf(d.getYear());
        };
    }

    private static String label(String key, Granularity g) {
        return switch (g) {
            case DAY -> key.substring(8) + "/" + key.substring(5, 7);
            case MONTH -> key.substring(5) + "/" + key.substring(0, 4);
            case YEAR -> key;
        };
    }
}
