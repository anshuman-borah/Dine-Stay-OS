package project.EnterpriseSaas.demo.modules.hotel.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "hotel_room_types",
        indexes = {
                @Index(name = "idx_hotel_room_types_tenant_branch", columnList = "tenant_id, branch_id"),
                @Index(name = "idx_hotel_room_types_tenant_id", columnList = "tenant_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "base_rate", precision = 12, scale = 2, nullable = false)
    private BigDecimal baseRate;

    @Column(name = "max_occupancy", nullable = false)
    @Builder.Default
    private Integer maxOccupancy = 2;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "amenities", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> amenities = new ArrayList<>();

    @Column(name = "total_rooms", nullable = false)
    @Builder.Default
    private Integer totalRooms = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "channel_manager_id", length = 100)
    private String channelManagerId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}