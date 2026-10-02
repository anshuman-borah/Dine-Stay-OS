package project.EnterpriseSaas.demo.modules.menu.entity;

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
import project.EnterpriseSaas.demo.common.enums.ItemType;
import project.EnterpriseSaas.demo.modules.branch.entity.Branch;
import project.EnterpriseSaas.demo.modules.tenant.entity.Tenant;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "menu_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MenuItem {

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
    @JoinColumn(name = "branch_id")
    @JsonIgnore
    private Branch branch;

    @JsonProperty("branchId")
    public UUID getBranchId() {
        return branch != null ? branch.getId() : null;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @JsonIgnore
    private Category category;

    @JsonProperty("categoryId")
    public UUID getCategoryId() {
        return category != null ? category.getId() : null;
    }

    @OneToMany(mappedBy = "menuItem", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<MenuItemVariation> variations = new ArrayList<>();

    @Column(name = "gst_rate_id")
    private UUID gstRateId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "short_code")
    private String shortCode;

    private String sku;

    private String barcode;

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "type", columnDefinition = "item_type")
    @Builder.Default
    private ItemType type = ItemType.food;

    @Column(precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "cost_price", precision = 10, scale = 2)
    private BigDecimal costPrice;

    @Builder.Default
    @Column(name = "is_veg", nullable = false)
    private Boolean isVeg = true;

    @Builder.Default
    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;

    @Builder.Default
    @Column(name = "is_addon", nullable = false)
    private Boolean isAddon = false;

    @Builder.Default
    @Column(name = "track_inventory", nullable = false)
    private Boolean trackInventory = false;

    @Column(name = "inventory_item_id")
    private UUID inventoryItemId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> tags = List.of();

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

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