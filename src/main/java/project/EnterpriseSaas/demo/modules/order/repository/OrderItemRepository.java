//package project.EnterpriseSaas.demo.modules.order.repository;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//import project.EnterpriseSaas.demo.modules.order.entity.OrderItem;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//@Repository
//public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
//
//    @Query("SELECT i FROM OrderItem i WHERE i.id = :id AND i.tenant.id = :tenantId")
//    Optional<OrderItem> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
//
//    @Query("SELECT i FROM OrderItem i WHERE i.order.id = :orderId")
//    List<OrderItem> findByOrderId(@Param("orderId") UUID orderId);
//}

package project.EnterpriseSaas.demo.modules.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.EnterpriseSaas.demo.modules.order.entity.OrderItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    // 🟢 THE FIX: JOIN FETCH pulls the MenuItem and Variations in a single database round-trip!
    @Query("SELECT i FROM OrderItem i " +
            "LEFT JOIN FETCH i.menuItem " +
            "LEFT JOIN FETCH i.variation " +
            "WHERE i.order.id = :orderId")
    List<OrderItem> findByOrderId(@Param("orderId") UUID orderId);

    @Query("SELECT i FROM OrderItem i WHERE i.id = :id AND i.tenant.id = :tenantId")
    Optional<OrderItem> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
}