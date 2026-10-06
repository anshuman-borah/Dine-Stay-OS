package project.EnterpriseSaas.demo.modules.table.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import project.EnterpriseSaas.demo.common.dto.ApiResponse;
import project.EnterpriseSaas.demo.modules.table.dto.CreateSectionDto;
import project.EnterpriseSaas.demo.modules.table.dto.CreateTableDto;
import project.EnterpriseSaas.demo.modules.table.dto.UpdateTableDto;
import project.EnterpriseSaas.demo.modules.table.entity.Table;
import project.EnterpriseSaas.demo.modules.table.entity.TableSection;
import project.EnterpriseSaas.demo.modules.table.service.TableService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tables")
@RequiredArgsConstructor
public class TableController {

    private final TableService tableService;

    // ── Sections ─────────────────────────────────────────────────────────────

    @GetMapping("/sections")
    @PreAuthorize("hasAnyRole('WAITER', 'CASHIER', 'KITCHEN', 'INVENTORY', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<List<TableSection>>> listSections(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tableService.findAllSections(branchId, tenantId)));
    }

    @PostMapping("/sections")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER', 'RESTAURANT_MANAGER', 'WAITER')")
    public ResponseEntity<ApiResponse<TableSection>> createSection(
            @Valid @RequestBody CreateSectionDto dto,
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tableService.createSection(dto, tenantId, branchId)));
    }

    @PutMapping("/sections/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER', 'RESTAURANT_MANAGER', 'WAITER')")
    public ResponseEntity<ApiResponse<TableSection>> updateSection(
            @PathVariable UUID id,
            @Valid @RequestBody CreateSectionDto dto,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tableService.updateSection(id, tenantId, dto)));
    }

    @DeleteMapping("/sections/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER', 'RESTAURANT_MANAGER', 'WAITER')")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> removeSection(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tableService.removeSection(id, tenantId)));
    }

    // ── Tables ───────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('WAITER', 'CASHIER', 'KITCHEN', 'INVENTORY', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<List<Table>>> findAll(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tableService.findAll(branchId, tenantId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('WAITER', 'CASHIER', 'KITCHEN', 'INVENTORY', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<Table>> findOne(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tableService.findOne(id, tenantId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER', 'RESTAURANT_MANAGER', 'WAITER')")
    public ResponseEntity<ApiResponse<Table>> create(
            @Valid @RequestBody CreateTableDto dto,
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tableService.create(dto, tenantId, branchId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('WAITER', 'CASHIER', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<Table>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTableDto dto,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tableService.update(id, tenantId, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER', 'RESTAURANT_MANAGER', 'WAITER')")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> remove(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tableService.remove(id, tenantId)));
    }
}