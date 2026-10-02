package project.EnterpriseSaas.demo.modules.auth.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginDto {

    @Email(message = "Invalid email format")
    private String email;

    private String phone;

    private String pin;

    private String password;

    private UUID tenantId;

    private String tenantSlug;

    private UUID branchId;
}