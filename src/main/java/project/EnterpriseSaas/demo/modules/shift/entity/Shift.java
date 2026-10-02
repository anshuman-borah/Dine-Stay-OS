package project.EnterpriseSaas.demo.modules.shift.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import project.EnterpriseSaas.demo.common.enums.ShiftDepartment;
import project.EnterpriseSaas.demo.common.enums.ShiftStatus;
import project.EnterpriseSaas.demo.modules.branch.entity.Branch;
import project.EnterpriseSaas.demo.modules.tenant.entity.Tenant;
import project.EnterpriseSaas.demo.modules.user.entity.User;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "shifts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_shifts_branch_dept_number", columnNames = {"branch_id", "department", "shift_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @JsonIgnore
    private Tenant tenant;

    @JsonProperty("tenantId")
    public UUID getTenantId() {
        return tenant != null ? tenant.getId() : null;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    @JsonIgnore
    private Branch branch;

    @JsonProperty("branchId")
    public UUID getBranchId() {
        return branch != null ? branch.getId() : null;
    }

    @Column(name = "shift_number", nullable = false)
    private String shiftNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "department", nullable = false)
    @Builder.Default
    private ShiftDepartment department = ShiftDepartment.restaurant;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ShiftStatus status = ShiftStatus.open;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "opened_by")
    @JsonIgnore
    private User openedBy;

    @JsonProperty("openedByUser")
    public Object getOpenedByUser() {
        if (openedBy == null) return null;
        return java.util.Map.of(
                "id", openedBy.getId(),
                "firstName", openedBy.getFirstName(),
                "lastName", openedBy.getLastName() != null ? openedBy.getLastName() : "",
                "role", openedBy.getRole().name(),
                "fullName", openedBy.getFullName()
        );
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "closed_by")
    @JsonIgnore
    private User closedBy;

    @JsonProperty("closedByUser")
    public Object getClosedByUser() {
        if (closedBy == null) return null;
        return java.util.Map.of(
                "id", closedBy.getId(),
                "firstName", closedBy.getFirstName(),
                "lastName", closedBy.getLastName() != null ? closedBy.getLastName() : "",
                "role", closedBy.getRole().name(),
                "fullName", closedBy.getFullName()
        );
    }

    @Column(name = "opening_cash", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal openingCash = BigDecimal.ZERO;

    @Column(name = "closing_cash", precision = 12, scale = 2)
    private BigDecimal closingCash;

    @Column(name = "expected_cash", precision = 12, scale = 2)
    private BigDecimal expectedCash;

    @Column(name = "cash_difference", precision = 12, scale = 2)
    private BigDecimal cashDifference;

    @Column(name = "total_sales", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalSales = BigDecimal.ZERO;

    @Column(name = "total_orders", nullable = false)
    @Builder.Default
    private Integer totalOrders = 0;

    @Column(name = "cash_sales", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal cashSales = BigDecimal.ZERO;

    @Column(name = "card_sales", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal cardSales = BigDecimal.ZERO;

    @Column(name = "upi_sales", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal upiSales = BigDecimal.ZERO;

    @Column(name = "wallet_sales", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal walletSales = BigDecimal.ZERO;

    @Column(name = "credit_sales", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal creditSales = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal complimentary = BigDecimal.ZERO;

    @Column(name = "total_refund", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalRefund = BigDecimal.ZERO;

    @Column(name = "total_cgst", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalCgst = BigDecimal.ZERO;

    @Column(name = "total_sgst", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalSgst = BigDecimal.ZERO;

    @Column(name = "total_igst", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalIgst = BigDecimal.ZERO;

    @Column(columnDefinition = "text")
    private String notes;

    @Builder.Default
    @Column(name = "opened_at", nullable = false)
    private OffsetDateTime openedAt = OffsetDateTime.now();

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}