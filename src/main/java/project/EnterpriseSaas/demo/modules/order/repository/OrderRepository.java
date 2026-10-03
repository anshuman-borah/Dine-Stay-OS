package project.EnterpriseSaas.demo.modules.order.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.EnterpriseSaas.demo.common.enums.OrderStatus;
import project.EnterpriseSaas.demo.modules.order.entity.Order;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query("SELECT o FROM Order o WHERE o.offlineId = :offlineId AND o.tenant.id = :tenantId")
    Optional<Order> findByOfflineIdAndTenantId(@Param("offlineId") String offlineId, @Param("tenantId") UUID tenantId);

    @Query("SELECT o FROM Order o WHERE o.id = :id AND o.tenant.id = :tenantId")
    Optional<Order> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    // 🟢 THE FIX: Pulls Order, Tenant, Branch, Table, Shift, and Waiter in 1 single query instead of 6!
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.tenant " +
            "LEFT JOIN FETCH o.branch " +
            "LEFT JOIN FETCH o.table " +
            "LEFT JOIN FETCH o.shift " +
            "LEFT JOIN FETCH o.waiter " +
            "WHERE o.id = :id AND o.tenant.id = :tenantId")
    Optional<Order> findByIdAndTenantIdWithRelations(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT o FROM Order o WHERE o.branch.id = :branchId AND o.tenant.id = :tenantId ORDER BY o.createdAt DESC")
    List<Order> findByBranchAndTenant(@Param("branchId") UUID branchId, @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.branch.id = :branchId AND o.tenant.id = :tenantId AND o.status IN :statuses ORDER BY o.createdAt DESC")
    List<Order> findByBranchAndTenantAndStatusIn(
            @Param("branchId") UUID branchId,
            @Param("tenantId") UUID tenantId,
            @Param("statuses") Collection<OrderStatus> statuses,
            Pageable pageable
    );

    @Query("SELECT COUNT(o) FROM Order o WHERE o.branch.id = :branchId AND o.orderNumber LIKE CONCAT(:prefix, '%')")
    long countByBranchIdAndOrderNumberPrefix(@Param("branchId") UUID branchId, @Param("prefix") String prefix);
}