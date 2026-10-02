package project.EnterpriseSaas.demo.modules.tenant.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import project.EnterpriseSaas.demo.common.dto.ApiResponse;
import project.EnterpriseSaas.demo.modules.tenant.dto.RazorpayKeysDto;
import project.EnterpriseSaas.demo.modules.tenant.dto.UpdateTenantDto;
import project.EnterpriseSaas.demo.modules.tenant.entity.Tenant;
import project.EnterpriseSaas.demo.modules.tenant.service.TenantService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenant")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
    @PreAuthorize("hasAnyRole('WAITER', 'CASHIER', 'KITCHEN', 'INVENTORY', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<Tenant>> getMe(@RequestHeader("x-tenant-id") UUID tenantId) {
        Tenant tenant = tenantService.findById(tenantId);
        return ResponseEntity.ok(ApiResponse.ok(tenant));
    }

    @PutMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<Tenant>> update(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestBody UpdateTenantDto dto
    ) {
        Tenant updated = tenantService.update(tenantId, dto);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    // ── Razorpay Integration ──────────────────────────────────────────────────

    @GetMapping("/razorpay")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRazorpay(@RequestHeader("x-tenant-id") UUID tenantId) {
        Map<String, Object> status = tenantService.getRazorpayStatus(tenantId);
        return ResponseEntity.ok(ApiResponse.ok(status));
    }

    @PostMapping("/razorpay")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveRazorpay(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @Valid @RequestBody RazorpayKeysDto dto
    ) {
        Map<String, Object> result = tenantService.saveRazorpayKeys(tenantId, dto.getKeyId(), dto.getKeySecret());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @DeleteMapping("/razorpay")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> disconnectRazorpay(@RequestHeader("x-tenant-id") UUID tenantId) {
        Map<String, Object> result = tenantService.disconnectRazorpay(tenantId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}