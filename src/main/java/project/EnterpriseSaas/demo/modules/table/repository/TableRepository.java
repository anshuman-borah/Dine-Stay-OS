package project.EnterpriseSaas.demo.modules.table.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.EnterpriseSaas.demo.modules.table.entity.Table;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TableRepository extends JpaRepository<Table, UUID> {

    @Query("SELECT t FROM Table t WHERE t.tenant.id = :tenantId AND t.branch.id = :branchId AND t.isActive = true ORDER BY t.tableNumber ASC")
    List<Table> findAllByBranchAndTenant(@Param("branchId") UUID branchId, @Param("tenantId") UUID tenantId);

    @Query("SELECT t FROM Table t WHERE t.id = :id AND t.tenant.id = :tenantId")
    Optional<Table> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(t) FROM Table t WHERE t.section.id = :sectionId AND t.tenant.id = :tenantId AND t.isActive = true")
    long countBySectionIdAndTenantId(@Param("sectionId") UUID sectionId, @Param("tenantId") UUID tenantId);

    // Prevents duplicate table numbers in the same branch
    boolean existsByBranch_IdAndTableNumberIgnoreCaseAndIsActiveTrue(UUID branchId, String tableNumber);
}