package project.EnterpriseSaas.demo.modules.billing.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.EnterpriseSaas.demo.common.enums.BillSource;
import project.EnterpriseSaas.demo.modules.billing.entity.Bill;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillRepository extends JpaRepository<Bill, UUID> {

    @Query("SELECT b FROM Bill b WHERE b.id = :id AND b.tenant.id = :tenantId")
    Optional<Bill> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    // 🛡️ THE BULLETPROOF FIX: We bypass the IS NULL cast bug by using COALESCE
    @Query("SELECT b FROM Bill b WHERE b.tenant.id = :tenantId " +
            "AND (COALESCE(:branchId, NULL) IS NULL OR b.branch.id = :branchId) " +
            "AND (b.createdAt >= :from) " +
            "AND (b.createdAt <= :to) " +
            "AND (COALESCE(:source, NULL) IS NULL OR b.source = :source) " +
            "ORDER BY b.createdAt DESC")
    Page<Bill> findBillsWithFilters(
            @Param("tenantId") UUID tenantId,
            @Param("branchId") UUID branchId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("source") BillSource source,
            Pageable pageable
    );

    @Query("SELECT COUNT(b) FROM Bill b WHERE b.tenant.id = :tenantId AND b.billNumber LIKE CONCAT(:prefix, '%')")
    long countByTenantIdAndBillNumberPrefix(@Param("tenantId") UUID tenantId, @Param("prefix") String prefix);
}