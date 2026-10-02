package project.EnterpriseSaas.demo.modules.inventory.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import project.EnterpriseSaas.demo.common.dto.ApiResponse;
import project.EnterpriseSaas.demo.modules.inventory.dto.CreateInventoryItemDto;
import project.EnterpriseSaas.demo.modules.inventory.dto.InventoryTransactionDto;
import project.EnterpriseSaas.demo.modules.inventory.dto.UpdateInventoryItemDto;
import project.EnterpriseSaas.demo.modules.inventory.entity.InventoryItem;
import project.EnterpriseSaas.demo.modules.inventory.entity.InventoryTransaction;
import project.EnterpriseSaas.demo.modules.inventory.service.InventoryService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/items")
    @PreAuthorize("hasAnyRole('INVENTORY', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<List<InventoryItem>>> getItems(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader(value = "x-branch-id", required = false) UUID branchId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getItems(tenantId, branchId)));
    }

    @GetMapping("/items/{id}")
    @PreAuthorize("hasAnyRole('INVENTORY', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<InventoryItem>> getItem(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getItem(id, tenantId)));
    }

    @GetMapping("/items/{id}/ledger")
    @PreAuthorize("hasAnyRole('INVENTORY', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<List<InventoryTransaction>>> getLedger(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestParam(value = "limit", required = false, defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getLedger(id, tenantId, limit)));
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('INVENTORY', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<List<InventoryItem>>> getLowStock(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader(value = "x-branch-id", required = false) UUID branchId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getLowStockAlerts(tenantId, branchId)));
    }

    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<InventoryItem>> createItem(
            @Valid @RequestBody CreateInventoryItemDto dto,
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.createItem(dto, tenantId, branchId)));
    }

    @PutMapping("/items/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<InventoryItem>> updateItem(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInventoryItemDto dto,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.updateItem(id, tenantId, dto)));
    }

    @PostMapping("/transactions")
    @PreAuthorize("hasAnyRole('INVENTORY', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<InventoryTransaction>> recordTxn(
            @Valid @RequestBody InventoryTransactionDto dto,
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.recordTransaction(tenantId, branchId, dto, userId)));
    }
}