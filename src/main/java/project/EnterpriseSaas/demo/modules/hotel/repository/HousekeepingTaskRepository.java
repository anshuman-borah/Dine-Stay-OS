package project.EnterpriseSaas.demo.modules.hotel.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.EnterpriseSaas.demo.common.enums.HkStatus;
import project.EnterpriseSaas.demo.modules.hotel.entity.HousekeepingTask;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HousekeepingTaskRepository extends JpaRepository<HousekeepingTask, UUID> {

    Optional<HousekeepingTask> findByIdAndTenantId(UUID id, UUID tenantId);

    @EntityGraph(attributePaths = {"room", "room.roomType"})
    List<HousekeepingTask> findByTenantIdAndScheduledForOrderByPriorityDescCreatedAtAsc(UUID tenantId, LocalDate date);

    @EntityGraph(attributePaths = {"room", "room.roomType"})
    List<HousekeepingTask> findByTenantIdAndBranchIdAndScheduledForOrderByPriorityDescCreatedAtAsc(UUID tenantId, UUID branchId, LocalDate date);

    Optional<HousekeepingTask> findByRoomIdAndTenantIdAndScheduledForAndStatusIn(
            UUID roomId,
            UUID tenantId,
            LocalDate scheduledFor,
            Collection<HkStatus> statuses
    );

    @Modifying
    @Query("UPDATE HousekeepingTask h SET h.status = :status, h.completedAt = :completedAt, h.notes = :notes " +
            "WHERE h.room.id = :roomId AND h.tenantId = :tenantId AND h.scheduledFor = :scheduledFor AND h.status IN :statuses")
    void autoResolvePendingTasks(
            @Param("roomId") UUID roomId,
            @Param("tenantId") UUID tenantId,
            @Param("scheduledFor") LocalDate scheduledFor,
            @Param("statuses") Collection<HkStatus> statuses,
            @Param("status") HkStatus status,
            @Param("completedAt") LocalDateTime completedAt,
            @Param("notes") String notes
    );
}