package project.EnterpriseSaas.demo.modules.order.entity;

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
import project.EnterpriseSaas.demo.common.enums.OrderStatus;
import project.EnterpriseSaas.demo.common.enums.OrderType;
import project.EnterpriseSaas.demo.modules.branch.entity.Branch;
import project.EnterpriseSaas.demo.modules.shift.entity.Shift;
import project.EnterpriseSaas.demo.modules.table.entity.Table;
import project.EnterpriseSaas.demo.modules.tenant.entity.Tenant;
import project.EnterpriseSaas.demo.modules.user.entity.User;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@jakarta.persistence.Table(name = "orders", uniqueConstraints = {
        @UniqueConstraint(name = "uk_orders_branch_order_number", columnNames = {"branch_id", "order_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Order {

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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "table_id")
    private Table table;

    @JsonProperty("tableId")
    public UUID getTableId() {
        return table != null ? table.getId() : null;
    }

    @JsonProperty("tableName")
    public String getTableName() {
        return table != null ? table.getTableNumber() : null;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    @JsonIgnore
    private Shift shift;

    @JsonProperty("shiftId")
    public UUID getShiftId() {
        return shift != null ? shift.getId() : null;
    }

    @OneToMany(mappedBy = "order", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "order_number", nullable = false)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "order_type", columnDefinition = "order_type")
    @Builder.Default
    private OrderType type = OrderType.dine_in;

    @JsonProperty("orderType")
    public OrderType getOrderType() {
        return type;
    }

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", columnDefinition = "order_status")
    @Builder.Default
    private OrderStatus status = OrderStatus.placed;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "customer_gstin")
    private String customerGstin;

    @Column(name = "customer_address", columnDefinition = "text")
    private String customerAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waiter_id")
    @JsonIgnore
    private User waiter;

    @JsonProperty("waiterId")
    public UUID getWaiterId() {
        return waiter != null ? waiter.getId() : null;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id")
    @JsonIgnore
    private User cashier;

    @JsonProperty("cashierId")
    public UUID getCashierId() {
        return cashier != null ? cashier.getId() : null;
    }

    @Builder.Default
    @Column(name = "cover_count", nullable = false)
    private Integer covers = 1;

    @Column(columnDefinition = "text")
    private String notes;

    @Builder.Default
    @Column(name = "kot_printed", nullable = false)
    private Boolean kotPrinted = false;

    @Builder.Default
    @Column(name = "bill_printed", nullable = false)
    private Boolean billPrinted = false;

    @Column(precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "discount_percent", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(name = "taxable_amount", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal taxableAmount = BigDecimal.ZERO;

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
    @Builder.Default
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Column(name = "paid_amount", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "change_amount", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal changeAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "is_complimentary", nullable = false)
    private Boolean isComplimentary = false;

    @Builder.Default
    @Column(name = "is_sales_return", nullable = false)
    private Boolean isSalesReturn = false;

    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;

    @Builder.Default
    @Column(nullable = false)
    private Boolean synced = true;

    @Column(name = "offline_id")
    private String offlineId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> metadata = Map.of();

    @Column(name = "placed_at")
    private OffsetDateTime placedAt;

    @Column(name = "served_at")
    private OffsetDateTime servedAt;

    @Column(name = "billed_at")
    private OffsetDateTime billedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}