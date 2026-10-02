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
import project.EnterpriseSaas.demo.common.enums.KdsStatus;
import project.EnterpriseSaas.demo.modules.menu.entity.MenuItem;
import project.EnterpriseSaas.demo.modules.menu.entity.MenuItemVariation;
import project.EnterpriseSaas.demo.modules.tenant.entity.Tenant;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private Order order;

    @JsonProperty("orderId")
    public UUID getOrderId() {
        return order != null ? order.getId() : null;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @JsonIgnore
    private Tenant tenant;

    @JsonProperty("tenantId")
    public UUID getTenantId() {
        return tenant != null ? tenant.getId() : null;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id")
    @JsonIgnore
    private MenuItem menuItem;

    @JsonProperty("menuItemId")
    public UUID getMenuItemId() {
        return menuItem != null ? menuItem.getId() : null;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variation_id")
    @JsonIgnore
    private MenuItemVariation variation;

    @JsonProperty("variationId")
    public UUID getVariationId() {
        return variation != null ? variation.getId() : null;
    }

    @Column(name = "variation_name")
    private String variationName;

    @Column(nullable = false)
    private String name;

    private String sku;

    @Column(precision = 10, scale = 3, nullable = false)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "cost_price", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "taxable_amount", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal taxableAmount = BigDecimal.ZERO;

    @Column(name = "gst_rate", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal gstRate = BigDecimal.ZERO;

    @Column(name = "cgst_rate", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal cgstRate = BigDecimal.ZERO;

    @Column(name = "sgst_rate", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal sgstRate = BigDecimal.ZERO;

    @Column(name = "igst_rate", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal igstRate = BigDecimal.ZERO;

    @Column(name = "cgst_amount", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal cgstAmount = BigDecimal.ZERO;

    @Column(name = "sgst_amount", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal sgstAmount = BigDecimal.ZERO;

    @Column(name = "igst_amount", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal igstAmount = BigDecimal.ZERO;

    @Column(name = "cess_amount", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal cessAmount = BigDecimal.ZERO;

    @Column(name = "line_total", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal lineTotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "is_veg", nullable = false)
    private Boolean isVeg = true;

    @Column(columnDefinition = "text")
    private String notes;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "kds_status", columnDefinition = "kds_status")
    @Builder.Default
    private KdsStatus kdsStatus = KdsStatus.pending;

    @Column(name = "kds_acknowledged_at")
    private OffsetDateTime kdsAcknowledgedAt;

    @Column(name = "kds_ready_at")
    private OffsetDateTime kdsReadyAt;

    @Builder.Default
    @Column(name = "is_voided", nullable = false)
    private Boolean isVoided = false;

    @Column(name = "void_reason", columnDefinition = "text")
    private String voidReason;

    @Column(name = "kot_number")
    private String kotNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<Map<String, Object>> modifiers = List.of();

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}