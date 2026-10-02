package project.EnterpriseSaas.demo.modules.table.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTableDto {

    @JsonAlias({"name", "table_number", "number"})
    private String tableNumber;

    private String name;

    private String status; // e.g. "available", "occupied", "reserved", "dirty", "out_of_order"

    private UUID sectionId;

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    private Boolean isActive;

    private String qrCode;

    private Double posX;

    private Double posY;

    public String getEffectiveTableNumber() {
        if (tableNumber != null && !tableNumber.isBlank()) return tableNumber;
        if (name != null && !name.isBlank()) return name;
        return null;
    }
}