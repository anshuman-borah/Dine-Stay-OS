package project.EnterpriseSaas.demo.modules.inventory.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.EnterpriseSaas.demo.modules.inventory.entity.InventoryItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    @Query("SELECT i FROM InventoryItem i WHERE i.tenant.id = :tenantId AND i.isActive = true " +
            "AND (:branchId IS NULL OR i.branch.id = :branchId) " +
            "ORDER BY i.createdAt DESC")
    List<InventoryItem> findItems(
            @Param("tenantId") UUID tenantId,
            @Param("branchId") UUID branchId
    );

    @Query("SELECT i FROM InventoryItem i WHERE i.id = :id AND i.tenant.id = :tenantId")
    Optional<InventoryItem> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.id = :id AND i.tenant.id = :tenantId")
    Optional<InventoryItem> findByIdAndTenantIdForUpdate(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT i FROM InventoryItem i WHERE i.tenant.id = :tenantId AND i.isActive = true " +
            "AND (:branchId IS NULL OR i.branch.id = :branchId) " +
            "AND i.currentStock <= i.reorderLevel " +
            "ORDER BY i.currentStock ASC")
    List<InventoryItem> findLowStockItems(
            @Param("tenantId") UUID tenantId,
            @Param("branchId") UUID branchId
    );
}