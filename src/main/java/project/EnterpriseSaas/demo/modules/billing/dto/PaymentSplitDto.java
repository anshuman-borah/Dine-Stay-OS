package project.EnterpriseSaas.demo.modules.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import project.EnterpriseSaas.demo.common.enums.PaymentMethod;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSplitDto {

    @NotNull(message = "Payment method is required")
    private PaymentMethod method;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Size(max = 100, message = "Reference number cannot exceed 100 characters")
    private String referenceNo;

    @Size(max = 4, message = "Card last 4 digits cannot exceed 4 characters")
    private String cardLast4;

    @Size(max = 50, message = "UPI ID cannot exceed 50 characters")
    private String upiId;

    @Size(max = 50, message = "Wallet name cannot exceed 50 characters")
    private String walletName;
}