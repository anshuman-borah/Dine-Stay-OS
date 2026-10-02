package project.EnterpriseSaas.demo.modules.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoidBillDto {

    @NotBlank(message = "Reason is required")
    @Size(max = 300, message = "Reason cannot exceed 300 characters")
    private String reason;
}