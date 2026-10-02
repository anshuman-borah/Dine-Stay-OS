package project.EnterpriseSaas.demo.modules.hotel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.EnterpriseSaas.demo.modules.hotel.entity.FolioCharge;

import java.util.List;
import java.util.UUID;

@Repository
public interface FolioChargeRepository extends JpaRepository<FolioCharge, UUID> {

    List<FolioCharge> findByReservationIdAndTenantIdOrderByCreatedAtAsc(UUID reservationId, UUID tenantId);
}