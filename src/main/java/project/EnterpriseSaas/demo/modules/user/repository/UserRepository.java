package project.EnterpriseSaas.demo.modules.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.EnterpriseSaas.demo.common.enums.UserRole;
import project.EnterpriseSaas.demo.modules.user.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCaseAndIsActiveTrue(String email);

    Optional<User> findByPhoneAndIsActiveTrue(String phone);

    Optional<User> findByEmailIgnoreCaseAndTenant_IdAndIsActiveTrue(String email, UUID tenantId);

    Optional<User> findByPhoneAndTenant_IdAndIsActiveTrue(String phone, UUID tenantId);

    Optional<User> findByEmailIgnoreCaseAndRoleAndIsActiveTrue(String email, UserRole role);

    Optional<User> findByIdAndTenant_Id(UUID id, UUID tenantId);

    List<User> findByTenant_IdAndIsActiveTrueOrderByCreatedAtDesc(UUID tenantId);

    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId AND u.isActive = true " +
            "AND (u.branch.id = :branchId OR u.role IN ('owner', 'manager')) " +
            "ORDER BY u.createdAt DESC")
    List<User> findBranchStaffAndManagers(
            @Param("tenantId") UUID tenantId,
            @Param("branchId") UUID branchId
    );

    // 🛡️ FIX: Added CAST to resolve PostgreSQL 'could not determine data type' (42P18) crash
    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId AND (" +
            "(CAST(:email AS text) IS NOT NULL AND LOWER(u.email) = LOWER(CAST(:email AS text))) OR " +
            "(CAST(:phone AS text) IS NOT NULL AND u.phone = CAST(:phone AS text)))")
    Optional<User> findByEmailOrPhoneWithinTenant(
            @Param("tenantId") UUID tenantId,
            @Param("email") String email,
            @Param("phone") String phone
    );

    long countByTenant_IdAndRoleAndIsActiveTrue(UUID tenantId, UserRole role);

    long countByTenant_IdAndRole(UUID tenantId, UserRole role);
}