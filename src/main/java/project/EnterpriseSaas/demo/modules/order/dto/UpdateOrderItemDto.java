package project.EnterpriseSaas.demo.modules.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderItemDto {

    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    private Boolean isVoided;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;

    private String voidReason;
}