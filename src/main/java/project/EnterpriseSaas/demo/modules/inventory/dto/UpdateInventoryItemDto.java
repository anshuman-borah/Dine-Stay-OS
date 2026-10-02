package project.EnterpriseSaas.demo.modules.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateInventoryItemDto {

    @Size(max = 150, message = "Name cannot exceed 150 characters")
    private String name;

    @Size(max = 50, message = "SKU cannot exceed 50 characters")
    private String sku;

    @Size(max = 20, message = "Unit cannot exceed 20 characters")
    private String unit;

    @DecimalMin(value = "0.0", message = "Min stock level cannot be negative")
    private BigDecimal minStockLevel;

    @DecimalMin(value = "0.0", message = "Reorder level cannot be negative")
    private BigDecimal reorderLevel;

    @DecimalMin(value = "0.0", message = "Cost price cannot be negative")
    private BigDecimal costPrice;

    @Size(max = 100, message = "Category cannot exceed 100 characters")
    private String category;

    private Boolean isActive;
}