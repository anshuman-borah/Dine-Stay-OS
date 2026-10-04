//package project.EnterpriseSaas.demo.modules.order.controller;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//import project.EnterpriseSaas.demo.common.dto.ApiResponse;
//import project.EnterpriseSaas.demo.common.enums.OrderStatus;
//import project.EnterpriseSaas.demo.modules.order.dto.AddItemsBodyDto;
//import project.EnterpriseSaas.demo.modules.order.dto.ApplyDiscountDto;
//import project.EnterpriseSaas.demo.modules.order.dto.CreateOrderDto;
//import project.EnterpriseSaas.demo.modules.order.entity.Order;
//import project.EnterpriseSaas.demo.modules.order.service.OrderService;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//
//@RestController
//@RequestMapping("/api/v1/orders")
//@RequiredArgsConstructor
//@Slf4j
//public class OrderController {
//
//    private final OrderService orderService;
//    private final KafkaTemplate<String, String> kafkaTemplate;
//    private final ObjectMapper objectMapper;
//
//    @PostMapping
//    public ResponseEntity<ApiResponse<Order>> create(
//            @Valid @RequestBody CreateOrderDto dto,
//            @RequestHeader("x-tenant-id") UUID tenantId,
//            @RequestHeader("x-branch-id") UUID branchId
//    ) {
//        UUID effectiveBranchId = dto.getBranchId() != null ? dto.getBranchId() : branchId;
//
//        // 1. Synchronously create the base order so we get the ID instantly for the frontend
//        Order baseOrder = orderService.createOrder(dto, tenantId, effectiveBranchId);
//
//        // 2. If there are items to cook, let Kafka handle broadcasting to the KDS
//        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
//            try {
//                Map<String, Object> event = new HashMap<>();
//                event.put("orderId", baseOrder.getId().toString());
//                event.put("tenantId", tenantId.toString());
//                event.put("items", dto.getItems());
//
//                kafkaTemplate.send("kds-new-items-topic", objectMapper.writeValueAsString(event));
//                log.info("🚀 [API] Pushed KDS broadcast task to Kafka for order {}", baseOrder.getOrderNumber());
//            } catch (Exception e) {
//                log.error("Failed to queue KDS task: {}", e.getMessage());
//            }
//        }
//
//        // 3. Return the base order immediately so the POS UI stays lightning fast
//        return ResponseEntity.ok(ApiResponse.ok(baseOrder));
//    }
//
//    @GetMapping
//    public ResponseEntity<ApiResponse<List<Order>>> findAll(
//            @RequestHeader("x-tenant-id") UUID tenantId,
//            @RequestHeader("x-branch-id") UUID branchId,
//            @RequestParam(value = "status", required = false) String status,
//            @RequestParam(value = "limit", required = false, defaultValue = "100") int limit
//    ) {
//        List<Order> orders = orderService.findAll(branchId, tenantId, status, limit);
//        return ResponseEntity.ok(ApiResponse.ok(orders));
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<ApiResponse<Order>> findOne(
//            @PathVariable UUID id,
//            @RequestHeader("x-tenant-id") UUID tenantId
//    ) {
//        return ResponseEntity.ok(ApiResponse.ok(orderService.findOne(id, tenantId)));
//    }
//
//    @PostMapping("/{id}/items")
//    public ResponseEntity<ApiResponse<Order>> addItems(
//            @PathVariable UUID id,
//            @Valid @RequestBody AddItemsBodyDto body,
//            @RequestHeader("x-tenant-id") UUID tenantId
//    ) {
//        Order updated = orderService.addItems(id, body.getItems(), tenantId);
//        return ResponseEntity.ok(ApiResponse.ok(updated));
//    }
//
//    @PatchMapping("/{id}/status")
//    public ResponseEntity<ApiResponse<Order>> updateStatus(
//            @PathVariable UUID id,
//            @RequestBody Map<String, String> body,
//            @RequestHeader("x-tenant-id") UUID tenantId
//    ) {
//        OrderStatus status = OrderStatus.valueOf(body.get("status").toLowerCase());
//        Order updated = orderService.updateStatus(id, status, tenantId);
//        return ResponseEntity.ok(ApiResponse.ok(updated));
//    }
//
//    @PatchMapping("/{id}/discount")
//    public ResponseEntity<ApiResponse<Order>> applyDiscount(
//            @PathVariable UUID id,
//            @Valid @RequestBody ApplyDiscountDto dto,
//            @RequestHeader("x-tenant-id") UUID tenantId
//    ) {
//        Order updated = orderService.applyDiscount(id, dto, tenantId);
//        return ResponseEntity.ok(ApiResponse.ok(updated));
//    }
//
//    @PatchMapping("/items/{itemId}/void")
//    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
//    public ResponseEntity<ApiResponse<Order>> voidItem(
//            @PathVariable UUID itemId,
//            @RequestBody Map<String, String> body,
//            @RequestHeader("x-tenant-id") UUID tenantId
//    ) {
//        Order updated = orderService.voidItem(itemId, body.get("reason"), tenantId);
//        return ResponseEntity.ok(ApiResponse.ok(updated));
//    }
//}
package project.EnterpriseSaas.demo.modules.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import project.EnterpriseSaas.demo.common.dto.ApiResponse;
import project.EnterpriseSaas.demo.common.enums.OrderStatus;
import project.EnterpriseSaas.demo.modules.order.dto.AddItemsBodyDto;
import project.EnterpriseSaas.demo.modules.order.dto.ApplyDiscountDto;
import project.EnterpriseSaas.demo.modules.order.dto.CreateOrderDto;
import project.EnterpriseSaas.demo.modules.order.entity.Order;
import project.EnterpriseSaas.demo.modules.order.service.OrderService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // 🟢 ADDED TOGGLE
    @Value("${app.kafka.enabled:false}")
    private boolean kafkaEnabled;

    @PostMapping
    public ResponseEntity<ApiResponse<Order>> create(
            @Valid @RequestBody CreateOrderDto dto,
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId
    ) {
        UUID effectiveBranchId = dto.getBranchId() != null ? dto.getBranchId() : branchId;

        // 1. Synchronously create the base order so we get the ID instantly for the frontend
        Order baseOrder = orderService.createOrder(dto, tenantId, effectiveBranchId);

        // 2. 🟢 TOGGLE APPLIED: KDS broadcast
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            try {
                if (kafkaEnabled) {
                    Map<String, Object> event = new HashMap<>();
                    event.put("orderId", baseOrder.getId().toString());
                    event.put("tenantId", tenantId.toString());
                    event.put("items", dto.getItems());

                    kafkaTemplate.send("kds-new-items-topic", objectMapper.writeValueAsString(event));
                    log.info("🚀 [API] Pushed KDS broadcast task to Kafka for order {}", baseOrder.getOrderNumber());
                } else {
                    log.info("⚡ [API] Kafka disabled - skipped KDS Kafka broadcast. (Fallback handled by Spring Events inside OrderService)");
                }
            } catch (Exception e) {
                log.error("Failed to queue KDS task: {}", e.getMessage());
            }
        }

        // 3. Return the base order immediately
        return ResponseEntity.ok(ApiResponse.ok(baseOrder));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Order>>> findAll(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "limit", required = false, defaultValue = "100") int limit
    ) {
        List<Order> orders = orderService.findAll(branchId, tenantId, status, limit);
        return ResponseEntity.ok(ApiResponse.ok(orders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Order>> findOne(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.findOne(id, tenantId)));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<ApiResponse<Order>> addItems(
            @PathVariable UUID id,
            @Valid @RequestBody AddItemsBodyDto body,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        Order updated = orderService.addItems(id, body.getItems(), tenantId);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Order>> updateStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        OrderStatus status = OrderStatus.valueOf(body.get("status").toLowerCase());
        Order updated = orderService.updateStatus(id, status, tenantId);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    @PatchMapping("/{id}/discount")
    public ResponseEntity<ApiResponse<Order>> applyDiscount(
            @PathVariable UUID id,
            @Valid @RequestBody ApplyDiscountDto dto,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        Order updated = orderService.applyDiscount(id, dto, tenantId);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    @PatchMapping("/items/{itemId}/void")
    @PreAuthorize("hasAnyRole('MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<Order>> voidItem(
            @PathVariable UUID itemId,
            @RequestBody Map<String, String> body,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        Order updated = orderService.voidItem(itemId, body.get("reason"), tenantId);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }
}