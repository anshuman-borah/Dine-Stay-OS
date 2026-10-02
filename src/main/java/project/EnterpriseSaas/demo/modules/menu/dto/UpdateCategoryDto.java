package project.EnterpriseSaas.demo.modules.menu.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCategoryDto {

    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @Size(max = 300, message = "Description cannot exceed 300 characters")
    private String description;

    private Integer sortOrder;

    private Boolean isActive;

    private String color;

    private String icon;
}