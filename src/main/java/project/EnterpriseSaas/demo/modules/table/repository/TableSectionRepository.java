package project.EnterpriseSaas.demo.modules.table.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.EnterpriseSaas.demo.modules.table.entity.TableSection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TableSectionRepository extends JpaRepository<TableSection, UUID> {

    @Query("SELECT s FROM TableSection s WHERE s.tenant.id = :tenantId AND s.branch.id = :branchId AND s.isActive = true ORDER BY s.sortOrder ASC, s.name ASC")
    List<TableSection> findAllByBranchAndTenant(@Param("branchId") UUID branchId, @Param("tenantId") UUID tenantId);

    @Query("SELECT s FROM TableSection s WHERE s.id = :id AND s.tenant.id = :tenantId")
    Optional<TableSection> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
}