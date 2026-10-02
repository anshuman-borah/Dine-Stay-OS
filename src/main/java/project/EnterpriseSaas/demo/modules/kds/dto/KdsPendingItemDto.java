package project.EnterpriseSaas.demo.modules.kds.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import project.EnterpriseSaas.demo.common.enums.KdsStatus;
import project.EnterpriseSaas.demo.common.enums.OrderStatus;
import project.EnterpriseSaas.demo.common.enums.OrderType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KdsPendingItemDto {

    @JsonProperty("order_item_id")
    private UUID orderItemId;

    @JsonProperty("item_name")
    private String itemName;

    @JsonProperty("name")
    public String getName() {
        return itemName;
    }

    @JsonProperty("quantity")
    private BigDecimal quantity;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("kds_status")
    private KdsStatus kdsStatus;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("kds_ready_at")
    private OffsetDateTime kdsReadyAt;

    @JsonProperty("menu_item_id")
    private UUID menuItemId;

    @JsonProperty("order_id")
    private UUID orderId;

    // Both aliases so KDS screen and POS always find the order number
    @JsonProperty("order_order_number")
    private String orderOrderNumber;

    @JsonProperty("order_number")
    public String getOrderNumber() {
        return orderOrderNumber;
    }

    @JsonProperty("order_type")
    private OrderType orderType;

    @JsonProperty("order_status")
    private OrderStatus orderStatus;

    @JsonProperty("table_name")
    private String tableName;

    @JsonProperty("category_name")
    private String categoryName;

    @JsonProperty("age_seconds")
    private Long ageSeconds;
}