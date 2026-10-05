// package project.EnterpriseSaas.demo.modules.reports.service;

// import com.fasterxml.jackson.core.type.TypeReference;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.http.HttpStatus;
// import org.springframework.jdbc.core.JdbcTemplate;
// import org.springframework.stereotype.Service;
// import org.springframework.web.server.ResponseStatusException;

// import java.math.BigDecimal;
// import java.math.RoundingMode;
// import java.time.LocalDate;
// import java.time.OffsetDateTime;
// import java.time.format.DateTimeFormatter;
// import java.time.temporal.ChronoUnit;
// import java.util.*;

// @Service
// @RequiredArgsConstructor
// @Slf4j
// public class ReportsService {

//     private final JdbcTemplate jdbcTemplate;
//     private final ObjectMapper objectMapper;

//     private void assertDate(String value, String name) {
//         try {
//             LocalDate.parse(value);
//         } catch (Exception e) {
//             throw new ResponseStatusException(
//                     HttpStatus.BAD_REQUEST,
//                     String.format("Invalid date for '%s': \"%s\". Use YYYY-MM-DD format.", name, value)
//             );
//         }
//     }

//     private OffsetDateTime toStartOfDay(String dateStr) {
//         return OffsetDateTime.parse(dateStr + "T00:00:00.000+05:30");
//     }

//     private OffsetDateTime toEndOfDay(String dateStr) {
//         return OffsetDateTime.parse(dateStr + "T23:59:59.999+05:30");
//     }

//     // ── Helper to safely bypass Postgres NULL casting issues ───────────────
//     private String getBranchClause(UUID branchId, String columnPrefix) {
//         if (branchId == null) return "";
//         return " AND " + columnPrefix + "branch_id = '" + branchId.toString() + "' ";
//     }

//     // ── Daily Sales ────────────────────────────────────────────────────────────

//     public List<Map<String, Object>> getDailySales(UUID branchId, UUID tenantId, String from, String to) {
//         assertDate(from, "from");
//         assertDate(to, "to");
//         OffsetDateTime start = toStartOfDay(from);
//         OffsetDateTime end = toEndOfDay(to);

//         String sql = """
//             SELECT
//               date_trunc('day', created_at AT TIME ZONE 'Asia/Kolkata')
//                 AT TIME ZONE 'Asia/Kolkata'                              AS date,
//               COUNT(*)::int                     AS total_bills,
//               COALESCE(SUM(grand_total),     0) AS gross_sales,
//               COALESCE(SUM(discount_amount), 0) AS total_discount,
//               COALESCE(SUM(total_tax),       0) AS total_tax,
//               COALESCE(SUM(cgst_amount),     0) AS cgst,
//               COALESCE(SUM(sgst_amount),     0) AS sgst,
//               COALESCE(SUM(igst_amount),     0) AS igst
//             FROM bills
//             WHERE tenant_id = ?
//             """ + getBranchClause(branchId, "") + """
//               AND status NOT IN ('void', 'refunded')
//               AND created_at BETWEEN ? AND ?
//             GROUP BY date_trunc('day', created_at AT TIME ZONE 'Asia/Kolkata')
//             ORDER BY date ASC
//         """;

//         return jdbcTemplate.queryForList(sql, tenantId, start, end);
//     }

//     // ── Item Sales ─────────────────────────────────────────────────────────────

//     public List<Map<String, Object>> getItemSalesReport(UUID branchId, UUID tenantId, String from, String to) {
//         assertDate(from, "from");
//         assertDate(to, "to");
//         OffsetDateTime start = toStartOfDay(from);
//         OffsetDateTime end = toEndOfDay(to);

//         String sql = """
//             SELECT
//               oi.name                                   AS item_name,
//               oi.menu_item_id,
//               SUM(oi.quantity)::numeric                 AS total_qty,
//               COALESCE(SUM(oi.line_total), 0)::numeric  AS total_revenue,
//               SUM(
//                 oi.taxable_amount::numeric
//                 * (1.0 - COALESCE(
//                     o.discount_amount::numeric / NULLIF(o.subtotal::numeric, 0), 0
//                   ))
//               )                                         AS taxable,
//               COUNT(DISTINCT oi.order_id)::int          AS order_count
//             FROM order_items oi
//             JOIN orders o ON o.id = oi.order_id
//             WHERE o.tenant_id  = ?
//             """ + getBranchClause(branchId, "o.") + """
//               AND oi.is_voided = false
//               AND o.status     = 'billed'
//               AND o.created_at BETWEEN ? AND ?
//             GROUP BY oi.name, oi.menu_item_id
//             ORDER BY total_revenue DESC
//             LIMIT 100
//         """;

//         return jdbcTemplate.queryForList(sql, tenantId, start, end);
//     }

//     // ── Payment Methods ────────────────────────────────────────────────────────

//     public List<Map<String, Object>> getPaymentMethodReport(UUID branchId, UUID tenantId, String from, String to) {
//         assertDate(from, "from");
//         assertDate(to, "to");
//         OffsetDateTime start = toStartOfDay(from);
//         OffsetDateTime end = toEndOfDay(to);

//         String sql = """
//             SELECT
//               method,
//               COUNT(*)::int            AS transaction_count,
//               COALESCE(SUM(amount), 0) AS total_amount
//             FROM payments
//             WHERE tenant_id  = ?
//             """ + getBranchClause(branchId, "") + """
//               AND status     = 'success'
//               AND created_at BETWEEN ? AND ?
//             GROUP BY method
//             ORDER BY total_amount DESC
//         """;

//         return jdbcTemplate.queryForList(sql, tenantId, start, end);
//     }

//     // ── Hourly Report ──────────────────────────────────────────────────────────

//     public List<Map<String, Object>> getHourlyReport(UUID branchId, UUID tenantId, String date) {
//         assertDate(date, "date");
//         OffsetDateTime start = toStartOfDay(date);
//         OffsetDateTime end = toEndOfDay(date);

//         String sql = """
//             SELECT
//               EXTRACT(HOUR FROM created_at AT TIME ZONE 'Asia/Kolkata')::int AS hour,
//               COUNT(*)::int                 AS total_bills,
//               COALESCE(SUM(grand_total), 0) AS revenue
//             FROM bills
//             WHERE tenant_id = ?
//             """ + getBranchClause(branchId, "") + """
//               AND status NOT IN ('void', 'refunded')
//               AND created_at BETWEEN ? AND ?
//             GROUP BY EXTRACT(HOUR FROM created_at AT TIME ZONE 'Asia/Kolkata')
//             ORDER BY hour
//         """;

//         return jdbcTemplate.queryForList(sql, tenantId, start, end);
//     }

//     // ── Category Report ────────────────────────────────────────────────────────

//     public List<Map<String, Object>> getCategoryReport(UUID branchId, UUID tenantId, String from, String to) {
//         assertDate(from, "from");
//         assertDate(to, "to");
//         OffsetDateTime start = toStartOfDay(from);
//         OffsetDateTime end = toEndOfDay(to);

//         String sql = """
//             SELECT
//               COALESCE(c.name, 'Uncategorised') AS category_name,
//               SUM(oi.quantity)::numeric          AS total_qty,
//               COALESCE(SUM(oi.line_total), 0)   AS total_revenue
//             FROM order_items   oi
//             JOIN   orders      o  ON o.id  = oi.order_id
//             LEFT JOIN menu_items mi ON mi.id = oi.menu_item_id
//             LEFT JOIN categories  c  ON c.id  = mi.category_id
//             WHERE o.tenant_id  = ?
//             """ + getBranchClause(branchId, "o.") + """
//               AND oi.is_voided = false
//               AND o.status     = 'billed'
//               AND o.created_at BETWEEN ? AND ?
//             GROUP BY COALESCE(c.name, 'Uncategorised')
//             ORDER BY total_revenue DESC
//         """;

//         return jdbcTemplate.queryForList(sql, tenantId, start, end);
//     }

//     // ── GST Report ─────────────────────────────────────────────────────────────

//     public List<Map<String, Object>> getGstReport(UUID branchId, UUID tenantId, String from, String to) {
//         assertDate(from, "from");
//         assertDate(to, "to");
//         OffsetDateTime start = toStartOfDay(from);
//         OffsetDateTime end = toEndOfDay(to);

//         String sql = """
//             SELECT
//               DATE_TRUNC('month', issued_at AT TIME ZONE 'Asia/Kolkata')
//                 AT TIME ZONE 'Asia/Kolkata'                              AS month,
//               COALESCE(SUM(taxable_amount), 0) AS taxable_value,
//               COALESCE(SUM(cgst_amount),    0) AS cgst,
//               COALESCE(SUM(sgst_amount),    0) AS sgst,
//               COALESCE(SUM(igst_amount),    0) AS igst,
//               COALESCE(SUM(cess_amount),    0) AS cess,
//               COALESCE(SUM(total_tax),      0) AS total_tax,
//               COALESCE(SUM(grand_total),    0) AS gross_value,
//               COUNT(*)::int                    AS total_invoices
//             FROM bills
//             WHERE tenant_id = ?
//             """ + getBranchClause(branchId, "") + """
//               AND status NOT IN ('void', 'refunded')
//               AND issued_at BETWEEN ? AND ?
//             GROUP BY DATE_TRUNC('month', issued_at AT TIME ZONE 'Asia/Kolkata')
//             ORDER BY month
//         """;

//         return jdbcTemplate.queryForList(sql, tenantId, start, end);
//     }

//     // ── GSTR-1 JSON Export ─────────────────────────────────────────────────────

//     public Map<String, Object> getGstr1Export(UUID branchId, UUID tenantId, String from, String to) {
//         assertDate(from, "from");
//         assertDate(to, "to");
//         OffsetDateTime start = toStartOfDay(from);
//         OffsetDateTime end = toEndOfDay(to);

//         String sql = """
//             SELECT
//               b.bill_number,
//               b.issued_at,
//               b.customer_name,
//               b.customer_phone,
//               b.customer_gstin,
//               b.supply_type,
//               b.taxable_amount,
//               b.cgst_amount,
//               b.sgst_amount,
//               b.igst_amount,
//               b.cess_amount,
//               b.total_tax,
//               b.grand_total,
//               b.gst_summary,
//               br.name          AS branch_name,
//               br.gstin         AS branch_gstin,
//               br.address_line1 AS branch_address,
//               br.state_code    AS branch_state_code
//             FROM bills b
//             LEFT JOIN branches br ON br.id = b.branch_id
//             WHERE b.tenant_id = ?
//             """ + getBranchClause(branchId, "b.") + """
//               AND b.status NOT IN ('void', 'refunded')
//               AND b.issued_at BETWEEN ? AND ?
//             ORDER BY b.issued_at ASC
//         """;

//         List<Map<String, Object>> bills = jdbcTemplate.queryForList(sql, tenantId, start, end);

//         List<Map<String, Object>> b2bList = new ArrayList<>();
//         List<Map<String, Object>> b2csList = new ArrayList<>();

//         for (Map<String, Object> bill : bills) {
//             String customerGstin = (String) bill.get("customer_gstin");
//             boolean isInterState = customerGstin != null && !customerGstin.isBlank();

//             OffsetDateTime issuedAt = (OffsetDateTime) bill.get("issued_at");
//             String invoiceDate = issuedAt != null
//                     ? issuedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
//                     : "";

//             String gstSummaryRaw = (String) bill.get("gst_summary");
//             List<Map<String, Object>> gstSummaries = List.of();
//             if (gstSummaryRaw != null) {
//                 try {
//                     gstSummaries = objectMapper.readValue(gstSummaryRaw, new TypeReference<>() {});
//                 } catch (Exception ignored) {}
//             }

//             List<Map<String, Object>> items = new ArrayList<>();
//             for (Map<String, Object> g : gstSummaries) {
//                 BigDecimal gstRate = new BigDecimal(g.getOrDefault("gstRate", 0).toString());
//                 BigDecimal taxableAmount = new BigDecimal(g.getOrDefault("taxableAmount", 0).toString());
//                 BigDecimal cgstAmount = new BigDecimal(g.getOrDefault("cgstAmount", 0).toString());
//                 BigDecimal sgstAmount = new BigDecimal(g.getOrDefault("sgstAmount", 0).toString());
//                 BigDecimal igstAmount = new BigDecimal(g.getOrDefault("igstAmount", 0).toString());

