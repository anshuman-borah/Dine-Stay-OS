package project.EnterpriseSaas.demo.modules.hotel.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.EnterpriseSaas.demo.common.enums.*;
import project.EnterpriseSaas.demo.modules.branch.entity.Branch;
import project.EnterpriseSaas.demo.modules.branch.repository.BranchRepository;
import project.EnterpriseSaas.demo.modules.hotel.dto.HotelDtos.IncomingBookingPayload;
import project.EnterpriseSaas.demo.modules.hotel.entity.*;
import project.EnterpriseSaas.demo.modules.hotel.repository.*;
import project.EnterpriseSaas.demo.modules.tenant.entity.Tenant;
import project.EnterpriseSaas.demo.modules.tenant.repository.TenantRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChannelManagerService {

    private final RoomTypeRepository roomTypeRepo;
    private final RoomRepository roomRepo;
    private final GuestRepository guestRepo;
    private final ReservationRepository reservationRepo;
    private final TenantRepository tenantRepo;
    private final BranchRepository branchRepo;

    @Transactional
    public Reservation processIncomingBooking(UUID tenantId, UUID branchId, IncomingBookingPayload payload) {
        log.info("Processing incoming booking from Channel Manager: {}", payload.getChannelManagerId());

        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
        Branch branch = branchRepo.findById(branchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));

        RoomType roomType = roomTypeRepo.findByTenantIdAndBranchIdAndIsActiveTrueOrderByNameAsc(tenantId, branchId).stream()
                .filter(rt -> payload.getRoomTypeId().equals(rt.getChannelManagerId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "RoomType mapping not found for CM ID: " + payload.getRoomTypeId()));

        Room availableRoom = roomRepo.findRoomsWithFilters(tenantId, branchId, RoomStatus.available).stream()
                .filter(r -> r.getRoomType().getId().equals(roomType.getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No available rooms for mapped RoomType: " + roomType.getName()));

        Guest guest = guestRepo.searchGuests(tenantId, payload.getGuest().getPhone(), org.springframework.data.domain.Pageable.unpaged()).stream()
                .findFirst()
                .orElseGet(() -> {
                    Guest newGuest = Guest.builder()
                            .tenantId(tenantId)
                            .name((payload.getGuest().getFirstName() + " " + payload.getGuest().getLastName()).trim())
                            .email(payload.getGuest().getEmail())
                            .phone(payload.getGuest().getPhone())
                            .build();
                    return guestRepo.save(newGuest);
                });

        LocalDate checkIn = LocalDate.parse(payload.getCheckInDate());
        LocalDate checkOut = LocalDate.parse(payload.getCheckOutDate());
        long numNights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (numNights <= 0) numNights = 1;

        BigDecimal totalAmount = payload.getTotalAmount();
        BigDecimal ratePerNight = totalAmount.divide(BigDecimal.valueOf(numNights), 2, RoundingMode.HALF_UP);
        BigDecimal taxRate = BigDecimal.valueOf(0.12);
        BigDecimal subtotal = totalAmount.divide(BigDecimal.ONE.add(taxRate), 2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = totalAmount.subtract(subtotal);

        Reservation reservation = Reservation.builder()
                .tenantId(tenantId)
                .branchId(branchId)
                .room(availableRoom)
                .primaryGuest(guest)
                .numAdults(payload.getNumAdults() != null ? payload.getNumAdults() : 1)
                .numChildren(payload.getNumChildren() != null ? payload.getNumChildren() : 0)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .status(ReservationStatus.confirmed)
                .ratePerNight(ratePerNight)
                .numNights((int) numNights)
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .balanceDue(totalAmount)
                .source(BookingSource.ota)
                .bookingRef(payload.getChannelManagerId())
                .notes("Auto-synced from Channel Manager")
                .build();

        Reservation saved = reservationRepo.save(reservation);

        availableRoom.setStatus(RoomStatus.reserved);
        roomRepo.save(availableRoom);

        return saved;
    }

    @Transactional
    public void processCancellation(UUID tenantId, UUID branchId, String bookingRef) {
        log.info("Processing Channel Manager cancellation: {}", bookingRef);
        Reservation reservation = reservationRepo.findReservationsWithFilters(tenantId, branchId, null, null, null, bookingRef, org.springframework.data.domain.Pageable.unpaged()).stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cancellation received for unknown bookingRef: " + bookingRef));

        if (reservation.getStatus() == ReservationStatus.cancelled) return;

        reservation.setStatus(ReservationStatus.cancelled);
        reservation.setNotes((reservation.getNotes() != null ? reservation.getNotes() + "\n" : "") + "Cancelled via Channel Manager");
        reservationRepo.save(reservation);

        Room room = reservation.getRoom();
        if (room != null && room.getStatus() == RoomStatus.reserved) {
            room.setStatus(RoomStatus.available);
            roomRepo.save(room);
        }
    }

    @Transactional
    public Reservation processModification(UUID tenantId, UUID branchId, IncomingBookingPayload payload) {
        log.info("Processing Channel Manager modification: {}", payload.getChannelManagerId());
        Reservation reservation = reservationRepo.findReservationsWithFilters(tenantId, branchId, null, null, null, payload.getChannelManagerId(), org.springframework.data.domain.Pageable.unpaged()).stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cannot modify unknown bookingRef: " + payload.getChannelManagerId()));

        LocalDate checkIn = LocalDate.parse(payload.getCheckInDate());
        LocalDate checkOut = LocalDate.parse(payload.getCheckOutDate());
        long numNights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (numNights <= 0) numNights = 1;

        BigDecimal totalAmount = payload.getTotalAmount();
        BigDecimal ratePerNight = totalAmount.divide(BigDecimal.valueOf(numNights), 2, RoundingMode.HALF_UP);
        BigDecimal taxRate = BigDecimal.valueOf(0.12);
        BigDecimal subtotal = totalAmount.divide(BigDecimal.ONE.add(taxRate), 2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = totalAmount.subtract(subtotal);

        reservation.setCheckInDate(checkIn);
        reservation.setCheckOutDate(checkOut);
        reservation.setNumAdults(payload.getNumAdults());
        reservation.setNumChildren(payload.getNumChildren());
        reservation.setNumNights((int) numNights);
        reservation.setRatePerNight(ratePerNight);
        reservation.setSubtotal(subtotal);
        reservation.setTaxAmount(taxAmount);
        reservation.setTotalAmount(totalAmount);
        reservation.setBalanceDue(totalAmount);
        reservation.setNotes((reservation.getNotes() != null ? reservation.getNotes() + "\n" : "") + "Modified via Channel Manager");

        return reservationRepo.save(reservation);
    }
}