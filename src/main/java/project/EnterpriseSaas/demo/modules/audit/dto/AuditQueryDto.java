package project.EnterpriseSaas.demo.modules.audit.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditQueryDto {

    @Min(value = 1, message = "Page must be at least 1")
    @Builder.Default
    private Integer page = 1;

    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 500, message = "Limit cannot exceed 500")
    @Builder.Default
    private Integer limit = 100;

    private String entity;

    private String action;

    private UUID userId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;
}