//                 items.add(Map.of(
//                         "num", 1,
//                         "itm_det", Map.of(
//                                 "rt", gstRate,
//                                 "txval", taxableAmount,
//                                 "camt", isInterState ? BigDecimal.ZERO : cgstAmount,
//                                 "samt", isInterState ? BigDecimal.ZERO : sgstAmount,
//                                 "iamt", isInterState ? igstAmount : BigDecimal.ZERO,
//                                 "csamt", 0
//                         )
//                 ));
//             }

//             Map<String, Object> invoice = Map.of(
//                     "inum", bill.get("bill_number"),
//                     "idt", invoiceDate,
//                     "val", bill.get("grand_total"),
//                     "pos", bill.getOrDefault("branch_state_code", "27"),
//                     "rchrg", "N",
//                     "inv_typ", isInterState ? "R" : "B2CL",
//                     "itms", items
//             );

//             if (isInterState) {
//                 Map<String, Object> existing = b2bList.stream()
//                         .filter(b -> customerGstin.equals(b.get("ctin")))
//                         .findFirst()
//                         .orElse(null);

//                 if (existing != null) {
//                     @SuppressWarnings("unchecked")
//                     List<Map<String, Object>> invs = (List<Map<String, Object>>) existing.get("inv");
//                     invs.add(invoice);
//                 } else {
//                     List<Map<String, Object>> invs = new ArrayList<>();
//                     invs.add(invoice);
//                     b2bList.add(new HashMap<>(Map.of("ctin", customerGstin, "inv", invs)));
//                 }
//             } else {
//                 b2csList.add(invoice);
//             }
//         }

//         String branchGstin = bills.isEmpty() ? "" : (String) bills.getFirst().getOrDefault("branch_gstin", "");
//         String returnPeriod = from.substring(0, 7).replace("-", "");

//         return Map.of(
//                 "gstin", branchGstin,
//                 "fp", returnPeriod,
//                 "version", "GST3.0.4",
//                 "hash", "hash",
//                 "b2b", b2bList,
//                 "b2cs", b2csList
//         );
//     }

//     // ── Shift Report ───────────────────────────────────────────────────────────

//     public List<Map<String, Object>> getShiftReport(UUID branchId, UUID tenantId, String from, String to) {
//         assertDate(from, "from");
//         assertDate(to, "to");
//         OffsetDateTime start = toStartOfDay(from);
//         OffsetDateTime end = toEndOfDay(to);

//         String sql = """
//             SELECT
//               s.id                                                       AS shift_id,
//               s.shift_number,
//               s.opened_at,
//               s.closed_at,
//               CONCAT(u.first_name, ' ', COALESCE(u.last_name, ''))      AS cashier_name,
//               s.opening_cash,
//               s.closing_cash,
//               s.expected_cash,
//               s.cash_difference,
//               COALESCE(s.total_sales,   0)                               AS total_sales,
//               COALESCE(s.total_orders,  0)                               AS total_orders,
//               COALESCE(s.cash_sales,    0)                               AS cash_sales,
//               COALESCE(s.card_sales,    0)                               AS card_sales,
//               COALESCE(s.upi_sales,     0)                               AS upi_sales,
//               COALESCE(s.wallet_sales,  0)                               AS wallet_sales,
//               COALESCE(s.credit_sales,  0)                               AS credit_sales,
//               COALESCE(s.complimentary, 0)                               AS complimentary,
//               COALESCE(s.total_cgst,    0)                               AS total_cgst,
//               COALESCE(s.total_sgst,    0)                               AS total_sgst,
//               COALESCE(s.total_igst,    0)                               AS total_igst,
//               s.status
//             FROM shifts s
//             LEFT JOIN users u ON u.id = s.opened_by
//             WHERE s.tenant_id = ?
//             """ + getBranchClause(branchId, "s.") + """
//               AND s.opened_at BETWEEN ? AND ?
//             ORDER BY s.opened_at DESC
//         """;

//         return jdbcTemplate.queryForList(sql, tenantId, start, end);
//     }

//     // ── Waiter Performance Report ──────────────────────────────────────────────

//     public List<Map<String, Object>> getWaiterReport(UUID branchId, UUID tenantId, String from, String to) {
//         assertDate(from, "from");
//         assertDate(to, "to");
//         OffsetDateTime start = toStartOfDay(from);
//         OffsetDateTime end = toEndOfDay(to);

//         String sql = """
//             SELECT
//               CONCAT(u.first_name, ' ', COALESCE(u.last_name, ''))      AS waiter_name,
//               u.id                                                       AS waiter_id,
//               u.employee_code,
//               COUNT(DISTINCT o.id)::int                                  AS total_orders,
//               COALESCE(SUM(o.grand_total), 0)                            AS total_revenue,
//               COALESCE(
//                 SUM(o.grand_total) / NULLIF(COUNT(DISTINCT o.id), 0), 0
//               )                                                          AS avg_order_value,
//               COUNT(DISTINCT o.id) FILTER (WHERE o.order_type = 'dine_in')::int
//                                                                          AS dine_in_orders,
//               COUNT(DISTINCT o.id) FILTER (WHERE o.order_type = 'takeaway')::int
//                                                                          AS takeaway_orders,
//               COUNT(DISTINCT o.table_id)                                 AS tables_served,
//               COALESCE(AVG(
//                 EXTRACT(EPOCH FROM (o.billed_at - o.placed_at)) / 60
//               ) FILTER (WHERE o.billed_at IS NOT NULL AND o.placed_at IS NOT NULL), 0)
//                                                                          AS avg_turnaround_min
//             FROM orders o
//             JOIN users u ON u.id = o.waiter_id
//             WHERE o.tenant_id  = ?
//             """ + getBranchClause(branchId, "o.") + """
//               AND o.status     = 'billed'
//               AND o.created_at BETWEEN ? AND ?
//             GROUP BY u.id, u.first_name, u.last_name, u.employee_code
//             ORDER BY total_revenue DESC
//         """;

//         return jdbcTemplate.queryForList(sql, tenantId, start, end);
//     }

//     // ── Live POS Dashboard Summary ─────────────────────────────────────────────

//     public Map<String, Object> getDashboardSummary(UUID branchId, UUID tenantId) {
//         String today = LocalDate.now().toString();
//         OffsetDateTime dayStart = toStartOfDay(today);
//         OffsetDateTime dayEnd = toEndOfDay(today);
//         OffsetDateTime weekStart = toStartOfDay(LocalDate.now().minusDays(7).toString());
//         OffsetDateTime weekEnd = toEndOfDay(today);

//         String sqlTodaySales = "SELECT COALESCE(SUM(grand_total), 0) AS today_sales, COUNT(*)::int AS today_bills FROM bills WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND (source = 'pos' OR source IS NULL) AND status NOT IN ('void', 'refunded') AND created_at BETWEEN ? AND ?";
//         Map<String, Object> todaySalesMap = jdbcTemplate.queryForMap(sqlTodaySales, tenantId, dayStart, dayEnd);

//         String sqlWeekSales = "SELECT COALESCE(SUM(grand_total), 0) AS week_sales FROM bills WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND (source = 'pos' OR source IS NULL) AND status NOT IN ('void', 'refunded') AND created_at BETWEEN ? AND ?";
//         Map<String, Object> weekSalesMap = jdbcTemplate.queryForMap(sqlWeekSales, tenantId, weekStart, weekEnd);

//         String sqlPending = "SELECT COUNT(*)::int AS pending FROM orders WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND status NOT IN ('billed', 'cancelled')";
//         Integer pending = jdbcTemplate.queryForObject(sqlPending, Integer.class, tenantId);

//         String sqlLowStock = "SELECT COUNT(*)::int AS low_stock FROM inventory_items WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND current_stock <= min_stock_level AND is_active = true";
//         Integer lowStock = jdbcTemplate.queryForObject(sqlLowStock, Integer.class, tenantId);

//         String sqlOrderStats = "SELECT COUNT(*) FILTER (WHERE status = 'billed' AND NOT is_complimentary)::int AS successful, COUNT(*) FILTER (WHERE status = 'cancelled')::int AS cancelled, COUNT(*) FILTER (WHERE is_complimentary = true)::int AS complimentary, COUNT(*) FILTER (WHERE is_sales_return  = true)::int AS returns FROM orders WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND created_at BETWEEN ? AND ?";
//         Map<String, Object> orderStats = jdbcTemplate.queryForMap(sqlOrderStats, tenantId, dayStart, dayEnd);

//         String sqlRevLeakage = "SELECT (SELECT COUNT(*)::int FROM order_items oi JOIN orders o ON o.id = oi.order_id WHERE o.tenant_id = ? " + getBranchClause(branchId, "o.") + " AND oi.is_voided = true AND oi.created_at BETWEEN ? AND ?) AS voided_items, (SELECT COUNT(*)::int FROM orders WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND status = 'cancelled' AND grand_total > 0 AND created_at BETWEEN ? AND ?) AS cancelled_with_value";
//         Map<String, Object> revLeakage = jdbcTemplate.queryForMap(sqlRevLeakage, tenantId, dayStart, dayEnd, tenantId, dayStart, dayEnd);

//         String sqlTurnaround = "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (billed_at - placed_at)) / 60), 0) AS avg_turnaround_minutes FROM orders WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND order_type = 'dine_in' AND placed_at IS NOT NULL AND billed_at IS NOT NULL AND billed_at BETWEEN ? AND ?";
//         Double turnaround = jdbcTemplate.queryForObject(sqlTurnaround, Double.class, tenantId, dayStart, dayEnd);

//         return Map.of(
//                 "todaySales", todaySalesMap.getOrDefault("today_sales", 0),
//                 "todayBills", todaySalesMap.getOrDefault("today_bills", 0),
//                 "weekSales", weekSalesMap.getOrDefault("week_sales", 0),
//                 "pendingOrders", pending != null ? pending : 0,
//                 "lowStockAlerts", lowStock != null ? lowStock : 0,
//                 "orderStats", Map.of(
//                         "successful", orderStats.getOrDefault("successful", 0),
//                         "cancelled", orderStats.getOrDefault("cancelled", 0),
//                         "complimentary", orderStats.getOrDefault("complimentary", 0),
//                         "returns", orderStats.getOrDefault("returns", 0)
//                 ),
//                 "revenueLeakage", Map.of(
//                         "voidedItems", revLeakage.getOrDefault("voided_items", 0),
//                         "cancelledWithValue", revLeakage.getOrDefault("cancelled_with_value", 0)
//                 ),
//                 "tableStats", Map.of(
//                         "avgTurnaroundMinutes", Math.round(turnaround != null ? turnaround : 0)
//                 )
//         );
//     }

//     // ── Live Hotel Dashboard Summary ───────────────────────────────────────────

//     public Map<String, Object> getHotelDashboardSummary(UUID branchId, UUID tenantId) {
//         String today = LocalDate.now().toString();
//         OffsetDateTime dayStart = toStartOfDay(today);
//         OffsetDateTime dayEnd = toEndOfDay(today);

//         String sqlTodaySales = "SELECT COALESCE(SUM(grand_total), 0) AS revenue, COUNT(*)::int AS count FROM bills WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND source = 'hotel' AND status NOT IN ('void', 'refunded') AND created_at BETWEEN ? AND ?";
//         Map<String, Object> todaySalesMap = jdbcTemplate.queryForMap(sqlTodaySales, tenantId, dayStart, dayEnd);

//         String sqlWeekSales = "SELECT COALESCE(SUM(grand_total), 0) AS revenue FROM bills WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND source = 'hotel' AND status NOT IN ('void', 'refunded') AND created_at >= NOW() - INTERVAL '7 days'";
//         BigDecimal weekSales = jdbcTemplate.queryForObject(sqlWeekSales, BigDecimal.class, tenantId);

//         String sqlCheckins = "SELECT COUNT(*)::int AS count FROM hotel_reservations WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND check_in_date = ? AND status NOT IN ('cancelled', 'no_show')";
//         Integer checkins = jdbcTemplate.queryForObject(sqlCheckins, Integer.class, tenantId, LocalDate.now());

//         String sqlCheckouts = "SELECT COUNT(*)::int AS count FROM hotel_reservations WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND check_out_date = ? AND status NOT IN ('cancelled', 'no_show')";
//         Integer checkouts = jdbcTemplate.queryForObject(sqlCheckouts, Integer.class, tenantId, LocalDate.now());

