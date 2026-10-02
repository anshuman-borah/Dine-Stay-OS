package project.EnterpriseSaas.demo.modules.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import project.EnterpriseSaas.demo.common.dto.ApiResponse;
import project.EnterpriseSaas.demo.modules.user.dto.CreateUserDto;
import project.EnterpriseSaas.demo.modules.user.dto.UpdateUserDto;
import project.EnterpriseSaas.demo.modules.user.service.UserService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'RESTAURANT_MANAGER', 'HOTEL_MANAGER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> findAll(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader(value = "x-branch-id", required = false) UUID branchId
    ) {
        List<Map<String, Object>> users = userService.findAll(tenantId, branchId);
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> findOne(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userService.sanitizeUser(userService.findOne(id, tenantId))));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'RESTAURANT_MANAGER', 'HOTEL_MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(
            @Valid @RequestBody CreateUserDto dto,
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader(value = "x-branch-id", required = false) UUID branchId,
            Authentication authentication
    ) {
        String actorRole = getActorRole(authentication);
        validateRoleHierarchy(actorRole, dto.getRole().name());

        UUID finalBranchId = ("owner".equalsIgnoreCase(actorRole) && dto.getBranchId() != null)
                ? dto.getBranchId()
                : branchId;

        Map<String, Object> user = userService.create(dto, tenantId, finalBranchId);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'RESTAURANT_MANAGER', 'HOTEL_MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserDto dto,
            @RequestHeader("x-tenant-id") UUID tenantId,
            Authentication authentication
    ) {
        String actorRole = getActorRole(authentication);
        if (dto.getRole() != null) {
            validateRoleHierarchy(actorRole, dto.getRole().name());
        }
        if (dto.getBranchId() != null && !"owner".equalsIgnoreCase(actorRole)) {
            dto.setBranchId(null);
        }

        Map<String, Object> user = userService.update(id, tenantId, dto);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> remove(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        Map<String, Object> result = userService.permanentDelete(id, tenantId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PatchMapping("/{id}/permissions")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'RESTAURANT_MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updatePermissions(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        Object permissionsObj = body.get("permissions");
        if (!(permissionsObj instanceof Map<?, ?> rawMap)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No valid permission keys provided");
        }

        Map<String, Object> filtered = new HashMap<>();
        if (rawMap.containsKey("canManageTables")) {
            filtered.put("canManageTables", Boolean.TRUE.equals(rawMap.get("canManageTables")));
        }

        if (filtered.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No valid permission keys provided");
        }

        Map<String, Object> user = userService.updatePermissions(id, tenantId, filtered);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    // ── Hierarchy Validation ─────────────────────────────────────────────────

    private void validateRoleHierarchy(String actorRole, String targetRole) {
        actorRole = actorRole.toLowerCase();
        targetRole = targetRole.toLowerCase();

        if (List.of("owner", "manager").contains(targetRole) && !"owner".equals(actorRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owners can create Branch Managers or Owners");
        }
        if (List.of("restaurant_manager", "hotel_manager").contains(targetRole) && !List.of("owner", "manager").contains(actorRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Owners and Branch Managers can create Department Managers");
        }
        if ("restaurant_manager".equals(actorRole) && !List.of("cashier", "waiter", "kitchen", "inventory").contains(targetRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Restaurant Managers can only create restaurant staff");
        }
        if ("hotel_manager".equals(actorRole) && !List.of("receptionist", "housekeeping").contains(targetRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Hotel Managers can only create hotel staff");
        }
    }

    private String getActorRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "").toLowerCase())
                .orElse("cashier");
    }
}