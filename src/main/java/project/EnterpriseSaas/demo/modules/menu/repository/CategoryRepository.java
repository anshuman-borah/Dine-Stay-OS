package project.EnterpriseSaas.demo.modules.menu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.EnterpriseSaas.demo.modules.menu.entity.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    @Query("SELECT c FROM Category c WHERE c.tenant.id = :tenantId AND c.isActive = true " +
            "AND (:branchId IS NULL OR c.branch.id = :branchId) " +
            "ORDER BY c.sortOrder ASC, c.name ASC")
    List<Category> findCategories(
            @Param("tenantId") UUID tenantId,
            @Param("branchId") UUID branchId
    );

    @Query("SELECT c FROM Category c WHERE c.id = :id AND c.tenant.id = :tenantId")
    Optional<Category> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
}