//         String sqlRooms = "SELECT COUNT(*)::int AS total_rooms, COUNT(*) FILTER (WHERE status = 'available')::int AS available_rooms, COUNT(*) FILTER (WHERE status = 'occupied')::int AS occupied_rooms, COUNT(*) FILTER (WHERE status = 'cleaning')::int AS cleaning_rooms, COUNT(*) FILTER (WHERE status = 'reserved')::int AS reserved_rooms, COUNT(*) FILTER (WHERE status IN ('maintenance', 'out_of_order'))::int AS maintenance_rooms FROM hotel_rooms WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND (is_active = true OR is_active IS NULL)";
//         Map<String, Object> roomsData = jdbcTemplate.queryForMap(sqlRooms, tenantId);

//         String sqlWeeklyChart = "SELECT TO_CHAR(date_trunc('day', created_at AT TIME ZONE 'Asia/Kolkata'), 'Mon DD') AS date, COALESCE(SUM(grand_total), 0) AS revenue FROM bills WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND source = 'hotel' AND status NOT IN ('void', 'refunded') AND created_at >= NOW() - INTERVAL '7 days' GROUP BY date_trunc('day', created_at AT TIME ZONE 'Asia/Kolkata') ORDER BY date_trunc('day', created_at AT TIME ZONE 'Asia/Kolkata') ASC";
//         List<Map<String, Object>> weeklyChart = jdbcTemplate.queryForList(sqlWeeklyChart, tenantId);

//         long totalRooms = ((Number) roomsData.getOrDefault("total_rooms", 0)).longValue();
//         long occupiedRooms = ((Number) roomsData.getOrDefault("occupied_rooms", 0)).longValue();
//         long occupancyRate = totalRooms > 0 ? Math.round(((double) occupiedRooms / totalRooms) * 100) : 0;

//         BigDecimal todayRev = (BigDecimal) todaySalesMap.getOrDefault("revenue", BigDecimal.ZERO);
//         long adr = occupiedRooms > 0 ? todayRev.divide(BigDecimal.valueOf(occupiedRooms), RoundingMode.HALF_UP).longValue() : 0;

//         return Map.of(
//                 "todaySales", todayRev,
//                 "todayBills", todaySalesMap.getOrDefault("count", 0),
//                 "weekSales", weekSales != null ? weekSales : BigDecimal.ZERO,
//                 "todayCheckins", checkins != null ? checkins : 0,
//                 "todayCheckouts", checkouts != null ? checkouts : 0,
//                 "occupancyRate", occupancyRate,
//                 "adr", adr,
//                 "roomStats", Map.of(
//                         "total", totalRooms,
//                         "available", roomsData.getOrDefault("available_rooms", 0),
//                         "occupied", occupiedRooms,
//                         "cleaning", roomsData.getOrDefault("cleaning_rooms", 0),
//                         "reserved", roomsData.getOrDefault("reserved_rooms", 0),
//                         "maintenance", roomsData.getOrDefault("maintenance_rooms", 0)
//                 ),
//                 "weeklyChart", weeklyChart
//         );
//     }

//     // ── Executive Owner Dashboard Summary ──────────────────────────────────────

//     public Map<String, Object> getOwnerDashboardSummary(UUID branchId, UUID tenantId) {
//         String today = LocalDate.now().toString();
//         OffsetDateTime dayStart = toStartOfDay(today);
//         OffsetDateTime dayEnd = toEndOfDay(today);

//         String sqlTotalToday = "SELECT COALESCE(SUM(grand_total), 0) AS revenue, COUNT(*) AS bills FROM bills WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND status NOT IN ('void', 'refunded') AND created_at BETWEEN ? AND ?";
//         Map<String, Object> totalToday = jdbcTemplate.queryForMap(sqlTotalToday, tenantId, dayStart, dayEnd);

//         String sqlTotalWeek = "SELECT COALESCE(SUM(grand_total), 0) AS revenue FROM bills WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND status NOT IN ('void', 'refunded') AND created_at >= NOW() - INTERVAL '7 days'";
//         BigDecimal totalWeek = jdbcTemplate.queryForObject(sqlTotalWeek, BigDecimal.class, tenantId);

//         String sqlPosToday = "SELECT COALESCE(SUM(grand_total), 0) AS revenue, COUNT(*) AS bills FROM bills WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND (source = 'pos' OR source IS NULL) AND status NOT IN ('void', 'refunded') AND created_at BETWEEN ? AND ?";
//         Map<String, Object> posToday = jdbcTemplate.queryForMap(sqlPosToday, tenantId, dayStart, dayEnd);

//         String sqlHotelToday = "SELECT COALESCE(SUM(grand_total), 0) AS revenue, COUNT(*) AS bills FROM bills WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND source = 'hotel' AND status NOT IN ('void', 'refunded') AND created_at BETWEEN ? AND ?";
//         Map<String, Object> hotelToday = jdbcTemplate.queryForMap(sqlHotelToday, tenantId, dayStart, dayEnd);

//         String sqlPending = "SELECT COUNT(*) AS count FROM orders WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND status NOT IN ('billed', 'cancelled')";
//         Long pending = jdbcTemplate.queryForObject(sqlPending, Long.class, tenantId);

//         String sqlRoomsCount = "SELECT COUNT(*) FROM hotel_rooms WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND status != 'out_of_order'";
//         Long totalRooms = jdbcTemplate.queryForObject(sqlRoomsCount, Long.class, tenantId);

//         String sqlOccupiedCount = "SELECT COUNT(*) FROM hotel_reservations WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND status = 'checked_in'";
//         Long occupiedRooms = jdbcTemplate.queryForObject(sqlOccupiedCount, Long.class, tenantId);

//         String sqlCheckins = "SELECT COUNT(*) AS count FROM hotel_reservations WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND check_in_date = ? AND status NOT IN ('cancelled', 'no_show')";
//         Long checkins = jdbcTemplate.queryForObject(sqlCheckins, Long.class, tenantId, LocalDate.now());

//         String sqlCheckouts = "SELECT COUNT(*) AS count FROM hotel_reservations WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND check_out_date = ? AND status NOT IN ('cancelled', 'no_show')";
//         Long checkouts = jdbcTemplate.queryForObject(sqlCheckouts, Long.class, tenantId, LocalDate.now());

//         String sqlWeeklyChart = "SELECT TO_CHAR(date_trunc('day', created_at AT TIME ZONE 'Asia/Kolkata'), 'Mon DD') AS date, COALESCE(SUM(grand_total) FILTER (WHERE source='pos' OR source IS NULL), 0) AS pos, COALESCE(SUM(grand_total) FILTER (WHERE source='hotel'), 0) AS hotel, COALESCE(SUM(grand_total), 0) AS total FROM bills WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND status NOT IN ('void', 'refunded') AND created_at >= NOW() - INTERVAL '7 days' GROUP BY date_trunc('day', created_at AT TIME ZONE 'Asia/Kolkata') ORDER BY date_trunc('day', created_at AT TIME ZONE 'Asia/Kolkata') ASC";
//         List<Map<String, Object>> weeklyChart = jdbcTemplate.queryForList(sqlWeeklyChart, tenantId);

//         String sqlPayments = "SELECT method, COALESCE(SUM(amount),0) AS total FROM payments WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND status='success' AND created_at BETWEEN ? AND ? GROUP BY method ORDER BY total DESC";
//         List<Map<String, Object>> paymentBreakdown = jdbcTemplate.queryForList(sqlPayments, tenantId, dayStart, dayEnd);

//         String sqlLowStock = "SELECT COUNT(*) AS count FROM inventory_items WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND current_stock <= min_stock_level AND is_active = true";
//         Long lowStock = jdbcTemplate.queryForObject(sqlLowStock, Long.class, tenantId);

//         List<Map<String, Object>> branchComparison = List.of();
//         if (branchId == null) {
//             String sqlCompare = """
//                 SELECT b.name AS branch_name, COALESCE(SUM(bills.grand_total), 0) AS revenue
//                 FROM branches b
//                 LEFT JOIN bills ON bills.branch_id = b.id 
//                   AND bills.status NOT IN ('void','refunded') AND bills.created_at >= NOW() - INTERVAL '7 days'
//                 WHERE b.tenant_id = ?
//                 GROUP BY b.id, b.name ORDER BY revenue DESC
//             """;
//             branchComparison = jdbcTemplate.queryForList(sqlCompare, tenantId);
//         }

//         long tRooms = totalRooms != null ? totalRooms : 0;
//         long oRooms = occupiedRooms != null ? occupiedRooms : 0;
//         long occupancyRate = tRooms > 0 ? Math.round(((double) oRooms / tRooms) * 100) : 0;

//         Map<String, Object> summary = new LinkedHashMap<>();
//         summary.put("totalRevenueToday", totalToday.getOrDefault("revenue", 0));
//         summary.put("totalBillsToday", totalToday.getOrDefault("bills", 0));
//         summary.put("totalRevenueWeek", totalWeek != null ? totalWeek : BigDecimal.ZERO);
//         summary.put("posRevenueToday", posToday.getOrDefault("revenue", 0));
//         summary.put("posBillsToday", posToday.getOrDefault("bills", 0));
//         summary.put("hotelRevenueToday", hotelToday.getOrDefault("revenue", 0));
//         summary.put("hotelBillsToday", hotelToday.getOrDefault("bills", 0));
//         summary.put("pendingOrders", pending != null ? pending : 0);
//         summary.put("occupancyRate", occupancyRate);
//         summary.put("todayCheckins", checkins != null ? checkins : 0);
//         summary.put("todayCheckouts", checkouts != null ? checkouts : 0);
//         summary.put("lowStockAlerts", lowStock != null ? lowStock : 0);
//         summary.put("weeklyChart", weeklyChart);
//         summary.put("paymentBreakdown", paymentBreakdown);
//         summary.put("branchComparison", branchComparison);

//         return summary;
//     }

//     // ── Cross-Branch Performance (Owner Only) ──────────────────────────────────

//     public Map<String, Object> getBranchPerformance(UUID tenantId, String from, String to) {
//         assertDate(from, "from");
//         assertDate(to, "to");
//         OffsetDateTime start = toStartOfDay(from);
//         OffsetDateTime end = toEndOfDay(to);

//         LocalDate fromDate = LocalDate.parse(from);
//         LocalDate toDate = LocalDate.parse(to);
//         long diffDays = Math.max(1, ChronoUnit.DAYS.between(fromDate, toDate));
//         String prevEnd = fromDate.minusDays(1).toString();
//         String prevStart = fromDate.minusDays(diffDays).toString();
//         OffsetDateTime pStart = toStartOfDay(prevStart);
//         OffsetDateTime pEnd = toEndOfDay(prevEnd);

//         String sqlBranches = "SELECT id, name, code, type, city, is_hq FROM branches WHERE tenant_id = ? AND is_active = true ORDER BY is_hq DESC, name ASC";
//         List<Map<String, Object>> branches = jdbcTemplate.queryForList(sqlBranches, tenantId);

//         String sqlCurrent = """
//             SELECT
//                b.id AS branch_id,
//                COALESCE(SUM(bi.grand_total),0)::numeric AS revenue,
//                COUNT(DISTINCT bi.id)::int                AS bills,
//                COALESCE(SUM(bi.grand_total) FILTER (WHERE bi.source='pos' OR bi.source IS NULL),0) AS pos_revenue,
//                COALESCE(SUM(bi.grand_total) FILTER (WHERE bi.source='hotel'),0)                    AS hotel_revenue,
//                COUNT(DISTINCT o.id)::int                 AS orders
//             FROM branches b
//             LEFT JOIN bills  bi ON bi.branch_id = b.id AND bi.tenant_id = ? AND bi.status NOT IN ('void','refunded') AND bi.created_at BETWEEN ? AND ?
//             LEFT JOIN orders o ON o.branch_id = b.id AND o.tenant_id = ? AND o.status = 'billed' AND o.created_at BETWEEN ? AND ?
//             WHERE b.tenant_id = ? AND b.is_active = true
//             GROUP BY b.id
//         """;
//         List<Map<String, Object>> current = jdbcTemplate.queryForList(sqlCurrent, tenantId, start, end, tenantId, start, end, tenantId);

//         String sqlPrevious = """
//             SELECT
//                b.id AS branch_id,
//                COALESCE(SUM(bi.grand_total),0)::numeric AS revenue
//             FROM branches b
//             LEFT JOIN bills bi ON bi.branch_id = b.id AND bi.tenant_id = ? AND bi.status NOT IN ('void','refunded') AND bi.created_at BETWEEN ? AND ?
//             WHERE b.tenant_id = ? AND b.is_active = true
//             GROUP BY b.id
//         """;
//         List<Map<String, Object>> previous = jdbcTemplate.queryForList(sqlPrevious, tenantId, pStart, pEnd, tenantId);

