package project.EnterpriseSaas.demo.modules.branch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import project.EnterpriseSaas.demo.common.enums.BranchType;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchDto {

    @NotBlank(message = "Branch name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Branch code is required")
    @Size(max = 20, message = "Code cannot exceed 20 characters")
    private String code;

    private BranchType type;

    @Size(max = 15, message = "GSTIN cannot exceed 15 characters")
    private String gstin;

    @Size(max = 50, message = "FSSAI No cannot exceed 50 characters")
    private String fssaiNo;

    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String addressLine1;

    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @Size(max = 100, message = "State cannot exceed 100 characters")
    private String state;

    @Size(max = 2, message = "State code must be 2 characters")
    private String stateCode;

    @Size(max = 20, message = "Pincode cannot exceed 20 characters")
    private String pincode;

    @Size(max = 20, message = "Phone cannot exceed 20 characters")
    private String phone;

    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @Size(max = 50, message = "Timezone cannot exceed 50 characters")
    private String timezone;

    @Size(max = 10, message = "Currency cannot exceed 10 characters")
    private String currency;

    private Boolean isHq;

    private Boolean isActive;

    private Map<String, Object> settings;
}