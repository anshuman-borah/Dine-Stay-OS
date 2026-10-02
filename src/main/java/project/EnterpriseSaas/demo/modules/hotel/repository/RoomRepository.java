package project.EnterpriseSaas.demo.modules.hotel.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.EnterpriseSaas.demo.common.enums.RoomStatus;
import project.EnterpriseSaas.demo.modules.hotel.entity.Room;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    Optional<Room> findByTenantIdAndBranchIdAndRoomNumber(UUID tenantId, UUID branchId, String roomNumber);

    Optional<Room> findByIdAndTenantId(UUID id, UUID tenantId);

    @EntityGraph(attributePaths = {"roomType"})
    @Query("SELECT r FROM Room r WHERE r.tenantId = :tenantId AND r.isActive = true " +
            "AND (:branchId IS NULL OR r.branchId = :branchId) " +
            "AND (CAST(:status AS text) IS NULL OR r.status = :status) " +
            "ORDER BY r.floor ASC, r.roomNumber ASC")
    List<Room> findRoomsWithFilters(
            @Param("tenantId") UUID tenantId,
            @Param("branchId") UUID branchId,
            @Param("status") RoomStatus status
    );

    @Query("SELECT r FROM Room r WHERE r.tenantId = :tenantId AND r.isActive = true " +
            "AND (:branchId IS NULL OR r.branchId = :branchId)")
    List<Room> findAllActiveRoomsByTenantAndBranch(@Param("tenantId") UUID tenantId, @Param("branchId") UUID branchId);
}