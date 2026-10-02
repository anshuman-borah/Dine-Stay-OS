package project.EnterpriseSaas.demo.modules.hotel.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import project.EnterpriseSaas.demo.common.enums.HkPriority;
import project.EnterpriseSaas.demo.common.enums.HkStatus;
import project.EnterpriseSaas.demo.common.enums.HkTaskType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "hotel_housekeeping_tasks",
        indexes = {
                @Index(name = "idx_hk_tasks_tenant_branch_scheduled", columnList = "tenant_id, branch_id, scheduled_for"),
                @Index(name = "idx_hk_tasks_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_hk_tasks_room_id", columnList = "room_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HousekeepingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "room_id", nullable = false, insertable = false, updatable = false)
    private UUID roomId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "reservation_id")
    private UUID reservationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false)
    private HkTaskType taskType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private HkStatus status = HkStatus.pending;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    @Builder.Default
    private HkPriority priority = HkPriority.normal;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "scheduled_for", nullable = false)
    private LocalDate scheduledFor;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}