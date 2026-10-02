package project.EnterpriseSaas.demo.modules.billing.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillEmailDto {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
}