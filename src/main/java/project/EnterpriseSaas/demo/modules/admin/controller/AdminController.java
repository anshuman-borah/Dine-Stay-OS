package project.EnterpriseSaas.demo.modules.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import project.EnterpriseSaas.demo.common.dto.ApiResponse;
import project.EnterpriseSaas.demo.modules.admin.service.AdminService;
import project.EnterpriseSaas.demo.modules.plan.entity.Plan;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPERADMIN')") // REVERTED BACK TO UPPERCASE!
public class AdminController {

    private final AdminService adminService;

    // ── Platform KPI Stats ───────────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getStats()));
    }

    // ── Paginated Tenants List ───────────────────────────────────────────────

    @GetMapping("/tenants")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listTenants(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "search", required = false) String search
    ) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listTenants(page, limit, search)));
    }

    // ── Tenant Detail ────────────────────────────────────────────────────────

    @GetMapping("/tenants/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTenant(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getTenant(id)));
    }

    // ── Tenant Suspensions / Activations ─────────────────────────────────────

    @PatchMapping("/tenants/{id}/suspend")
    public ResponseEntity<ApiResponse<Map<String, Object>>> suspend(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.setTenantActive(id, false)));
    }

    @PatchMapping("/tenants/{id}/activate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.setTenantActive(id, true)));
    }

    // ── Plan Override ────────────────────────────────────────────────────────

    @PatchMapping("/tenants/{id}/plan")
    public ResponseEntity<ApiResponse<Map<String, Object>>> changePlan(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body
    ) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.changePlan(id, body.get("planCode"), body.get("status"))));
    }

    // ── Subscriptions Overview ───────────────────────────────────────────────

    @GetMapping("/subscriptions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSubscriptions(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "25") int limit,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "search", required = false) String search
    ) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getSubscriptions(page, limit, status, search)));
    }

    // ── Create Tenant ────────────────────────────────────────────────────────

    @PostMapping("/tenants")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createTenant(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.createTenant(body)));
    }

    // ── Delete Tenant ────────────────────────────────────────────────────────

    @DeleteMapping("/tenants/{id}")
    public ResponseEntity<Void> deleteTenant(@PathVariable UUID id) {
        adminService.deleteTenant(id);
        return ResponseEntity.noContent().build();
    }

    // ── Platform-wide Activity Feed ──────────────────────────────────────────

    @GetMapping("/activity")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActivity(
            @RequestParam(value = "limit", defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getRecentActivity(limit)));
    }

    // ── Chart Trends ─────────────────────────────────────────────────────────

    @GetMapping("/charts/orders")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getOrdersTrend() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getOrdersTrend()));
    }

    @GetMapping("/charts/signups")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSignupsTrend() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getSignupsTrend()));
    }

    // ── Plan Management ──────────────────────────────────────────────────────

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<Plan>>> listPlans() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.listPlans()));
    }

    @PostMapping("/plans")
    public ResponseEntity<ApiResponse<Plan>> createPlan(@RequestBody Plan plan) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.createPlan(plan)));
    }

    @PatchMapping("/plans/{id}")
    public ResponseEntity<ApiResponse<Plan>> updatePlan(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.updatePlan(id, body)));
    }

    @DeleteMapping("/plans/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deletePlan(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.deletePlan(id)));
    }

    // ── Superadmin Platform Settings ─────────────────────────────────────────

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSettings() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getSettings()));
    }

    @PatchMapping("/settings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateSettings(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.updateSettings(body)));
    }
}