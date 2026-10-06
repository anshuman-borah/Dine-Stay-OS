package project.EnterpriseSaas.demo.modules.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.EnterpriseSaas.demo.common.enums.BranchType;
import project.EnterpriseSaas.demo.common.enums.UserRole;
import project.EnterpriseSaas.demo.modules.branch.entity.Branch;
import project.EnterpriseSaas.demo.modules.branch.repository.BranchRepository;
import project.EnterpriseSaas.demo.modules.tenant.entity.Tenant;
import project.EnterpriseSaas.demo.modules.tenant.repository.TenantRepository;
import project.EnterpriseSaas.demo.modules.user.dto.CreateUserDto;
import project.EnterpriseSaas.demo.modules.user.dto.UpdateUserDto;
import project.EnterpriseSaas.demo.modules.user.entity.User;
import project.EnterpriseSaas.demo.modules.user.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepo;
    private final TenantRepository tenantRepo;
    private final BranchRepository branchRepo;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate; // 🟢 Added to handle direct database operations

    // ── Find All Users ───────────────────────────────────────────────────────

    public List<Map<String, Object>> findAll(UUID tenantId, UUID branchId) {
        List<User> users;
        if (branchId != null) {
            users = userRepo.findBranchStaffAndManagers(tenantId, branchId);
        } else {
            users = userRepo.findByTenant_IdAndIsActiveTrueOrderByCreatedAtDesc(tenantId);
        }
        return users.stream().map(this::sanitizeUser).toList();
    }

    // ── Find Single User ─────────────────────────────────────────────────────

    public User findOne(UUID id, UUID tenantId) {
        return userRepo.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    // ── Create User ──────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> create(CreateUserDto dto, UUID tenantId, UUID branchId) {
        if (branchId == null && dto.getRole() != UserRole.owner) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A specific branch must be assigned for the " + dto.getRole() + " role"
            );
        }

        userRepo.findByEmailOrPhoneWithinTenant(tenantId, dto.getEmail(), dto.getPhone())
                .ifPresent(u -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists with this email/phone");
                });

        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

        Branch branch = null;
        if (branchId != null) {
            branch = branchRepo.findById(branchId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
        }

        String passwordHash = passwordEncoder.encode(dto.getPassword());
        String pinHash = dto.getPin() != null ? passwordEncoder.encode(dto.getPin()) : null;

        User user = User.builder()
                .tenant(tenant)
                .branch(branch)
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .passwordHash(passwordHash)
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .role(dto.getRole())
                .employeeCode(dto.getEmployeeCode())
                .pin(pinHash)
                .isActive(true)
                .settings(new HashMap<>())
                .permissions(new HashMap<>())
                .build();

        user = userRepo.save(user);

        log.info("Created user {} ({}) for tenant {}", user.getId(), user.getRole(), tenantId);

        return sanitizeUser(user);
    }

    // ── Update User ──────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> update(UUID id, UUID tenantId, UpdateUserDto dto) {
        User user = findOne(id, tenantId);

        if (user.getRole() == UserRole.owner && dto.getRole() != null && dto.getRole() != UserRole.owner) {
            long ownerCount = userRepo.countByTenant_IdAndRoleAndIsActiveTrue(tenantId, UserRole.owner);
            if (ownerCount <= 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot change the role of the last active owner");
            }
        }

        UUID targetBranchId = dto.getBranchId() != null
                ? dto.getBranchId()
                : (user.getBranch() != null ? user.getBranch().getId() : null);
        UserRole targetRole = dto.getRole() != null ? dto.getRole() : user.getRole();

        if (targetBranchId == null && targetRole != UserRole.owner) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A specific branch must be assigned for the " + targetRole + " role"
            );
        }

        if (dto.getBranchId() != null) {
            Branch branch = branchRepo.findById(dto.getBranchId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
            user.setBranch(branch);
        }

        if (dto.getFirstName() != null) user.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) user.setLastName(dto.getLastName());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getRole() != null) user.setRole(dto.getRole());
        if (dto.getEmployeeCode() != null) user.setEmployeeCode(dto.getEmployeeCode());
        if (dto.getIsActive() != null) user.setIsActive(dto.getIsActive());

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }

        if (dto.getPin() != null && !dto.getPin().isBlank()) {
            user.setPin(passwordEncoder.encode(dto.getPin()));
        }

        user = userRepo.save(user);
        return sanitizeUser(user);
    }

    // ── Permanent Delete ─────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> permanentDelete(UUID id, UUID tenantId) {
        User user = findOne(id, tenantId);

        if (user.getRole() == UserRole.owner) {
            long ownerCount = userRepo.countByTenant_IdAndRole(tenantId, UserRole.owner);
            if (ownerCount <= 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot permanently delete the last owner account");
            }
        }

        try {
            // 1. Delete associated security tokens
            jdbcTemplate.update("DELETE FROM password_reset_tokens WHERE user_id = ?", id);

            // 2. 🟢 Anonymize historical records to keep financial history safe while removing FK locks
            jdbcTemplate.update("UPDATE shifts SET opened_by = NULL WHERE opened_by = ?", id);
            jdbcTemplate.update("UPDATE shifts SET closed_by = NULL WHERE closed_by = ?", id);
            
            jdbcTemplate.update("UPDATE orders SET created_by = NULL WHERE created_by = ?", id);
            jdbcTemplate.update("UPDATE orders SET waiter_id = NULL WHERE waiter_id = ?", id);
            jdbcTemplate.update("UPDATE orders SET cashier_id = NULL WHERE cashier_id = ?", id);
            
            jdbcTemplate.update("UPDATE order_items SET voided_by = NULL WHERE voided_by = ?", id);
            jdbcTemplate.update("UPDATE inventory_transactions SET created_by = NULL WHERE created_by = ?", id);
            
            // Handle optional hotel/audit records without crashing if columns differ
            try { jdbcTemplate.update("UPDATE hotel_reservations SET created_by_id = NULL WHERE created_by_id = ?", id); } catch (Exception ignored) {}
            try { jdbcTemplate.update("UPDATE audit_logs SET user_id = NULL WHERE user_id = ?", id); } catch (Exception ignored) {}

            // 3. Delete the user
            userRepo.delete(user);
            log.info("Permanently deleted user {} for tenant {}", id, tenantId);

        } catch (Exception e) {
            log.error("Failed to delete user {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database prevented user deletion: " + e.getMessage());
        }

        return Map.of("success", true, "message", "User permanently deleted successfully");
    }

    // ── Update Permissions ───────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> updatePermissions(UUID id, UUID tenantId, Map<String, Object> permissions) {
        User user = findOne(id, tenantId);
        Map<String, Object> current = new HashMap<>(user.getPermissions() != null ? user.getPermissions() : Map.of());
        current.putAll(permissions);
        user.setPermissions(current);
        user = userRepo.save(user);
        return sanitizeUser(user);
    }

    public Map<String, Object> sanitizeUser(User user) {
        Map<String, Object> safe = new HashMap<>();
        safe.put("id", user.getId());
        safe.put("tenantId", user.getTenant() != null ? user.getTenant().getId() : null);
        safe.put("branchId", user.getBranch() != null ? user.getBranch().getId() : null);
        safe.put("email", user.getEmail());
        safe.put("phone", user.getPhone());
        safe.put("firstName", user.getFirstName());
        safe.put("lastName", user.getLastName());
        safe.put("role", user.getRole());
        safe.put("employeeCode", user.getEmployeeCode());
        safe.put("permissions", user.getPermissions());
        safe.put("isActive", user.getIsActive());
        safe.put("createdAt", user.getCreatedAt());
        return safe;
    }
}