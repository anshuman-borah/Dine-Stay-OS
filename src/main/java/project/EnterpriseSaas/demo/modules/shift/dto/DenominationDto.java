package project.EnterpriseSaas.demo.modules.shift.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DenominationDto {

    @Min(value = 0, message = "Count cannot be negative")
    private Integer note2000;

    @Min(value = 0, message = "Count cannot be negative")
    private Integer note500;

    @Min(value = 0, message = "Count cannot be negative")
    private Integer note200;

    @Min(value = 0, message = "Count cannot be negative")
    private Integer note100;

    @Min(value = 0, message = "Count cannot be negative")
    private Integer note50;

    @Min(value = 0, message = "Count cannot be negative")
    private Integer note20;

    @Min(value = 0, message = "Count cannot be negative")
    private Integer note10;

    @Min(value = 0, message = "Count cannot be negative")
    private Integer coin5;

    @Min(value = 0, message = "Count cannot be negative")
    private Integer coin2;

    @Min(value = 0, message = "Count cannot be negative")
    private Integer coin1;
}