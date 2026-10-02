package project.EnterpriseSaas.demo.modules.inventory.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.EnterpriseSaas.demo.modules.inventory.entity.InventoryTransaction;

import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {

    @Query("SELECT t FROM InventoryTransaction t WHERE t.inventoryItem.id = :itemId AND t.tenant.id = :tenantId ORDER BY t.createdAt DESC")
    List<InventoryTransaction> findLedger(
            @Param("itemId") UUID itemId,
            @Param("tenantId") UUID tenantId,
            Pageable pageable
    );
}