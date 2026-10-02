package project.EnterpriseSaas.demo.modules.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import project.EnterpriseSaas.demo.common.enums.OrderType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderDto {

    private OrderType orderType;

    // Alias for orderType (frontend sends 'type')
    private OrderType type;

    private UUID tableId;

    @Size(max = 100, message = "Customer name cannot exceed 100 characters")
    private String customerName;

    @Size(max = 20, message = "Customer phone cannot exceed 20 characters")
    private String customerPhone;

    private String customerGstin;

    @Size(max = 1000, message = "Customer address cannot exceed 1000 characters")
    private String customerAddress;

    private UUID shiftId;

    @Min(value = 0, message = "Cover count cannot be negative")
    private Integer coverCount;

    @Min(value = 0, message = "Covers cannot be negative")
    private Integer covers;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    @Valid
    private List<CreateOrderItemDto> items;

    private UUID waiterId;

    private Boolean isComplimentary;

    private Boolean isSalesReturn;

    private LocalDateTime scheduledAt;

    // Offline sync fields
    private String offlineId;

    private Boolean isOfflineSync;

    // Injected server-side
    private UUID branchId;
    private UUID tenantId;

    public OrderType getEffectiveOrderType() {
        return orderType != null ? orderType : type;
    }

    public Integer getEffectiveCovers() {
        return coverCount != null ? coverCount : covers;
    }
}