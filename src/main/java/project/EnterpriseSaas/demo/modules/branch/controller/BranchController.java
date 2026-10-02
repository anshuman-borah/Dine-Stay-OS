package project.EnterpriseSaas.demo.modules.branch.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import project.EnterpriseSaas.demo.common.dto.ApiResponse;
import project.EnterpriseSaas.demo.modules.branch.dto.BranchDto;
import project.EnterpriseSaas.demo.modules.branch.entity.Branch;
import project.EnterpriseSaas.demo.modules.branch.service.BranchService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Branch>>> findAll(@RequestHeader("x-tenant-id") UUID tenantId) {
        List<Branch> branches = branchService.findAll(tenantId);
        return ResponseEntity.ok(ApiResponse.ok(branches));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Branch>> findOne(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        Branch branch = branchService.findOne(id, tenantId);
        return ResponseEntity.ok(ApiResponse.ok(branch));
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<Branch>> create(
            @Valid @RequestBody BranchDto dto,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        Branch created = branchService.create(dto, tenantId);
        return ResponseEntity.ok(ApiResponse.ok(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Branch>> update(
            @PathVariable UUID id,
            @Valid @RequestBody BranchDto dto,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        Branch updated = branchService.update(id, tenantId, dto);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }
}