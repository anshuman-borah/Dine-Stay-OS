//package project.EnterpriseSaas.demo.modules.shift.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.server.ResponseStatusException;
//import project.EnterpriseSaas.demo.common.enums.ShiftDepartment;
//import project.EnterpriseSaas.demo.common.enums.ShiftStatus;
//import project.EnterpriseSaas.demo.modules.branch.entity.Branch;
//import project.EnterpriseSaas.demo.modules.branch.repository.BranchRepository;
//import project.EnterpriseSaas.demo.modules.shift.dto.CloseShiftDto;
//import project.EnterpriseSaas.demo.modules.shift.dto.DenominationDto;
//import project.EnterpriseSaas.demo.modules.shift.dto.OpenShiftDto;
//import project.EnterpriseSaas.demo.modules.shift.entity.Shift;
//import project.EnterpriseSaas.demo.modules.shift.entity.ShiftDenomination;
//import project.EnterpriseSaas.demo.modules.shift.repository.ShiftDenominationRepository;
//import project.EnterpriseSaas.demo.modules.shift.repository.ShiftRepository;
//import project.EnterpriseSaas.demo.modules.tenant.entity.Tenant;
//import project.EnterpriseSaas.demo.modules.tenant.repository.TenantRepository;
//import project.EnterpriseSaas.demo.modules.user.entity.User;
//import project.EnterpriseSaas.demo.modules.user.repository.UserRepository;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.time.OffsetDateTime;
//import java.util.*;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class ShiftService {
//
//    private final ShiftRepository shiftRepo;
//    private final ShiftDenominationRepository denomRepo;
//    private final TenantRepository tenantRepo;
//    private final BranchRepository branchRepo;
//    private final UserRepository userRepo;
//    private final JdbcTemplate jdbcTemplate;
//
//    // ── Open Shift ───────────────────────────────────────────────────────────
//
//    @Transactional
//    public Shift openShift(
//            UUID branchId,
//            UUID tenantId,
//            UUID userId,
//            BigDecimal openingCash,
//            DenominationDto denominations,
//            ShiftDepartment department
//    ) {
//        Optional<Shift> existing = shiftRepo.findActiveShift(branchId, tenantId, ShiftStatus.open, department);
//        if (existing.isPresent()) {
//            String label = department == ShiftDepartment.hotel ? "hotel" : "restaurant";
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A " + label + " shift is already open for this branch");
//        }
//
//        Tenant tenant = tenantRepo.findById(tenantId)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
//
//        Branch branch = branchRepo.findById(branchId)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
//
//        User user = userRepo.findById(userId)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
//
//        String prefix = department == ShiftDepartment.hotel ? "HSH" : "RSH";
//        long count = shiftRepo.countByBranchAndTenantAndDepartment(branchId, tenantId, department);
//        String shiftNumber = String.format("%s-%04d", prefix, count + 1);
//
//        Shift shift = Shift.builder()
//                .tenant(tenant)
//                .branch(branch)
//                .shiftNumber(shiftNumber)
//                .department(department)
//                .status(ShiftStatus.open)
//                .openedBy(user)
//                .openingCash(openingCash != null ? openingCash : BigDecimal.ZERO)
//                .openedAt(OffsetDateTime.now())
//                .build();
//
//        Shift saved = shiftRepo.save(shift);
//
//        if (denominations != null) {
//            saveDenominations(saved, denominations, true);
//        }
//
//        log.info("Opened new {} shift: {} for branch {}", department, shiftNumber, branchId);
//        return saved;
//    }
//
//    // ── Close Shift ──────────────────────────────────────────────────────────
//
//    @Transactional
//    public Map<String, Object> closeShift(
//            UUID shiftId,
//            UUID tenantId,
//            UUID userId,
//            BigDecimal closingCash,
//            DenominationDto denominations,
//            String notes
//    ) {
//        Shift shift = shiftRepo.findByIdAndTenantIdAndStatus(shiftId, tenantId, ShiftStatus.open)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Open shift not found"));
//
//        User user = userRepo.findById(userId)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
//
//        BigDecimal expectedCash = shift.getOpeningCash()
//                .add(shift.getCashSales())
//                .subtract(shift.getTotalRefund());
//
//        BigDecimal cashDiff = (closingCash != null ? closingCash : BigDecimal.ZERO).subtract(expectedCash);
//
//        shift.setStatus(ShiftStatus.closed);
//        shift.setClosedBy(user);
//        shift.setClosingCash(closingCash != null ? closingCash : BigDecimal.ZERO);
//        shift.setExpectedCash(expectedCash);
//        shift.setCashDifference(cashDiff);
//        shift.setClosedAt(OffsetDateTime.now());
//        if (notes != null) shift.setNotes(notes);
//
//        shiftRepo.save(shift);
//
//        if (denominations != null) {
//            saveDenominations(shift, denominations, false);
//        }
//
//        log.info("Closed shift: {}. Expected = ₹{}, Actual = ₹{}, Diff = ₹{}",
//                shift.getShiftNumber(), expectedCash, closingCash, cashDiff);
//
//        return getShiftSummary(shift.getId(), tenantId);
//    }
//
//    // ── Get Active Shift ─────────────────────────────────────────────────────
//
//    public Map<String, Object> getActiveShift(UUID branchId, UUID tenantId, ShiftDepartment department) {
//        Optional<Shift> shiftOpt = shiftRepo.findActiveShift(branchId, tenantId, ShiftStatus.open, department);
//        if (shiftOpt.isEmpty()) return null;
//        return getShiftSummary(shiftOpt.get().getId(), tenantId);
//    }
//
//    // ── Get Shift Summary ────────────────────────────────────────────────────
//
//    public Map<String, Object> getShiftSummary(UUID shiftId, UUID tenantId) {
//        Shift shift = shiftRepo.findByIdAndTenantId(shiftId, tenantId)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shift not found"));
//
//        List<ShiftDenomination> denoms = denomRepo.findByShiftId(shiftId);
//
//        Map<String, Object> summary = new LinkedHashMap<>();
//        summary.put("id", shift.getId());
//        summary.put("shiftNumber", shift.getShiftNumber());
//        summary.put("shift_number", shift.getShiftNumber());
//        summary.put("department", shift.getDepartment());
//        summary.put("status", shift.getStatus());
//        summary.put("openingCash", shift.getOpeningCash());
//        summary.put("opening_cash", shift.getOpeningCash());
//        summary.put("closingCash", shift.getClosingCash());
//        summary.put("closing_cash", shift.getClosingCash());
//        summary.put("expectedCash", shift.getExpectedCash());
//        summary.put("expected_cash", shift.getExpectedCash());
//        summary.put("cashDifference", shift.getCashDifference());
//        summary.put("cash_difference", shift.getCashDifference());
//        summary.put("totalSales", shift.getTotalSales());
//        summary.put("total_sales", shift.getTotalSales());
//        summary.put("totalOrders", shift.getTotalOrders());
//        summary.put("total_orders", shift.getTotalOrders());
//        summary.put("cashSales", shift.getCashSales());
//        summary.put("cash_sales", shift.getCashSales());
//        summary.put("cardSales", shift.getCardSales());
//        summary.put("card_sales", shift.getCardSales());
//        summary.put("upiSales", shift.getUpiSales());
//        summary.put("upi_sales", shift.getUpiSales());
//        summary.put("walletSales", shift.getWalletSales());
//        summary.put("wallet_sales", shift.getWalletSales());
//        summary.put("creditSales", shift.getCreditSales());
//        summary.put("credit_sales", shift.getCreditSales());
//        summary.put("complimentary", shift.getComplimentary());
//        summary.put("totalRefund", shift.getTotalRefund());
//        summary.put("notes", shift.getNotes());
//        summary.put("openedAt", shift.getOpenedAt());
//        summary.put("opened_at", shift.getOpenedAt());
//        summary.put("closedAt", shift.getClosedAt());
//        summary.put("closed_at", shift.getClosedAt());
//        summary.put("denominations", denoms);
//        summary.put("openedByUser", shift.getOpenedByUser());
//        summary.put("closedByUser", shift.getClosedByUser());
//
//        summary.put("paymentBreakdown", Map.of(
//                "cash", shift.getCashSales(),
//                "card", shift.getCardSales(),
//                "upi", shift.getUpiSales(),
//                "wallet", shift.getWalletSales(),
//                "credit", shift.getCreditSales(),
//                "complimentary", shift.getComplimentary()
//        ));
//
//        BigDecimal totalTax = shift.getTotalCgst().add(shift.getTotalSgst()).add(shift.getTotalIgst());
//        summary.put("gstBreakdown", Map.of(
//                "cgst", shift.getTotalCgst(),
//                "sgst", shift.getTotalSgst(),
//                "igst", shift.getTotalIgst(),
//                "total", totalTax
//        ));
//
//        return summary;
//    }
//
//    // ── List Shifts (Robust Native SQL) ───────────────────────────────────────
//
//    public List<Map<String, Object>> listShifts(
//            UUID branchId,
//            UUID tenantId,
//            int limit,
//            int offset,
//            String status,
//            String startDateStr,
//            String endDateStr,
//            String shiftNumber,
//            ShiftDepartment department
//    ) {
//        StringBuilder sql = new StringBuilder("""
//            SELECT
//              s.id,
//              s.shift_number,
//              s.status,
//              s.department,
//              s.opening_cash,
//              s.closing_cash,
//              s.expected_cash,
//              s.cash_difference,
//              s.total_sales,
//              s.total_orders,
//              s.cash_sales,
//              s.card_sales,
//              s.upi_sales,
//              s.wallet_sales,
//              s.credit_sales,
//              s.complimentary,
//              s.total_refund,
//              s.total_cgst,
//              s.total_sgst,
//              s.total_igst,
//              s.notes,
//              s.opened_at,
//              s.closed_at,
//              s.opened_by,
//              s.closed_by,
//              u1.first_name AS opened_first_name,
//              u1.last_name  AS opened_last_name,
//              u1.role       AS opened_role,
//              u2.first_name AS closed_first_name,
//              u2.last_name  AS closed_last_name,
//              u2.role       AS closed_role
//            FROM shifts s
//            LEFT JOIN users u1 ON u1.id = s.opened_by
//            LEFT JOIN users u2 ON u2.id = s.closed_by
//            WHERE s.branch_id  = ?
//              AND s.tenant_id  = ?
//              AND s.department = ?
//        """);
//
//        List<Object> params = new ArrayList<>();
//        params.add(branchId);
//        params.add(tenantId);
//        params.add(department.name());
//
//        if (status != null && !status.isBlank()) {
//            sql.append(" AND s.status = ?");
//            params.add(status.toLowerCase());
//        }
//        if (shiftNumber != null && !shiftNumber.isBlank()) {
//            sql.append(" AND s.shift_number ILIKE ?");
//            params.add("%" + shiftNumber + "%");
//        }
//        if (startDateStr != null && !startDateStr.isBlank()) {
//            sql.append(" AND s.opened_at >= ?");
//            params.add(OffsetDateTime.parse(startDateStr));
//        }
//        if (endDateStr != null && !endDateStr.isBlank()) {
//            sql.append(" AND s.opened_at <= ?");
//            params.add(OffsetDateTime.parse(endDateStr));
//        }
//
//        // Changed from created_at to opened_at to guarantee the column exists
//        sql.append(" ORDER BY s.opened_at DESC LIMIT ? OFFSET ?");
//        params.add(limit);
//        params.add(offset);
//
//        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
//            Map<String, Object> map = new LinkedHashMap<>();
//            map.put("id", rs.getObject("id", UUID.class));
//            map.put("shiftNumber", rs.getString("shift_number"));
//            map.put("shift_number", rs.getString("shift_number"));
//            map.put("status", rs.getString("status"));
//            map.put("department", rs.getString("department"));
//            map.put("openingCash", rs.getBigDecimal("opening_cash"));
//            map.put("opening_cash", rs.getBigDecimal("opening_cash"));
//            map.put("closingCash", rs.getBigDecimal("closing_cash"));
//            map.put("closing_cash", rs.getBigDecimal("closing_cash"));
//            map.put("expectedCash", rs.getBigDecimal("expected_cash"));
//            map.put("expected_cash", rs.getBigDecimal("expected_cash"));
//            map.put("cashDifference", rs.getBigDecimal("cash_difference"));
//            map.put("cash_difference", rs.getBigDecimal("cash_difference"));
//            map.put("totalSales", rs.getBigDecimal("total_sales"));
//            map.put("total_sales", rs.getBigDecimal("total_sales"));
//            map.put("totalOrders", rs.getInt("total_orders"));
//            map.put("total_orders", rs.getInt("total_orders"));
//            map.put("cashSales", rs.getBigDecimal("cash_sales"));
//            map.put("cash_sales", rs.getBigDecimal("cash_sales"));
//            map.put("cardSales", rs.getBigDecimal("card_sales"));
//            map.put("card_sales", rs.getBigDecimal("card_sales"));
//            map.put("upiSales", rs.getBigDecimal("upi_sales"));
//            map.put("upi_sales", rs.getBigDecimal("upi_sales"));
//            map.put("notes", rs.getString("notes"));
//            map.put("openedAt", rs.getObject("opened_at", OffsetDateTime.class));
//            map.put("opened_at", rs.getObject("opened_at", OffsetDateTime.class));
//            map.put("closedAt", rs.getObject("closed_at", OffsetDateTime.class));
//            map.put("closed_at", rs.getObject("closed_at", OffsetDateTime.class));
//
//            String opFirst = rs.getString("opened_first_name");
//            String opLast = rs.getString("opened_last_name");
//            String opRole = rs.getString("opened_role");
//            if (opFirst != null) {
//                map.put("openedByUser", Map.of(
//                        "firstName", opFirst,
//                        "lastName", opLast != null ? opLast : "",
//                        "role", opRole != null ? opRole : "",
//                        "fullName", (opFirst + " " + (opLast != null ? opLast : "")).trim()
//                ));
//            } else {
//                map.put("openedByUser", null);
//            }
//
//            String clFirst = rs.getString("closed_first_name");
//            String clLast = rs.getString("closed_last_name");
//            String clRole = rs.getString("closed_role");
//            if (clFirst != null) {
//                map.put("closedByUser", Map.of(
//                        "firstName", clFirst,
//                        "lastName", clLast != null ? clLast : "",
//                        "role", clRole != null ? clRole : "",
//                        "fullName", (clFirst + " " + (clLast != null ? clLast : "")).trim()
//                ));
//            } else {
//                map.put("closedByUser", null);
//            }
//
//            return map;
//        }, params.toArray());
//    }
//
//    // ── Get Shift Stats (Robust Native SQL) ────────────────────────────────────
//
//    public Map<String, Object> getShiftStats(
//            UUID branchId,
//            UUID tenantId,
//            OffsetDateTime startDate,
//            OffsetDateTime endDate,
//            ShiftDepartment department
//    ) {
//        StringBuilder sql = new StringBuilder("""
//            SELECT
//              COUNT(*)::int AS total_shifts,
//              COALESCE(SUM(total_sales), 0) AS total_sales,
//              COALESCE(SUM(cash_difference), 0) AS total_cash_difference,
//              COALESCE(AVG(total_sales), 0) AS avg_shift_value
//            FROM shifts
//            WHERE branch_id = ? AND tenant_id = ? AND department = ? AND status = 'closed'
//        """);
//        List<Object> params = new ArrayList<>();
//        params.add(branchId);
//        params.add(tenantId);
//        params.add(department.name());
//
//        if (startDate != null) {
//            sql.append(" AND closed_at >= ?");
//            params.add(startDate);
//        }
//        if (endDate != null) {
//            sql.append(" AND closed_at <= ?");
//            params.add(endDate);
//        }
//
//        Map<String, Object> row = jdbcTemplate.queryForMap(sql.toString(), params.toArray());
//        return Map.of(
//                "totalShifts", row.getOrDefault("total_shifts", 0),
//                "totalSales", row.getOrDefault("total_sales", 0),
//                "totalCashDifference", row.getOrDefault("total_cash_difference", 0),
//                "averageShiftValue", row.getOrDefault("avg_shift_value", 0)
//        );
//    }
//
//    private void saveDenominations(Shift shift, DenominationDto d, boolean isOpening) {
//        ShiftDenomination denom = ShiftDenomination.builder()
//                .shift(shift)
//                .isOpening(isOpening)
//                .note2000(d.getNote2000() != null ? d.getNote2000() : 0)
//                .note500(d.getNote500() != null ? d.getNote500() : 0)
//                .note200(d.getNote200() != null ? d.getNote200() : 0)
//                .note100(d.getNote100() != null ? d.getNote100() : 0)
//                .note50(d.getNote50() != null ? d.getNote50() : 0)
//                .note20(d.getNote20() != null ? d.getNote20() : 0)
//                .note10(d.getNote10() != null ? d.getNote10() : 0)
//                .coin5(d.getCoin5() != null ? d.getCoin5() : 0)
//                .coin2(d.getCoin2() != null ? d.getCoin2() : 0)
//                .coin1(d.getCoin1() != null ? d.getCoin1() : 0)
//                .build();
//
//        denomRepo.save(denom);
//    }
//}

