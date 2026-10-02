package project.EnterpriseSaas.demo.modules.hotel.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.EnterpriseSaas.demo.modules.hotel.entity.Guest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GuestRepository extends JpaRepository<Guest, UUID> {

    Optional<Guest> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Guest> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    @Query("SELECT g FROM Guest g WHERE g.tenantId = :tenantId " +
            "AND (LOWER(g.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(g.phone) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(g.email) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY g.totalStays DESC")
    List<Guest> searchGuests(
            @Param("tenantId") UUID tenantId,
            @Param("query") String query,
            Pageable pageable
    );
}