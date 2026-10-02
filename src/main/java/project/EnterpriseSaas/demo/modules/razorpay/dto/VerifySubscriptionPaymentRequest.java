package project.EnterpriseSaas.demo.modules.razorpay.dto;

import lombok.Data;

@Data
public class VerifySubscriptionPaymentRequest {
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
    private String planCode;
}