package project.EnterpriseSaas.demo.modules.tenant.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;
import project.EnterpriseSaas.demo.common.enums.TaxRegime;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    private String email;

    private String phone;

    private String gstin;

    private String pan;

    @Column(name = "fssai_no")
    private String fssaiNo;

    @Column(name = "address_line1", columnDefinition = "text")
    private String addressLine1;

    @Column(name = "address_line2", columnDefinition = "text")
    private String addressLine2;

    private String city;

    private String state;

    @Column(name = "state_code", length = 2)
    private String stateCode;

    private String pincode;

    @Builder.Default
    @Column(nullable = false)
    private String country = "India";

    @Column(name = "logo_url", columnDefinition = "text")
    private String logoUrl;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "tax_regime", columnDefinition = "tax_regime")
    @Builder.Default
    private TaxRegime taxRegime = TaxRegime.regular;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> settings = Map.of();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}