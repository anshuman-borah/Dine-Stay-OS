package project.EnterpriseSaas.demo.modules.audit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.EnterpriseSaas.demo.modules.audit.entity.AuditLog;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByEntityAndEntityIdAndTenantIdOrderByCreatedAtDesc(
            String entity,
            String entityId,
            UUID tenantId,
            Pageable pageable
    );

    List<AuditLog> findByUserIdAndTenantIdOrderByCreatedAtDesc(
            UUID userId,
            UUID tenantId,
            Pageable pageable
    );

    @Modifying
    @Transactional
    @Query("DELETE FROM AuditLog a WHERE a.createdAt < :cutoffDate")
    void deleteLogsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    // 🟢 FIXED: Added CAST checks for :from and :to so that if the user
    // doesn't select a date, it doesn't break the query!
    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId " +
            "AND (CAST(:entity AS text) IS NULL OR a.entity = :entity) " +
            "AND (CAST(:action AS text) IS NULL OR a.action = :action) " +
            "AND (CAST(:userId AS uuid) IS NULL OR a.userId = :userId) " +
            "AND (CAST(:from AS timestamp) IS NULL OR a.createdAt >= :from) " +
            "AND (CAST(:to AS timestamp) IS NULL OR a.createdAt <= :to)")
    Page<AuditLog> findAuditLogsWithFilters(
            @Param("tenantId") UUID tenantId,
            @Param("entity") String entity,
            @Param("action") String action,
            @Param("userId") UUID userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );
}