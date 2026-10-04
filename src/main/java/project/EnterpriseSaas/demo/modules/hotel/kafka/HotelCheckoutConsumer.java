//package project.EnterpriseSaas.demo.modules.hotel.kafka;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.stereotype.Component;
//import project.EnterpriseSaas.demo.modules.hotel.service.HotelService;
//import project.EnterpriseSaas.demo.modules.inventory.service.InventoryService;
//import project.EnterpriseSaas.demo.modules.order.gateway.OrdersGateway;
//import project.EnterpriseSaas.demo.modules.hotel.repository.FolioChargeRepository;
//import project.EnterpriseSaas.demo.modules.hotel.entity.Room;
//import project.EnterpriseSaas.demo.modules.hotel.repository.RoomRepository;
//import project.EnterpriseSaas.demo.modules.hotel.entity.HousekeepingTask;
//import project.EnterpriseSaas.demo.modules.hotel.repository.HousekeepingTaskRepository; // 🟢 IMPORTED REPO
//import project.EnterpriseSaas.demo.common.enums.*;
//
//import java.time.LocalDate;
//import java.time.ZoneId;
//import java.util.Map;
//import java.util.UUID;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class HotelCheckoutConsumer {
//
//    private final HotelService hotelService;
//    private final InventoryService inventoryService;
//    private final FolioChargeRepository folioRepo;
//    private final RoomRepository roomRepo;
//    private final HousekeepingTaskRepository hkRepo; // 🟢 INJECTED REPO
//    private final OrdersGateway ordersGateway;
//    private final ObjectMapper objectMapper;
//
//    @KafkaListener(topics = "hotel-checkouts", groupId = "hotel-module-group")
//    public void handleCheckout(String message) {
//        try {
//            Map<String, String> event = objectMapper.readValue(message, Map.class);
//            UUID tenantId = UUID.fromString(event.get("tenantId"));
//            UUID branchId = UUID.fromString(event.get("branchId"));
//            UUID roomId = UUID.fromString(event.get("roomId"));
//            UUID reservationId = UUID.fromString(event.get("reservationId"));
//
//            log.info("Kafka caught Checkout event for Room {}. Processing async tasks...", roomId);
//
//            // 1. Create the Housekeeping Task
//            Room room = roomRepo.findById(roomId).orElseThrow();
//
//            HousekeepingTask task = HousekeepingTask.builder()
//                    .tenantId(tenantId)
//                    .branchId(branchId)
//                    .roomId(roomId)
//                    .room(room)
//                    .reservationId(reservationId)
//                    .taskType(HkTaskType.checkout_clean)
//                    .status(HkStatus.pending)
//                    .priority(HkPriority.high)
//                    .scheduledFor(LocalDate.now(ZoneId.of("Asia/Kolkata")))
//                    .build();
//
//            // 🟢 SAVING THE TASK TO THE DB
//            hkRepo.save(task);
//
//            // 2. Deduct Minibar items from Inventory
//            var charges = folioRepo.findByReservationIdAndTenantIdOrderByCreatedAtAsc(reservationId, tenantId);
//            inventoryService.deductMinibarItems(tenantId, branchId, charges);
//
//            // 3. Broadcast WebSocket event to update the Housekeeping Dashboard instantly
//            ordersGateway.broadcastHousekeepingUpdate(branchId.toString(), "hk:taskCreated");
//
//        } catch (Exception e) {
//            log.error("Failed to process Kafka checkout event. Error: {}", e.getMessage());
//        }
//    }
//}
package project.EnterpriseSaas.demo.modules.hotel.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import project.EnterpriseSaas.demo.modules.hotel.service.HotelService;
import project.EnterpriseSaas.demo.modules.inventory.service.InventoryService;
import project.EnterpriseSaas.demo.modules.order.gateway.OrdersGateway;
import project.EnterpriseSaas.demo.modules.hotel.repository.FolioChargeRepository;
import project.EnterpriseSaas.demo.modules.hotel.entity.Room;
import project.EnterpriseSaas.demo.modules.hotel.repository.RoomRepository;
import project.EnterpriseSaas.demo.modules.hotel.entity.HousekeepingTask;
import project.EnterpriseSaas.demo.modules.hotel.repository.HousekeepingTaskRepository;
import project.EnterpriseSaas.demo.common.enums.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class HotelCheckoutConsumer {

    private final HotelService hotelService;
    private final InventoryService inventoryService;
    private final FolioChargeRepository folioRepo;
    private final RoomRepository roomRepo;
    private final HousekeepingTaskRepository hkRepo;
    private final OrdersGateway ordersGateway;
    private final ObjectMapper objectMapper;

    // ── KAFKA LISTENER ───────────────────────────────────────────────────────
    @KafkaListener(
            topics = "hotel-checkouts",
            groupId = "hotel-module-group",
            autoStartup = "${app.kafka.enabled:false}" // 🟢 Toggle added
    )
    public void handleCheckout(String message) {
        try {
            Map<String, String> event = objectMapper.readValue(message, Map.class);
            UUID tenantId = UUID.fromString(event.get("tenantId"));
            UUID branchId = UUID.fromString(event.get("branchId"));
            UUID roomId = UUID.fromString(event.get("roomId"));
            UUID reservationId = UUID.fromString(event.get("reservationId"));

            log.info("📥 [KAFKA] Caught Checkout event for Room {}.", roomId);
            executeCheckoutTasks(tenantId, branchId, roomId, reservationId);
        } catch (Exception e) {
            log.error("❌ [KAFKA] Failed to process checkout event. Error: {}", e.getMessage());
        }
    }

    // ── DIRECT ASYNC FALLBACK ────────────────────────────────────────────────
    @Async
    public void processCheckoutAsync(UUID tenantId, UUID branchId, UUID roomId, UUID reservationId) {
        log.info("⚡ [ASYNC THREAD] Processing Checkout tasks directly for Room {}", roomId);
        try {
            executeCheckoutTasks(tenantId, branchId, roomId, reservationId);
        } catch (Exception e) {
            log.error("❌ [ASYNC THREAD] Failed to process checkout event. Error: {}", e.getMessage());
        }
    }

    // ── CORE LOGIC ───────────────────────────────────────────────────────────
    private void executeCheckoutTasks(UUID tenantId, UUID branchId, UUID roomId, UUID reservationId) {
        Room room = roomRepo.findById(roomId).orElseThrow();
        HousekeepingTask task = HousekeepingTask.builder()
                .tenantId(tenantId).branchId(branchId).roomId(roomId).room(room)
                .reservationId(reservationId).taskType(HkTaskType.checkout_clean)
                .status(HkStatus.pending).priority(HkPriority.high)
                .scheduledFor(LocalDate.now(ZoneId.of("Asia/Kolkata"))).build();
        hkRepo.save(task);

        var charges = folioRepo.findByReservationIdAndTenantIdOrderByCreatedAtAsc(reservationId, tenantId);
        inventoryService.deductMinibarItems(tenantId, branchId, charges);
        ordersGateway.broadcastHousekeepingUpdate(branchId.toString(), "hk:taskCreated");
    }
}