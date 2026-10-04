//package project.EnterpriseSaas.demo.modules.hotel.kafka;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.stereotype.Component;
//import project.EnterpriseSaas.demo.modules.hotel.service.HotelService;
//import project.EnterpriseSaas.demo.modules.kafka.events.RoomChargeEvent;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class HotelKafkaConsumer {
//
//    private final HotelService hotelService;
//    private final ObjectMapper objectMapper; // Inject ObjectMapper
//
//    @KafkaListener(topics = "restaurant-room-charges", groupId = "hotel-module-group")
//    public void handleRoomCharge(String message) { // Accept String instead of RoomChargeEvent
//        try {
//            // Manually parse the JSON string into your Record
//            RoomChargeEvent event = objectMapper.readValue(message, RoomChargeEvent.class);
//
//            log.info("Kafka caught Room Charge event: ₹{} to Room {}", event.amount(), event.roomNumber());
//
//            hotelService.chargeToRoomFolio(
//                    event.tenantId(),
//                    event.branchId(),
//                    event.roomNumber(),
//                    event.amount(),
//                    event.orderNumber()
//            );
//        } catch (Exception e) {
//            log.error("Failed to parse or apply Room Charge from Kafka. Message: {}. Error: {}", message, e.getMessage());
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
import project.EnterpriseSaas.demo.modules.kafka.events.RoomChargeEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class HotelKafkaConsumer {

    private final HotelService hotelService;
    private final ObjectMapper objectMapper;

    // ── KAFKA LISTENER (Disabled on Render) ──────────────────────────────────
    @KafkaListener(
            topics = "restaurant-room-charges",
            groupId = "hotel-module-group",
            autoStartup = "${app.kafka.enabled:false}" // 🟢 Toggles off if Kafka is disabled
    )
    public void handleRoomCharge(String message) {
        try {
            RoomChargeEvent event = objectMapper.readValue(message, RoomChargeEvent.class);
            log.info("📥 [KAFKA] Caught Room Charge event: ₹{} to Room {}", event.amount(), event.roomNumber());
            hotelService.chargeToRoomFolio(event.tenantId(), event.branchId(), event.roomNumber(), event.amount(), event.orderNumber());
        } catch (Exception e) {
            log.error("❌ [KAFKA] Failed to apply Room Charge. Error: {}", e.getMessage());
        }
    }

    // ── DIRECT ASYNC FALLBACK (Runs on Render) ───────────────────────────────
    @Async // 🟢 Runs in background thread!
    public void processRoomChargeAsync(RoomChargeEvent event) {
        log.info("⚡ [ASYNC THREAD] Processing Room Charge directly: ₹{} to Room {}", event.amount(), event.roomNumber());
        try {
            hotelService.chargeToRoomFolio(event.tenantId(), event.branchId(), event.roomNumber(), event.amount(), event.orderNumber());
        } catch (Exception e) {
            log.error("❌ [ASYNC THREAD] Failed to apply Room Charge. Error: {}", e.getMessage());
        }
    }
}