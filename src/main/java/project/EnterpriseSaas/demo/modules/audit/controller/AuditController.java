package project.EnterpriseSaas.demo.modules.audit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import project.EnterpriseSaas.demo.common.dto.ApiResponse;
import project.EnterpriseSaas.demo.modules.audit.dto.AuditQueryDto;
import project.EnterpriseSaas.demo.modules.audit.entity.AuditLog;
import project.EnterpriseSaas.demo.modules.audit.service.AuditService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/tenant")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTenantLogs(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @Valid @ModelAttribute AuditQueryDto query
    ) {
        return ResponseEntity.ok(ApiResponse.ok(auditService.findByTenant(tenantId, query)));
    }

    @GetMapping("/entity/{entity}/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getEntityLogs(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @PathVariable("entity") String entity,
            @PathVariable("id") String id
    ) {
        return ResponseEntity.ok(ApiResponse.ok(auditService.findByEntity(entity, id, tenantId)));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getUserLogs(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @PathVariable("userId") UUID userId,
            @RequestParam(value = "limit", defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.ok(auditService.findByUser(userId, tenantId, limit)));
    }
}