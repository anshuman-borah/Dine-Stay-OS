
package project.EnterpriseSaas.demo.modules.billing.service;

import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.EnterpriseSaas.demo.common.enums.*;
import project.EnterpriseSaas.demo.modules.billing.dto.CreateBillDto;
import project.EnterpriseSaas.demo.modules.billing.dto.PaymentSplitDto;
import project.EnterpriseSaas.demo.modules.billing.entity.Bill;
import project.EnterpriseSaas.demo.modules.billing.entity.Payment;
import project.EnterpriseSaas.demo.modules.billing.repository.BillRepository;
import project.EnterpriseSaas.demo.modules.billing.repository.PaymentRepository;
import project.EnterpriseSaas.demo.modules.branch.entity.Branch;
import project.EnterpriseSaas.demo.modules.branch.repository.BranchRepository;
import project.EnterpriseSaas.demo.modules.core.service.EmailService;
import project.EnterpriseSaas.demo.modules.order.entity.Order;
import project.EnterpriseSaas.demo.modules.order.entity.OrderItem;
import project.EnterpriseSaas.demo.modules.order.repository.OrderItemRepository;
import project.EnterpriseSaas.demo.modules.order.repository.OrderRepository;
import project.EnterpriseSaas.demo.modules.shift.entity.Shift;
import project.EnterpriseSaas.demo.modules.shift.repository.ShiftRepository;
import project.EnterpriseSaas.demo.modules.table.entity.Table;
import project.EnterpriseSaas.demo.modules.table.repository.TableRepository;
import project.EnterpriseSaas.demo.modules.tenant.entity.Tenant;
import project.EnterpriseSaas.demo.modules.tenant.repository.TenantRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

    private final BillRepository billRepo;
    private final PaymentRepository paymentRepo;
    private final OrderRepository orderRepo;
    private final OrderItemRepository itemRepo;
    private final ShiftRepository shiftRepo;
    private final BranchRepository branchRepo;
    private final TenantRepository tenantRepo;
    private final TableRepository tableRepo;
    private final EmailService emailService;

    // Kafka and Jackson injected here to push events!
    private final org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    // ── Create Bill & Process Payments ────────────────────────────────────────

    @Transactional
    public Bill createBill(CreateBillDto dto, UUID tenantId, UUID branchId) {
        Order order = orderRepo.findByIdAndTenantIdWithRelations(dto.getOrderId(), tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getStatus() == OrderStatus.billed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order already billed");
        }
        if (order.getStatus() == OrderStatus.cancelled) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order is cancelled");
        }

        BigDecimal effectiveGrandTotal = order.getGrandTotal();
        BigDecimal totalPaid = dto.getPayments().stream()
                .map(PaymentSplitDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!Boolean.TRUE.equals(dto.getIsOfflineSync()) && totalPaid.compareTo(effectiveGrandTotal.subtract(BigDecimal.valueOf(0.01))) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("Insufficient payment. Expected ₹%.2f, got ₹%.2f", effectiveGrandTotal, totalPaid)
            );
        }

        Tenant tenant = order.getTenant();
        Branch branch = order.getBranch();

        // Resolve Shift
        Shift shift = null;
        if (dto.getShiftId() != null) {
            shift = shiftRepo.findById(dto.getShiftId()).orElse(null);
        }
        if (shift == null && branch != null) {
            shift = shiftRepo.findActiveShift(branch.getId(), tenantId, ShiftStatus.open, ShiftDepartment.restaurant).orElse(null);
        }

        String billNumber = generateBillNumber(tenantId);
        GstType supplyType = dto.getCustomerGstin() != null ? GstType.igst : (dto.getSupplyType() != null ? dto.getSupplyType() : GstType.cgst_sgst);

        List<OrderItem> items = itemRepo.findByOrderId(order.getId());
        List<Map<String, Object>> gstSummary = buildGstSummary(items);

        BigDecimal changeAmount = totalPaid.subtract(effectiveGrandTotal).max(BigDecimal.ZERO);

        Bill bill = Bill.builder()
                .tenant(tenant)
                .branch(branch)
                .order(order)
                .shift(shift)
                .source(BillSource.pos)
                .billNumber(billNumber)
                .invoiceNumber(billNumber)
                .status(InvoiceStatus.paid)
                .customerName(dto.getCustomerName() != null ? dto.getCustomerName() : order.getCustomerName())
                .customerPhone(dto.getCustomerPhone() != null ? dto.getCustomerPhone() : order.getCustomerPhone())
                .customerGstin(dto.getCustomerGstin() != null ? dto.getCustomerGstin() : order.getCustomerGstin())
                .customerAddress(dto.getCustomerAddress())
                .supplyType(supplyType)
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .taxableAmount(order.getTaxableAmount())
                .cgstAmount(supplyType == GstType.igst ? BigDecimal.ZERO : order.getCgstAmount())
                .sgstAmount(supplyType == GstType.igst ? BigDecimal.ZERO : order.getSgstAmount())
                .igstAmount(supplyType == GstType.igst ? order.getCgstAmount().add(order.getSgstAmount()) : order.getIgstAmount())
                .cessAmount(order.getCessAmount())
                .totalTax(order.getTotalTax())
                .roundOff(order.getRoundOff())
                .grandTotal(effectiveGrandTotal)
                .paidAmount(totalPaid)
                .changeAmount(changeAmount)
                .gstSummary(gstSummary)
                .notes(dto.getNotes())
                .issuedAt(OffsetDateTime.now())
                .build();

        Bill savedBill = billRepo.save(bill);

        boolean isSplit = dto.getPayments().size() > 1;
        final Shift effectiveShift = shift;
        List<Payment> payments = dto.getPayments().stream().map(p -> Payment.builder()
                .tenant(tenant)
                .branch(branch)
                .bill(savedBill)
                .order(order)
                .shift(effectiveShift)
                .method(p.getMethod())
                .amount(p.getAmount())
                .referenceNo(p.getReferenceNo())
                .cardLast4(p.getCardLast4())
                .upiId(p.getUpiId())
                .walletName(p.getWalletName())
                .isSplit(isSplit)
                .status("success")
                .processedAt(OffsetDateTime.now())
                .build()
        ).toList();

        paymentRepo.saveAll(payments);

        order.setStatus(OrderStatus.billed);
        order.setBilledAt(OffsetDateTime.now());
        if (shift != null) order.setShift(shift);
        orderRepo.save(order);

        // Auto-free the Table back to AVAILABLE
        if (order.getTable() != null) {
            Table table = order.getTable();
            table.setStatus(TableStatus.available);
            tableRepo.save(table);
            log.info("Table {} auto-released to AVAILABLE", table.getTableNumber());
        }

        // Update Shift running totals safely (adjusting for change returned)
        if (shift != null) {
            updateShiftTotals(shift, savedBill, dto.getPayments());
        }

        // 🟢 HOTEL INTEGRATION: Fire Room Charge events to Kafka!
        for (PaymentSplitDto p : dto.getPayments()) {
            if (p.getMethod() == PaymentMethod.room_charge) {
                // The cashier types the room number into the frontend, sent in referenceNo
                String roomNumber = p.getReferenceNo();

                try {
                    Map<String, Object> event = new HashMap<>();
                    event.put("tenantId", tenantId.toString());
                    event.put("branchId", branchId != null ? branchId.toString() : null);
                    event.put("roomNumber", roomNumber);
                    event.put("amount", p.getAmount());
                    event.put("orderNumber", order.getOrderNumber() != null ? order.getOrderNumber() : order.getId().toString());

                    String message = objectMapper.writeValueAsString(event);
                    kafkaTemplate.send("restaurant-room-charges", message);

                    log.info("🚀 [API] Fired RoomChargeEvent to Kafka for Room: {}", roomNumber);
                } catch (Exception e) {
                    log.error("Failed to push RoomChargeEvent to Kafka", e);
                }
            }
        }

        return savedBill;
    }

    // ── Helper: Update Shift Running Totals Safely ────────────────────────────

    private void updateShiftTotals(Shift shift, Bill bill, List<PaymentSplitDto> payments) {
        shift.setTotalSales(shift.getTotalSales().add(bill.getGrandTotal()));
        shift.setTotalOrders(shift.getTotalOrders() + 1);

        BigDecimal cashAdded = BigDecimal.ZERO;

        for (PaymentSplitDto p : payments) {
            BigDecimal amt = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
            switch (p.getMethod()) {
                case cash -> cashAdded = cashAdded.add(amt);
                case card -> shift.setCardSales(shift.getCardSales().add(amt));
                case upi -> shift.setUpiSales(shift.getUpiSales().add(amt));
                case wallet -> shift.setWalletSales(shift.getWalletSales().add(amt));
                // Treat room_charge the same as credit so the drawer stays perfectly balanced!
                case credit, room_charge -> shift.setCreditSales(shift.getCreditSales().add(amt));
                case complimentary -> shift.setComplimentary(shift.getComplimentary().add(amt));
            }
        }

        // Subtract change handed back to customer from the shift's cash pool
        if (cashAdded.compareTo(BigDecimal.ZERO) > 0) {
            cashAdded = cashAdded.subtract(bill.getChangeAmount());
            shift.setCashSales(shift.getCashSales().add(cashAdded));
        }

        shift.setTotalCgst(shift.getTotalCgst().add(bill.getCgstAmount()));
        shift.setTotalSgst(shift.getTotalSgst().add(bill.getSgstAmount()));
        shift.setTotalIgst(shift.getTotalIgst().add(bill.getIgstAmount()));

        shiftRepo.save(shift);
        log.info("Shift {} totals updated: Total Sales = ₹{}, Cash (after change) = ₹{}",
                shift.getShiftNumber(), shift.getTotalSales(), shift.getCashSales());
    }

    // ── Get Bill ─────────────────────────────────────────────────────────────

    public Bill getBill(UUID billId, UUID tenantId) {
        return billRepo.findByIdAndTenantId(billId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bill not found"));
    }

    // ── List Bills (Paginated) ───────────────────────────────────────────────

    public Map<String, Object> listBills(
            UUID branchId,
            UUID tenantId,
            OffsetDateTime from,
            OffsetDateTime to,
            int page,
            int limit,
            String sourceStr
    ) {
        BillSource source = (sourceStr != null && !sourceStr.isBlank()) ? BillSource.valueOf(sourceStr.toLowerCase()) : null;
        PageRequest pageRequest = PageRequest.of(Math.max(0, page - 1), limit);

        // Provide extreme fallback dates to protect the Postgres query from NULL casts
        OffsetDateTime effectiveFrom = from != null ? from : OffsetDateTime.parse("2000-01-01T00:00:00Z");
        OffsetDateTime effectiveTo = to != null ? to : OffsetDateTime.parse("2100-01-01T00:00:00Z");

        Page<Bill> billPage = billRepo.findBillsWithFilters(tenantId, branchId, effectiveFrom, effectiveTo, source, pageRequest);

        return Map.of(
                "data", billPage.getContent(),
                "total", billPage.getTotalElements(),
                "page", page,
                "limit", limit
        );
    }

    // ── Void Bill ────────────────────────────────────────────────────────────

    @Transactional
    public Bill voidBill(UUID billId, UUID tenantId, String reason) {
        Bill bill = getBill(billId, tenantId);
        if (bill.getStatus() == InvoiceStatus.voided) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Already voided");
        }

        bill.setStatus(InvoiceStatus.voided);
        bill.setNotes("VOIDED: " + reason);

        // Reverse the revenue out of the active shift to keep the drawer balanced
        Shift shift = bill.getShift();
        if (shift != null) {
            shift.setTotalSales(shift.getTotalSales().subtract(bill.getGrandTotal()));
            shift.setTotalOrders(Math.max(0, shift.getTotalOrders() - 1));
            shift.setTotalCgst(shift.getTotalCgst().subtract(bill.getCgstAmount()));
            shift.setTotalSgst(shift.getTotalSgst().subtract(bill.getSgstAmount()));
            shift.setTotalIgst(shift.getTotalIgst().subtract(bill.getIgstAmount()));

            BigDecimal cashDeducted = BigDecimal.ZERO;
            for (Payment p : bill.getPayments()) {
                BigDecimal amt = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
                switch (p.getMethod()) {
                    case cash -> cashDeducted = cashDeducted.add(amt);
                    case card -> shift.setCardSales(shift.getCardSales().subtract(amt));
                    case upi -> shift.setUpiSales(shift.getUpiSales().subtract(amt));
                    case wallet -> shift.setWalletSales(shift.getWalletSales().subtract(amt));
                    // Reversing room charge
                    case credit, room_charge -> shift.setCreditSales(shift.getCreditSales().subtract(amt));
                    case complimentary -> shift.setComplimentary(shift.getComplimentary().subtract(amt));
                }
                p.setStatus("voided");
            }
            // Reverse the exact cash impact (payment minus change)
            if (cashDeducted.compareTo(BigDecimal.ZERO) > 0) {
                cashDeducted = cashDeducted.subtract(bill.getChangeAmount());
                shift.setCashSales(shift.getCashSales().subtract(cashDeducted));
            }
            shiftRepo.save(shift);
            log.info("Shift {} totals reversed for voided bill {}", shift.getShiftNumber(), bill.getBillNumber());
        }

        // Cancel the order so it doesn't show up in revenue reports
        if (bill.getOrder() != null) {
            Order order = bill.getOrder();
            order.setStatus(OrderStatus.cancelled);
            orderRepo.save(order);
        }

        Bill savedBill = billRepo.save(bill);

        // 🟢 FIRE AUDIT EVENT TO KAFKA
        try {
            Map<String, String> auditEvent = new HashMap<>();
            auditEvent.put("tenantId", tenantId.toString());
            auditEvent.put("branchId", bill.getBranch() != null ? bill.getBranch().getId().toString() : null);
            auditEvent.put("entity", "BILL");
            auditEvent.put("entityId", billId.toString());
            auditEvent.put("action", "VOID");
            auditEvent.put("metadata", "Reason: " + reason + " | Amount: " + bill.getGrandTotal());

            kafkaTemplate.send("audit-logs", objectMapper.writeValueAsString(auditEvent));
            log.info("🚀 [API] Fired AuditEvent to Kafka for voided bill {}", billId);
        } catch (Exception e) {
            log.error("Failed to push audit event to Kafka", e);
        }

        return savedBill;
    }

    // ── Email Bill via KAFKA ─────────────────────────────────────────────────
    public Map<String, Boolean> emailBill(UUID billId, UUID tenantId, String email) {
        try {
            // Create a simple JSON message package
            Map<String, String> event = new HashMap<>();
            event.put("billId", billId.toString());
            event.put("tenantId", tenantId.toString());
            event.put("email", email);

            String message = objectMapper.writeValueAsString(event);

            // Fire and forget! Instantly drop it in the Kafka queue
            kafkaTemplate.send("bill-emails-topic", message);

            log.info("🚀 [API] Pushed email task to Kafka for bill {}", billId);
        } catch (Exception e) {
            log.error("Failed to push email task to Kafka", e);
        }

        return Map.of("sent", true);
    }

    // ── Reprint Bill ─────────────────────────────────────────────────────────

    @Transactional
    public Bill reprintBill(UUID billId, UUID tenantId) {
        Bill bill = getBill(billId, tenantId);
        if (bill.getStatus() == InvoiceStatus.voided) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot reprint a voided bill");
        }
        short currentCount = bill.getPrintedCount() != null ? bill.getPrintedCount() : 0;
        bill.setPrintedCount((short) (currentCount + 1));
        bill.setPrintedAt(OffsetDateTime.now());
        return billRepo.save(bill);
    }

    // ── Per-Tenant Razorpay Integration ──────────────────────────────────────

    public Map<String, Object> createRazorpayOrderForBilling(UUID tenantId, BigDecimal amount, String receipt) {
        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

        Map<String, Object> settings = tenant.getSettings();
        @SuppressWarnings("unchecked")
        Map<String, Object> rzp = (settings != null && settings.get("razorpay") instanceof Map)
                ? (Map<String, Object>) settings.get("razorpay")
                : null;

        if (rzp == null || rzp.get("keyId") == null || rzp.get("keySecret") == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Razorpay is not configured for this account. Please connect Razorpay in Settings."
            );
        }

        String keyId = (String) rzp.get("keyId");
        String keySecret = (String) rzp.get("keySecret");
        long amountPaise = amount.multiply(BigDecimal.valueOf(100)).longValue();

        try {
            RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", receipt != null ? receipt : "pos_" + Instant.now().toEpochMilli());

            com.razorpay.Order order = razorpay.orders.create(orderRequest);

            return Map.of(
                    "orderId", order.get("id"),
                    "amount", order.get("amount"),
                    "currency", order.get("currency"),
                    "keyId", keyId
            );
        } catch (Exception e) {
            log.error("Failed to create Razorpay order for POS: {}", e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to initialize payment gateway: " + e.getMessage()
            );
        }
    }

    public Map<String, Object> verifyRazorpayBillingPayment(
            UUID tenantId,
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature
    ) {
        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

        Map<String, Object> settings = tenant.getSettings();
        @SuppressWarnings("unchecked")
        Map<String, Object> rzp = (settings != null && settings.get("razorpay") instanceof Map)
                ? (Map<String, Object>) settings.get("razorpay")
                : null;

        String keySecret = rzp != null ? (String) rzp.get("keySecret") : null;
        if (keySecret == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Razorpay is not configured for this account.");
        }

        try {
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String generatedSignature = HexFormat.of().formatHex(hash);

            if (!generatedSignature.equals(razorpaySignature)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment signature");
            }

            return Map.of("valid", true, "paymentId", razorpayPaymentId);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment signature");
        }
    }

    // ── Private Helpers ────────────────────────────────______________________

    private List<Map<String, Object>> buildGstSummary(List<OrderItem> items) {
        Map<BigDecimal, Map<String, Object>> summary = new HashMap<>();

        for (OrderItem item : items) {
            if (Boolean.TRUE.equals(item.getIsVoided())) continue;
            BigDecimal rate = item.getGstRate() != null ? item.getGstRate() : BigDecimal.ZERO;

            Map<String, Object> group = summary.computeIfAbsent(rate, r -> {
                Map<String, Object> g = new HashMap<>();
                g.put("gstRate", r);
                g.put("taxableAmount", BigDecimal.ZERO);
                g.put("cgstAmount", BigDecimal.ZERO);
                g.put("sgstAmount", BigDecimal.ZERO);
                g.put("igstAmount", BigDecimal.ZERO);
                g.put("totalTax", BigDecimal.ZERO);
                return g;
            });

            group.put("taxableAmount", ((BigDecimal) group.get("taxableAmount")).add(item.getTaxableAmount()));
            group.put("cgstAmount", ((BigDecimal) group.get("cgstAmount")).add(item.getCgstAmount()));
            group.put("sgstAmount", ((BigDecimal) group.get("sgstAmount")).add(item.getSgstAmount()));
            group.put("igstAmount", ((BigDecimal) group.get("igstAmount")).add(item.getIgstAmount()));
            group.put("totalTax", ((BigDecimal) group.get("totalTax")).add(item.getCgstAmount().add(item.getSgstAmount())));
        }

        return new ArrayList<>(summary.values());
    }

    private String generateBillNumber(UUID tenantId) {
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String prefix = "INV-" + today + "-";
        long count = billRepo.countByTenantIdAndBillNumberPrefix(tenantId, prefix);
        return prefix + String.format("%05d", count + 1);
    }
}