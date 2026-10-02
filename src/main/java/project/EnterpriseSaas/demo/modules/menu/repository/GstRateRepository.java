package project.EnterpriseSaas.demo.modules.billing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.EnterpriseSaas.demo.modules.billing.entity.GstRate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GstRateRepository extends JpaRepository<GstRate, UUID> {

    @Query("SELECT g FROM GstRate g WHERE g.tenant.id = :tenantId AND g.isActive = true")
    List<GstRate> findByTenantIdAndIsActiveTrue(@Param("tenantId") UUID tenantId);

    @Query("SELECT g FROM GstRate g WHERE g.id = :id AND g.tenant.id = :tenantId")
    Optional<GstRate> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(g) FROM GstRate g WHERE g.tenant.id = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);
}