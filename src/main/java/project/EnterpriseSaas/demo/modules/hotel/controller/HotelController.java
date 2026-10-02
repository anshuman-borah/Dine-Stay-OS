package project.EnterpriseSaas.demo.modules.hotel.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import project.EnterpriseSaas.demo.common.dto.ApiResponse;
import project.EnterpriseSaas.demo.common.enums.*;
import project.EnterpriseSaas.demo.modules.billing.entity.Bill;
import project.EnterpriseSaas.demo.modules.hotel.dto.HotelDtos.*;
import project.EnterpriseSaas.demo.modules.hotel.entity.*;
import project.EnterpriseSaas.demo.modules.hotel.service.HotelService;
import project.EnterpriseSaas.demo.modules.shift.service.ShiftService;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hotel")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;
    private final ShiftService shiftService;

    // ── Dashboard ──────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'CASHIER', 'RECEPTIONIST', 'HOTEL_MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hotelService.getDashboard(tenantId, branchId)));
    }

    // Connects to Next.js /hotel/dashboard/page.tsx
    @GetMapping("/reports/hotel-dashboard")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'CASHIER', 'RECEPTIONIST', 'HOTEL_MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHotelDashboardAnalytics(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hotelService.getHotelDashboardAnalytics(tenantId, branchId)));
    }

    // ── Room Types ─────────────────────────────────────────────────────────────

    @GetMapping("/room-types")
    public ResponseEntity<ApiResponse<List<RoomType>>> listRoomTypes(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader(value = "x-branch-id", required = false) UUID branchId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hotelService.listRoomTypes(tenantId, branchId)));
    }

    @PostMapping("/room-types")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'HOTEL_MANAGER')")
    public ResponseEntity<ApiResponse<RoomType>> createRoomType(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId,
            @RequestBody CreateRoomTypeDto dto
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hotelService.createRoomType(tenantId, branchId, dto)));
    }

    @PatchMapping("/room-types/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'HOTEL_MANAGER')")
    public ResponseEntity<ApiResponse<RoomType>> updateRoomType(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hotelService.updateRoomType(id, tenantId, body)));
    }

    @DeleteMapping("/room-types/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'HOTEL_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteRoomType(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        hotelService.deleteRoomType(id, tenantId);
        return ResponseEntity.noContent().build();
    }

    // ── Rooms ──────────────────────────────────────────────────────────────────

    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<Room>>> listRooms(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId,
            @RequestParam(value = "status", required = false) String status
    ) {
        RoomStatus roomStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            for (RoomStatus rs : RoomStatus.values()) {
                if (rs.name().equalsIgnoreCase(status.trim())) {
                    roomStatus = rs;
                    break;
                }
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(hotelService.listRooms(tenantId, branchId, roomStatus)));
    }

    @PostMapping("/rooms")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'HOTEL_MANAGER')")
    public ResponseEntity<ApiResponse<Room>> createRoom(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId,
            @RequestBody CreateRoomDto dto
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hotelService.createRoom(tenantId, branchId, dto)));
    }

    @PatchMapping("/rooms/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'HOTEL_MANAGER')")
    public ResponseEntity<ApiResponse<Room>> updateRoom(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hotelService.updateRoom(id, tenantId, body)));
    }

    @PatchMapping("/rooms/{id}/status")
    public ResponseEntity<ApiResponse<Room>> updateRoomStatus(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestBody Map<String, String> body
    ) {
        RoomStatus roomStatus = null;
        String status = body.get("status");
        if (status != null && !status.trim().isEmpty()) {
            for (RoomStatus rs : RoomStatus.values()) {
                if (rs.name().equalsIgnoreCase(status.trim())) {
                    roomStatus = rs;
                    break;
                }
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(hotelService.updateRoomStatus(id, tenantId, roomStatus)));
    }

    // ── Guests ─────────────────────────────────────────────────────────────────

    @GetMapping("/guests")
    public ResponseEntity<ApiResponse<List<Guest>>> searchGuests(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestParam(value = "q", required = false) String q
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hotelService.searchGuests(tenantId, q)));
    }

    @GetMapping("/guests/{id}")
    public ResponseEntity<ApiResponse<Guest>> getGuest(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hotelService.getGuest(id, tenantId)));
    }

    @PostMapping("/guests")
    public ResponseEntity<ApiResponse<Guest>> createGuest(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestBody CreateGuestDto dto
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hotelService.createGuest(tenantId, dto)));
    }

    @PatchMapping("/guests/{id}")
    public ResponseEntity<ApiResponse<Guest>> updateGuest(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hotelService.updateGuest(id, tenantId, body)));
    }

    // ── Reservations ───────────────────────────────────────────────────────────

    @GetMapping("/reservations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listReservations(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "50") int limit
    ) {
        // Safe case-insensitive enum parsing
        ReservationStatus reservationStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            for (ReservationStatus rs : ReservationStatus.values()) {
                if (rs.name().equalsIgnoreCase(status.trim())) {
                    reservationStatus = rs;
                    break;
                }
            }
        }

        // Safe HashMap to allow null values without crashing
        Map<String, Object> params = new HashMap<>();
        params.put("branchId", branchId);
        params.put("status", reservationStatus);
        params.put("from", from);
        params.put("to", to);
        params.put("search", search);
        params.put("page", page);
        params.put("limit", limit);

        Map<String, Object> result = hotelService.listReservations(tenantId, params);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/reservations/{id}")
    public ResponseEntity<ApiResponse<Reservation>> getReservation(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hotelService.getReservation(id, tenantId)));
    }

    @PostMapping("/reservations")
    public ResponseEntity<ApiResponse<Reservation>> createReservation(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId,
            @RequestBody CreateReservationDto dto,
            Principal principal
    ) {
        UUID userId = UUID.fromString(principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(hotelService.createReservation(tenantId, branchId, dto, userId)));
    }

    @PostMapping("/reservations/{id}/check-in")
    public ResponseEntity<ApiResponse<Reservation>> checkIn(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hotelService.checkIn(id, tenantId)));
    }

    @PostMapping("/reservations/{id}/check-out")
    public ResponseEntity<ApiResponse<Reservation>> checkOut(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hotelService.checkOut(id, tenantId)));
    }

    // 🟢 UPDATED: Cancel now pulls userId from Principal and passes it to the Service
    @PostMapping("/reservations/{id}/cancel")
    public ResponseEntity<ApiResponse<Reservation>> cancel(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestBody Map<String, String> body,
            Principal principal
    ) {
        String reason = body.getOrDefault("reason", "Cancelled by frontdesk staff");
        UUID userId = UUID.fromString(principal.getName()); // Extract logged-in user

        return ResponseEntity.ok(ApiResponse.ok(hotelService.cancelReservation(id, tenantId, reason, userId)));
    }

    // ── Folio Charges ──────────────────────────────────────────────────────────

    @GetMapping("/reservations/{id}/folio")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFolio(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hotelService.getFolio(id, tenantId)));
    }

    @PostMapping("/reservations/{id}/folio/charges")
    public ResponseEntity<ApiResponse<FolioCharge>> addCharge(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestBody AddFolioChargeDto dto
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hotelService.addFolioCharge(id, tenantId, dto)));
    }

    // ── Settlement & Invoicing ────────────────────────────────────────────────

    @PostMapping("/reservations/{id}/bill")
    public ResponseEntity<ApiResponse<Bill>> generateBill(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId,
            @RequestBody Map<String, Object> body
    ) {
        PaymentMethod paymentMethod = PaymentMethod.valueOf(body.getOrDefault("paymentMethod", "cash").toString().toLowerCase());
        BigDecimal amountPaid = new BigDecimal(body.getOrDefault("amountPaid", "0").toString());
        UUID shiftId = body.containsKey("shiftId") ? UUID.fromString(body.get("shiftId").toString()) : null;

        if (shiftId == null) {
            Map<String, Object> activeShift = shiftService.getActiveShift(branchId, tenantId, ShiftDepartment.hotel);
            if (activeShift != null && activeShift.get("id") != null) {
                shiftId = (UUID) activeShift.get("id");
            }
        }

        if (paymentMethod == PaymentMethod.cash && shiftId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active hotel shift. Open a shift before accepting cash.");
        }

        return ResponseEntity.ok(ApiResponse.ok(hotelService.generateBill(id, tenantId, paymentMethod, amountPaid, shiftId)));
    }

    // ── Reports ─────────────────────────────────────────────────────────────────

    @GetMapping("/reports/gstr1-export")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'HOTEL_MANAGER')")
    public ResponseEntity<Map<String, Object>> exportGstr1(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId,
            @RequestParam("from") String from,
            @RequestParam("to") String to
    ) {
        return ResponseEntity.ok(hotelService.getGstr1Export(tenantId, branchId, LocalDate.parse(from), LocalDate.parse(to)));
    }

    @GetMapping("/reports/revenue")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'HOTEL_MANAGER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRevenueReport(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId,
            @RequestParam("from") String from,
            @RequestParam("to") String to
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hotelService.getRevenueReport(tenantId, branchId, LocalDate.parse(from), LocalDate.parse(to))));
    }

    @GetMapping("/reports/bookings")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getBookingsReport(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId,
            @RequestParam("from") String from,
            @RequestParam("to") String to
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hotelService.getBookingsReport(tenantId, branchId, LocalDate.parse(from), LocalDate.parse(to))));
    }

    @GetMapping("/reports/rooms")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRoomsReport(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId,
            @RequestParam("from") String from,
            @RequestParam("to") String to
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hotelService.getRoomsPerformanceReport(tenantId, branchId, LocalDate.parse(from), LocalDate.parse(to))));
    }

    @GetMapping("/reports/occupancy-summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOccupancySummary(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hotelService.getOccupancySummary(tenantId, branchId)));
    }

    @GetMapping("/reports/payments")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'HOTEL_MANAGER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPaymentsReport(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId,
            @RequestParam("from") String from,
            @RequestParam("to") String to
    ) {
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);

        OffsetDateTime fromTime = fromDate.atStartOfDay(ZoneId.of("Asia/Kolkata")).toOffsetDateTime();
        OffsetDateTime toTime = toDate.atTime(23, 59, 59).atZone(ZoneId.of("Asia/Kolkata")).toOffsetDateTime();

        return ResponseEntity.ok(ApiResponse.ok(hotelService.getPaymentsReport(tenantId, branchId, fromTime, toTime)));
    }

    @GetMapping("/reports/gst")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'HOTEL_MANAGER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getGstReport(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId,
            @RequestParam("from") String from,
            @RequestParam("to") String to
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hotelService.getGstReport(tenantId, branchId, LocalDate.parse(from), LocalDate.parse(to))));
    }

    @GetMapping("/reports/frontdesk")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'HOTEL_MANAGER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFrontDeskReport(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId,
            @RequestParam("from") String from,
            @RequestParam("to") String to
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hotelService.getFrontDeskReport(tenantId, branchId, LocalDate.parse(from), LocalDate.parse(to))));
    }

    // ── Housekeeping ────────────────────────────────────────────────────────────

    @GetMapping("/housekeeping")
    public ResponseEntity<ApiResponse<List<HousekeepingTask>>> listTasks(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId,
            @RequestParam(value = "date", required = false) String date
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hotelService.listHousekeepingTasks(tenantId, branchId, date)));
    }

    @PostMapping("/housekeeping")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<HousekeepingTask>> createTask(
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestHeader("x-branch-id") UUID branchId,
            @RequestBody CreateHkTaskDto dto
    ) {
        return ResponseEntity.ok(ApiResponse.ok(hotelService.createHousekeepingTask(tenantId, branchId, dto)));
    }

    @PatchMapping("/housekeeping/{id}")
    public ResponseEntity<ApiResponse<HousekeepingTask>> updateTask(
            @PathVariable UUID id,
            @RequestHeader("x-tenant-id") UUID tenantId,
            @RequestBody Map<String, Object> body
    ) {
        String statusStr = body.get("status").toString();
        HkStatus status = null;

        for (HkStatus s : HkStatus.values()) {
            if (s.name().equalsIgnoreCase(statusStr)) {
                status = s;
                break;
            }
        }

        String notes = (String) body.get("notes");
        return ResponseEntity.ok(ApiResponse.ok(hotelService.updateHousekeepingTask(id, tenantId, status, notes)));
    }
}