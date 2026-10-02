package project.EnterpriseSaas.demo.modules.menu.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMenuItemDto {

    private UUID categoryId;

    @Size(max = 150, message = "Name cannot exceed 150 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @DecimalMin(value = "0.0", message = "Price cannot be negative")
    private BigDecimal price;

    // Aligned to entity
    private UUID gstRateId;
    private String sku;
    private String barcode;
    private String shortCode;

    private Boolean isVeg;

    private Boolean isActive;

    private Boolean isFeatured;

    @Min(value = 0, message = "Sort order cannot be negative")
    private Integer sortOrder;

    private String imageUrl;

    private List<String> tags;

    private List<Map<String, Object>> modifiers;
}