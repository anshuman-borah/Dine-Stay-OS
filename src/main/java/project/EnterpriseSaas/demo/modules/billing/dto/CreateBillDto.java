package project.EnterpriseSaas.demo.modules.billing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import project.EnterpriseSaas.demo.common.enums.GstType;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBillDto {

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    private UUID shiftId;

    @Size(max = 100, message = "Customer name cannot exceed 100 characters")
    private String customerName;

    @Size(max = 20, message = "Customer phone cannot exceed 20 characters")
    private String customerPhone;

    @Size(max = 15, message = "Customer GSTIN cannot exceed 15 characters")
    private String customerGstin;

    @Size(max = 500, message = "Customer address cannot exceed 500 characters")
    private String customerAddress;

    private GstType supplyType;

    @NotEmpty(message = "At least one payment split is required")
    @Valid
    private List<PaymentSplitDto> payments;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;

    // Offline sync flag — tells backend to auto-adjust payment to server grandTotal
    private Boolean isOfflineSync;

    // Injected server-side
    private UUID branchId;
    private UUID tenantId;
}