package project.EnterpriseSaas.demo.modules.table.dto;

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
public class CreateSectionDto {

    @NotBlank(message = "Section name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    private Integer sortOrder;

    private Boolean isActive;
}