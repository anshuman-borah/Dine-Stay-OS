package project.EnterpriseSaas.demo.modules.reports.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import project.EnterpriseSaas.demo.common.dto.ApiResponse;
import project.EnterpriseSaas.demo.modules.reports.service.ReportsService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportsController {

    private final ReportsService reportsService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('CASHIER', 'MANAGER', 'OWNER', 'RESTAURANT_MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader(value = "x-branch-id", required = false) UUID branchId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reportsService.getDashboardSummary(branchId, tenantId)));
    }

    @GetMapping("/hotel-dashboard")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER', 'HOTEL_MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> hotelDashboard(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader(value = "x-branch-id", required = false) UUID branchId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reportsService.getHotelDashboardSummary(branchId, tenantId)));
    }

    @GetMapping("/owner-dashboard")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ownerDashboard(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader(value = "x-branch-id", required = false) UUID branchId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reportsService.getOwnerDashboardSummary(branchId, tenantId)));
    }

    @GetMapping("/hourly")
    @PreAuthorize("hasAnyRole('CASHIER', 'MANAGER', 'OWNER', 'RESTAURANT_MANAGER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> hourly(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader(value = "x-branch-id", required = false) UUID branchId,
            @RequestParam("date") String date
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reportsService.getHourlyReport(branchId, tenantId, date)));
    }

    @GetMapping("/daily-sales")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER', 'RESTAURANT_MANAGER', 'HOTEL_MANAGER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> dailySales(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader(value = "x-branch-id", required = false) UUID branchId,
            @RequestParam("from") String from,
            @RequestParam("to") String to
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reportsService.getDailySales(branchId, tenantId, from, to)));
    }

    @GetMapping("/items")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER', 'RESTAURANT_MANAGER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> itemSales(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader(value = "x-branch-id", required = false) UUID branchId,
            @RequestParam("from") String from,
            @RequestParam("to") String to
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reportsService.getItemSalesReport(branchId, tenantId, from, to)));
    }

    @GetMapping("/payments")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER', 'RESTAURANT_MANAGER', 'HOTEL_MANAGER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> payments(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader(value = "x-branch-id", required = false) UUID branchId,
            @RequestParam("from") String from,
            @RequestParam("to") String to
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reportsService.getPaymentMethodReport(branchId, tenantId, from, to)));
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER', 'RESTAURANT_MANAGER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> categories(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader(value = "x-branch-id", required = false) UUID branchId,
            @RequestParam("from") String from,
            @RequestParam("to") String to
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reportsService.getCategoryReport(branchId, tenantId, from, to)));
    }

    @GetMapping("/gst")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> gst(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader(value = "x-branch-id", required = false) UUID branchId,
            @RequestParam("from") String from,
            @RequestParam("to") String to
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reportsService.getGstReport(branchId, tenantId, from, to)));
    }

    @GetMapping("/gstr1-export")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<Map<String, Object>> gstr1Export(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader(value = "x-branch-id", required = false) UUID branchId,
            @RequestParam("from") String from,
            @RequestParam("to") String to
    ) {
        Map<String, Object> data = reportsService.getGstr1Export(branchId, tenantId, from, to);
        return ResponseEntity.ok()
                .header("Content-Disposition", String.format("attachment; filename=\"GSTR1_%s_%s.json\"", from, to))
                .body(data);
    }

    @GetMapping("/shifts")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> shifts(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader(value = "x-branch-id", required = false) UUID branchId,
            @RequestParam("from") String from,
            @RequestParam("to") String to
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reportsService.getShiftReport(branchId, tenantId, from, to)));
    }

    @GetMapping("/waiters")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> waiters(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader(value = "x-branch-id", required = false) UUID branchId,
            @RequestParam("from") String from,
            @RequestParam("to") String to
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reportsService.getWaiterReport(branchId, tenantId, from, to)));
    }

    @GetMapping("/branch-performance")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> branchPerformance(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestParam("from") String from,
            @RequestParam("to") String to
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reportsService.getBranchPerformance(tenantId, from, to)));
    }

    @GetMapping("/branch-summary")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'RESTAURANT_MANAGER', 'HOTEL_MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> branchSummary(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader(value = "x-branch-id", required = false) UUID branchId,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reportsService.getBranchSummary(branchId, tenantId, from, to)));
    }
}