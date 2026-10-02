package project.EnterpriseSaas.demo.modules.hotel.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import project.EnterpriseSaas.demo.common.enums.RoomStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "hotel_rooms",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_hotel_rooms_tenant_branch_number",
                        columnNames = {"tenant_id", "branch_id", "room_number"}
                )
        },
        indexes = {
                @Index(name = "idx_hotel_rooms_tenant_id", columnList = "tenant_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "room_type_id", nullable = false, insertable = false, updatable = false)
    private UUID roomTypeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @Column(name = "room_number", length = 20, nullable = false)
    private String roomNumber;

    @Column(name = "floor", nullable = false)
    @Builder.Default
    private Integer floor = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private RoomStatus status = RoomStatus.available;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "amenities_override", columnDefinition = "jsonb")
    private List<String> amenitiesOverride;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}