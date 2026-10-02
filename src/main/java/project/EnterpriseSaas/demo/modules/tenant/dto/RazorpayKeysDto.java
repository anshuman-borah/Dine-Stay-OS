package project.EnterpriseSaas.demo.modules.tenant.dto;

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
public class RazorpayKeysDto {

    @NotBlank(message = "Key ID is required")
    @Size(min = 10, message = "Key ID must be at least 10 characters")
    private String keyId;

    @NotBlank(message = "Key Secret is required")
    @Size(min = 10, message = "Key Secret must be at least 10 characters")
    private String keySecret;
}