package project.EnterpriseSaas.demo.modules.razorpay.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import project.EnterpriseSaas.demo.modules.plan.entity.Plan;
import project.EnterpriseSaas.demo.modules.plan.repository.PlanRepository;
import project.EnterpriseSaas.demo.modules.subscription.service.SubscriptionService;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayService {

    private final PlanRepository planRepo;
    private final SubscriptionService subService;

    @Value("${razorpay.key.id:}")
    private String platformKeyId;

    @Value("${razorpay.key.secret:}")
    private String platformKeySecret;

    // ── Create Subscription Order ─────────────────────────────────────────────

    public Map<String, Object> createSubscriptionOrder(String planCode, UUID tenantId) {
        Plan plan = planRepo.findByCode(planCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plan code not found"));

        long amountInPaise = plan.getPriceMonthly().multiply(BigDecimal.valueOf(100)).longValue();

        // If no keys configured in dev, provide simulated dev order for seamless testing
        if (platformKeyId == null || platformKeyId.isBlank() || platformKeySecret == null || platformKeySecret.isBlank()) {
            log.warn("RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET not set — creating mock development order.");
            return Map.of(
                    "orderId", "order_mock_" + Instant.now().toEpochMilli(),
                    "amount", amountInPaise,
                    "currency", "INR",
                    "keyId", "rzp_test_mock_key",
                    "isMock", true
            );
        }

        try {
            RazorpayClient razorpay = new RazorpayClient(platformKeyId, platformKeySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "sub_" + tenantId.toString().substring(0, 8) + "_" + Instant.now().toEpochMilli());

            JSONObject notes = new JSONObject();
            notes.put("tenantId", tenantId.toString());
            notes.put("planCode", planCode);
            orderRequest.put("notes", notes);

            Order order = razorpay.orders.create(orderRequest);

            return Map.of(
                    "orderId", order.get("id"),
                    "amount", order.get("amount"),
                    "currency", order.get("currency"),
                    "keyId", platformKeyId,
                    "isMock", false
            );
        } catch (Exception err) {
            log.error("Razorpay subscription error: {}", err.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order creation failed: " + err.getMessage());
        }
    }

    // ── Verify Subscription Payment ──────────────────────────────────────────

    public Map<String, Object> verifySubscriptionPayment(
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature,
            String planCode,
            UUID tenantId
    ) {
        // Handle mock dev order validation
        if (razorpayOrderId != null && razorpayOrderId.startsWith("order_mock_")) {
            subService.upgradeSubscription(tenantId, planCode, Map.of(
                    "razorpayOrderId", razorpayOrderId,
                    "razorpayPaymentId", razorpayPaymentId != null ? razorpayPaymentId : "mock_payment",
                    "verifiedAt", Instant.now().toString(),
                    "mode", "development_mock"
            ));
            return Map.of("success", true, "message", "Dev simulated subscription upgraded successfully");
        }

        if (platformKeySecret == null || platformKeySecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razorpay secret not configured");
        }

        try {
            // Verify cryptographic HMAC signature (Matching your BillingService logic)
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(platformKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String generatedSignature = HexFormat.of().formatHex(hash);

            if (!generatedSignature.equals(razorpaySignature)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment signature verification failed");
            }

            // Update tenant subscription in database
            subService.upgradeSubscription(tenantId, planCode, Map.of(
                    "razorpayOrderId", razorpayOrderId,
                    "razorpayPaymentId", razorpayPaymentId,
                    "verifiedAt", Instant.now().toString(),
                    "mode", "live_razorpay"
            ));

            return Map.of("success", true, "message", "Subscription upgraded successfully");

        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment signature verification failed");
        }
    }
}