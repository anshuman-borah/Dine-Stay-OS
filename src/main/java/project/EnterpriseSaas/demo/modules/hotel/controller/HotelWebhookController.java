package project.EnterpriseSaas.demo.modules.hotel.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import project.EnterpriseSaas.demo.modules.hotel.dto.HotelDtos.GuestPayload;
import project.EnterpriseSaas.demo.modules.hotel.dto.HotelDtos.IncomingBookingPayload;
import project.EnterpriseSaas.demo.modules.hotel.service.ChannelManagerService;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/hotel/webhooks")
@RequiredArgsConstructor
public class HotelWebhookController {

    private final ChannelManagerService cmService;

    // Inject from application.yml or environment variables
    @Value("${app.webhooks.channel-manager.secret:test_secret_key}")
    private String validApiKey;

    @PostMapping("/channel-manager/{tenantId}/{branchId}")
    public ResponseEntity<Map<String, String>> handleChannelManagerWebhook(
            @PathVariable UUID tenantId,
            @PathVariable UUID branchId,
            @RequestHeader("x-api-key") String apiKey,
            @RequestBody Map<String, Object> payload
    ) {
        if (!validApiKey.equals(apiKey)) {
            log.warn("Unauthorized webhook attempt for tenant {}", tenantId);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid API Key");
        }

        String event = (String) payload.get("event");
        @SuppressWarnings("unchecked")
        Map<String, Object> bookingRaw = (Map<String, Object>) payload.get("booking");

        if ("BookingCreated".equals(event) || "BookingModified".equals(event)) {
            @SuppressWarnings("unchecked")
            Map<String, String> guestRaw = (Map<String, String>) bookingRaw.get("guest");

            IncomingBookingPayload bookingData = IncomingBookingPayload.builder()
                    .channelManagerId((String) bookingRaw.get("id"))
                    .roomTypeId((String) bookingRaw.get("roomTypeId"))
                    .guest(GuestPayload.builder()
                            .firstName(guestRaw.get("firstName"))
                            .lastName(guestRaw.get("lastName"))
                            .email(guestRaw.get("email"))
                            .phone(guestRaw.get("phone"))
                            .build())
                    .checkInDate((String) bookingRaw.get("checkInDate"))
                    .checkOutDate((String) bookingRaw.get("checkOutDate"))
                    .numAdults((Integer) bookingRaw.getOrDefault("numAdults", 1))
                    .numChildren((Integer) bookingRaw.getOrDefault("numChildren", 0))
                    .totalAmount(new BigDecimal(bookingRaw.get("totalAmount").toString()))
                    .build();

            if ("BookingCreated".equals(event)) {
                cmService.processIncomingBooking(tenantId, branchId, bookingData);
            } else {
                cmService.processModification(tenantId, branchId, bookingData);
            }
        } else if ("BookingCancelled".equals(event)) {
            cmService.processCancellation(tenantId, branchId, (String) bookingRaw.get("id"));
        }

        return ResponseEntity.ok(Map.of("status", "success"));
    }
}