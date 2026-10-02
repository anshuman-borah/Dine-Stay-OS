package project.EnterpriseSaas.demo.modules.hotel.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import project.EnterpriseSaas.demo.common.enums.BookingSource;
import project.EnterpriseSaas.demo.common.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "hotel_reservations",
        indexes = {
                @Index(name = "idx_hotel_res_tenant_branch", columnList = "tenant_id, branch_id"),
                @Index(name = "idx_hotel_res_tenant_check_in", columnList = "tenant_id, check_in_date"),
                @Index(name = "idx_hotel_res_tenant_id", columnList = "tenant_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "primary_guest_id", nullable = false)
    private Guest primaryGuest;

    @Column(name = "num_adults", nullable = false)
    @Builder.Default
    private Integer numAdults = 1;

    @Column(name = "num_children", nullable = false)
    @Builder.Default
    private Integer numChildren = 0;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "actual_check_in")
    private LocalDateTime actualCheckIn;

    @Column(name = "actual_check_out")
    private LocalDateTime actualCheckOut;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.confirmed;

    @Column(name = "rate_per_night", precision = 12, scale = 2, nullable = false)
    private BigDecimal ratePerNight;

    @Column(name = "num_nights", nullable = false)
    private Integer numNights;

    @Column(name = "subtotal", precision = 12, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "tax_amount", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "advance_paid", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal advancePaid = BigDecimal.ZERO;

    @Column(name = "balance_due", precision = 12, scale = 2, nullable = false)
    private BigDecimal balanceDue;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    @Builder.Default
    private BookingSource source = BookingSource.walk_in;

    @Column(name = "booking_ref", length = 100)
    private String bookingRef;

    @Column(name = "special_requests", columnDefinition = "text")
    private String specialRequests;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", columnDefinition = "text")
    private String cancelReason;

    @Column(name = "created_by_id")
    private UUID createdById;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Helper getters for frontend mapping
    public UUID getRoomId() {
        return room != null ? room.getId() : null;
    }

    public UUID getPrimaryGuestId() {
        return primaryGuest != null ? primaryGuest.getId() : null;
    }
}