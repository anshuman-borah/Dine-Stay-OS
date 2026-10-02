package project.EnterpriseSaas.demo.modules.tenant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import project.EnterpriseSaas.demo.common.enums.TaxRegime;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTenantDto {
    private String name;
    private String gstin;
    private String pan;
    private String fssaiNo;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String stateCode;
    private String pincode;
    private String country;
    private String email;
    private String phone;
    private String logoUrl;
    private TaxRegime taxRegime;
    private Map<String, Object> settings;
}