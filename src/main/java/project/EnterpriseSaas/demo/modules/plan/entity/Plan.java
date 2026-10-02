package project.EnterpriseSaas.demo.modules.plan.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "price_monthly", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal priceMonthly = BigDecimal.ZERO;

    @Column(name = "price_annual", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal priceAnnual = BigDecimal.ZERO;

    @Column(name = "max_branches", nullable = false)
    @Builder.Default
    private Integer maxBranches = 1;

    @Column(name = "max_users", nullable = false)
    @Builder.Default
    private Integer maxUsers = 5;

    @Column(name = "max_menu_items", nullable = false)
    @Builder.Default
    private Integer maxMenuItems = 100;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> features = List.of();

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}