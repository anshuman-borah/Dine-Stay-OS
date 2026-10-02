package project.EnterpriseSaas.demo.modules.kds.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import project.EnterpriseSaas.demo.common.dto.ApiResponse;
import project.EnterpriseSaas.demo.common.enums.KdsStatus;
import project.EnterpriseSaas.demo.modules.kds.dto.KdsPendingItemDto;
import project.EnterpriseSaas.demo.modules.kds.service.KdsService;
import project.EnterpriseSaas.demo.modules.order.entity.OrderItem;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kds")
@RequiredArgsConstructor
public class KdsController {

    private final KdsService kdsService;

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('KITCHEN', 'CASHIER', 'WAITER', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<List<KdsPendingItemDto>>> getPending(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(kdsService.getPendingItems(branchId, tenantId)));
    }

    @PatchMapping("/items/{id}/status")
    @PreAuthorize("hasAnyRole('KITCHEN', 'CASHIER', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<OrderItem>> updateStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        KdsStatus status = KdsStatus.valueOf(body.get("status").toLowerCase());
        return ResponseEntity.ok(ApiResponse.ok(kdsService.updateItemStatus(id, status, tenantId)));
    }

    @PatchMapping("/items/{id}/bump")
    @PreAuthorize("hasAnyRole('KITCHEN', 'CASHIER', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<OrderItem>> bump(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(kdsService.bumpItem(id, tenantId)));
    }

    @PatchMapping("/orders/{orderId}/bump")
    @PreAuthorize("hasAnyRole('KITCHEN', 'CASHIER', 'WAITER', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bumpOrder(
            @PathVariable UUID orderId,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(kdsService.bumpOrderItems(orderId, tenantId)));
    }

    @PatchMapping("/items/{id}/recall")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<OrderItem>> recall(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(kdsService.recallItem(id, tenantId)));
    }
}