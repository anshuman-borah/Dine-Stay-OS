package project.EnterpriseSaas.demo.modules.menu.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import project.EnterpriseSaas.demo.common.dto.ApiResponse;
import project.EnterpriseSaas.demo.modules.billing.entity.GstRate;
import project.EnterpriseSaas.demo.modules.menu.dto.CreateCategoryDto;
import project.EnterpriseSaas.demo.modules.menu.dto.CreateMenuItemDto;
import project.EnterpriseSaas.demo.modules.menu.dto.UpdateCategoryDto;
import project.EnterpriseSaas.demo.modules.menu.dto.UpdateMenuItemDto;
import project.EnterpriseSaas.demo.modules.menu.entity.Category;
import project.EnterpriseSaas.demo.modules.menu.entity.MenuItem;
import project.EnterpriseSaas.demo.modules.menu.entity.MenuItemVariation;
import project.EnterpriseSaas.demo.modules.menu.service.MenuService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    // ── Categories ───────────────────────────────────────────────────────────

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('WAITER', 'CASHIER', 'KITCHEN', 'INVENTORY', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<List<Category>>> getCategories(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader(value = "x-branch-id", required = false) UUID branchId
    ) {
        List<Category> categories = menuService.getCategories(tenantId, branchId);
        return ResponseEntity.ok(ApiResponse.ok(categories));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<Category>> createCategory(
            @Valid @RequestBody CreateCategoryDto dto,
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader(value = "x-branch-id", required = false) UUID branchId
    ) {
        Category category = menuService.createCategory(tenantId, branchId, dto);
        return ResponseEntity.ok(ApiResponse.ok(category));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<Category>> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryDto dto,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        Category updated = menuService.updateCategory(id, tenantId, dto);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> removeCategory(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.removeCategory(id, tenantId)));
    }

    // ── Items ────────────────────────────────────────────────────────────────

    @GetMapping("/items")
    @PreAuthorize("hasAnyRole('WAITER', 'CASHIER', 'KITCHEN', 'INVENTORY', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<List<MenuItem>>> getItems(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader(value = "x-branch-id", required = false) UUID branchId,
            @RequestParam(value = "categoryId", required = false) UUID categoryId
    ) {
        List<MenuItem> items = menuService.getItems(tenantId, branchId, categoryId);
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    @GetMapping("/items/{id}")
    @PreAuthorize("hasAnyRole('WAITER', 'CASHIER', 'KITCHEN', 'INVENTORY', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<MenuItem>> getItem(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.getItem(id, tenantId)));
    }

    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<MenuItem>> createItem(
            @Valid @RequestBody CreateMenuItemDto dto,
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader(value = "x-branch-id", required = false) UUID branchId
    ) {
        MenuItem item = menuService.createItem(tenantId, branchId, dto);
        return ResponseEntity.ok(ApiResponse.ok(item));
    }

    @PutMapping("/items/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<MenuItem>> updateItem(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMenuItemDto dto,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        MenuItem updated = menuService.updateItem(id, tenantId, dto);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    @DeleteMapping("/items/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> removeItem(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.removeItem(id, tenantId)));
    }

    // ── Variations ───────────────────────────────────────────────────────────

    @GetMapping("/items/{id}/variations")
    @PreAuthorize("hasAnyRole('WAITER', 'CASHIER', 'KITCHEN', 'INVENTORY', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<List<MenuItemVariation>>> getVariations(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.getVariations(id, tenantId)));
    }

    @PostMapping("/items/{id}/variations")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<MenuItemVariation>> createVariation(
            @PathVariable UUID id,
            @RequestBody MenuItemVariation body,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.createVariation(id, tenantId, body)));
    }

    @PutMapping("/items/{itemId}/variations/{varId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<MenuItemVariation>> updateVariation(
            @PathVariable UUID varId,
            @RequestBody MenuItemVariation body,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.updateVariation(varId, tenantId, body)));
    }

    @DeleteMapping("/items/{itemId}/variations/{varId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> removeVariation(
            @PathVariable UUID varId,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.removeVariation(varId, tenantId)));
    }

    // ── GST Rates ────────────────────────────────────────────────────────────

    @GetMapping("/gst-rates")
    @PreAuthorize("hasAnyRole('CASHIER', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<List<GstRate>>> getGstRates(@RequestHeader("x-tenant-id") UUID tenantId) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.getGstRates(tenantId)));
    }

    @PostMapping("/gst-rates")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<GstRate>> createGstRate(
            @RequestBody GstRate body,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.createGstRate(tenantId, body)));
    }

    @PutMapping("/gst-rates/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<GstRate>> updateGstRate(
            @PathVariable UUID id,
            @RequestBody GstRate body,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.updateGstRate(id, tenantId, body)));
    }

    @DeleteMapping("/gst-rates/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> removeGstRate(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.removeGstRate(id, tenantId)));
    }

    @PostMapping("/gst-rates/seed")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> seedGstRates(@RequestHeader("x-tenant-id") UUID tenantId) {
        return ResponseEntity.ok(ApiResponse.ok(menuService.seedDefaultGstRates(tenantId)));
    }
}