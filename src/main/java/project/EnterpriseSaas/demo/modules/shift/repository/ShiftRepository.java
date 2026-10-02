package project.EnterpriseSaas.demo.modules.shift.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.EnterpriseSaas.demo.common.enums.ShiftDepartment;
import project.EnterpriseSaas.demo.common.enums.ShiftStatus;
import project.EnterpriseSaas.demo.modules.shift.entity.Shift;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, UUID> {

    @Query("SELECT s FROM Shift s WHERE s.branch.id = :branchId AND s.tenant.id = :tenantId " +
            "AND s.status = :status AND s.department = :department")
    Optional<Shift> findActiveShift(
            @Param("branchId") UUID branchId,
            @Param("tenantId") UUID tenantId,
            @Param("status") ShiftStatus status,
            @Param("department") ShiftDepartment department
    );

    @Query("SELECT s FROM Shift s WHERE s.id = :id AND s.tenant.id = :tenantId AND s.status = :status")
    Optional<Shift> findByIdAndTenantIdAndStatus(
            @Param("id") UUID id,
            @Param("tenantId") UUID tenantId,
            @Param("status") ShiftStatus status
    );

    @Query("SELECT s FROM Shift s WHERE s.id = :id AND s.tenant.id = :tenantId")
    Optional<Shift> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(s) FROM Shift s WHERE s.branch.id = :branchId AND s.tenant.id = :tenantId AND s.department = :department")
    long countByBranchAndTenantAndDepartment(
            @Param("branchId") UUID branchId,
            @Param("tenantId") UUID tenantId,
            @Param("department") ShiftDepartment department
    );

    // 🛡️ JPQL handles NULL parameters naturally without native CAST issues
    @Query("SELECT s FROM Shift s WHERE s.branch.id = :branchId AND s.tenant.id = :tenantId " +
            "AND s.department = :department " +
            "AND (:status IS NULL OR s.status = :status) " +
            "ORDER BY s.createdAt DESC")
    Page<Shift> findShiftsByBranchAndDepartment(
            @Param("branchId") UUID branchId,
            @Param("tenantId") UUID tenantId,
            @Param("department") ShiftDepartment department,
            @Param("status") ShiftStatus status,
            Pageable pageable
    );

    // 🛡️ Removing CAST. Passing null to a standard JPQL timestamp comparison works fine.
    @Query("SELECT s FROM Shift s WHERE s.branch.id = :branchId AND s.tenant.id = :tenantId " +
            "AND s.department = :department AND s.status = :status " +
            "AND (:startDate IS NULL OR s.closedAt >= :startDate) " +
            "AND (:endDate IS NULL OR s.closedAt <= :endDate)")
    List<Shift> findClosedShiftsForStats(
            @Param("branchId") UUID branchId,
            @Param("tenantId") UUID tenantId,
            @Param("department") ShiftDepartment department,
            @Param("status") ShiftStatus status,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate
    );
}