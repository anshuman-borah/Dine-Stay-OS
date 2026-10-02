package project.EnterpriseSaas.demo.modules.razorpay.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import project.EnterpriseSaas.demo.modules.razorpay.dto.CreateSubscriptionOrderRequest;
import project.EnterpriseSaas.demo.modules.razorpay.dto.VerifySubscriptionPaymentRequest;
import project.EnterpriseSaas.demo.modules.razorpay.service.RazorpayService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/razorpay")
@RequiredArgsConstructor
public class RazorpayController {

    private final RazorpayService razorpayService;

    @PostMapping("/create-order")
    public Map<String, Object> createOrder(
            @RequestBody CreateSubscriptionOrderRequest request,
            @RequestHeader("X-Tenant-Id") UUID tenantId // Replace with your auth context/custom annotation if applicable
    ) {
        return razorpayService.createSubscriptionOrder(request.getPlanCode(), tenantId);
    }

    @PostMapping("/verify-payment")
    public Map<String, Object> verifyPayment(
            @RequestBody VerifySubscriptionPaymentRequest request,
            @RequestHeader("X-Tenant-Id") UUID tenantId
    ) {
        return razorpayService.verifySubscriptionPayment(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature(),
                request.getPlanCode(),
                tenantId
        );
    }
}