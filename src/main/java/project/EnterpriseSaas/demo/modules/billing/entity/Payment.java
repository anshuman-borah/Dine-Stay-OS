package project.EnterpriseSaas.demo.modules.billing.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import project.EnterpriseSaas.demo.common.enums.PaymentMethod;
import project.EnterpriseSaas.demo.modules.branch.entity.Branch;
import project.EnterpriseSaas.demo.modules.order.entity.Order;
import project.EnterpriseSaas.demo.modules.shift.entity.Shift;
import project.EnterpriseSaas.demo.modules.tenant.entity.Tenant;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Payment {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id")
    @JsonIgnore
    private Bill bill;

    @JsonProperty("billId")
    public UUID getBillId() {
        return bill != null ? bill.getId() : null;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @JsonIgnore
    private Order order;

    @JsonProperty("orderId")
    public UUID getOrderId() {
        return order != null ? order.getId() : null;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    @JsonIgnore
    private Shift shift;

    @JsonProperty("shiftId")
    public UUID getShiftId() {
        return shift != null ? shift.getId() : null;
    }

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "method", columnDefinition = "payment_method", nullable = false)
    private PaymentMethod method;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "reference_no")
    private String referenceNo;

    @Column(name = "card_last4", length = 4)
    private String cardLast4;

    @Column(name = "upi_id")
    private String upiId;

    @Column(name = "wallet_name")
    private String walletName;

    @Builder.Default
    @Column(name = "is_split", nullable = false)
    private Boolean isSplit = false;

    @Builder.Default
    @Column(nullable = false)
    private String status = "success";

    @Column(columnDefinition = "text")
    private String notes;

    @Builder.Default
    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt = OffsetDateTime.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}