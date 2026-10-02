package project.EnterpriseSaas.demo.modules.shift.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CloseShiftDto {

    @NotNull(message = "Closing cash is required")
    @DecimalMin(value = "0.0", message = "Closing cash cannot be negative")
    private BigDecimal closingCash;

    @Valid
    private DenominationDto denominations;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;
}