//         Map<String, Map<String, Object>> currMap = new HashMap<>();
//         for (Map<String, Object> r : current) currMap.put(r.get("branch_id").toString(), r);

//         Map<String, Map<String, Object>> prevMap = new HashMap<>();
//         for (Map<String, Object> r : previous) prevMap.put(r.get("branch_id").toString(), r);

//         List<Map<String, Object>> branchList = new ArrayList<>();
//         for (Map<String, Object> b : branches) {
//             String idStr = b.get("id").toString();
//             Map<String, Object> c = currMap.getOrDefault(idStr, Map.of());
//             Map<String, Object> p = prevMap.getOrDefault(idStr, Map.of());

//             BigDecimal rev = new BigDecimal(c.getOrDefault("revenue", BigDecimal.ZERO).toString());
//             BigDecimal prevRev = new BigDecimal(p.getOrDefault("revenue", BigDecimal.ZERO).toString());
//             Integer growthPct = null;
//             if (prevRev.compareTo(BigDecimal.ZERO) > 0) {
//                 growthPct = rev.subtract(prevRev).multiply(BigDecimal.valueOf(100)).divide(prevRev, 0, RoundingMode.HALF_UP).intValue();
//             }

//             Map<String, Object> map = new HashMap<>();
//             map.put("branchId", b.get("id"));
//             map.put("branchName", b.get("name"));
//             map.put("branchCode", b.get("code"));
//             map.put("type", b.get("type"));
//             map.put("city", b.get("city"));
//             map.put("isHq", b.get("is_hq"));
//             map.put("revenue", rev);
//             map.put("posRevenue", c.getOrDefault("pos_revenue", 0));
//             map.put("hotelRevenue", c.getOrDefault("hotel_revenue", 0));
//             map.put("bills", c.getOrDefault("bills", 0));
//             map.put("orders", c.getOrDefault("orders", 0));
//             map.put("growthPct", growthPct);

//             branchList.add(map);
//         }

//         branchList.sort((a, b) -> new BigDecimal(b.get("revenue").toString()).compareTo(new BigDecimal(a.get("revenue").toString())));

//         BigDecimal totalRevenue = branchList.stream().map(b -> (BigDecimal) b.get("revenue")).reduce(BigDecimal.ZERO, BigDecimal::add);
//         long totalOrders = branchList.stream().mapToLong(b -> ((Number) b.get("orders")).longValue()).sum();
//         long totalBills = branchList.stream().mapToLong(b -> ((Number) b.get("bills")).longValue()).sum();
//         long avgRevenue = branchList.isEmpty() ? 0 : totalRevenue.divide(BigDecimal.valueOf(branchList.size()), 0, RoundingMode.HALF_UP).longValue();

//         return Map.of(
//                 "totalBranches", branchList.size(),
//                 "totalRevenue", totalRevenue,
//                 "totalOrders", totalOrders,
//                 "totalBills", totalBills,
//                 "avgRevenue", avgRevenue,
//                 "topBranch", branchList.isEmpty() ? Map.of() : branchList.getFirst(),
//                 "branches", branchList,
//                 "period", Map.of("from", from, "to", to, "prevFrom", prevStart, "prevTo", prevEnd)
//         );
//     }

//     // ── Single Branch Combined Summary ──────────────────────────────────────────

//     public Map<String, Object> getBranchSummary(UUID branchId, UUID tenantId, String from, String to) {
//         String today = LocalDate.now().toString();
//         String rangeFrom = from != null ? from : today;
//         String rangeTo = to != null ? to : today;
//         String monthStart = today.substring(0, 7) + "-01";

//         OffsetDateTime rangeStart = toStartOfDay(rangeFrom);
//         OffsetDateTime rangeEnd = toEndOfDay(rangeTo);
//         OffsetDateTime monthS = toStartOfDay(monthStart);
//         OffsetDateTime monthE = toEndOfDay(today);
//         OffsetDateTime weekS = toStartOfDay(LocalDate.now().minusDays(7).toString());
//         OffsetDateTime weekE = toEndOfDay(today);

//         String sqlRevenueRange = "SELECT COALESCE(SUM(grand_total),0) AS revenue, COUNT(*)::int AS bills FROM bills WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND status NOT IN ('void','refunded') AND created_at BETWEEN ? AND ?";
//         Map<String, Object> revenueToday = jdbcTemplate.queryForMap(sqlRevenueRange, tenantId, rangeStart, rangeEnd);

//         String sqlRevenueMonth = "SELECT COALESCE(SUM(grand_total),0) AS revenue FROM bills WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND status NOT IN ('void','refunded') AND created_at BETWEEN ? AND ?";
//         BigDecimal revenueMonth = jdbcTemplate.queryForObject(sqlRevenueMonth, BigDecimal.class, tenantId, monthS, monthE);

//         String sqlRevenueWeek = "SELECT COALESCE(SUM(grand_total),0) AS revenue FROM bills WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND status NOT IN ('void','refunded') AND created_at BETWEEN ? AND ?";
//         BigDecimal revenueWeek = jdbcTemplate.queryForObject(sqlRevenueWeek, BigDecimal.class, tenantId, weekS, weekE);

//         String sqlRestaurantToday = "SELECT COALESCE(SUM(grand_total),0) AS revenue, COUNT(*)::int AS bills FROM bills WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND (source='pos' OR source IS NULL) AND status NOT IN ('void','refunded') AND created_at BETWEEN ? AND ?";
//         Map<String, Object> restaurantToday = jdbcTemplate.queryForMap(sqlRestaurantToday, tenantId, rangeStart, rangeEnd);

//         String sqlHotelToday = "SELECT COALESCE(SUM(grand_total),0) AS revenue, COUNT(*)::int AS bills FROM bills WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND source='hotel' AND status NOT IN ('void','refunded') AND created_at BETWEEN ? AND ?";
//         Map<String, Object> hotelToday = jdbcTemplate.queryForMap(sqlHotelToday, tenantId, rangeStart, rangeEnd);

//         String sqlOrdersToday = "SELECT COUNT(*)::int AS total_orders, COUNT(*) FILTER (WHERE status='billed')::int AS billed_orders, COUNT(*) FILTER (WHERE status NOT IN ('billed','cancelled'))::int AS pending_orders, COALESCE(AVG(grand_total) FILTER (WHERE status='billed'), 0) AS avg_order_value FROM orders WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND created_at BETWEEN ? AND ?";
//         Map<String, Object> ordersToday = jdbcTemplate.queryForMap(sqlOrdersToday, tenantId, rangeStart, rangeEnd);

//         String sqlBillsToday = "SELECT COUNT(*)::int AS bills FROM bills WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND created_at BETWEEN ? AND ?";
//         Integer billsToday = jdbcTemplate.queryForObject(sqlBillsToday, Integer.class, tenantId, rangeStart, rangeEnd);

//         String sqlOpenShifts = "SELECT COUNT(*)::int AS count FROM shifts WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND status='open'";
//         Integer openShifts = jdbcTemplate.queryForObject(sqlOpenShifts, Integer.class, tenantId);

//         String sqlLowStock = "SELECT COUNT(*)::int AS count FROM inventory_items WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND current_stock <= min_stock_level AND is_active = true";
//         Integer lowStock = jdbcTemplate.queryForObject(sqlLowStock, Integer.class, tenantId);

//         String sqlHotelReservations = "SELECT COUNT(*) FILTER (WHERE check_in_date = ? AND status NOT IN ('cancelled','no_show'))::int AS reservations_today, COUNT(*) FILTER (WHERE check_in_date = ? AND status NOT IN ('cancelled','no_show'))::int AS checkins_today, COUNT(*) FILTER (WHERE check_out_date = ? AND status NOT IN ('cancelled','no_show'))::int AS checkouts_today, COUNT(*) FILTER (WHERE status='checked_in')::int AS in_house, (SELECT COUNT(*)::int FROM hotel_rooms WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND is_active=true) AS total_rooms, (SELECT COUNT(*) FILTER (WHERE status='available')::int FROM hotel_rooms WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND is_active=true) AS available_rooms FROM hotel_reservations WHERE tenant_id = ? " + getBranchClause(branchId, "");
//         Map<String, Object> hotel = jdbcTemplate.queryForMap(sqlHotelReservations, LocalDate.now(), LocalDate.now(), LocalDate.now(), tenantId, tenantId, tenantId);

//         String sqlHkPending = "SELECT COUNT(*)::int AS pending FROM hotel_housekeeping_tasks WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND status = 'pending' AND scheduled_for = ?";
//         Integer housekeepingPending = jdbcTemplate.queryForObject(sqlHkPending, Integer.class, tenantId, LocalDate.now());

//         String sqlStaff = "SELECT role, COUNT(*)::int AS count FROM users WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND is_active=true GROUP BY role";
//         List<Map<String, Object>> staff = jdbcTemplate.queryForList(sqlStaff, tenantId);

//         String sqlPayments = "SELECT method, COALESCE(SUM(amount),0) AS total, COUNT(*)::int AS txns FROM payments WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND status='success' AND created_at BETWEEN ? AND ? GROUP BY method ORDER BY total DESC";
//         List<Map<String, Object>> paymentBreakdown = jdbcTemplate.queryForList(sqlPayments, tenantId, rangeStart, rangeEnd);

//         String sqlWeeklyChart = "SELECT TO_CHAR(date_trunc('day', created_at AT TIME ZONE 'Asia/Kolkata'), 'Mon DD') AS date, COALESCE(SUM(grand_total) FILTER (WHERE source='pos' OR source IS NULL), 0) AS pos, COALESCE(SUM(grand_total) FILTER (WHERE source='hotel'), 0) AS hotel, COALESCE(SUM(grand_total), 0) AS total FROM bills WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND status NOT IN ('void','refunded') AND created_at BETWEEN ? AND ? GROUP BY date_trunc('day', created_at AT TIME ZONE 'Asia/Kolkata') ORDER BY date_trunc('day', created_at AT TIME ZONE 'Asia/Kolkata') ASC";
//         List<Map<String, Object>> weeklyChart = jdbcTemplate.queryForList(sqlWeeklyChart, tenantId, rangeStart, rangeEnd);

//         Map<String, Integer> staffMap = new HashMap<>();
//         int totalStaff = 0;
//         for (Map<String, Object> r : staff) {
//             String roleName = r.get("role").toString();
//             int countVal = ((Number) r.get("count")).intValue();
//             staffMap.put(roleName, countVal);
//             totalStaff += countVal;
//         }

//         long totalRooms = ((Number) hotel.getOrDefault("total_rooms", 0)).longValue();
//         long inHouse = ((Number) hotel.getOrDefault("in_house", 0)).longValue();
//         long occupancyPct = totalRooms > 0 ? Math.round(((double) inHouse / totalRooms) * 100) : 0;

