package project.EnterpriseSaas.demo.modules.billing.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import project.EnterpriseSaas.demo.common.dto.ApiResponse;
import project.EnterpriseSaas.demo.modules.billing.dto.BillEmailDto;
import project.EnterpriseSaas.demo.modules.billing.dto.CreateBillDto;
import project.EnterpriseSaas.demo.modules.billing.dto.VoidBillDto;
import project.EnterpriseSaas.demo.modules.billing.entity.Bill;
import project.EnterpriseSaas.demo.modules.billing.service.BillingService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @PostMapping("/bills")
    public ResponseEntity<ApiResponse<Bill>> createBill(
            @Valid @RequestBody CreateBillDto dto,
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId
    ) {
        UUID effectiveBranchId = dto.getBranchId() != null ? dto.getBranchId() : branchId;
        Bill bill = billingService.createBill(dto, tenantId, effectiveBranchId);
        return ResponseEntity.ok(ApiResponse.ok(bill));
    }

    @PostMapping("/razorpay/create-order")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createRazorpayOrder(
            @RequestBody Map<String, Object> body,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String receipt = (String) body.get("receipt");
        Map<String, Object> order = billingService.createRazorpayOrderForBilling(tenantId, amount, receipt);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(order));
    }

    @PostMapping("/razorpay/verify-payment")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyRazorpayPayment(
            @RequestBody Map<String, String> body,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        Map<String, Object> result = billingService.verifyRazorpayBillingPayment(
                tenantId,
                body.get("razorpayOrderId"),
                body.get("razorpayPaymentId"),
                body.get("razorpaySignature")
        );
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/bills")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listBills(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "limit", required = false, defaultValue = "50") int limit,
            @RequestParam(value = "source", required = false) String source
    ) {
        OffsetDateTime fromDate = from != null ? OffsetDateTime.parse(from) : null;
        OffsetDateTime toDate = to != null ? OffsetDateTime.parse(to) : null;
        Map<String, Object> result = billingService.listBills(branchId, tenantId, fromDate, toDate, page, limit, source);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/bills/{id}")
    public ResponseEntity<ApiResponse<Bill>> getBill(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(billingService.getBill(id, tenantId)));
    }

    @PatchMapping("/bills/{id}/void")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<Bill>> voidBill(
            @PathVariable UUID id,
            @Valid @RequestBody VoidBillDto dto,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(billingService.voidBill(id, tenantId, dto.getReason())));
    }

    @PostMapping("/bills/{id}/email")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> emailBill(
            @PathVariable UUID id,
            @Valid @RequestBody BillEmailDto dto,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(billingService.emailBill(id, tenantId, dto.getEmail())));
    }

    @PostMapping("/bills/{id}/reprint")
    public ResponseEntity<ApiResponse<Bill>> reprintBill(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(billingService.reprintBill(id, tenantId)));
    }
}