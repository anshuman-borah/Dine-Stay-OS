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
public class CreateMenuItemDto {

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    @NotBlank(message = "Item name is required")
    @Size(max = 150, message = "Name cannot exceed 150 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price cannot be negative")
    private BigDecimal price;

    // Replaced hsnCode/gstRate with the correct entity mapping
    private UUID gstRateId;

    @Size(max = 50, message = "SKU cannot exceed 50 characters")
    private String sku;

    @Size(max = 50, message = "Barcode cannot exceed 50 characters")
    private String barcode;

    @Size(max = 20, message = "Short code cannot exceed 20 characters")
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