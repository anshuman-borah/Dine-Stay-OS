package project.EnterpriseSaas.demo.modules.hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import project.EnterpriseSaas.demo.common.enums.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class HotelDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRoomTypeDto {
        private String name;
        private String description;
        private BigDecimal baseRate;
        private Integer maxOccupancy;
        private List<String> amenities;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRoomDto {
        private UUID roomTypeId;
        private String roomNumber;
        private Integer floor;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateGuestDto {
        private String name;
        private String phone;
        private String email;
        private IdType idType;
        private String idNumber;
        private String nationality;
        private String address;
        private String city;
        private String state;
        private String pincode;
        private String dob; // Expected format: YYYY-MM-DD
        private Gender gender;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateReservationDto {
        private UUID roomId;
        private UUID primaryGuestId;
        private CreateGuestDto guest; // Used if creating a new guest on the fly
        private Integer numAdults;
        private Integer numChildren;
        private String checkInDate;  // Expected format: YYYY-MM-DD
        private String checkOutDate; // Expected format: YYYY-MM-DD
        private BigDecimal ratePerNight;
        private BigDecimal advancePaid;
        private BookingSource source;
        private String bookingRef;
        private String specialRequests;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AddFolioChargeDto {
        private String description;
        private BigDecimal amount;
        private ChargeType chargeType;
        private String referenceId;
        private String date; // Expected format: YYYY-MM-DD
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateHkTaskDto {
        private UUID roomId;
        private HkTaskType taskType;
        private HkPriority priority;
        private String scheduledFor; // Expected format: YYYY-MM-DD
        private String notes;
        private UUID assignedTo;
    }

    // ─── Webhook Payloads (Channel Manager) ──────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IncomingBookingPayload {
        private String channelManagerId;
        private String roomTypeId;
        private GuestPayload guest;
        private String checkInDate;  // Expected format: YYYY-MM-DD
        private String checkOutDate; // Expected format: YYYY-MM-DD
        private Integer numAdults;
        private Integer numChildren;
        private BigDecimal totalAmount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GuestPayload {
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
    }
}