//         return Map.of(
//                 "branch", Map.of("id", branchId != null ? branchId : ""),
//                 "period", Map.of("from", rangeFrom, "to", rangeTo),
//                 "revenue", Map.of(
//                         "total", revenueToday.getOrDefault("revenue", 0),
//                         "week", revenueWeek != null ? revenueWeek : BigDecimal.ZERO,
//                         "month", revenueMonth != null ? revenueMonth : BigDecimal.ZERO,
//                         "restaurant", restaurantToday.getOrDefault("revenue", 0),
//                         "hotel", hotelToday.getOrDefault("revenue", 0),
//                         "totalBills", revenueToday.getOrDefault("bills", 0)
//                 ),
//                 "restaurant", Map.of(
//                         "ordersToday", ordersToday.getOrDefault("total_orders", 0),
//                         "billedOrders", ordersToday.getOrDefault("billed_orders", 0),
//                         "pendingOrders", ordersToday.getOrDefault("pending_orders", 0),
//                         "billsToday", billsToday != null ? billsToday : 0,
//                         "avgOrderValue", Math.round(((Number) ordersToday.getOrDefault("avg_order_value", 0)).doubleValue()),
//                         "openShifts", openShifts != null ? openShifts : 0,
//                         "lowStockItems", lowStock != null ? lowStock : 0
//                 ),
//                 "hotel", Map.of(
//                         "reservationsToday", hotel.getOrDefault("reservations_today", 0),
//                         "checkinsToday", hotel.getOrDefault("checkins_today", 0),
//                         "checkoutsToday", hotel.getOrDefault("checkouts_today", 0),
//                         "inHouse", inHouse,
//                         "totalRooms", totalRooms,
//                         "availableRooms", hotel.getOrDefault("available_rooms", 0),
//                         "occupancyPct", occupancyPct,
//                         "housekeepingPending", housekeepingPending != null ? housekeepingPending : 0
//                 ),
//                 "staff", Map.of(
//                         "total", totalStaff,
//                         "managers", staffMap.getOrDefault("manager", 0) + staffMap.getOrDefault("restaurant_manager", 0) + staffMap.getOrDefault("hotel_manager", 0),
//                         "cashiers", staffMap.getOrDefault("cashier", 0),
//                         "waiters", staffMap.getOrDefault("waiter", 0),
//                         "kitchen", staffMap.getOrDefault("kitchen", 0),
//                         "housekeeping", staffMap.getOrDefault("housekeeping", 0),
//                         "receptionist", staffMap.getOrDefault("receptionist", 0),
//                         "byRole", staffMap
//                 ),
//                 "paymentBreakdown", paymentBreakdown,
//                 "weeklyChart", weeklyChart,
//                 "alerts", Map.of(
//                         "lowStock", lowStock != null ? lowStock : 0,
//                         "openShifts", openShifts != null ? openShifts : 0,
//                         "housekeepingPending", housekeepingPending != null ? housekeepingPending : 0
//                 )
//         );
//     }
// }

