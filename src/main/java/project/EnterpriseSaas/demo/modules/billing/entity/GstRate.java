package project.EnterpriseSaas.demo.modules.billing.entity;

import project.EnterpriseSaas.demo.modules.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "gst_rates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GstRate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private String name;

    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal rate;

    @Column(name = "cgst_rate", precision = 5, scale = 2)
    private BigDecimal cgstRate;

    @Column(name = "sgst_rate", precision = 5, scale = 2)
    private BigDecimal sgstRate;

    @Column(name = "igst_rate", precision = 5, scale = 2)
    private BigDecimal igstRate;

    @Builder.Default
    @Column(name = "cess_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal cessRate = BigDecimal.ZERO;

    @Column(name = "hsn_sac_code")
    private String hsnSacCode;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}