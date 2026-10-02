package project.EnterpriseSaas.demo.modules.razorpay.dto;

import lombok.Data;

@Data
public class CreateSubscriptionOrderRequest {
    private String planCode;
}