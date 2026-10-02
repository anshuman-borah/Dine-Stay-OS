package project.EnterpriseSaas.demo.modules.shift.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import project.EnterpriseSaas.demo.common.dto.ApiResponse;
import project.EnterpriseSaas.demo.common.enums.ShiftDepartment;
import project.EnterpriseSaas.demo.modules.shift.dto.CloseShiftDto;
import project.EnterpriseSaas.demo.modules.shift.dto.OpenShiftDto;
import project.EnterpriseSaas.demo.modules.shift.entity.Shift;
import project.EnterpriseSaas.demo.modules.shift.service.ShiftService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shifts")
@RequiredArgsConstructor
public class ShiftsController {

    private final ShiftService shiftService;

    @PostMapping("/open")
    @PreAuthorize("hasAnyRole('CASHIER', 'RESTAURANT_MANAGER', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<Shift>> openShift(
            @Valid @RequestBody OpenShiftDto dto,
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        Shift shift = shiftService.openShift(branchId, tenantId, userId, dto.getOpeningCash(), dto.getDenominations(), ShiftDepartment.restaurant);
        return ResponseEntity.ok(ApiResponse.ok(shift));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('CASHIER', 'RESTAURANT_MANAGER', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> closeShift(
            @PathVariable UUID id,
            @Valid @RequestBody CloseShiftDto dto,
            @RequestHeader("x-tenant-id") UUID tenantId,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        Map<String, Object> summary = shiftService.closeShift(id, tenantId, userId, dto.getClosingCash(), dto.getDenominations(), dto.getNotes());
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('CASHIER', 'WAITER', 'KITCHEN', 'INVENTORY', 'RESTAURANT_MANAGER', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrent(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(shiftService.getActiveShift(branchId, tenantId, ShiftDepartment.restaurant)));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('CASHIER', 'WAITER', 'KITCHEN', 'INVENTORY', 'RESTAURANT_MANAGER', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActive(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(shiftService.getActiveShift(branchId, tenantId, ShiftDepartment.restaurant)));
    }

    @GetMapping("/stats/summary")
    @PreAuthorize("hasAnyRole('CASHIER', 'RESTAURANT_MANAGER', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate
    ) {
        OffsetDateTime start = startDate != null ? OffsetDateTime.parse(startDate) : null;
        OffsetDateTime end = endDate != null ? OffsetDateTime.parse(endDate) : null;
        Map<String, Object> stats = shiftService.getShiftStats(branchId, tenantId, start, end, ShiftDepartment.restaurant);
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @GetMapping("/{id}/summary")
    @PreAuthorize("hasAnyRole('CASHIER', 'RESTAURANT_MANAGER', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(shiftService.getShiftSummary(id, tenantId)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CASHIER', 'RESTAURANT_MANAGER', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> list(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId,
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit,
            @RequestParam(value = "offset", required = false, defaultValue = "0") int offset,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "shiftNumber", required = false) String shiftNumber
    ) {
        List<Map<String, Object>> list = shiftService.listShifts(branchId, tenantId, limit, offset, status, startDate, endDate, shiftNumber, ShiftDepartment.restaurant);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }
}