package project.EnterpriseSaas.demo.modules.hotel.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import project.EnterpriseSaas.demo.common.enums.ChargeType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "hotel_folio_charges",
        indexes = {
                @Index(name = "idx_folio_charges_tenant_res", columnList = "tenant_id, reservation_id"),
                @Index(name = "idx_folio_charges_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_folio_charges_reservation_id", columnList = "reservation_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolioCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId;

    @Column(name = "description", length = 255, nullable = false)
    private String description;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_type", nullable = false)
    private ChargeType chargeType;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "date", nullable = false)
    @Builder.Default
    private LocalDate date = LocalDate.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}