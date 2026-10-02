package project.EnterpriseSaas.demo.modules.subscription.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import project.EnterpriseSaas.demo.common.dto.ApiResponse;
import project.EnterpriseSaas.demo.modules.plan.entity.Plan;
import project.EnterpriseSaas.demo.modules.subscription.entity.Subscription;
import project.EnterpriseSaas.demo.modules.subscription.service.SubscriptionService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subService;

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<Plan>>> getPlans() {
        return ResponseEntity.ok(ApiResponse.ok(subService.getPlans()));
    }

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Subscription>> getCurrent(@RequestHeader("x-tenant-id") UUID tenantId) {
        return ResponseEntity.ok(ApiResponse.ok(subService.getSubscription(tenantId)));
    }

    @GetMapping("/limits")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLimits(@RequestHeader("x-tenant-id") UUID tenantId) {
        // Safe for all authenticated roles so the UI can lock features based on plan limits
        return ResponseEntity.ok(ApiResponse.ok(subService.checkLimits(tenantId)));
    }
}