package project.EnterpriseSaas.demo.modules.reports.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportsService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private void assertDate(String value, String name) {
        try {
            LocalDate.parse(value);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("Invalid date for '%s': \"%s\". Use YYYY-MM-DD format.", name, value)
            );
        }
    }

    private OffsetDateTime toStartOfDay(String dateStr) {
        return OffsetDateTime.parse(dateStr + "T00:00:00.000+05:30");
    }

    private OffsetDateTime toEndOfDay(String dateStr) {
        return OffsetDateTime.parse(dateStr + "T23:59:59.999+05:30");
    }

    private String getBranchClause(UUID branchId, String columnPrefix) {
        if (branchId == null) return "";
        return " AND " + columnPrefix + "branch_id = '" + branchId.toString() + "' ";
    }

    // ── Daily Sales ────────────────────────────────────────────────────────────

    public List<Map<String, Object>> getDailySales(UUID branchId, UUID tenantId, String from, String to) {
        assertDate(from, "from");
        assertDate(to, "to");
        OffsetDateTime start = toStartOfDay(from);
        OffsetDateTime end = toEndOfDay(to);

        String sql = """
            SELECT
              date_trunc('day', created_at AT TIME ZONE 'Asia/Kolkata')
                AT TIME ZONE 'Asia/Kolkata'                              AS date,
              COUNT(*)::int                     AS total_bills,
              COALESCE(SUM(grand_total),     0) AS gross_sales,
              COALESCE(SUM(discount_amount), 0) AS total_discount,
              COALESCE(SUM(total_tax),       0) AS total_tax,
              COALESCE(SUM(cgst_amount),     0) AS cgst,
              COALESCE(SUM(sgst_amount),     0) AS sgst,
              COALESCE(SUM(igst_amount),     0) AS igst
            FROM bills
            WHERE tenant_id = ?
            """ + getBranchClause(branchId, "") + """
              AND status NOT IN ('void', 'refunded')
              AND created_at BETWEEN ? AND ?
            GROUP BY date_trunc('day', created_at AT TIME ZONE 'Asia/Kolkata')
            ORDER BY date ASC
        """;

        return jdbcTemplate.queryForList(sql, tenantId, start, end);
    }

    // ── Item Sales ─────────────────────────────────────────────────────────────

    public List<Map<String, Object>> getItemSalesReport(UUID branchId, UUID tenantId, String from, String to) {
        assertDate(from, "from");
        assertDate(to, "to");
        OffsetDateTime start = toStartOfDay(from);
        OffsetDateTime end = toEndOfDay(to);

        String sql = """
            SELECT
              oi.name                                         AS item_name,
              oi.menu_item_id,
              SUM(oi.quantity)::numeric                 AS total_qty,
              COALESCE(SUM(oi.line_total), 0)::numeric  AS total_revenue,
              SUM(
                oi.taxable_amount::numeric
                * (1.0 - COALESCE(
                    o.discount_amount::numeric / NULLIF(o.subtotal::numeric, 0), 0
                  ))
              )                                         AS taxable,
              COUNT(DISTINCT oi.order_id)::int          AS order_count
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            WHERE o.tenant_id  = ?
            """ + getBranchClause(branchId, "o.") + """
              AND oi.is_voided = false
              AND o.status     = 'billed'
              AND o.created_at BETWEEN ? AND ?
            GROUP BY oi.name, oi.menu_item_id
            ORDER BY total_revenue DESC
            LIMIT 100
        """;

        return jdbcTemplate.queryForList(sql, tenantId, start, end);
    }

    // ── Payment Methods ────────────────────────────────────────────────────────

    public List<Map<String, Object>> getPaymentMethodReport(UUID branchId, UUID tenantId, String from, String to) {
        assertDate(from, "from");
        assertDate(to, "to");
        OffsetDateTime start = toStartOfDay(from);
        OffsetDateTime end = toEndOfDay(to);

        String sql = """
            SELECT
              method,
              COUNT(*)::int            AS transaction_count,
              COALESCE(SUM(amount), 0) AS total_amount
            FROM payments
            WHERE tenant_id  = ?
            """ + getBranchClause(branchId, "") + """
              AND status     = 'success'
              AND created_at BETWEEN ? AND ?
            GROUP BY method
            ORDER BY total_amount DESC
        """;

        return jdbcTemplate.queryForList(sql, tenantId, start, end);
    }

    // ── Hourly Report ──────────────────────────────────────────────────────────

    public List<Map<String, Object>> getHourlyReport(UUID branchId, UUID tenantId, String date) {
        assertDate(date, "date");
        OffsetDateTime start = toStartOfDay(date);
        OffsetDateTime end = toEndOfDay(date);

        String sql = """
            SELECT
              EXTRACT(HOUR FROM created_at AT TIME ZONE 'Asia/Kolkata')::int AS hour,
              COUNT(*)::int                 AS total_bills,
              COALESCE(SUM(grand_total), 0) AS revenue
            FROM bills
            WHERE tenant_id = ?
            """ + getBranchClause(branchId, "") + """
              AND status NOT IN ('void', 'refunded')
              AND created_at BETWEEN ? AND ?
            GROUP BY EXTRACT(HOUR FROM created_at AT TIME ZONE 'Asia/Kolkata')
            ORDER BY hour
        """;

        return jdbcTemplate.queryForList(sql, tenantId, start, end);
    }

    // ── Category Report ────────────────────────────────────────────────────────

    public List<Map<String, Object>> getCategoryReport(UUID branchId, UUID tenantId, String from, String to) {
        assertDate(from, "from");
        assertDate(to, "to");
        OffsetDateTime start = toStartOfDay(from);
        OffsetDateTime end = toEndOfDay(to);

        String sql = """
            SELECT
              COALESCE(c.name, 'Uncategorised') AS category_name,
              SUM(oi.quantity)::numeric          AS total_qty,
              COALESCE(SUM(oi.line_total), 0)   AS total_revenue
            FROM order_items   oi
            JOIN   orders      o  ON o.id  = oi.order_id
            LEFT JOIN menu_items mi ON mi.id = oi.menu_item_id
            LEFT JOIN categories  c  ON c.id  = mi.category_id
            WHERE o.tenant_id  = ?
            """ + getBranchClause(branchId, "o.") + """
              AND oi.is_voided = false
              AND o.status     = 'billed'
              AND o.created_at BETWEEN ? AND ?
            GROUP BY COALESCE(c.name, 'Uncategorised')
            ORDER BY total_revenue DESC
        """;

        return jdbcTemplate.queryForList(sql, tenantId, start, end);
    }

    // ── GST Report ─────────────────────────────────────────────────────────────

    public List<Map<String, Object>> getGstReport(UUID branchId, UUID tenantId, String from, String to) {
        assertDate(from, "from");
        assertDate(to, "to");
        OffsetDateTime start = toStartOfDay(from);
        OffsetDateTime end = toEndOfDay(to);

        String sql = """
            SELECT
              DATE_TRUNC('month', issued_at AT TIME ZONE 'Asia/Kolkata')
                AT TIME ZONE 'Asia/Kolkata'                              AS month,
              COALESCE(SUM(taxable_amount), 0) AS taxable_value,
              COALESCE(SUM(cgst_amount),    0) AS cgst,
              COALESCE(SUM(sgst_amount),    0) AS sgst,
              COALESCE(SUM(igst_amount),    0) AS igst,
              COALESCE(SUM(cess_amount),    0) AS cess,
              COALESCE(SUM(total_tax),      0) AS total_tax,
              COALESCE(SUM(grand_total),    0) AS gross_value,
              COUNT(*)::int                    AS total_invoices
            FROM bills
            WHERE tenant_id = ?
            """ + getBranchClause(branchId, "") + """
              AND status NOT IN ('void', 'refunded')
              AND issued_at BETWEEN ? AND ?
            GROUP BY DATE_TRUNC('month', issued_at AT TIME ZONE 'Asia/Kolkata')
            ORDER BY month
        """;

        return jdbcTemplate.queryForList(sql, tenantId, start, end);
    }

    // ── GSTR-1 JSON Export ─────────────────────────────────────────────────────

    public Map<String, Object> getGstr1Export(UUID branchId, UUID tenantId, String from, String to) {
        assertDate(from, "from");
        assertDate(to, "to");
        OffsetDateTime start = toStartOfDay(from);
        OffsetDateTime end = toEndOfDay(to);

        String sql = """
            SELECT
              b.bill_number,
              b.issued_at,
              b.customer_name,
              b.customer_phone,
              b.customer_gstin,
              b.supply_type,
              b.taxable_amount,
              b.cgst_amount,
              b.sgst_amount,
              b.igst_amount,
              b.cess_amount,
              b.total_tax,
              b.grand_total,
              b.gst_summary,
              br.name          AS branch_name,
              br.gstin         AS branch_gstin,
              br.address_line1 AS branch_address,
              br.state_code    AS branch_state_code
            FROM bills b
            LEFT JOIN branches br ON br.id = b.branch_id
            WHERE b.tenant_id = ?
            """ + getBranchClause(branchId, "b.") + """
              AND b.status NOT IN ('void', 'refunded')
              AND b.issued_at BETWEEN ? AND ?
            ORDER BY b.issued_at ASC
        """;

        List<Map<String, Object>> bills = jdbcTemplate.queryForList(sql, tenantId, start, end);

        List<Map<String, Object>> b2bList = new ArrayList<>();
        List<Map<String, Object>> b2csList = new ArrayList<>();

        for (Map<String, Object> bill : bills) {
            String customerGstin = (String) bill.get("customer_gstin");
            boolean isInterState = customerGstin != null && !customerGstin.isBlank();

            OffsetDateTime issuedAt = (OffsetDateTime) bill.get("issued_at");
            String invoiceDate = issuedAt != null
                    ? issuedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : "";

            String gstSummaryRaw = (String) bill.get("gst_summary");
            List<Map<String, Object>> gstSummaries = List.of();
            if (gstSummaryRaw != null) {
                try {
                    gstSummaries = objectMapper.readValue(gstSummaryRaw, new TypeReference<>() {});
                } catch (Exception ignored) {}
            }

            List<Map<String, Object>> items = new ArrayList<>();
            for (Map<String, Object> g : gstSummaries) {
                BigDecimal gstRate = new BigDecimal(g.getOrDefault("gstRate", 0).toString());
                BigDecimal taxableAmount = new BigDecimal(g.getOrDefault("taxableAmount", 0).toString());
                BigDecimal cgstAmount = new BigDecimal(g.getOrDefault("cgstAmount", 0).toString());
                BigDecimal sgstAmount = new BigDecimal(g.getOrDefault("sgstAmount", 0).toString());
                BigDecimal igstAmount = new BigDecimal(g.getOrDefault("igstAmount", 0).toString());

                items.add(Map.of(
                        "num", 1,
                        "itm_det", Map.of(
                                "rt", gstRate,
                                "txval", taxableAmount,
                                "camt", isInterState ? BigDecimal.ZERO : cgstAmount,
                                "samt", isInterState ? BigDecimal.ZERO : sgstAmount,
                                "iamt", isInterState ? igstAmount : BigDecimal.ZERO,
                                "csamt", 0
                        )
                ));
            }

            Map<String, Object> invoice = Map.of(
                    "inum", bill.get("bill_number"),
                    "idt", invoiceDate,
                    "val", bill.get("grand_total"),
                    "pos", bill.getOrDefault("branch_state_code", "27"),
                    "rchrg", "N",
                    "inv_typ", isInterState ? "R" : "B2CL",
                    "itms", items
            );

            if (isInterState) {
                Map<String, Object> existing = b2bList.stream()
                        .filter(b -> customerGstin.equals(b.get("ctin")))
                        .findFirst()
                        .orElse(null);

                if (existing != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> invs = (List<Map<String, Object>>) existing.get("inv");
                    invs.add(invoice);
                } else {
                    List<Map<String, Object>> invs = new ArrayList<>();
                    invs.add(invoice);
                    b2bList.add(new HashMap<>(Map.of("ctin", customerGstin, "inv", invs)));
                }
            } else {
                b2csList.add(invoice);
            }
        }

        String branchGstin = bills.isEmpty() ? "" : (String) bills.getFirst().getOrDefault("branch_gstin", "");
        String returnPeriod = from.substring(0, 7).replace("-", "");

        return Map.of(
                "gstin", branchGstin,
                "fp", returnPeriod,
                "version", "GST3.0.4",
                "hash", "hash",
                "b2b", b2bList,
                "b2cs", b2csList
        );
    }

    // ── Shift Report ───────────────────────────────────────────────────────────

    public List<Map<String, Object>> getShiftReport(UUID branchId, UUID tenantId, String from, String to) {
        assertDate(from, "from");
        assertDate(to, "to");
        OffsetDateTime start = toStartOfDay(from);
        OffsetDateTime end = toEndOfDay(to);

        String sql = """
            SELECT
              s.id                                               AS shift_id,
              s.shift_number,
              s.opened_at,
              s.closed_at,
              CONCAT(u.first_name, ' ', COALESCE(u.last_name, ''))      AS cashier_name,
              s.opening_cash,
              s.closing_cash,
              s.expected_cash,
              s.cash_difference,
              COALESCE(s.total_sales,   0)                               AS total_sales,
              COALESCE(s.total_orders,  0)                               AS total_orders,
              COALESCE(s.cash_sales,    0)                               AS cash_sales,
              COALESCE(s.card_sales,    0)                               AS card_sales,
              COALESCE(s.upi_sales,     0)                               AS upi_sales,
              COALESCE(s.wallet_sales,  0)                               AS wallet_sales,
              COALESCE(s.credit_sales,  0)                               AS credit_sales,
              COALESCE(s.complimentary, 0)                               AS complimentary,
              COALESCE(s.total_cgst,    0)                               AS total_cgst,
              COALESCE(s.total_sgst,    0)                               AS total_sgst,
              COALESCE(s.total_igst,    0)                               AS total_igst,
              s.status
            FROM shifts s
            LEFT JOIN users u ON u.id = s.opened_by
            WHERE s.tenant_id = ?
            """ + getBranchClause(branchId, "s.") + """
              AND s.opened_at BETWEEN ? AND ?
            ORDER BY s.opened_at DESC
        """;

        return jdbcTemplate.queryForList(sql, tenantId, start, end);
    }

    // ── Waiter Performance Report ──────────────────────────────────────────────

    public List<Map<String, Object>> getWaiterReport(UUID branchId, UUID tenantId, String from, String to) {
        assertDate(from, "from");
        assertDate(to, "to");
        OffsetDateTime start = toStartOfDay(from);
        OffsetDateTime end = toEndOfDay(to);

        String sql = """
            SELECT
              CONCAT(u.first_name, ' ', COALESCE(u.last_name, ''))      AS waiter_name,
              u.id                                                       AS waiter_id,
              u.employee_code,
              COUNT(DISTINCT o.id)::int                                  AS total_orders,
              COALESCE(SUM(o.grand_total), 0)                            AS total_revenue,
              COALESCE(
                SUM(o.grand_total) / NULLIF(COUNT(DISTINCT o.id), 0), 0
              )                                                          AS avg_order_value,
              COUNT(DISTINCT o.id) FILTER (WHERE o.order_type = 'dine_in')::int
                                                                         AS dine_in_orders,
              COUNT(DISTINCT o.id) FILTER (WHERE o.order_type = 'takeaway')::int
                                                                         AS takeaway_orders,
              COUNT(DISTINCT o.table_id)                                 AS tables_served,
              COALESCE(AVG(
                EXTRACT(EPOCH FROM (o.billed_at - o.placed_at)) / 60
              ) FILTER (WHERE o.billed_at IS NOT NULL AND o.placed_at IS NOT NULL), 0)
                                                                         AS avg_turnaround_min
            FROM orders o
            JOIN users u ON u.id = o.waiter_id
            WHERE o.tenant_id  = ?
            """ + getBranchClause(branchId, "o.") + """
              AND o.status     = 'billed'
              AND o.created_at BETWEEN ? AND ?
            GROUP BY u.id, u.first_name, u.last_name, u.employee_code
            ORDER BY total_revenue DESC
        """;

        return jdbcTemplate.queryForList(sql, tenantId, start, end);
    }

    // ── Live POS Dashboard Summary ─────────────────────────────────────────────

    public Map<String, Object> getDashboardSummary(UUID branchId, UUID tenantId) {
        String today = LocalDate.now().toString();
        OffsetDateTime dayStart = toStartOfDay(today);
        OffsetDateTime dayEnd = toEndOfDay(today);
        OffsetDateTime weekStart = toStartOfDay(LocalDate.now().minusDays(7).toString());
        OffsetDateTime weekEnd = toEndOfDay(today);

        String sqlTodaySales = "SELECT COALESCE(SUM(grand_total), 0) AS today_sales, COUNT(*)::int AS today_bills FROM bills WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND (source = 'pos' OR source IS NULL) AND status NOT IN ('void', 'refunded') AND created_at BETWEEN ? AND ?";
        Map<String, Object> todaySalesMap = jdbcTemplate.queryForMap(sqlTodaySales, tenantId, dayStart, dayEnd);

        String sqlWeekSales = "SELECT COALESCE(SUM(grand_total), 0) AS week_sales FROM bills WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND (source = 'pos' OR source IS NULL) AND status NOT IN ('void', 'refunded') AND created_at BETWEEN ? AND ?";
        Map<String, Object> weekSalesMap = jdbcTemplate.queryForMap(sqlWeekSales, tenantId, weekStart, weekEnd);

        String sqlPending = "SELECT COUNT(*)::int AS pending FROM orders WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND status NOT IN ('billed', 'cancelled')";
        Integer pending = jdbcTemplate.queryForObject(sqlPending, Integer.class, tenantId);

        String sqlLowStock = "SELECT COUNT(*)::int AS low_stock FROM inventory_items WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND current_stock <= min_stock_level AND is_active = true";
        Integer lowStock = jdbcTemplate.queryForObject(sqlLowStock, Integer.class, tenantId);

        String sqlOrderStats = "SELECT COUNT(*) FILTER (WHERE status = 'billed' AND NOT is_complimentary)::int AS successful, COUNT(*) FILTER (WHERE status = 'cancelled')::int AS cancelled, COUNT(*) FILTER (WHERE is_complimentary = true)::int AS complimentary, COUNT(*) FILTER (WHERE is_sales_return  = true)::int AS returns FROM orders WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND created_at BETWEEN ? AND ?";
        Map<String, Object> orderStats = jdbcTemplate.queryForMap(sqlOrderStats, tenantId, dayStart, dayEnd);

        String sqlRevLeakage = "SELECT (SELECT COUNT(*)::int FROM order_items oi JOIN orders o ON o.id = oi.order_id WHERE o.tenant_id = ? " + getBranchClause(branchId, "o.") + " AND oi.is_voided = true AND oi.created_at BETWEEN ? AND ?) AS voided_items, (SELECT COUNT(*)::int FROM orders WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND status = 'cancelled' AND grand_total > 0 AND created_at BETWEEN ? AND ?) AS cancelled_with_value";
        Map<String, Object> revLeakage = jdbcTemplate.queryForMap(sqlRevLeakage, tenantId, dayStart, dayEnd, tenantId, dayStart, dayEnd);

        String sqlTurnaround = "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (billed_at - placed_at)) / 60), 0) AS avg_turnaround_minutes FROM orders WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND order_type = 'dine_in' AND placed_at IS NOT NULL AND billed_at IS NOT NULL AND billed_at BETWEEN ? AND ?";
        Double turnaround = jdbcTemplate.queryForObject(sqlTurnaround, Double.class, tenantId, dayStart, dayEnd);

        return Map.of(
                "todaySales", todaySalesMap.getOrDefault("today_sales", 0),
                "todayBills", todaySalesMap.getOrDefault("today_bills", 0),
                "weekSales", weekSalesMap.getOrDefault("week_sales", 0),
                "pendingOrders", pending != null ? pending : 0,
                "lowStockAlerts", lowStock != null ? lowStock : 0,
                "orderStats", Map.of(
                        "successful", orderStats.getOrDefault("successful", 0),
                        "cancelled", orderStats.getOrDefault("cancelled", 0),
                        "complimentary", orderStats.getOrDefault("complimentary", 0),
                        "returns", orderStats.getOrDefault("returns", 0)
                ),
                "revenueLeakage", Map.of(
                        "voidedItems", revLeakage.getOrDefault("voided_items", 0),
                        "cancelledWithValue", revLeakage.getOrDefault("cancelled_with_value", 0)
                ),
                "tableStats", Map.of(
                        "avgTurnaroundMinutes", Math.round(turnaround != null ? turnaround : 0)
                )
        );
    }

    // ── Live Hotel Dashboard Summary ───────────────────────────────────────────

    public Map<String, Object> getHotelDashboardSummary(UUID branchId, UUID tenantId) {
        String today = LocalDate.now().toString();
        OffsetDateTime dayStart = toStartOfDay(today);
        OffsetDateTime dayEnd = toEndOfDay(today);

        String sqlTodaySales = "SELECT COALESCE(SUM(grand_total), 0) AS revenue, COUNT(*)::int AS count FROM bills WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND source = 'hotel' AND status NOT IN ('void', 'refunded') AND created_at BETWEEN ? AND ?";
        Map<String, Object> todaySalesMap = jdbcTemplate.queryForMap(sqlTodaySales, tenantId, dayStart, dayEnd);

        String sqlWeekSales = "SELECT COALESCE(SUM(grand_total), 0) AS revenue FROM bills WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND source = 'hotel' AND status NOT IN ('void', 'refunded') AND created_at >= NOW() - INTERVAL '7 days'";
        BigDecimal weekSales = jdbcTemplate.queryForObject(sqlWeekSales, BigDecimal.class, tenantId);

        String sqlCheckins = "SELECT COUNT(*)::int AS count FROM hotel_reservations WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND check_in_date = ? AND status NOT IN ('cancelled', 'no_show')";
        Integer checkins = jdbcTemplate.queryForObject(sqlCheckins, Integer.class, tenantId, LocalDate.now());

        String sqlCheckouts = "SELECT COUNT(*)::int AS count FROM hotel_reservations WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND check_out_date = ? AND status NOT IN ('cancelled', 'no_show')";
        Integer checkouts = jdbcTemplate.queryForObject(sqlCheckouts, Integer.class, tenantId, LocalDate.now());

        String sqlRooms = "SELECT COUNT(*)::int AS total_rooms, COUNT(*) FILTER (WHERE status = 'available')::int AS available_rooms, COUNT(*) FILTER (WHERE status = 'occupied')::int AS occupied_rooms, COUNT(*) FILTER (WHERE status = 'cleaning')::int AS cleaning_rooms, COUNT(*) FILTER (WHERE status = 'reserved')::int AS reserved_rooms, COUNT(*) FILTER (WHERE status IN ('maintenance', 'out_of_order'))::int AS maintenance_rooms FROM hotel_rooms WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND (is_active = true OR is_active IS NULL)";
        Map<String, Object> roomsData = jdbcTemplate.queryForMap(sqlRooms, tenantId);

        String sqlWeeklyChart = "SELECT TO_CHAR(date_trunc('day', created_at AT TIME ZONE 'Asia/Kolkata'), 'Mon DD') AS date, COALESCE(SUM(grand_total), 0) AS revenue FROM bills WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND source = 'hotel' AND status NOT IN ('void', 'refunded') AND created_at >= NOW() - INTERVAL '7 days' GROUP BY date_trunc('day', created_at AT TIME ZONE 'Asia/Kolkata') ORDER BY date_trunc('day', created_at AT TIME ZONE 'Asia/Kolkata') ASC";
        List<Map<String, Object>> weeklyChart = jdbcTemplate.queryForList(sqlWeeklyChart, tenantId);

        long totalRooms = ((Number) roomsData.getOrDefault("total_rooms", 0)).longValue();
        long occupiedRooms = ((Number) roomsData.getOrDefault("occupied_rooms", 0)).longValue();
        long occupancyRate = totalRooms > 0 ? Math.round(((double) occupiedRooms / totalRooms) * 100) : 0;

        BigDecimal todayRev = (BigDecimal) todaySalesMap.getOrDefault("revenue", BigDecimal.ZERO);
        long adr = occupiedRooms > 0 ? todayRev.divide(BigDecimal.valueOf(occupiedRooms), RoundingMode.HALF_UP).longValue() : 0;

        return Map.of(
                "todaySales", todayRev,
                "todayBills", todaySalesMap.getOrDefault("count", 0),
                "weekSales", weekSales != null ? weekSales : BigDecimal.ZERO,
                "todayCheckins", checkins != null ? checkins : 0,
                "todayCheckouts", checkouts != null ? checkouts : 0,
                "occupancyRate", occupancyRate,
                "adr", adr,
                "roomStats", Map.of(
                        "total", totalRooms,
                        "available", roomsData.getOrDefault("available_rooms", 0),
                        "occupied", occupiedRooms,
                        "cleaning", roomsData.getOrDefault("cleaning_rooms", 0),
                        "reserved", roomsData.getOrDefault("reserved_rooms", 0),
                        "maintenance", roomsData.getOrDefault("maintenance_rooms", 0)
                ),
                "weeklyChart", weeklyChart
        );
    }

    // ── Executive Owner Dashboard Summary ──────────────────────────────────────

    public Map<String, Object> getOwnerDashboardSummary(UUID branchId, UUID tenantId) {
        return getBranchSummary(branchId, tenantId, LocalDate.now().minusDays(7).toString(), LocalDate.now().toString());
    }

    // ── Cross-Branch Performance (Owner Only) ──────────────────────────────────

    public Map<String, Object> getBranchPerformance(UUID tenantId, String from, String to) {
        assertDate(from, "from");
        assertDate(to, "to");
        OffsetDateTime start = toStartOfDay(from);
        OffsetDateTime end = toEndOfDay(to);

        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        long diffDays = Math.max(1, ChronoUnit.DAYS.between(fromDate, toDate));
        String prevEnd = fromDate.minusDays(1).toString();
        String prevStart = fromDate.minusDays(diffDays).toString();
        OffsetDateTime pStart = toStartOfDay(prevStart);
        OffsetDateTime pEnd = toEndOfDay(prevEnd);

        String sqlBranches = "SELECT id, name, code, type, city, is_hq FROM branches WHERE tenant_id = ? AND is_active = true ORDER BY is_hq DESC, name ASC";
        List<Map<String, Object>> branches = jdbcTemplate.queryForList(sqlBranches, tenantId);

        String sqlCurrent = """
            WITH branch_bills AS (
                SELECT branch_id, 
                       COUNT(id) AS bills,
                       COALESCE(SUM(grand_total), 0) AS raw_revenue,
                       COALESCE(SUM(grand_total) FILTER (WHERE source='pos' OR source IS NULL), 0) AS pos_revenue,
                       COALESCE(SUM(grand_total) FILTER (WHERE source='hotel'), 0) AS hotel_revenue
                FROM bills
                WHERE tenant_id = ? AND status NOT IN ('void','refunded') AND created_at BETWEEN ? AND ?
                GROUP BY branch_id
            ),
            branch_orders AS (
                SELECT branch_id, COUNT(id) AS orders
                FROM orders
                WHERE tenant_id = ? AND status = 'billed' AND created_at BETWEEN ? AND ?
                GROUP BY branch_id
            ),
            folio_deductions AS (
                SELECT r.branch_id, COALESCE(SUM(fc.amount), 0) AS deduction
                FROM hotel_folio_charges fc
                JOIN hotel_reservations r ON r.id = fc.reservation_id
                WHERE r.tenant_id = ? AND fc.description LIKE 'Restaurant POS Order %' AND fc.created_at BETWEEN ? AND ?
                GROUP BY r.branch_id
            )
            SELECT 
                b.id AS branch_id,
                COALESCE(bb.raw_revenue, 0) - COALESCE(fd.deduction, 0) AS revenue,
                COALESCE(bb.bills, 0) AS bills,
                COALESCE(bb.pos_revenue, 0) AS pos_revenue,
                COALESCE(bb.hotel_revenue, 0) - COALESCE(fd.deduction, 0) AS hotel_revenue,
                COALESCE(bo.orders, 0) AS orders
            FROM branches b
            LEFT JOIN branch_bills bb ON bb.branch_id = b.id
            LEFT JOIN branch_orders bo ON bo.branch_id = b.id
            LEFT JOIN folio_deductions fd ON fd.branch_id = b.id
            WHERE b.tenant_id = ? AND b.is_active = true
        """;
        List<Map<String, Object>> current = jdbcTemplate.queryForList(sqlCurrent, tenantId, start, end, tenantId, start, end, tenantId, start, end, tenantId);

        String sqlPrevious = """
            WITH prev_bills AS (
                SELECT branch_id, COALESCE(SUM(grand_total), 0) AS raw_revenue
                FROM bills
                WHERE tenant_id = ? AND status NOT IN ('void','refunded') AND created_at BETWEEN ? AND ?
                GROUP BY branch_id
            ),
            prev_folios AS (
                SELECT r.branch_id, COALESCE(SUM(fc.amount), 0) AS deduction
                FROM hotel_folio_charges fc
                JOIN hotel_reservations r ON r.id = fc.reservation_id
                WHERE r.tenant_id = ? AND fc.description LIKE 'Restaurant POS Order %' AND fc.created_at BETWEEN ? AND ?
                GROUP BY r.branch_id
            )
            SELECT b.id AS branch_id, COALESCE(pb.raw_revenue, 0) - COALESCE(pf.deduction, 0) AS revenue
            FROM branches b
            LEFT JOIN prev_bills pb ON pb.branch_id = b.id
            LEFT JOIN prev_folios pf ON pf.branch_id = b.id
            WHERE b.tenant_id = ? AND b.is_active = true
        """;
        List<Map<String, Object>> previous = jdbcTemplate.queryForList(sqlPrevious, tenantId, pStart, pEnd, tenantId, pStart, pEnd, tenantId);

        Map<String, Map<String, Object>> currMap = new HashMap<>();
        for (Map<String, Object> r : current) currMap.put(r.get("branch_id").toString(), r);

        Map<String, Map<String, Object>> prevMap = new HashMap<>();
        for (Map<String, Object> r : previous) prevMap.put(r.get("branch_id").toString(), r);

        List<Map<String, Object>> branchList = new ArrayList<>();
        for (Map<String, Object> b : branches) {
            String idStr = b.get("id").toString();
            Map<String, Object> c = currMap.getOrDefault(idStr, Map.of());
            Map<String, Object> p = prevMap.getOrDefault(idStr, Map.of());

            BigDecimal rev = new BigDecimal(c.getOrDefault("revenue", BigDecimal.ZERO).toString());
            BigDecimal prevRev = new BigDecimal(p.getOrDefault("revenue", BigDecimal.ZERO).toString());
            Integer growthPct = null;
            if (prevRev.compareTo(BigDecimal.ZERO) > 0) {
                growthPct = rev.subtract(prevRev).multiply(BigDecimal.valueOf(100)).divide(prevRev, 0, RoundingMode.HALF_UP).intValue();
            }

            Map<String, Object> map = new HashMap<>();
            map.put("branchId", b.get("id"));
            map.put("branchName", b.get("name"));
            map.put("branchCode", b.get("code"));
            map.put("type", b.get("type"));
            map.put("city", b.get("city"));
            map.put("isHq", b.get("is_hq"));
            map.put("revenue", rev);
            map.put("posRevenue", c.getOrDefault("pos_revenue", 0));
            map.put("hotelRevenue", c.getOrDefault("hotel_revenue", 0));
            map.put("bills", c.getOrDefault("bills", 0));
            map.put("orders", c.getOrDefault("orders", 0));
            map.put("growthPct", growthPct);

            branchList.add(map);
        }

        branchList.sort((a, b) -> new BigDecimal(b.get("revenue").toString()).compareTo(new BigDecimal(a.get("revenue").toString())));

        BigDecimal totalRevenue = branchList.stream().map(b -> (BigDecimal) b.get("revenue")).reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalOrders = branchList.stream().mapToLong(b -> ((Number) b.get("orders")).longValue()).sum();
        long totalBills = branchList.stream().mapToLong(b -> ((Number) b.get("bills")).longValue()).sum();
        long avgRevenue = branchList.isEmpty() ? 0 : totalRevenue.divide(BigDecimal.valueOf(branchList.size()), 0, RoundingMode.HALF_UP).longValue();

        return Map.of(
                "totalBranches", branchList.size(),
                "totalRevenue", totalRevenue,
                "totalOrders", totalOrders,
                "totalBills", totalBills,
                "avgRevenue", avgRevenue,
                "topBranch", branchList.isEmpty() ? Map.of() : branchList.getFirst(),
                "branches", branchList,
                "period", Map.of("from", from, "to", to, "prevFrom", prevStart, "prevTo", prevEnd)
        );
    }

    // ── Single Branch Combined Summary ──────────────────────────────────────────

    public Map<String, Object> getBranchSummary(UUID branchId, UUID tenantId, String from, String to) {
        String today = LocalDate.now().toString();
        String rangeFrom = from != null ? from : today;
        String rangeTo = to != null ? to : today;
        String monthStart = today.substring(0, 7) + "-01";

        OffsetDateTime rangeStart = toStartOfDay(rangeFrom);
        OffsetDateTime rangeEnd = toEndOfDay(rangeTo);
        OffsetDateTime monthS = toStartOfDay(monthStart);
        OffsetDateTime monthE = toEndOfDay(today);
        OffsetDateTime weekS = toStartOfDay(LocalDate.now().minusDays(7).toString());
        OffsetDateTime weekE = toEndOfDay(today);

        String sqlRevenueRange = "SELECT COALESCE(SUM(grand_total),0) AS revenue, COUNT(*)::int AS bills FROM bills WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND status NOT IN ('void','refunded') AND created_at BETWEEN ? AND ?";
        Map<String, Object> revenueToday = jdbcTemplate.queryForMap(sqlRevenueRange, tenantId, rangeStart, rangeEnd);

        String sqlRevenueMonth = "SELECT COALESCE(SUM(grand_total),0) AS revenue FROM bills WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND status NOT IN ('void','refunded') AND created_at BETWEEN ? AND ?";
        BigDecimal revenueMonth = jdbcTemplate.queryForObject(sqlRevenueMonth, BigDecimal.class, tenantId, monthS, monthE);

        String sqlRevenueWeek = "SELECT COALESCE(SUM(grand_total),0) AS revenue FROM bills WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND status NOT IN ('void','refunded') AND created_at BETWEEN ? AND ?";
        BigDecimal revenueWeek = jdbcTemplate.queryForObject(sqlRevenueWeek, BigDecimal.class, tenantId, weekS, weekE);

        String sqlRestaurantToday = "SELECT COALESCE(SUM(grand_total),0) AS revenue, COUNT(*)::int AS bills FROM bills WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND (source='pos' OR source IS NULL) AND status NOT IN ('void','refunded') AND created_at BETWEEN ? AND ?";
        Map<String, Object> restaurantToday = jdbcTemplate.queryForMap(sqlRestaurantToday, tenantId, rangeStart, rangeEnd);

        String sqlHotelToday = "SELECT COALESCE(SUM(grand_total),0) AS revenue, COUNT(*)::int AS bills FROM bills WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND source='hotel' AND status NOT IN ('void','refunded') AND created_at BETWEEN ? AND ?";
        Map<String, Object> hotelToday = jdbcTemplate.queryForMap(sqlHotelToday, tenantId, rangeStart, rangeEnd);

        String sqlFolioRange = "SELECT COALESCE(SUM(fc.amount),0) FROM hotel_folio_charges fc JOIN hotel_reservations r ON r.id = fc.reservation_id WHERE r.tenant_id = ? " + getBranchClause(branchId, "r.") + " AND fc.description LIKE 'Restaurant POS Order %' AND fc.created_at BETWEEN ? AND ?";
        BigDecimal folioRange = jdbcTemplate.queryForObject(sqlFolioRange, BigDecimal.class, tenantId, rangeStart, rangeEnd);
        
        String sqlFolioMonth = "SELECT COALESCE(SUM(fc.amount),0) FROM hotel_folio_charges fc JOIN hotel_reservations r ON r.id = fc.reservation_id WHERE r.tenant_id = ? " + getBranchClause(branchId, "r.") + " AND fc.description LIKE 'Restaurant POS Order %' AND fc.created_at BETWEEN ? AND ?";
        BigDecimal folioMonth = jdbcTemplate.queryForObject(sqlFolioMonth, BigDecimal.class, tenantId, monthS, monthE);
        
        String sqlFolioWeek = "SELECT COALESCE(SUM(fc.amount),0) FROM hotel_folio_charges fc JOIN hotel_reservations r ON r.id = fc.reservation_id WHERE r.tenant_id = ? " + getBranchClause(branchId, "r.") + " AND fc.description LIKE 'Restaurant POS Order %' AND fc.created_at BETWEEN ? AND ?";
        BigDecimal folioWeek = jdbcTemplate.queryForObject(sqlFolioWeek, BigDecimal.class, tenantId, weekS, weekE);

        BigDecimal revenueRangeClean = new BigDecimal(revenueToday.get("revenue").toString()).subtract(folioRange);
        BigDecimal hotelRangeClean = new BigDecimal(hotelToday.get("revenue").toString()).subtract(folioRange);
        BigDecimal revenueMonthClean = (revenueMonth != null ? revenueMonth : BigDecimal.ZERO).subtract(folioMonth);
        BigDecimal revenueWeekClean = (revenueWeek != null ? revenueWeek : BigDecimal.ZERO).subtract(folioWeek);

        String sqlOrdersToday = "SELECT COUNT(*)::int AS total_orders, COUNT(*) FILTER (WHERE status='billed')::int AS billed_orders, COUNT(*) FILTER (WHERE status NOT IN ('billed','cancelled'))::int AS pending_orders, COALESCE(AVG(grand_total) FILTER (WHERE status='billed'), 0) AS avg_order_value FROM orders WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND created_at BETWEEN ? AND ?";
        Map<String, Object> ordersToday = jdbcTemplate.queryForMap(sqlOrdersToday, tenantId, rangeStart, rangeEnd);

        String sqlBillsToday = "SELECT COUNT(*)::int AS bills FROM bills WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND created_at BETWEEN ? AND ?";
        Integer billsToday = jdbcTemplate.queryForObject(sqlBillsToday, Integer.class, tenantId, rangeStart, rangeEnd);

        String sqlOpenShifts = "SELECT COUNT(*)::int AS count FROM shifts WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND status='open'";
        Integer openShifts = jdbcTemplate.queryForObject(sqlOpenShifts, Integer.class, tenantId);

        String sqlLowStock = "SELECT COUNT(*)::int AS count FROM inventory_items WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND current_stock <= min_stock_level AND is_active = true";
        Integer lowStock = jdbcTemplate.queryForObject(sqlLowStock, Integer.class, tenantId);

        String sqlHotelReservations = "SELECT COUNT(*) FILTER (WHERE check_in_date = ? AND status NOT IN ('cancelled','no_show'))::int AS reservations_today, COUNT(*) FILTER (WHERE check_in_date = ? AND status NOT IN ('cancelled','no_show'))::int AS checkins_today, COUNT(*) FILTER (WHERE check_out_date = ? AND status NOT IN ('cancelled','no_show'))::int AS checkouts_today, COUNT(*) FILTER (WHERE status='checked_in')::int AS in_house, (SELECT COUNT(*)::int FROM hotel_rooms WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND is_active=true) AS total_rooms, (SELECT COUNT(*) FILTER (WHERE status='available')::int FROM hotel_rooms WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND is_active=true) AS available_rooms FROM hotel_reservations WHERE tenant_id = ? " + getBranchClause(branchId, "");
        Map<String, Object> hotel = jdbcTemplate.queryForMap(sqlHotelReservations, LocalDate.now(), LocalDate.now(), LocalDate.now(), tenantId, tenantId, tenantId);

        String sqlHkPending = "SELECT COUNT(*)::int AS pending FROM hotel_housekeeping_tasks WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND status = 'pending' AND scheduled_for = ?";
        Integer housekeepingPending = jdbcTemplate.queryForObject(sqlHkPending, Integer.class, tenantId, LocalDate.now());

        String sqlStaff = "SELECT role, COUNT(*)::int AS count FROM users WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND is_active=true GROUP BY role";
        List<Map<String, Object>> staff = jdbcTemplate.queryForList(sqlStaff, tenantId);

        String sqlPayments = "SELECT method, COALESCE(SUM(amount),0) AS total, COUNT(*)::int AS txns FROM payments WHERE tenant_id = ? " + getBranchClause(branchId, "") + " AND status='success' AND created_at BETWEEN ? AND ? GROUP BY method ORDER BY total DESC";
        List<Map<String, Object>> paymentBreakdown = jdbcTemplate.queryForList(sqlPayments, tenantId, rangeStart, rangeEnd);

        // 🟢 THE BULLETPROOF FIX: Safely groups daily charts using CTEs
        String sqlWeeklyChart = """
            WITH daily_bills AS (
                SELECT date_trunc('day', created_at AT TIME ZONE 'Asia/Kolkata') AS day_date,
                       COALESCE(SUM(grand_total) FILTER (WHERE source='pos' OR source IS NULL), 0) AS pos,
                       COALESCE(SUM(grand_total) FILTER (WHERE source='hotel'), 0) AS hotel,
                       COALESCE(SUM(grand_total), 0) AS total
                FROM bills
                WHERE tenant_id = ? """ + getBranchClause(branchId, "") + """
                  AND status NOT IN ('void','refunded') AND created_at BETWEEN ? AND ?
                GROUP BY day_date
            ),
            daily_folios AS (
                SELECT date_trunc('day', fc.created_at AT TIME ZONE 'Asia/Kolkata') AS day_date,
                       COALESCE(SUM(fc.amount), 0) AS deduction
                FROM hotel_folio_charges fc
                JOIN hotel_reservations r ON r.id = fc.reservation_id
                WHERE r.tenant_id = ? """ + getBranchClause(branchId, "r.") + """
                  AND fc.description LIKE 'Restaurant POS Order %' AND fc.created_at BETWEEN ? AND ?
                GROUP BY day_date
            )
            SELECT TO_CHAR(b.day_date, 'Mon DD') AS date,
                   b.pos,
                   b.hotel - COALESCE(f.deduction, 0) AS hotel,
                   b.total - COALESCE(f.deduction, 0) AS total
            FROM daily_bills b
            LEFT JOIN daily_folios f ON f.day_date = b.day_date
            ORDER BY b.day_date ASC
        """;
        List<Map<String, Object>> weeklyChart = jdbcTemplate.queryForList(sqlWeeklyChart, tenantId, rangeStart, rangeEnd, tenantId, rangeStart, rangeEnd);

        // 🟢 RESTORED: Owner Dashboard expects the branch comparison inside branch-summary API
        List<Map<String, Object>> branchComparison = List.of();
        if (branchId == null) {
            String sqlCompare = """
                WITH prev_bills AS (
                    SELECT branch_id, COALESCE(SUM(grand_total), 0) AS raw_revenue
                    FROM bills
                    WHERE tenant_id = ? AND status NOT IN ('void','refunded') AND created_at BETWEEN ? AND ?
                    GROUP BY branch_id
                ),
                prev_folios AS (
                    SELECT r.branch_id, COALESCE(SUM(fc.amount), 0) AS deduction
                    FROM hotel_folio_charges fc
                    JOIN hotel_reservations r ON r.id = fc.reservation_id
                    WHERE r.tenant_id = ? AND fc.description LIKE 'Restaurant POS Order %' AND fc.created_at BETWEEN ? AND ?
                    GROUP BY r.branch_id
                )
                SELECT b.name AS name, COALESCE(pb.raw_revenue, 0) - COALESCE(pf.deduction, 0) AS revenue
                FROM branches b
                LEFT JOIN prev_bills pb ON pb.branch_id = b.id
                LEFT JOIN prev_folios pf ON pf.branch_id = b.id
                WHERE b.tenant_id = ? AND b.is_active = true
                ORDER BY revenue DESC
            """;
            branchComparison = jdbcTemplate.queryForList(sqlCompare, tenantId, rangeStart, rangeEnd, tenantId, rangeStart, rangeEnd, tenantId);
        }

        Map<String, Integer> staffMap = new HashMap<>();
        int totalStaff = 0;
        for (Map<String, Object> r : staff) {
            String roleName = r.get("role").toString();
            int countVal = ((Number) r.get("count")).intValue();
            staffMap.put(roleName, countVal);
            totalStaff += countVal;
        }

        long totalRooms = ((Number) hotel.getOrDefault("total_rooms", 0)).longValue();
        long inHouse = ((Number) hotel.getOrDefault("in_house", 0)).longValue();
        long occupancyPct = totalRooms > 0 ? Math.round(((double) inHouse / totalRooms) * 100) : 0;

        return Map.of(
                "branch", Map.of("id", branchId != null ? branchId : ""),
                "period", Map.of("from", rangeFrom, "to", rangeTo, "prevFrom", rangeFrom, "prevTo", rangeTo),
                "revenue", Map.of(
                        "total", revenueRangeClean,
                        "week", revenueWeekClean,
                        "month", revenueMonthClean,
                        "restaurant", restaurantToday.getOrDefault("revenue", 0),
                        "hotel", hotelRangeClean,
                        "totalBills", revenueToday.getOrDefault("bills", 0)
                ),
                "restaurant", Map.of(
                        "ordersToday", ordersToday.getOrDefault("total_orders", 0),
                        "billedOrders", ordersToday.getOrDefault("billed_orders", 0),
                        "pendingOrders", ordersToday.getOrDefault("pending_orders", 0),
                        "billsToday", billsToday != null ? billsToday : 0,
                        "avgOrderValue", Math.round(((Number) ordersToday.getOrDefault("avg_order_value", 0)).doubleValue()),
                        "openShifts", openShifts != null ? openShifts : 0,
                        "lowStockItems", lowStock != null ? lowStock : 0
                ),
                "hotel", Map.of(
                        "reservationsToday", hotel.getOrDefault("reservations_today", 0),
                        "checkinsToday", hotel.getOrDefault("checkins_today", 0),
                        "checkoutsToday", hotel.getOrDefault("checkouts_today", 0),
                        "inHouse", inHouse,
                        "totalRooms", totalRooms,
                        "availableRooms", hotel.getOrDefault("available_rooms", 0),
                        "occupancyPct", occupancyPct,
                        "housekeepingPending", housekeepingPending != null ? housekeepingPending : 0
                ),
                "staff", Map.of(
                        "total", totalStaff,
                        "managers", staffMap.getOrDefault("manager", 0) + staffMap.getOrDefault("restaurant_manager", 0) + staffMap.getOrDefault("hotel_manager", 0),
                        "cashiers", staffMap.getOrDefault("cashier", 0),
                        "waiters", staffMap.getOrDefault("waiter", 0),
                        "kitchen", staffMap.getOrDefault("kitchen", 0),
                        "housekeeping", staffMap.getOrDefault("housekeeping", 0),
                        "receptionist", staffMap.getOrDefault("receptionist", 0),
                        "byRole", staffMap
                ),
                "paymentBreakdown", paymentBreakdown,
                "weeklyChart", weeklyChart,
                "alerts", Map.of(
                        "lowStock", lowStock != null ? lowStock : 0,
                        "openShifts", openShifts != null ? openShifts : 0,
                        "housekeepingPending", housekeepingPending != null ? housekeepingPending : 0
                ),
                "branchComparison", branchComparison
        );
    }
}