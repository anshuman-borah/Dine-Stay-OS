package project.EnterpriseSaas.demo.modules.menu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.EnterpriseSaas.demo.modules.menu.entity.MenuItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    @Query("SELECT DISTINCT m FROM MenuItem m LEFT JOIN FETCH m.variations WHERE m.tenant.id = :tenantId AND m.isActive = true " +
            "AND (:branchId IS NULL OR m.branch IS NULL OR m.branch.id = :branchId) " +
            "AND (:categoryId IS NULL OR m.category.id = :categoryId) " +
            "ORDER BY m.sortOrder ASC, m.name ASC")
    List<MenuItem> findMenuItems(
            @Param("tenantId") UUID tenantId,
            @Param("branchId") UUID branchId,
            @Param("categoryId") UUID categoryId
    );

    @Query("SELECT m FROM MenuItem m LEFT JOIN FETCH m.variations WHERE m.id = :id AND m.tenant.id = :tenantId")
    Optional<MenuItem> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
}