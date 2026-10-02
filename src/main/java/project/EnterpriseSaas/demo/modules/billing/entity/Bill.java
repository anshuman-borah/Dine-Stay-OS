package project.EnterpriseSaas.demo.modules.billing.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;
import project.EnterpriseSaas.demo.common.enums.BillSource;
import project.EnterpriseSaas.demo.common.enums.GstType;
import project.EnterpriseSaas.demo.common.enums.InvoiceStatus;
import project.EnterpriseSaas.demo.modules.branch.entity.Branch;
import project.EnterpriseSaas.demo.modules.order.entity.Order;
import project.EnterpriseSaas.demo.modules.shift.entity.Shift;
import project.EnterpriseSaas.demo.modules.tenant.entity.Tenant;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "bills", uniqueConstraints = {
        @UniqueConstraint(name = "uk_bills_branch_bill_number", columnNames = {"branch_id", "bill_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Bill {

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
    @JoinColumn(name = "order_id")
    @JsonIgnore
    private Order order;

    @JsonProperty("orderId")
    public UUID getOrderId() {
        return order != null ? order.getId() : null;
    }

    @Column(name = "reservation_id")
    private UUID reservationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    @JsonIgnore
    private Shift shift;

    @JsonProperty("shiftId")
    public UUID getShiftId() {
        return shift != null ? shift.getId() : null;
    }

    @OneToMany(mappedBy = "bill", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    @Builder.Default
    private BillSource source = BillSource.pos;

    @Column(name = "bill_number", nullable = false)
    private String billNumber;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", columnDefinition = "invoice_status")
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.issued;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "customer_gstin")
    private String customerGstin;

    @Column(name = "customer_address", columnDefinition = "text")
    private String customerAddress;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "supply_type", columnDefinition = "gst_type")
    @Builder.Default
    private GstType supplyType = GstType.cgst_sgst;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "taxable_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal taxableAmount;

    @Column(name = "cgst_amount", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal cgstAmount = BigDecimal.ZERO;

    @Column(name = "sgst_amount", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal sgstAmount = BigDecimal.ZERO;

    @Column(name = "igst_amount", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal igstAmount = BigDecimal.ZERO;

    @Column(name = "cess_amount", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal cessAmount = BigDecimal.ZERO;

    @Column(name = "total_tax", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalTax = BigDecimal.ZERO;

    @Column(name = "round_off", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal roundOff = BigDecimal.ZERO;

    @Column(name = "grand_total", precision = 12, scale = 2, nullable = false)
    private BigDecimal grandTotal;

    @Column(name = "paid_amount", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "change_amount", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal changeAmount = BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gst_summary", columnDefinition = "jsonb")
    @Builder.Default
    private List<Map<String, Object>> gstSummary = List.of();

    @Column(columnDefinition = "text")
    private String notes;

    @Builder.Default
    @Column(name = "is_refunded", nullable = false)
    private Boolean isRefunded = false;

    @Column(name = "refund_amount", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "printed_count", columnDefinition = "smallint", nullable = false)
    private Short printedCount = 0;

    @Column(name = "printed_at")
    private OffsetDateTime printedAt;

    @Builder.Default
    @Column(name = "issued_at", nullable = false)
    private OffsetDateTime issuedAt = OffsetDateTime.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}