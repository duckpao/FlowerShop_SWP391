package com.example.flowershop.service;

import com.example.flowershop.entity.Shop;
import com.example.flowershop.entity.enums.OrderStatus;
import com.example.flowershop.repository.FavoriteShopRepository;
import com.example.flowershop.repository.OrderDetailRepository;
import com.example.flowershop.repository.OrderRepository;
import com.example.flowershop.repository.PaymentRepository;
import com.example.flowershop.repository.RefundRepository;
import com.example.flowershop.repository.ShopRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.EnumMap;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ShopReportService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public enum Period {
        DAY, MONTH, YEAR
    }

    public record PeriodResult(Period type, String value, LocalDate startDate, LocalDate endDate) {
    }

    public record FollowerMetrics(long current, long newInPeriod) {
    }

    public record OrderMetrics(long total, long processing, long delivering, long completed,
            long cancelled, long refunded) {
    }

    public record RevenueMetrics(BigDecimal grossReceived, BigDecimal refundedAmount,
            BigDecimal netRevenue) {
    }

    public record ProductPerformance(String productId, String productName,
            long completedQuantity, long returnedQuantity, long quantityDifference,
            long completedOrderCount, long returnedOrderCount, BigDecimal returnRate) {
    }

    public record ReportResult(String shopId, String shopName, PeriodResult period,
            FollowerMetrics followers, OrderMetrics orders, RevenueMetrics revenue,
            List<ProductPerformance> products) {
    }

    private record Range(Period period, String value, LocalDate start, LocalDate endExclusive) {
    }

    private final ShopRepository shops;
    private final FavoriteShopRepository followers;
    private final OrderRepository orders;
    private final OrderDetailRepository orderDetails;
    private final PaymentRepository payments;
    private final RefundRepository refunds;

    public ShopReportService(ShopRepository shops, FavoriteShopRepository followers,
            OrderRepository orders, OrderDetailRepository orderDetails,
            PaymentRepository payments, RefundRepository refunds) {
        this.shops = shops;
        this.followers = followers;
        this.orders = orders;
        this.orderDetails = orderDetails;
        this.payments = payments;
        this.refunds = refunds;
    }

    public ReportResult report(String shopId, String actor, String requestedPeriod, String requestedValue) {
        Shop shop = owned(shopId, actor);
        Range range = range(requestedPeriod, requestedValue);
        LocalDateTime start = range.start().atStartOfDay();
        LocalDateTime end = range.endExclusive().atStartOfDay();

        EnumMap<OrderStatus, Long> statusCounts = new EnumMap<>(OrderStatus.class);
        for (Object[] row : orders.countStatusesForReport(shopId, start, end)) {
            statusCounts.put((OrderStatus) row[0], ((Number) row[1]).longValue());
        }
        long processing = count(statusCounts, OrderStatus.PENDING)
                + count(statusCounts, OrderStatus.AWAITING_DEPOSIT)
                + count(statusCounts, OrderStatus.DEPOSIT_PAID)
                + count(statusCounts, OrderStatus.PROCESSING);
        long delivering = count(statusCounts, OrderStatus.DELIVERING);
        long completed = count(statusCounts, OrderStatus.COMPLETED);
        long cancelled = count(statusCounts, OrderStatus.CANCELLED);
        long refunded = count(statusCounts, OrderStatus.REFUNDED);
        long total = statusCounts.values().stream().mapToLong(Long::longValue).sum();

        BigDecimal gross = zero(payments.sumSuccessfulAmountForReport(shopId, start, end));
        BigDecimal refundedAmount = zero(refunds.sumProcessedAmountForReport(shopId, start, end));
        List<ProductPerformance> products = orderDetails.productPerformanceForReport(shopId, start, end)
                .stream().map(ShopReportService::productPerformance).toList();

        return new ReportResult(shopId, shop.getName(),
                new PeriodResult(range.period(), range.value(), range.start(), range.endExclusive().minusDays(1)),
                new FollowerMetrics(followers.countByIdShopId(shopId),
                        followers.countByIdShopIdAndCreatedDateGreaterThanEqualAndCreatedDateLessThan(shopId, start, end)),
                new OrderMetrics(total, processing, delivering, completed, cancelled, refunded),
                new RevenueMetrics(gross, refundedAmount, gross.subtract(refundedAmount)), products);
    }

    private Shop owned(String shopId, String actor) {
        Shop shop = shops.findById(shopId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy shop."));
        if (!shop.getOwner().getId().equals(actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không sở hữu shop này.");
        }
        return shop;
    }

    private static Range range(String requestedPeriod, String requestedValue) {
        Period period;
        try {
            period = requestedPeriod == null || requestedPeriod.isBlank()
                    ? Period.MONTH : Period.valueOf(requestedPeriod.strip().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Loại báo cáo phải là DAY, MONTH hoặc YEAR.");
        }
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        try {
            return switch (period) {
                case DAY -> {
                    LocalDate day = requestedValue == null || requestedValue.isBlank()
                            ? today : LocalDate.parse(requestedValue.strip());
                    yield new Range(period, day.toString(), day, day.plusDays(1));
                }
                case MONTH -> {
                    YearMonth month = requestedValue == null || requestedValue.isBlank()
                            ? YearMonth.from(today) : YearMonth.parse(requestedValue.strip());
                    yield new Range(period, month.toString(), month.atDay(1), month.plusMonths(1).atDay(1));
                }
                case YEAR -> {
                    Year year = requestedValue == null || requestedValue.isBlank()
                            ? Year.from(today) : Year.parse(requestedValue.strip());
                    yield new Range(period, year.toString(), year.atDay(1), year.plusYears(1).atDay(1));
                }
            };
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Mốc thời gian không đúng định dạng của loại báo cáo.");
        }
    }

    private static long count(EnumMap<OrderStatus, Long> counts, OrderStatus status) {
        return counts.getOrDefault(status, 0L);
    }

    private static BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static ProductPerformance productPerformance(Object[] row) {
        long completedQuantity = ((Number) row[2]).longValue();
        long returnedQuantity = ((Number) row[3]).longValue();
        long completedOrders = ((Number) row[4]).longValue();
        long returnedOrders = ((Number) row[5]).longValue();
        long outcomeQuantity = completedQuantity + returnedQuantity;
        BigDecimal returnRate = outcomeQuantity == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(returnedQuantity)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(outcomeQuantity), 2, RoundingMode.HALF_UP);
        return new ProductPerformance((String) row[0], (String) row[1], completedQuantity, returnedQuantity,
                completedQuantity - returnedQuantity, completedOrders, returnedOrders, returnRate);
    }
}
