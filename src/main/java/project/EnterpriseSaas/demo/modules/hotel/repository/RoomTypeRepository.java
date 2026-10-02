package project.EnterpriseSaas.demo.modules.hotel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.EnterpriseSaas.demo.modules.hotel.entity.RoomType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, UUID> {

    List<RoomType> findByTenantIdAndIsActiveTrueOrderByNameAsc(UUID tenantId);

    List<RoomType> findByTenantIdAndBranchIdAndIsActiveTrueOrderByNameAsc(UUID tenantId, UUID branchId);

    Optional<RoomType> findByIdAndTenantId(UUID id, UUID tenantId);

    @Modifying
    @Query("UPDATE RoomType rt SET rt.totalRooms = rt.totalRooms + :increment WHERE rt.id = :id")
    void incrementTotalRooms(@Param("id") UUID id, @Param("increment") int increment);
}