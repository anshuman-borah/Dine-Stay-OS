package project.EnterpriseSaas.demo.modules.shift.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenShiftDto {

    @NotNull(message = "Opening cash is required")
    @DecimalMin(value = "0.0", message = "Opening cash cannot be negative")
    private BigDecimal openingCash;

    @Valid
    private DenominationDto denominations;
}