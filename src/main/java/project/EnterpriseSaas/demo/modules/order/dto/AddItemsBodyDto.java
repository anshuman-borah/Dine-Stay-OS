package project.EnterpriseSaas.demo.modules.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddItemsBodyDto {

    @NotEmpty(message = "Items list cannot be empty")
    @Valid
    private List<AddOrderItemDto> items;

    private Boolean isOfflineSync;
}