package project.EnterpriseSaas.demo.modules.shift.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.EnterpriseSaas.demo.common.enums.ShiftDepartment;
import project.EnterpriseSaas.demo.common.enums.ShiftStatus;
import project.EnterpriseSaas.demo.modules.branch.entity.Branch;
import project.EnterpriseSaas.demo.modules.branch.repository.BranchRepository;
import project.EnterpriseSaas.demo.modules.shift.dto.CloseShiftDto;
import project.EnterpriseSaas.demo.modules.shift.dto.DenominationDto;
import project.EnterpriseSaas.demo.modules.shift.dto.OpenShiftDto;
import project.EnterpriseSaas.demo.modules.shift.entity.Shift;
import project.EnterpriseSaas.demo.modules.shift.entity.ShiftDenomination;
import project.EnterpriseSaas.demo.modules.shift.repository.ShiftDenominationRepository;
import project.EnterpriseSaas.demo.modules.shift.repository.ShiftRepository;
import project.EnterpriseSaas.demo.modules.tenant.entity.Tenant;
import project.EnterpriseSaas.demo.modules.tenant.repository.TenantRepository;
import project.EnterpriseSaas.demo.modules.user.entity.User;
import project.EnterpriseSaas.demo.modules.user.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShiftService {

    private final ShiftRepository shiftRepo;
    private final ShiftDenominationRepository denomRepo;
    private final TenantRepository tenantRepo;
    private final BranchRepository branchRepo;
    private final UserRepository userRepo;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public Shift openShift(
            UUID branchId,
            UUID tenantId,
            UUID userId,
            BigDecimal openingCash,
            DenominationDto denominations,
            ShiftDepartment department
    ) {
        Optional<Shift> existing = shiftRepo.findActiveShift(branchId, tenantId, ShiftStatus.open, department);
        if (existing.isPresent()) {
            String label = department == ShiftDepartment.hotel ? "hotel" : "restaurant";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A " + label + " shift is already open for this branch");
        }

        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

        Branch branch = branchRepo.findById(branchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String prefix = department == ShiftDepartment.hotel ? "HSH" : "RSH";
        long count = shiftRepo.countByBranchAndTenantAndDepartment(branchId, tenantId, department);
        String shiftNumber = String.format("%s-%04d", prefix, count + 1);

        // Safely initialize all null fields to ZERO to prevent closing crashes
        Shift shift = Shift.builder()
                .tenant(tenant)
                .branch(branch)
                .shiftNumber(shiftNumber)
                .department(department)
                .status(ShiftStatus.open)
                .openedBy(user)
                .openingCash(openingCash != null ? openingCash : BigDecimal.ZERO)
                .closingCash(BigDecimal.ZERO)
                .expectedCash(BigDecimal.ZERO)
                .cashDifference(BigDecimal.ZERO)
                .totalSales(BigDecimal.ZERO)
                .totalOrders(0)
                .cashSales(BigDecimal.ZERO)
                .cardSales(BigDecimal.ZERO)
                .upiSales(BigDecimal.ZERO)
                .walletSales(BigDecimal.ZERO)
                .creditSales(BigDecimal.ZERO)
                .complimentary(BigDecimal.ZERO)
                .totalRefund(BigDecimal.ZERO)
                .totalCgst(BigDecimal.ZERO)
                .totalSgst(BigDecimal.ZERO)
                .totalIgst(BigDecimal.ZERO)
                .openedAt(OffsetDateTime.now())
                .build();

        Shift saved = shiftRepo.save(shift);

        if (denominations != null) {
            saveDenominations(saved, denominations, true);
        }
        return saved;
    }

    @Transactional
    public Map<String, Object> closeShift(
            UUID shiftId,
            UUID tenantId,
            UUID userId,
            BigDecimal closingCash,
            DenominationDto denominations,
            String notes
    ) {
        Shift shift = shiftRepo.findByIdAndTenantIdAndStatus(shiftId, tenantId, ShiftStatus.open)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Open shift not found"));

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        BigDecimal opening = shift.getOpeningCash() != null ? shift.getOpeningCash() : BigDecimal.ZERO;
        BigDecimal cashSales = shift.getCashSales() != null ? shift.getCashSales() : BigDecimal.ZERO;
        BigDecimal refunds = shift.getTotalRefund() != null ? shift.getTotalRefund() : BigDecimal.ZERO;

        BigDecimal expectedCash = opening.add(cashSales).subtract(refunds);
        BigDecimal cashDiff = (closingCash != null ? closingCash : BigDecimal.ZERO).subtract(expectedCash);

        shift.setStatus(ShiftStatus.closed);
        shift.setClosedBy(user);
        shift.setClosingCash(closingCash != null ? closingCash : BigDecimal.ZERO);
        shift.setExpectedCash(expectedCash);
        shift.setCashDifference(cashDiff);
        shift.setClosedAt(OffsetDateTime.now());
        if (notes != null) shift.setNotes(notes);

        shiftRepo.save(shift);

        if (denominations != null) {
            saveDenominations(shift, denominations, false);
        }

        return getShiftSummary(shift.getId(), tenantId);
    }

    public Map<String, Object> getActiveShift(UUID branchId, UUID tenantId, ShiftDepartment department) {
        Optional<Shift> shiftOpt = shiftRepo.findActiveShift(branchId, tenantId, ShiftStatus.open, department);
        if (shiftOpt.isEmpty()) return null;
        return getShiftSummary(shiftOpt.get().getId(), tenantId);
    }

    public Map<String, Object> getShiftSummary(UUID shiftId, UUID tenantId) {
        Shift shift = shiftRepo.findByIdAndTenantId(shiftId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shift not found"));

        List<ShiftDenomination> denoms = denomRepo.findByShiftId(shiftId);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", shift.getId());
        summary.put("shiftNumber", shift.getShiftNumber());
        summary.put("department", shift.getDepartment());
        summary.put("status", shift.getStatus());
        summary.put("openingCash", shift.getOpeningCash());
        summary.put("closingCash", shift.getClosingCash());
        summary.put("expectedCash", shift.getExpectedCash());
        summary.put("cashDifference", shift.getCashDifference());
        summary.put("totalSales", shift.getTotalSales());
        summary.put("totalOrders", shift.getTotalOrders());
        summary.put("cashSales", shift.getCashSales());
        summary.put("cardSales", shift.getCardSales());
        summary.put("upiSales", shift.getUpiSales());
        summary.put("walletSales", shift.getWalletSales());
        summary.put("creditSales", shift.getCreditSales());
        summary.put("complimentary", shift.getComplimentary());
        summary.put("totalRefund", shift.getTotalRefund());
        summary.put("notes", shift.getNotes());
        summary.put("openedAt", shift.getOpenedAt());
        summary.put("closedAt", shift.getClosedAt());
        summary.put("denominations", denoms);
        summary.put("openedByUser", shift.getOpenedByUser());
        summary.put("closedByUser", shift.getClosedByUser());

        summary.put("paymentBreakdown", Map.of(
                "cash", shift.getCashSales() != null ? shift.getCashSales() : BigDecimal.ZERO,
                "card", shift.getCardSales() != null ? shift.getCardSales() : BigDecimal.ZERO,
                "upi", shift.getUpiSales() != null ? shift.getUpiSales() : BigDecimal.ZERO,
                "wallet", shift.getWalletSales() != null ? shift.getWalletSales() : BigDecimal.ZERO,
                "credit", shift.getCreditSales() != null ? shift.getCreditSales() : BigDecimal.ZERO,
                "complimentary", shift.getComplimentary() != null ? shift.getComplimentary() : BigDecimal.ZERO
        ));

        BigDecimal cgst = shift.getTotalCgst() != null ? shift.getTotalCgst() : BigDecimal.ZERO;
        BigDecimal sgst = shift.getTotalSgst() != null ? shift.getTotalSgst() : BigDecimal.ZERO;
        BigDecimal igst = shift.getTotalIgst() != null ? shift.getTotalIgst() : BigDecimal.ZERO;

        summary.put("gstBreakdown", Map.of(
                "cgst", cgst,
                "sgst", sgst,
                "igst", igst,
                "total", cgst.add(sgst).add(igst)
        ));

        return summary;
    }

    public List<Map<String, Object>> listShifts(
            UUID branchId,
            UUID tenantId,
            int limit,
            int offset,
            String status,
            String startDateStr,
            String endDateStr,
            String shiftNumber,
            ShiftDepartment department
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT
              s.id, s.shift_number, s.status, s.department, s.opening_cash, s.closing_cash,
              s.expected_cash, s.cash_difference, s.total_sales, s.total_orders, s.cash_sales,
              s.card_sales, s.upi_sales, s.wallet_sales, s.credit_sales, s.complimentary,
              s.total_refund, s.total_cgst, s.total_sgst, s.total_igst, s.notes, s.opened_at, s.closed_at,
              u1.first_name AS opened_first_name, u1.last_name AS opened_last_name, u1.role AS opened_role,
              u2.first_name AS closed_first_name, u2.last_name AS closed_last_name, u2.role AS closed_role
            FROM shifts s
            LEFT JOIN users u1 ON u1.id = s.opened_by
            LEFT JOIN users u2 ON u2.id = s.closed_by
            WHERE s.branch_id = ? AND s.tenant_id = ? AND s.department = ?
        """);

        List<Object> params = new ArrayList<>();
        params.add(branchId);
        params.add(tenantId);
        params.add(department.name());

        if (status != null && !status.isBlank()) {
            sql.append(" AND s.status = ?");
            params.add(status.toLowerCase());
        }
        if (shiftNumber != null && !shiftNumber.isBlank()) {
            sql.append(" AND s.shift_number ILIKE ?");
            params.add("%" + shiftNumber + "%");
        }
        if (startDateStr != null && !startDateStr.isBlank()) {
            sql.append(" AND s.opened_at >= ?");
            params.add(OffsetDateTime.parse(startDateStr));
        }
        if (endDateStr != null && !endDateStr.isBlank()) {
            sql.append(" AND s.opened_at <= ?");
            params.add(OffsetDateTime.parse(endDateStr));
        }

        sql.append(" ORDER BY s.opened_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", rs.getObject("id", UUID.class));
            map.put("shiftNumber", rs.getString("shift_number"));
            map.put("status", rs.getString("status"));
            map.put("department", rs.getString("department"));
            map.put("openingCash", rs.getBigDecimal("opening_cash"));
            map.put("closingCash", rs.getBigDecimal("closing_cash"));
            map.put("expectedCash", rs.getBigDecimal("expected_cash"));
            map.put("cashDifference", rs.getBigDecimal("cash_difference"));
            map.put("totalSales", rs.getBigDecimal("total_sales"));
            map.put("totalOrders", rs.getInt("total_orders"));
            map.put("cashSales", rs.getBigDecimal("cash_sales"));
            map.put("cardSales", rs.getBigDecimal("card_sales"));
            map.put("upiSales", rs.getBigDecimal("upi_sales"));
            map.put("walletSales", rs.getBigDecimal("wallet_sales"));
            map.put("creditSales", rs.getBigDecimal("credit_sales"));
            map.put("complimentary", rs.getBigDecimal("complimentary"));
            map.put("notes", rs.getString("notes"));
            map.put("openedAt", rs.getObject("opened_at", OffsetDateTime.class));
            map.put("closedAt", rs.getObject("closed_at", OffsetDateTime.class));

            String opFirst = rs.getString("opened_first_name");
            String opLast = rs.getString("opened_last_name");
            if (opFirst != null) {
                map.put("openedByUser", Map.of("fullName", (opFirst + " " + (opLast != null ? opLast : "")).trim(), "role", rs.getString("opened_role") != null ? rs.getString("opened_role") : ""));
            } else { map.put("openedByUser", null); }

            String clFirst = rs.getString("closed_first_name");
            String clLast = rs.getString("closed_last_name");
            if (clFirst != null) {
                map.put("closedByUser", Map.of("fullName", (clFirst + " " + (clLast != null ? clLast : "")).trim(), "role", rs.getString("closed_role") != null ? rs.getString("closed_role") : ""));
            } else { map.put("closedByUser", null); }

            return map;
        }, params.toArray());
    }

    public Map<String, Object> getShiftStats(
            UUID branchId,
            UUID tenantId,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            ShiftDepartment department
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT
              COUNT(*)::int AS total_shifts,
              COALESCE(SUM(total_sales), 0) AS total_sales,
              COALESCE(SUM(cash_difference), 0) AS total_cash_difference,
              COALESCE(AVG(total_sales), 0) AS avg_shift_value
            FROM shifts
            WHERE branch_id = ? AND tenant_id = ? AND department = ? AND status = 'closed'
        """);
        List<Object> params = new ArrayList<>();
        params.add(branchId);
        params.add(tenantId);
        params.add(department.name());

        if (startDate != null) {
            sql.append(" AND closed_at >= ?");
            params.add(startDate);
        }
        if (endDate != null) {
            sql.append(" AND closed_at <= ?");
            params.add(endDate);
        }

        Map<String, Object> row = jdbcTemplate.queryForMap(sql.toString(), params.toArray());
        return Map.of(
                "totalShifts", row.getOrDefault("total_shifts", 0),
                "totalSales", row.getOrDefault("total_sales", 0),
                "totalCashDifference", row.getOrDefault("total_cash_difference", 0),
                "averageShiftValue", row.getOrDefault("avg_shift_value", 0)
        );
    }

    private void saveDenominations(Shift shift, DenominationDto d, boolean isOpening) {
        ShiftDenomination denom = ShiftDenomination.builder()
                .shift(shift)
                .isOpening(isOpening)
                .note2000(d.getNote2000() != null ? d.getNote2000() : 0)
                .note500(d.getNote500() != null ? d.getNote500() : 0)
                .note200(d.getNote200() != null ? d.getNote200() : 0)
                .note100(d.getNote100() != null ? d.getNote100() : 0)
                .note50(d.getNote50() != null ? d.getNote50() : 0)
                .note20(d.getNote20() != null ? d.getNote20() : 0)
                .note10(d.getNote10() != null ? d.getNote10() : 0)
                .coin5(d.getCoin5() != null ? d.getCoin5() : 0)
                .coin2(d.getCoin2() != null ? d.getCoin2() : 0)
                .coin1(d.getCoin1() != null ? d.getCoin1() : 0)
                .build();
        denomRepo.save(denom);
    }
}