

// package project.EnterpriseSaas.demo.modules.hotel.service;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.annotation.Lazy;
// import org.springframework.cache.annotation.CacheEvict;
// import org.springframework.cache.annotation.Cacheable;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.dao.DataIntegrityViolationException;
// import org.springframework.data.domain.PageRequest;
// import org.springframework.data.domain.Pageable;
// import org.springframework.http.HttpStatus;
// import org.springframework.jdbc.core.JdbcTemplate;
// import org.springframework.kafka.core.KafkaTemplate;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;
// import org.springframework.web.server.ResponseStatusException;
// import project.EnterpriseSaas.demo.common.enums.*;
// import project.EnterpriseSaas.demo.modules.billing.entity.Bill;
// import project.EnterpriseSaas.demo.modules.billing.entity.Payment;
// import project.EnterpriseSaas.demo.modules.billing.repository.BillRepository;
// import project.EnterpriseSaas.demo.modules.billing.repository.PaymentRepository;
// import project.EnterpriseSaas.demo.modules.branch.repository.BranchRepository;
// import project.EnterpriseSaas.demo.modules.core.service.EmailService;
// import project.EnterpriseSaas.demo.modules.hotel.dto.HotelDtos.*;
// import project.EnterpriseSaas.demo.modules.hotel.entity.*;
// import project.EnterpriseSaas.demo.modules.hotel.repository.*;
// import project.EnterpriseSaas.demo.modules.shift.entity.Shift;
// import project.EnterpriseSaas.demo.modules.shift.repository.ShiftRepository;
// import project.EnterpriseSaas.demo.modules.tenant.repository.TenantRepository;
// import project.EnterpriseSaas.demo.modules.hotel.kafka.HotelCheckoutConsumer;
// import project.EnterpriseSaas.demo.modules.audit.service.AuditService;
// import project.EnterpriseSaas.demo.modules.audit.entity.AuditLog;

// import java.math.BigDecimal;
// import java.math.RoundingMode;
// import java.time.LocalDate;
// import java.time.LocalDateTime;
// import java.time.OffsetDateTime;
// import java.time.ZoneId;
// import java.time.temporal.ChronoUnit;
// import java.util.*;
// import java.util.concurrent.CompletableFuture;

// @Service
// @RequiredArgsConstructor
// @Slf4j
// @Transactional(readOnly = true)
// public class HotelService {

//     private final RoomTypeRepository roomTypeRepo;
//     private final RoomRepository roomRepo;
//     private final GuestRepository guestRepo;
//     private final ReservationRepository reservationRepo;
//     private final FolioChargeRepository folioRepo;
//     private final HousekeepingTaskRepository hkRepo;
//     private final BillRepository billRepo;
//     private final PaymentRepository paymentRepo;
//     private final TenantRepository tenantRepo;
//     private final BranchRepository branchRepo;
//     private final ShiftRepository shiftRepo;
//     private final JdbcTemplate jdbcTemplate;
//     private final EmailService emailService;

//     // 🟢 INJECTIONS
//     private final KafkaTemplate<String, String> kafkaTemplate;
//     private final ObjectMapper objectMapper;

//     @Value("${app.kafka.enabled:false}")
//     private boolean kafkaEnabled;

//     @Autowired
//     @Lazy // Resolves circular dependencies safely
//     private HotelCheckoutConsumer hotelCheckoutConsumer;

//     @Autowired
//     @Lazy
//     private AuditService auditService;

//     private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Kolkata");

//     @Transactional
//     @CacheEvict(value = "roomTypes", allEntries = true)
//     public RoomType createRoomType(UUID tenantId, UUID branchId, CreateRoomTypeDto dto) {
//         tenantRepo.findById(tenantId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
//         branchRepo.findById(branchId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));

//         RoomType rt = RoomType.builder()
//                 .tenantId(tenantId)
//                 .branchId(branchId)
//                 .name(dto.getName())
//                 .description(dto.getDescription())
//                 .baseRate(dto.getBaseRate())
//                 .maxOccupancy(dto.getMaxOccupancy() != null ? dto.getMaxOccupancy() : 2)
//                 .amenities(dto.getAmenities() != null ? dto.getAmenities() : List.of())
//                 .isActive(true)
//                 .build();
//         return roomTypeRepo.save(rt);
//     }

//     @Cacheable(value = "roomTypes")
//     public List<RoomType> listRoomTypes(UUID tenantId, UUID branchId) {
//         return branchId != null
//                 ? roomTypeRepo.findByTenantIdAndBranchIdAndIsActiveTrueOrderByNameAsc(tenantId, branchId)
//                 : roomTypeRepo.findByTenantIdAndIsActiveTrueOrderByNameAsc(tenantId);
//     }

//     @Transactional
//     @CacheEvict(value = "roomTypes", allEntries = true)
//     public RoomType updateRoomType(UUID id, UUID tenantId, Map<String, Object> data) {
//         RoomType rt = roomTypeRepo.findByIdAndTenantId(id, tenantId)
//                 .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "RoomType not found"));

//         if (data.containsKey("name")) rt.setName((String) data.get("name"));
//         if (data.containsKey("description")) rt.setDescription((String) data.get("description"));
//         if (data.containsKey("baseRate")) rt.setBaseRate(new BigDecimal(data.get("baseRate").toString()));
//         if (data.containsKey("maxOccupancy")) rt.setMaxOccupancy(Integer.parseInt(data.get("maxOccupancy").toString()));

//         return roomTypeRepo.save(rt);
//     }

//     @Transactional
//     @CacheEvict(value = "roomTypes", allEntries = true)
//     public void deleteRoomType(UUID id, UUID tenantId) {
//         RoomType rt = roomTypeRepo.findByIdAndTenantId(id, tenantId)
//                 .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "RoomType not found"));
//         rt.setIsActive(false);
//         roomTypeRepo.save(rt);
//     }

//     @Transactional
//     public Room createRoom(UUID tenantId, UUID branchId, CreateRoomDto dto) {
//         Optional<Room> exists = roomRepo.findByTenantIdAndBranchIdAndRoomNumber(tenantId, branchId, dto.getRoomNumber());
//         if (exists.isPresent()) {
//             throw new ResponseStatusException(HttpStatus.CONFLICT, "Room " + dto.getRoomNumber() + " already exists");
//         }

//         RoomType rt = roomTypeRepo.findByIdAndTenantId(dto.getRoomTypeId(), tenantId)
//                 .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room type not found"));

//         Room room = Room.builder()
//                 .tenantId(tenantId)
//                 .branchId(branchId)
//                 .roomTypeId(rt.getId())
//                 .roomType(rt)
//                 .roomNumber(dto.getRoomNumber())
//                 .floor(dto.getFloor() != null ? dto.getFloor() : 1)
//                 .notes(dto.getNotes())
//                 .status(RoomStatus.available)
//                 .isActive(true)
//                 .build();

//         Room saved = roomRepo.save(room);
//         roomTypeRepo.incrementTotalRooms(rt.getId(), 1);
//         return saved;
//     }

//     public List<Room> listRooms(UUID tenantId, UUID branchId, RoomStatus status) {
//         return roomRepo.findRoomsWithFilters(tenantId, branchId, status);
//     }

//     @Transactional
//     public Room updateRoom(UUID id, UUID tenantId, Map<String, Object> data) {
//         Room room = roomRepo.findByIdAndTenantId(id, tenantId)
//                 .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

//         boolean statusChanged = false;
//         RoomStatus oldStatus = room.getStatus();

//         if (data.containsKey("roomNumber")) room.setRoomNumber((String) data.get("roomNumber"));
//         if (data.containsKey("floor")) room.setFloor(Integer.parseInt(data.get("floor").toString()));
//         if (data.containsKey("notes")) room.setNotes((String) data.get("notes"));
//         if (data.containsKey("status")) {
//             RoomStatus status = RoomStatus.valueOf(data.get("status").toString().toLowerCase());
//             if (status != oldStatus) {
//                 room.setStatus(status);
//                 statusChanged = true;
//             }
//         }

//         Room saved = roomRepo.save(room);
//         if (statusChanged) {
//             syncHousekeepingOnStatusChange(id, tenantId, room.getStatus(), room.getBranchId());
//         }
//         return saved;
//     }

//     @Transactional
//     public Room updateRoomStatus(UUID id, UUID tenantId, RoomStatus status) {
//         Room room = roomRepo.findByIdAndTenantId(id, tenantId)
//                 .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

//         boolean statusChanged = status != room.getStatus();
//         room.setStatus(status);
//         Room saved = roomRepo.save(room);

//         if (statusChanged) {
//             syncHousekeepingOnStatusChange(id, tenantId, status, room.getBranchId());
//         }
//         return saved;
//     }

//     private void syncHousekeepingOnStatusChange(UUID roomId, UUID tenantId, RoomStatus status, UUID branchId) {
//         LocalDate today = LocalDate.now(HOTEL_ZONE);
//         if (status == RoomStatus.available) {
//             hkRepo.autoResolvePendingTasks(roomId, tenantId, today, List.of(HkStatus.pending, HkStatus.in_progress), HkStatus.done, LocalDateTime.now(HOTEL_ZONE), "Auto-resolved via Room Status update");
//         } else if (status == RoomStatus.cleaning || status == RoomStatus.maintenance) {
//             HkTaskType type = status == RoomStatus.cleaning ? HkTaskType.stayover : HkTaskType.maintenance;
//             Optional<HousekeepingTask> existing = hkRepo.findByRoomIdAndTenantIdAndScheduledForAndStatusIn(roomId, tenantId, today, List.of(HkStatus.pending, HkStatus.in_progress));
//             if (existing.isEmpty()) {
//                 Room room = roomRepo.findById(roomId).orElse(null);

//                 HousekeepingTask task = HousekeepingTask.builder()
//                         .tenantId(tenantId)
//                         .branchId(branchId)
//                         .roomId(roomId)
//                         .room(room)
//                         .taskType(type)
//                         .status(HkStatus.pending)
//                         .priority(HkPriority.normal)
//                         .scheduledFor(today)
//                         .notes("Auto-generated from room status update")
//                         .build();
//                 hkRepo.save(task);
//             }
//         }
//     }

//     @Transactional
//     public Guest createGuest(UUID tenantId, CreateGuestDto dto) {
//         Guest guest = Guest.builder()
//                 .tenantId(tenantId)
//                 .name(dto.getName())
//                 .phone(dto.getPhone())
//                 .email(dto.getEmail())
//                 .idType(dto.getIdType())
//                 .idNumber(dto.getIdNumber())
//                 .nationality(dto.getNationality() != null ? dto.getNationality() : "India")
//                 .address(dto.getAddress())
//                 .city(dto.getCity())
//                 .state(dto.getState())
//                 .pincode(dto.getPincode())
//                 .dob(dto.getDob() != null ? LocalDate.parse(dto.getDob()) : null)
//                 .gender(dto.getGender())
//                 .build();
//         return guestRepo.save(guest);
//     }

//     public List<Guest> searchGuests(UUID tenantId, String query) {
//         if (query == null || query.trim().length() < 2) {
//             return guestRepo.findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(0, 20));
//         }
//         return guestRepo.searchGuests(tenantId, query.trim(), PageRequest.of(0, 20));
//     }

//     public Guest getGuest(UUID id, UUID tenantId) {
//         return guestRepo.findByIdAndTenantId(id, tenantId)
//                 .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Guest not found"));
//     }

//     @Transactional
//     public Guest updateGuest(UUID id, UUID tenantId, Map<String, Object> data) {
//         Guest guest = getGuest(id, tenantId);
//         if (data.containsKey("name")) guest.setName((String) data.get("name"));
//         if (data.containsKey("phone")) guest.setPhone((String) data.get("phone"));
//         if (data.containsKey("email")) guest.setEmail((String) data.get("email"));
//         if (data.containsKey("nationality")) guest.setNationality((String) data.get("nationality"));
//         if (data.containsKey("address")) guest.setAddress((String) data.get("address"));
//         if (data.containsKey("city")) guest.setCity((String) data.get("city"));
//         if (data.containsKey("state")) guest.setState((String) data.get("state"));
//         if (data.containsKey("pincode")) guest.setPincode((String) data.get("pincode"));
//         if (data.containsKey("gender")) guest.setGender(Gender.valueOf(data.get("gender").toString().toLowerCase()));

//         return guestRepo.save(guest);
//     }

//     @Transactional
//     public Reservation createReservation(UUID tenantId, UUID branchId, CreateReservationDto dto, UUID userId) {
//         UUID guestId = dto.getPrimaryGuestId();
//         if (guestId == null && dto.getGuest() != null) {
//             Guest g = createGuest(tenantId, dto.getGuest());
//             guestId = g.getId();
//         }
//         if (guestId == null) {
//             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Guest information is required");
//         }

//         Guest guest = guestRepo.findById(guestId)
//                 .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Guest not found"));

//         Room room = roomRepo.findByIdAndTenantId(dto.getRoomId(), tenantId)
//                 .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

//         if (room.getStatus() == RoomStatus.maintenance || room.getStatus() == RoomStatus.out_of_order) {
//             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room is not available");
//         }

//         LocalDate checkIn = LocalDate.parse(dto.getCheckInDate());
//         LocalDate checkOut = LocalDate.parse(dto.getCheckOutDate());
//         if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
//             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Check-out date must be after Check-in");
//         }

//         List<Reservation> overlaps = reservationRepo.findOverlappingReservations(room.getId(), checkIn, checkOut,
//                 List.of(ReservationStatus.cancelled, ReservationStatus.no_show, ReservationStatus.checked_out));
//         if (!overlaps.isEmpty()) {
//             throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is already booked for selected dates");
//         }

//         long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
//         BigDecimal rate = dto.getRatePerNight() != null ? dto.getRatePerNight() : room.getRoomType().getBaseRate();
//         BigDecimal subtotal = rate.multiply(BigDecimal.valueOf(nights));
//         BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.12));
//         BigDecimal total = subtotal.add(tax);
//         BigDecimal advance = dto.getAdvancePaid() != null ? dto.getAdvancePaid() : BigDecimal.ZERO;

//         Reservation reservation = Reservation.builder()
//                 .tenantId(tenantId)
//                 .branchId(branchId)
//                 .room(room)
//                 .primaryGuest(guest)
//                 .numAdults(dto.getNumAdults() != null ? dto.getNumAdults() : 1)
//                 .numChildren(dto.getNumChildren() != null ? dto.getNumChildren() : 0)
//                 .checkInDate(checkIn)
//                 .checkOutDate(checkOut)
//                 .status(ReservationStatus.confirmed)
//                 .ratePerNight(rate)
//                 .numNights((int) nights)
//                 .subtotal(subtotal)
//                 .taxAmount(tax)
//                 .totalAmount(total)
//                 .advancePaid(advance)
//                 .balanceDue(total.subtract(advance))
//                 .source(dto.getSource() != null ? dto.getSource() : BookingSource.walk_in)
//                 .bookingRef(dto.getBookingRef())
//                 .specialRequests(dto.getSpecialRequests())
//                 .notes(dto.getNotes())
//                 .createdById(userId)
//                 .build();

//         Reservation saved = reservationRepo.save(reservation);

//         room.setStatus(RoomStatus.reserved);
//         roomRepo.save(room);

//         if (advance.compareTo(BigDecimal.ZERO) > 0) {
//             FolioCharge charge = FolioCharge.builder()
//                     .tenantId(tenantId)
//                     .reservationId(saved.getId())
//                     .description("Advance payment")
//                     .amount(advance.negate())
//                     .chargeType(ChargeType.advance)
//                     .date(LocalDate.now(HOTEL_ZONE))
//                     .build();
//             folioRepo.save(charge);
//         }

//         return saved;
//     }

//     public Map<String, Object> listReservations(UUID tenantId, Map<String, Object> query) {
//         UUID branchId = (UUID) query.get("branchId");
//         ReservationStatus statusObj = (ReservationStatus) query.get("status");
//         String status = statusObj != null ? statusObj.name() : null;

//         Object fromObj = query.get("from");
//         Object toObj = query.get("to");
//         String search = (String) query.get("search");

//         int page = query.containsKey("page") ? (int) query.get("page") : 1;
//         int limit = query.containsKey("limit") ? (int) query.get("limit") : 25;
//         int offset = (page - 1) * limit;

//         StringBuilder sql = new StringBuilder("""
//             SELECT
//                 r.id, r.status, r.check_in_date, r.check_out_date, r.num_nights,
//                 r.rate_per_night, r.total_amount, r.advance_paid, r.balance_due,
//                 r.num_adults, r.num_children, r.source, r.booking_ref,
//                 g.id AS guest_id, g.name AS guest_name, g.phone AS guest_phone, g.email AS guest_email,
//                 rm.id AS room_id, rm.room_number,
//                 rt.name AS room_type_name
//             FROM hotel_reservations r
//             LEFT JOIN hotel_guests g ON r.primary_guest_id = g.id
//             LEFT JOIN hotel_rooms rm ON r.room_id = rm.id
//             LEFT JOIN hotel_room_types rt ON rm.room_type_id = rt.id
//             WHERE r.tenant_id = ?
//         """);

//         StringBuilder countSql = new StringBuilder("""
//             SELECT COUNT(r.id)
//             FROM hotel_reservations r
//             LEFT JOIN hotel_guests g ON r.primary_guest_id = g.id
//             LEFT JOIN hotel_rooms rm ON r.room_id = rm.id
//             WHERE r.tenant_id = ?
//         """);

//         List<Object> params = new ArrayList<>();
//         params.add(tenantId);

//         if (branchId != null) {
//             sql.append(" AND r.branch_id = ?");
//             countSql.append(" AND r.branch_id = ?");
//             params.add(branchId);
//         }
//         if (status != null && !status.isEmpty()) {
//             sql.append(" AND r.status = ?");
//             countSql.append(" AND r.status = ?");
//             params.add(status);
//         }
//         if (fromObj != null && !fromObj.toString().trim().isEmpty()) {
//             sql.append(" AND r.check_in_date >= CAST(? AS DATE)");
//             countSql.append(" AND r.check_in_date >= CAST(? AS DATE)");
//             params.add(fromObj.toString().trim());
//         }
//         if (toObj != null && !toObj.toString().trim().isEmpty()) {
//             sql.append(" AND r.check_out_date <= CAST(? AS DATE)");
//             countSql.append(" AND r.check_out_date <= CAST(? AS DATE)");
//             params.add(toObj.toString().trim());
//         }
//         if (search != null && !search.trim().isEmpty()) {
//             String searchPattern = "%" + search.trim().toLowerCase() + "%";
//             String searchClause = " AND (LOWER(g.name) LIKE ? OR LOWER(g.phone) LIKE ? OR LOWER(rm.room_number) LIKE ? OR LOWER(r.booking_ref) LIKE ?)";
//             sql.append(searchClause);
//             countSql.append(searchClause);
//             params.add(searchPattern);
//             params.add(searchPattern);
//             params.add(searchPattern);
//             params.add(searchPattern);
//         }

//         Long total = jdbcTemplate.queryForObject(countSql.toString(), Long.class, params.toArray());

//         sql.append(" ORDER BY r.check_in_date DESC LIMIT ? OFFSET ?");
//         params.add(limit);
//         params.add(offset);

//         List<Map<String, Object>> data = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
//             Map<String, Object> map = new LinkedHashMap<>();
//             map.put("id", rs.getObject("id", UUID.class));
//             map.put("status", rs.getString("status"));
//             map.put("checkInDate", rs.getString("check_in_date"));
//             map.put("checkOutDate", rs.getString("check_out_date"));
//             map.put("numNights", rs.getInt("num_nights"));
//             map.put("ratePerNight", rs.getBigDecimal("rate_per_night"));
//             map.put("totalAmount", rs.getBigDecimal("total_amount"));
//             map.put("advancePaid", rs.getBigDecimal("advance_paid"));
//             map.put("balanceDue", rs.getBigDecimal("balance_due"));
//             map.put("numAdults", rs.getInt("num_adults"));
//             map.put("numChildren", rs.getInt("num_children"));
//             map.put("source", rs.getString("source"));
//             map.put("bookingRef", rs.getString("booking_ref"));

//             map.put("primaryGuest", Map.of(
//                     "id", rs.getObject("guest_id", UUID.class) != null ? rs.getObject("guest_id", UUID.class) : UUID.randomUUID(),
//                     "name", rs.getString("guest_name") != null ? rs.getString("guest_name") : "Unknown",
//                     "phone", rs.getString("guest_phone") != null ? rs.getString("guest_phone") : "No phone",
//                     "email", rs.getString("guest_email") != null ? rs.getString("guest_email") : ""
//             ));

//             map.put("room", Map.of(
//                     "id", rs.getObject("room_id", UUID.class) != null ? rs.getObject("room_id", UUID.class) : UUID.randomUUID(),
//                     "roomNumber", rs.getString("room_number") != null ? rs.getString("room_number") : "Unknown",
//                     "roomType", Map.of("name", rs.getString("room_type_name") != null ? rs.getString("room_type_name") : "Standard")
//             ));

//             return map;
//         }, params.toArray());

//         return Map.of(
//                 "data", data,
//                 "total", total != null ? total : 0,
//                 "page", page,
//                 "limit", limit
//         );
//     }

//     public Reservation getReservation(UUID id, UUID tenantId) {
//         return reservationRepo.findByIdAndTenantId(id, tenantId)
//                 .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));
//     }

//     @Transactional
//     public Reservation checkIn(UUID id, UUID tenantId) {
//         Reservation r = getReservation(id, tenantId);
//         if (r.getStatus() != ReservationStatus.confirmed) {
//             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot check-in active booking in status: " + r.getStatus());
//         }

//         r.setStatus(ReservationStatus.checked_in);
//         r.setActualCheckIn(LocalDateTime.now(HOTEL_ZONE));
//         reservationRepo.save(r);

//         Room room = r.getRoom();
//         room.setStatus(RoomStatus.occupied);
//         roomRepo.save(room);

//         return r;
//     }

//     @Transactional
//     public Reservation checkOut(UUID id, UUID tenantId) {
//         Reservation r = getReservation(id, tenantId);
//         if (r.getStatus() != ReservationStatus.checked_in) {
//             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot check-out active booking in status: " + r.getStatus());
//         }

//         r.setStatus(ReservationStatus.checked_out);
//         r.setActualCheckOut(LocalDateTime.now(HOTEL_ZONE));
//         reservationRepo.save(r);

//         Room room = r.getRoom();
//         room.setStatus(RoomStatus.cleaning);
//         roomRepo.save(room);

//         Guest guest = r.getPrimaryGuest();
//         guest.setTotalStays(guest.getTotalStays() + 1);
//         guestRepo.save(guest);

//         try {
//             // 🟢 CHECKOUT TOGGLE
//             if (kafkaEnabled) {
//                 Map<String, String> event = new HashMap<>();
//                 event.put("reservationId", id.toString());
//                 event.put("roomId", room.getId().toString());
//                 event.put("tenantId", tenantId.toString());
//                 event.put("branchId", r.getBranchId().toString());

//                 String message = objectMapper.writeValueAsString(event);
//                 kafkaTemplate.send("hotel-checkouts", message);
//                 log.info("🚀 [API] Fired ReservationCheckedOutEvent to Kafka for Reservation: {}", id);
//             } else {
//                 hotelCheckoutConsumer.processCheckoutAsync(tenantId, r.getBranchId(), room.getId(), id);
//                 log.info("⚡ [API] Kafka disabled - executed Direct Async Checkout tasks for Reservation {}", id);
//             }
//         } catch (Exception e) {
//             log.error("Failed to process checkout tasks", e);
//         }

//         return r;
//     }

//     @Transactional
//     public Reservation cancelReservation(UUID id, UUID tenantId, String reason, UUID userId) {
//         Reservation r = getReservation(id, tenantId);
//         if (List.of(ReservationStatus.checked_out, ReservationStatus.cancelled).contains(r.getStatus())) {
//             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reservation is already completed or cancelled");
//         }

//         r.setStatus(ReservationStatus.cancelled);
//         r.setCancelledAt(LocalDateTime.now(HOTEL_ZONE));
//         r.setCancelReason(reason);
//         reservationRepo.save(r);

//         Room room = r.getRoom();
//         if (room.getStatus() == RoomStatus.reserved) {
//             room.setStatus(RoomStatus.available);
//             roomRepo.save(room);
//         }

//         try {
//             // 🟢 AUDIT LOG TOGGLE
//             if (kafkaEnabled) {
//                 Map<String, String> auditEvent = new HashMap<>();
//                 auditEvent.put("tenantId", tenantId.toString());
//                 auditEvent.put("branchId", r.getBranchId() != null ? r.getBranchId().toString() : null);
//                 auditEvent.put("userId", userId != null ? userId.toString() : null);
//                 auditEvent.put("entity", "RESERVATION");
//                 auditEvent.put("entityId", id.toString());
//                 auditEvent.put("action", "CANCEL");
//                 auditEvent.put("metadata", "Cancel Reason: " + reason + " | Room: " + room.getRoomNumber());

//                 kafkaTemplate.send("audit-logs", objectMapper.writeValueAsString(auditEvent));
//                 log.info("🚀 [API] Fired AuditEvent to Kafka for cancelled reservation {}", id);
//             } else {
//                 AuditLog audit = AuditLog.builder()
//                         .tenantId(tenantId)
//                         .branchId(r.getBranchId())
//                         .userId(userId)
//                         .entity("RESERVATION")
//                         .entityId(id.toString())
//                         .action("CANCEL")
//                         .metadata(Map.of("details", "Cancel Reason: " + reason + " | Room: " + room.getRoomNumber()))
//                         .ipAddress("API-FALLBACK")
//                         .userAgent("SYSTEM")
//                         .build();
//                 auditService.log(audit);
//                 log.info("⚡ [API] Kafka disabled - executed Direct Async Audit Log for cancelled reservation {}", id);
//             }
//         } catch (Exception e) {
//             log.error("Failed to log audit event", e);
//         }

//         return r;
//     }

//     public Map<String, Object> getFolio(UUID reservationId, UUID tenantId) {
//         Reservation r = getReservation(reservationId, tenantId);
//         List<FolioCharge> extraCharges = folioRepo.findByReservationIdAndTenantIdOrderByCreatedAtAsc(reservationId, tenantId);

//         FolioCharge baseCharge = FolioCharge.builder()
//                 .id(UUID.randomUUID())
//                 .tenantId(tenantId)
//                 .reservationId(reservationId)
//                 .description("Room Charges (" + r.getNumNights() + " Nights)")
//                 .amount(r.getSubtotal())
//                 .chargeType(ChargeType.room_charge)
//                 .date(r.getCheckInDate())
//                 .createdAt(LocalDateTime.now(HOTEL_ZONE))
//                 .build();

//         FolioCharge taxCharge = FolioCharge.builder()
//                 .id(UUID.randomUUID())
//                 .tenantId(tenantId)
//                 .reservationId(reservationId)
//                 .description("Taxes (Accommodation)")
//                 .amount(r.getTaxAmount())
//                 .chargeType(ChargeType.service)
//                 .date(r.getCheckInDate())
//                 .createdAt(LocalDateTime.now(HOTEL_ZONE))
//                 .build();

//         List<FolioCharge> charges = new ArrayList<>();
//         charges.add(baseCharge);
//         charges.add(taxCharge);
//         charges.addAll(extraCharges);

//         BigDecimal totalCharges = charges.stream()
//                 .map(FolioCharge::getAmount)
//                 .filter(a -> a.compareTo(BigDecimal.ZERO) > 0)
//                 .reduce(BigDecimal.ZERO, BigDecimal::add);

//         BigDecimal totalPaid = charges.stream()
//                 .map(FolioCharge::getAmount)
//                 .filter(a -> a.compareTo(BigDecimal.ZERO) < 0)
//                 .map(BigDecimal::abs)
//                 .reduce(BigDecimal.ZERO, BigDecimal::add);

//         BigDecimal balance = totalCharges.subtract(totalPaid);

//         return Map.of(
//                 "reservation", r,
//                 "charges", charges,
//                 "totalCharges", totalCharges,
//                 "totalPaid", totalPaid,
//                 "balance", balance
//         );
//     }

//     @Transactional
//     public FolioCharge addFolioCharge(UUID reservationId, UUID tenantId, AddFolioChargeDto dto) {
//         getReservation(reservationId, tenantId);

//         BigDecimal amount = dto.getAmount();
//         if (dto.getChargeType() == ChargeType.advance || dto.getChargeType() == ChargeType.discount) {
//             amount = amount.abs().negate();
//         }

//         FolioCharge charge = FolioCharge.builder()
//                 .tenantId(tenantId)
//                 .reservationId(reservationId)
//                 .description(dto.getDescription())
//                 .amount(amount)
//                 .chargeType(dto.getChargeType())
//                 .referenceId(dto.getReferenceId())
//                 .date(dto.getDate() != null ? LocalDate.parse(dto.getDate()) : LocalDate.now(HOTEL_ZONE))
//                 .build();

//         return folioRepo.save(charge);
//     }

//     @Transactional
//     public Bill generateBill(
//             UUID reservationId,
//             UUID tenantId,
//             PaymentMethod paymentMethod,
//             BigDecimal amountPaid,
//             UUID shiftId
//     ) {
//         Map<String, Object> folio = getFolio(reservationId, tenantId);
//         Reservation r = (Reservation) folio.get("reservation");
//         @SuppressWarnings("unchecked")
//         List<FolioCharge> charges = (List<FolioCharge>) folio.get("charges");

//         Optional<Bill> existing = billRepo.findBillsWithFilters(tenantId, r.getBranchId(), null, null, BillSource.hotel, Pageable.unpaged())
//                 .stream()
//                 .filter(b -> reservationId.equals(b.getReservationId()))
//                 .findFirst();

//         if (existing.isPresent()) {
//             throw new ResponseStatusException(HttpStatus.CONFLICT, "Bill already generated for this reservation");
//         }

//         BigDecimal grandTotal = charges.stream()
//                 .map(FolioCharge::getAmount)
//                 .filter(a -> a.compareTo(BigDecimal.ZERO) > 0)
//                 .reduce(BigDecimal.ZERO, BigDecimal::add);

//         BigDecimal advances = charges.stream()
//                 .filter(c -> c.getChargeType() == ChargeType.advance)
//                 .map(FolioCharge::getAmount)
//                 .map(BigDecimal::abs)
//                 .reduce(BigDecimal.ZERO, BigDecimal::add);

//         BigDecimal totalPaidNow = advances.add(amountPaid != null ? amountPaid : BigDecimal.ZERO);
//         BigDecimal totalTax = r.getTaxAmount();
//         BigDecimal subtotal = grandTotal.subtract(totalTax);

//         String today = LocalDate.now(HOTEL_ZONE).format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
//         Shift shift = shiftId != null ? shiftRepo.findById(shiftId).orElse(null) : null;

//         Bill bill = Bill.builder()
//                 .tenant(tenantRepo.findById(tenantId).orElse(null))
//                 .branch(branchRepo.findById(r.getBranchId()).orElse(null))
//                 .reservationId(reservationId)
//                 .shift(shift)
//                 .source(BillSource.hotel)
//                 .status(grandTotal.subtract(totalPaidNow).compareTo(BigDecimal.valueOf(0.01)) <= 0 ? InvoiceStatus.paid : InvoiceStatus.issued)
//                 .customerName(r.getPrimaryGuest().getName())
//                 .customerPhone(r.getPrimaryGuest().getPhone())
//                 .supplyType(GstType.cgst_sgst)
//                 .subtotal(subtotal)
//                 .taxableAmount(subtotal)
//                 .cgstAmount(totalTax.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP))
//                 .sgstAmount(totalTax.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP))
//                 .igstAmount(BigDecimal.ZERO)
//                 .cessAmount(BigDecimal.ZERO)
//                 .totalTax(totalTax)
//                 .grandTotal(grandTotal)
//                 .paidAmount(totalPaidNow)
//                 .changeAmount(totalPaidNow.subtract(grandTotal).max(BigDecimal.ZERO))
//                 .notes("Hotel Bill for Room: " + r.getRoom().getRoomNumber())
//                 .build();

//         int maxRetries = 3;
//         Bill saved = null;
//         for (int i = 0; i < maxRetries; i++) {
//             try {
//                 long count = billRepo.countByTenantIdAndBillNumberPrefix(tenantId, "BILL-" + today + "-");
//                 String billNumber = String.format("BILL-%s-%05d", today, count + 1 + i);
//                 bill.setBillNumber(billNumber);
//                 bill.setInvoiceNumber(billNumber);
//                 saved = billRepo.saveAndFlush(bill);
//                 break;
//             } catch (DataIntegrityViolationException e) {
//                 if (i == maxRetries - 1) {
//                     throw new ResponseStatusException(HttpStatus.CONFLICT, "High checkout concurrency. Please retry.");
//                 }
//             }
//         }

//         if (amountPaid != null && amountPaid.compareTo(BigDecimal.ZERO) > 0) {
//             Payment p = Payment.builder()
//                     .tenant(saved.getTenant())
//                     .branch(saved.getBranch())
//                     .bill(saved)
//                     .shift(shift)
//                     .method(paymentMethod)
//                     .amount(amountPaid)
//                     .build();
//             paymentRepo.save(p);

//             FolioCharge settlement = FolioCharge.builder()
//                     .tenantId(tenantId)
//                     .reservationId(reservationId)
//                     .description("Bill Settlement")
//                     .amount(amountPaid.negate())
//                     .chargeType(ChargeType.settlement)
//                     .date(LocalDate.now(HOTEL_ZONE))
//                     .build();
//             folioRepo.save(settlement);
//         }

//         if (shift != null) {
//             updateHotelShiftTotals(shift, saved, paymentMethod, amountPaid, advances);
//         }

//         if (r.getPrimaryGuest().getEmail() != null && !r.getPrimaryGuest().getEmail().isBlank()) {
//             final Bill finalSaved = saved;
//             CompletableFuture.runAsync(() -> {
//                 try {
//                     emailService.sendReceipt(r.getPrimaryGuest().getEmail(), finalSaved);
//                     log.info("Receipt emailed asynchronously to {}", r.getPrimaryGuest().getEmail());
//                 } catch (Exception e) {
//                     log.error("Background email failed: {}", e.getMessage());
//                 }
//             });
//         }

//         return saved;
//     }

//     private void updateHotelShiftTotals(Shift shift, Bill bill, PaymentMethod method, BigDecimal currentPaid, BigDecimal advances) {
//         shift.setTotalSales(shift.getTotalSales().add(bill.getGrandTotal()));
//         shift.setTotalOrders(shift.getTotalOrders() + 1);

//         if (currentPaid != null && currentPaid.compareTo(BigDecimal.ZERO) > 0) {
//             switch (method) {
//                 case cash -> shift.setCashSales(shift.getCashSales().add(currentPaid));
//                 case card -> shift.setCardSales(shift.getCardSales().add(currentPaid));
//                 case upi -> shift.setUpiSales(shift.getUpiSales().add(currentPaid));
//                 case wallet -> shift.setWalletSales(shift.getWalletSales().add(currentPaid));
//                 case credit -> shift.setCreditSales(shift.getCreditSales().add(currentPaid));
//                 case complimentary -> shift.setComplimentary(shift.getComplimentary().add(currentPaid));
//             }
//         }

//         if (advances != null && advances.compareTo(BigDecimal.ZERO) > 0) {
//             shift.setCashSales(shift.getCashSales().add(advances));
//         }

//         shift.setTotalCgst(shift.getTotalCgst().add(bill.getCgstAmount()));
//         shift.setTotalSgst(shift.getTotalSgst().add(bill.getSgstAmount()));
//         shift.setTotalIgst(shift.getTotalIgst().add(bill.getIgstAmount()));

//         shiftRepo.save(shift);
//     }

//     public Map<String, Object> getHotelDashboardAnalytics(UUID tenantId, UUID branchId) {
//         LocalDate today = LocalDate.now(HOTEL_ZONE);
//         LocalDate weekAgo = today.minusDays(6);

//         String weeklySql = """
//             SELECT
//               r.check_in_date::text AS date,
//               COALESCE(SUM(r.total_amount), 0) AS revenue
//             FROM hotel_reservations r
//             WHERE r.tenant_id = ?
//               AND r.branch_id = ?
//               AND r.status NOT IN ('cancelled', 'no_show')
//               AND r.check_in_date BETWEEN ? AND ?
//             GROUP BY r.check_in_date
//             ORDER BY r.check_in_date ASC
//         """;
//         List<Map<String, Object>> weeklyChart = jdbcTemplate.queryForList(weeklySql, tenantId, branchId, weekAgo, today);

//         BigDecimal todaySales = BigDecimal.ZERO;
//         for (Map<String, Object> entry : weeklyChart) {
//             if (today.toString().equals(entry.get("date"))) {
//                 todaySales = new BigDecimal(entry.get("revenue").toString());
//             }
//         }

//         long todayCheckins = reservationRepo.countByTenantIdAndBranchIdAndCheckInDateAndStatus(tenantId, branchId, today, ReservationStatus.checked_in);
//         long todayCheckouts = reservationRepo.countByTenantIdAndBranchIdAndCheckOutDateAndStatus(tenantId, branchId, today, ReservationStatus.checked_out);

//         Map<String, Object> occupancySummary = getOccupancySummary(tenantId, branchId);
//         int occupancyRate = (int) occupancySummary.getOrDefault("occupancy_today", 0);

//         BigDecimal weekSales = weeklyChart.stream()
//                 .map(m -> new BigDecimal(m.get("revenue").toString()))
//                 .reduce(BigDecimal.ZERO, BigDecimal::add);

//         BigDecimal adr = todaySales.divide(BigDecimal.valueOf(Math.max(1, todayCheckins)), 2, RoundingMode.HALF_UP);

//         long todayBills = billRepo.countByTenantIdAndBillNumberPrefix(tenantId, "BILL-" + today.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + "-");

//         return Map.of(
//                 "todaySales", todaySales,
//                 "todayCheckins", todayCheckins,
//                 "todayCheckouts", todayCheckouts,
//                 "occupancyRate", occupancyRate,
//                 "weeklyChart", weeklyChart,
//                 "weekSales", weekSales,
//                 "adr", adr,
//                 "todayBills", todayBills,
//                 "roomStats", Map.of(
//                         "available", occupancySummary.getOrDefault("available_rooms", 0),
//                         "occupied", occupancySummary.getOrDefault("occupied_rooms", 0),
//                         "reserved", reservationRepo.countByTenantIdAndBranchIdAndStatus(tenantId, branchId, ReservationStatus.confirmed),
//                         "cleaning", occupancySummary.getOrDefault("maintenance_rooms", 0),
//                         "maintenance", occupancySummary.getOrDefault("maintenance_rooms", 0)
//                 )
//         );
//     }

//     public Map<String, Object> getOccupancySummary(UUID tenantId, UUID branchId) {
//         String sql = """
//             SELECT
//               COUNT(*)::int                                                         AS total_rooms,
//               COUNT(*) FILTER (WHERE status = 'occupied')::int                     AS occupied_rooms,
//               COUNT(*) FILTER (WHERE status = 'available')::int                    AS available_rooms,
//               COUNT(*) FILTER (WHERE status IN ('maintenance','out_of_order'))::int AS maintenance_rooms,
//               COALESCE(ROUND(
//                 COUNT(*) FILTER (WHERE status = 'occupied')::numeric /
//                 NULLIF(COUNT(*),0) * 100
//               ,0),0)::int                                                           AS occupancy_today
//             FROM hotel_rooms
//             WHERE tenant_id = ?
//               AND branch_id = ?
//               AND is_active = true
//         """;
//         List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, tenantId, branchId);
//         if (result.isEmpty()) {
//             return Map.of(
//                     "total_rooms", 0,
//                     "occupied_rooms", 0,
//                     "available_rooms", 0,
//                     "maintenance_rooms", 0,
//                     "occupancy_today", 0
//             );
//         }
//         return result.get(0);
//     }

//     public Map<String, Object> getGstr1Export(UUID tenantId, UUID branchId, LocalDate from, LocalDate to) {
//         List<Map<String, Object>> gstData = getGstReport(tenantId, branchId, from, to);
//         return Map.of(
//                 "version", "GSTR1_v2.0",
//                 "gstin", tenantRepo.findById(tenantId).map(t -> t.getGstin() != null ? t.getGstin() : "").orElse(""),
//                 "period", from.toString() + " to " + to.toString(),
//                 "b2b", List.of(),
//                 "b2c", gstData
//         );
//     }

//     public List<Map<String, Object>> getRevenueReport(UUID tenantId, UUID branchId, LocalDate from, LocalDate to) {
//         String sql = """
//             SELECT
//               r.check_in_date                 AS date,
//               COUNT(r.id)::int                AS bookings,
//               COALESCE(SUM(r.total_amount),0) AS revenue,
//               COALESCE(SUM(r.tax_amount),0)   AS tax,
//               COALESCE(SUM(r.total_amount - r.tax_amount),0) AS net_revenue
//             FROM hotel_reservations r
//             WHERE r.tenant_id  = ?
//               AND r.branch_id  = ?
//               AND r.status     NOT IN ('cancelled','no_show')
//               AND r.check_in_date BETWEEN ? AND ?
//             GROUP BY r.check_in_date
//             ORDER BY r.check_in_date
//         """;
//         return jdbcTemplate.queryForList(sql, tenantId, branchId, from, to);
//     }

//     public List<Map<String, Object>> getBookingsReport(UUID tenantId, UUID branchId, LocalDate from, LocalDate to) {
//         String sql = """
//             SELECT
//               r.id                                        AS booking_id,
//               r.booking_ref                               AS booking_ref,
//               g.name                                      AS guest_name,
//               g.email                                     AS guest_email,
//               g.phone                                     AS guest_phone,
//               rm.room_number,
//               r.check_in_date                             AS check_in,
//               r.check_out_date                            AS check_out,
//               r.num_nights                                AS nights,
//               r.status,
//               r.total_amount                              AS amount,
//               r.balance_due,
//               COALESCE(b.status, 'unpaid')          AS payment_status
//             FROM hotel_reservations r
//             LEFT JOIN hotel_guests   g  ON g.id  = r.primary_guest_id
//             LEFT JOIN hotel_rooms    rm ON rm.id = r.room_id
//             LEFT JOIN bills          b  ON b.reservation_id = r.id
//             WHERE r.tenant_id  = ?
//               AND r.branch_id  = ?
//               AND r.check_in_date BETWEEN ? AND ?
//             ORDER BY r.check_in_date DESC
//         """;
//         return jdbcTemplate.queryForList(sql, tenantId, branchId, from, to);
//     }

//     public List<Map<String, Object>> getRoomsPerformanceReport(UUID tenantId, UUID branchId, LocalDate from, LocalDate to) {
//         String sql = """
//             SELECT
//               rm.room_number,
//               rt.name                                              AS room_type,
//               COUNT(r.id)::int                                     AS bookings,
//               COALESCE(
//                 ROUND(
//                   COUNT(r.id)::numeric /
//                   NULLIF((?::date - ?::date + 1), 0) * 100
//                 ,0),0)::int                                        AS occupancy,
//               COALESCE(SUM(r.total_amount),0)                      AS revenue,
//               COALESCE(ROUND(AVG(r.rate_per_night)::numeric,2),0)  AS avg_rate
//             FROM hotel_rooms rm
//             LEFT JOIN hotel_room_types rt ON rt.id = rm.room_type_id
//             LEFT JOIN hotel_reservations r
//               ON r.room_id   = rm.id
//              AND r.status   NOT IN ('cancelled','no_show')
//              AND r.check_in_date BETWEEN ? AND ?
//             WHERE rm.tenant_id = ?
//               AND rm.branch_id = ?
//               AND rm.is_active = true
//             GROUP BY rm.id, rm.room_number, rt.name
//             ORDER BY revenue DESC
//         """;
//         return jdbcTemplate.queryForList(sql, to, from, from, to, tenantId, branchId);
//     }

//     public List<Map<String, Object>> getPaymentsReport(UUID tenantId, UUID branchId, OffsetDateTime from, OffsetDateTime to) {
//         String sql = """
//             SELECT
//               p.method,
//               COUNT(p.id)::int                 AS transaction_count,
//               COALESCE(SUM(p.amount),0)        AS total_amount
//             FROM payments p
//             JOIN bills b ON b.id = p.bill_id
//             WHERE p.tenant_id  = ?
//               AND b.branch_id  = ?
//               AND p.created_at BETWEEN ? AND ?
//             GROUP BY p.method
//             ORDER BY total_amount DESC
//         """;
//         return jdbcTemplate.queryForList(sql, tenantId, branchId, from, to);
//     }

//     public List<Map<String, Object>> getGstReport(UUID tenantId, UUID branchId, LocalDate from, LocalDate to) {
//         String sql = """
//             SELECT
//               TO_CHAR(DATE_TRUNC('month', r.check_in_date::date), 'YYYY-MM-DD') AS month,
//               COUNT(r.id)::int                                                    AS total_invoices,
//               COALESCE(SUM(r.total_amount - r.tax_amount),0)                      AS taxable_value,
//               COALESCE(SUM(r.tax_amount / 2),0)                                   AS cgst,
//               COALESCE(SUM(r.tax_amount / 2),0)                                   AS sgst,
//               0                                                                   AS igst,
//               COALESCE(SUM(r.tax_amount),0)                                       AS total_tax,
//               COALESCE(SUM(r.total_amount),0)                                     AS gross_value
//             FROM hotel_reservations r
//             WHERE r.tenant_id  = ?
//               AND r.branch_id  = ?
//               AND r.status     NOT IN ('cancelled','no_show')
//               AND r.check_in_date BETWEEN ? AND ?
//             GROUP BY DATE_TRUNC('month', r.check_in_date::date)
//             ORDER BY DATE_TRUNC('month', r.check_in_date::date)
//         """;
//         return jdbcTemplate.queryForList(sql, tenantId, branchId, from, to);
//     }

//     public List<Map<String, Object>> getFrontDeskReport(UUID tenantId, UUID branchId, LocalDate from, LocalDate to) {
//         String sql = """
//             SELECT
//               u.id                                           AS staff_id,
//               CONCAT(u.first_name, ' ', COALESCE(u.last_name, '')) AS staff_name,
//               COUNT(r.id) FILTER (WHERE r.actual_check_in  IS NOT NULL)::int AS check_ins,
//               COUNT(r.id) FILTER (WHERE r.actual_check_out IS NOT NULL)::int AS check_outs,
//               COUNT(r.id)::int                               AS bookings_handled,
//               COALESCE(SUM(r.total_amount),0)                AS revenue_managed,
//               COALESCE(ROUND(AVG(r.total_amount)::numeric,2),0) AS avg_booking_value
//             FROM hotel_reservations r
//             JOIN users u ON u.id = r.created_by_id
//             WHERE r.tenant_id  = ?
//               AND r.branch_id  = ?
//               AND r.status     NOT IN ('cancelled','no_show')
//               AND r.check_in_date BETWEEN ? AND ?
//             GROUP BY u.id, u.first_name, u.last_name
//             ORDER BY revenue_managed DESC
//         """;
//         return jdbcTemplate.queryForList(sql, tenantId, branchId, from, to);
//     }

//     public List<HousekeepingTask> listHousekeepingTasks(UUID tenantId, UUID branchId, String date) {
//         LocalDate localDate = date != null ? LocalDate.parse(date) : LocalDate.now(HOTEL_ZONE);
//         return branchId != null
//                 ? hkRepo.findByTenantIdAndBranchIdAndScheduledForOrderByPriorityDescCreatedAtAsc(tenantId, branchId, localDate)
//                 : hkRepo.findByTenantIdAndScheduledForOrderByPriorityDescCreatedAtAsc(tenantId, localDate);
//     }

//     @Transactional
//     public HousekeepingTask createHousekeepingTask(UUID tenantId, UUID branchId, CreateHkTaskDto dto) {
//         Room room = roomRepo.findByIdAndTenantId(dto.getRoomId(), tenantId)
//                 .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

//         HousekeepingTask task = HousekeepingTask.builder()
//                 .tenantId(tenantId)
//                 .branchId(branchId)
//                 .roomId(room.getId())
//                 .room(room)
//                 .taskType(dto.getTaskType())
//                 .priority(dto.getPriority() != null ? dto.getPriority() : HkPriority.normal)
//                 .scheduledFor(dto.getScheduledFor() != null ? LocalDate.parse(dto.getScheduledFor()) : LocalDate.now(HOTEL_ZONE))
//                 .notes(dto.getNotes())
//                 .status(HkStatus.pending)
//                 .build();

//         return hkRepo.save(task);
//     }

//     @Transactional
//     public HousekeepingTask updateHousekeepingTask(UUID id, UUID tenantId, HkStatus status, String notes) {
//         HousekeepingTask task = hkRepo.findByIdAndTenantId(id, tenantId)
//                 .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Housekeeping task not found"));

//         task.setStatus(status);
//         if (notes != null) task.setNotes(notes);
//         if (status == HkStatus.in_progress && task.getStartedAt() == null) {
//             task.setStartedAt(LocalDateTime.now(HOTEL_ZONE));
//         }
//         if (status == HkStatus.done && task.getCompletedAt() == null) {
//             task.setCompletedAt(LocalDateTime.now(HOTEL_ZONE));
//         }

//         HousekeepingTask saved = hkRepo.save(task);

//         if (status == HkStatus.done && task.getTaskType() == HkTaskType.checkout_clean) {
//             Room room = roomRepo.findByIdAndTenantId(task.getRoomId(), tenantId).orElse(null);
//             if (room != null) {
//                 room.setStatus(RoomStatus.available);
//                 roomRepo.save(room);
//             }
//         }

//         return saved;
//     }

//     @Transactional
//     public void chargeToRoomFolio(UUID tenantId, UUID branchId, String roomNumber, BigDecimal amount, String orderNumber) {
//         Room room = roomRepo.findByTenantIdAndBranchIdAndRoomNumber(tenantId, branchId, roomNumber)
//                 .orElseThrow(() -> new RuntimeException("Room " + roomNumber + " not found"));

//         String sql = "SELECT id FROM hotel_reservations WHERE room_id = ? AND status = 'checked_in' LIMIT 1";
//         List<UUID> activeResIds = jdbcTemplate.queryForList(sql, UUID.class, room.getId());

//         if (activeResIds.isEmpty()) {
//             throw new RuntimeException("No guest is currently checked into Room " + roomNumber);
//         }

//         FolioCharge charge = FolioCharge.builder()
//                 .tenantId(tenantId)
//                 .reservationId(activeResIds.get(0))
//                 .description("Restaurant POS Order " + (orderNumber != null ? "#" + orderNumber : ""))
//                 .amount(amount)
//                 .chargeType(ChargeType.service)
//                 .date(LocalDate.now(HOTEL_ZONE))
//                 .build();

//         folioRepo.save(charge);
//         log.info("Successfully charged ₹{} to Room {} for Order {}", amount, roomNumber, orderNumber);
//     }

//     public Map<String, Object> getDashboard(UUID tenantId, UUID branchId) {
//         LocalDate today = LocalDate.now(HOTEL_ZONE);
//         List<Room> rooms = roomRepo.findAllActiveRoomsByTenantAndBranch(tenantId, branchId);

//         Map<String, Long> statusCounts = new HashMap<>();
//         for (RoomStatus rs : RoomStatus.values()) {
//             statusCounts.put(rs.name(), 0L);
//         }
//         for (Room r : rooms) {
//             String name = r.getStatus().name();
//             statusCounts.put(name, statusCounts.getOrDefault(name, 0L) + 1);
//         }

//         long arrivals = branchId != null
//                 ? reservationRepo.countByTenantIdAndBranchIdAndCheckInDateAndStatus(tenantId, branchId, today, ReservationStatus.confirmed)
//                 : reservationRepo.countByTenantIdAndCheckInDateAndStatus(tenantId, today, ReservationStatus.confirmed);

//         long departures = branchId != null
//                 ? reservationRepo.countByTenantIdAndBranchIdAndCheckOutDateAndStatus(tenantId, branchId, today, ReservationStatus.checked_in)
//                 : reservationRepo.countByTenantIdAndCheckOutDateAndStatus(tenantId, today, ReservationStatus.checked_in);

//         long inHouse = branchId != null
//                 ? reservationRepo.countByTenantIdAndBranchIdAndStatus(tenantId, branchId, ReservationStatus.checked_in)
//                 : reservationRepo.countByTenantIdAndStatus(tenantId, ReservationStatus.checked_in);

//         long total = rooms.size();
//         long occupied = statusCounts.getOrDefault(RoomStatus.occupied.name(), 0L);
//         long occupancyPct = total > 0 ? Math.round(((double) occupied / total) * 100) : 0;

//         return Map.of(
//                 "totalRooms", total,
//                 "occupancyPct", occupancyPct,
//                 "roomsByStatus", statusCounts,
//                 "arrivalsToday", arrivals,
//                 "departuresToday", departures,
//                 "inHouse", inHouse
//         );
//     }
// }

package project.EnterpriseSaas.demo.modules.hotel.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.EnterpriseSaas.demo.common.enums.*;
import project.EnterpriseSaas.demo.modules.billing.entity.Bill;
import project.EnterpriseSaas.demo.modules.billing.entity.Payment;
import project.EnterpriseSaas.demo.modules.billing.repository.BillRepository;
import project.EnterpriseSaas.demo.modules.billing.repository.PaymentRepository;
import project.EnterpriseSaas.demo.modules.branch.repository.BranchRepository;
import project.EnterpriseSaas.demo.modules.core.service.EmailService;
import project.EnterpriseSaas.demo.modules.hotel.dto.HotelDtos.*;
import project.EnterpriseSaas.demo.modules.hotel.entity.*;
import project.EnterpriseSaas.demo.modules.hotel.repository.*;
import project.EnterpriseSaas.demo.modules.shift.entity.Shift;
import project.EnterpriseSaas.demo.modules.shift.repository.ShiftRepository;
import project.EnterpriseSaas.demo.modules.tenant.repository.TenantRepository;
import project.EnterpriseSaas.demo.modules.hotel.kafka.HotelCheckoutConsumer;
import project.EnterpriseSaas.demo.modules.audit.service.AuditService;
import project.EnterpriseSaas.demo.modules.audit.entity.AuditLog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class HotelService {

    private final RoomTypeRepository roomTypeRepo;
    private final RoomRepository roomRepo;
    private final GuestRepository guestRepo;
    private final ReservationRepository reservationRepo;
    private final FolioChargeRepository folioRepo;
    private final HousekeepingTaskRepository hkRepo;
    private final BillRepository billRepo;
    private final PaymentRepository paymentRepo;
    private final TenantRepository tenantRepo;
    private final BranchRepository branchRepo;
    private final ShiftRepository shiftRepo;
    private final JdbcTemplate jdbcTemplate;
    private final EmailService emailService;

    // 🟢 INJECTIONS
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.enabled:false}")
    private boolean kafkaEnabled;

    @Autowired
    @Lazy // Resolves circular dependencies safely
    private HotelCheckoutConsumer hotelCheckoutConsumer;

    @Autowired
    @Lazy
    private AuditService auditService;

    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Kolkata");

    @Transactional
    @CacheEvict(value = "roomTypes", allEntries = true)
    public RoomType createRoomType(UUID tenantId, UUID branchId, CreateRoomTypeDto dto) {
        tenantRepo.findById(tenantId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
        branchRepo.findById(branchId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));

        RoomType rt = RoomType.builder()
                .tenantId(tenantId)
                .branchId(branchId)
                .name(dto.getName())
                .description(dto.getDescription())
                .baseRate(dto.getBaseRate())
                .maxOccupancy(dto.getMaxOccupancy() != null ? dto.getMaxOccupancy() : 2)
                .amenities(dto.getAmenities() != null ? dto.getAmenities() : List.of())
                .isActive(true)
                .build();
        return roomTypeRepo.save(rt);
    }

    @Cacheable(value = "roomTypes")
    public List<RoomType> listRoomTypes(UUID tenantId, UUID branchId) {
        return branchId != null
                ? roomTypeRepo.findByTenantIdAndBranchIdAndIsActiveTrueOrderByNameAsc(tenantId, branchId)
                : roomTypeRepo.findByTenantIdAndIsActiveTrueOrderByNameAsc(tenantId);
    }

    @Transactional
    @CacheEvict(value = "roomTypes", allEntries = true)
    public RoomType updateRoomType(UUID id, UUID tenantId, Map<String, Object> data) {
        RoomType rt = roomTypeRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "RoomType not found"));

        if (data.containsKey("name")) rt.setName((String) data.get("name"));
        if (data.containsKey("description")) rt.setDescription((String) data.get("description"));
        if (data.containsKey("baseRate")) rt.setBaseRate(new BigDecimal(data.get("baseRate").toString()));
        if (data.containsKey("maxOccupancy")) rt.setMaxOccupancy(Integer.parseInt(data.get("maxOccupancy").toString()));

        return roomTypeRepo.save(rt);
    }

    @Transactional
    @CacheEvict(value = "roomTypes", allEntries = true)
    public void deleteRoomType(UUID id, UUID tenantId) {
        RoomType rt = roomTypeRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "RoomType not found"));
        rt.setIsActive(false);
        roomTypeRepo.save(rt);
    }

    @Transactional
    public Room createRoom(UUID tenantId, UUID branchId, CreateRoomDto dto) {
        Optional<Room> exists = roomRepo.findByTenantIdAndBranchIdAndRoomNumber(tenantId, branchId, dto.getRoomNumber());
        if (exists.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room " + dto.getRoomNumber() + " already exists");
        }

        RoomType rt = roomTypeRepo.findByIdAndTenantId(dto.getRoomTypeId(), tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room type not found"));

        Room room = Room.builder()
                .tenantId(tenantId)
                .branchId(branchId)
                .roomTypeId(rt.getId())
                .roomType(rt)
                .roomNumber(dto.getRoomNumber())
                .floor(dto.getFloor() != null ? dto.getFloor() : 1)
                .notes(dto.getNotes())
                .status(RoomStatus.available)
                .isActive(true)
                .build();

        Room saved = roomRepo.save(room);
        roomTypeRepo.incrementTotalRooms(rt.getId(), 1);
        return saved;
    }

    public List<Room> listRooms(UUID tenantId, UUID branchId, RoomStatus status) {
        return roomRepo.findRoomsWithFilters(tenantId, branchId, status);
    }

    @Transactional
    public Room updateRoom(UUID id, UUID tenantId, Map<String, Object> data) {
        Room room = roomRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        boolean statusChanged = false;
        RoomStatus oldStatus = room.getStatus();

        if (data.containsKey("roomNumber")) room.setRoomNumber((String) data.get("roomNumber"));
        if (data.containsKey("floor")) room.setFloor(Integer.parseInt(data.get("floor").toString()));
        if (data.containsKey("notes")) room.setNotes((String) data.get("notes"));
        if (data.containsKey("status")) {
            RoomStatus status = RoomStatus.valueOf(data.get("status").toString().toLowerCase());
            if (status != oldStatus) {
                room.setStatus(status);
                statusChanged = true;
            }
        }

        Room saved = roomRepo.save(room);
        if (statusChanged) {
            syncHousekeepingOnStatusChange(id, tenantId, room.getStatus(), room.getBranchId());
        }
        return saved;
    }

    @Transactional
    public Room updateRoomStatus(UUID id, UUID tenantId, RoomStatus status) {
        Room room = roomRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        boolean statusChanged = status != room.getStatus();
        room.setStatus(status);
        Room saved = roomRepo.save(room);

        if (statusChanged) {
            syncHousekeepingOnStatusChange(id, tenantId, status, room.getBranchId());
        }
        return saved;
    }

    private void syncHousekeepingOnStatusChange(UUID roomId, UUID tenantId, RoomStatus status, UUID branchId) {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        if (status == RoomStatus.available) {
            hkRepo.autoResolvePendingTasks(roomId, tenantId, today, List.of(HkStatus.pending, HkStatus.in_progress), HkStatus.done, LocalDateTime.now(HOTEL_ZONE), "Auto-resolved via Room Status update");
        } else if (status == RoomStatus.cleaning || status == RoomStatus.maintenance) {
            HkTaskType type = status == RoomStatus.cleaning ? HkTaskType.stayover : HkTaskType.maintenance;
            Optional<HousekeepingTask> existing = hkRepo.findByRoomIdAndTenantIdAndScheduledForAndStatusIn(roomId, tenantId, today, List.of(HkStatus.pending, HkStatus.in_progress));
            if (existing.isEmpty()) {
                Room room = roomRepo.findById(roomId).orElse(null);

                HousekeepingTask task = HousekeepingTask.builder()
                        .tenantId(tenantId)
                        .branchId(branchId)
                        .roomId(roomId)
                        .room(room)
                        .taskType(type)
                        .status(HkStatus.pending)
                        .priority(HkPriority.normal)
                        .scheduledFor(today)
                        .notes("Auto-generated from room status update")
                        .build();
                hkRepo.save(task);
            }
        }
    }

    @Transactional
    public Guest createGuest(UUID tenantId, CreateGuestDto dto) {
        Guest guest = Guest.builder()
                .tenantId(tenantId)
                .name(dto.getName())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .idType(dto.getIdType())
                .idNumber(dto.getIdNumber())
                .nationality(dto.getNationality() != null ? dto.getNationality() : "India")
                .address(dto.getAddress())
                .city(dto.getCity())
                .state(dto.getState())
                .pincode(dto.getPincode())
                .dob(dto.getDob() != null ? LocalDate.parse(dto.getDob()) : null)
                .gender(dto.getGender())
                .build();
        return guestRepo.save(guest);
    }

    public List<Guest> searchGuests(UUID tenantId, String query) {
        if (query == null || query.trim().length() < 2) {
            return guestRepo.findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(0, 20));
        }
        return guestRepo.searchGuests(tenantId, query.trim(), PageRequest.of(0, 20));
    }

    public Guest getGuest(UUID id, UUID tenantId) {
        return guestRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Guest not found"));
    }

    @Transactional
    public Guest updateGuest(UUID id, UUID tenantId, Map<String, Object> data) {
        Guest guest = getGuest(id, tenantId);
        if (data.containsKey("name")) guest.setName((String) data.get("name"));
        if (data.containsKey("phone")) guest.setPhone((String) data.get("phone"));
        if (data.containsKey("email")) guest.setEmail((String) data.get("email"));
        if (data.containsKey("nationality")) guest.setNationality((String) data.get("nationality"));
        if (data.containsKey("address")) guest.setAddress((String) data.get("address"));
        if (data.containsKey("city")) guest.setCity((String) data.get("city"));
        if (data.containsKey("state")) guest.setState((String) data.get("state"));
        if (data.containsKey("pincode")) guest.setPincode((String) data.get("pincode"));
        if (data.containsKey("gender")) guest.setGender(Gender.valueOf(data.get("gender").toString().toLowerCase()));

        return guestRepo.save(guest);
    }

    @Transactional
    public Reservation createReservation(UUID tenantId, UUID branchId, CreateReservationDto dto, UUID userId) {
        UUID guestId = dto.getPrimaryGuestId();
        if (guestId == null && dto.getGuest() != null) {
            Guest g = createGuest(tenantId, dto.getGuest());
            guestId = g.getId();
        }
        if (guestId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Guest information is required");
        }

        Guest guest = guestRepo.findById(guestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Guest not found"));

        Room room = roomRepo.findByIdAndTenantId(dto.getRoomId(), tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        if (room.getStatus() == RoomStatus.maintenance || room.getStatus() == RoomStatus.out_of_order) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room is not available");
        }

        LocalDate checkIn = LocalDate.parse(dto.getCheckInDate());
        LocalDate checkOut = LocalDate.parse(dto.getCheckOutDate());
        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Check-out date must be after Check-in");
        }

        List<Reservation> overlaps = reservationRepo.findOverlappingReservations(room.getId(), checkIn, checkOut,
                List.of(ReservationStatus.cancelled, ReservationStatus.no_show, ReservationStatus.checked_out));
        if (!overlaps.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room is already booked for selected dates");
        }

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal rate = dto.getRatePerNight() != null ? dto.getRatePerNight() : room.getRoomType().getBaseRate();
        BigDecimal subtotal = rate.multiply(BigDecimal.valueOf(nights));
        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.12));
        BigDecimal total = subtotal.add(tax);
        BigDecimal advance = dto.getAdvancePaid() != null ? dto.getAdvancePaid() : BigDecimal.ZERO;

        Reservation reservation = Reservation.builder()
                .tenantId(tenantId)
                .branchId(branchId)
                .room(room)
                .primaryGuest(guest)
                .numAdults(dto.getNumAdults() != null ? dto.getNumAdults() : 1)
                .numChildren(dto.getNumChildren() != null ? dto.getNumChildren() : 0)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .status(ReservationStatus.confirmed)
                .ratePerNight(rate)
                .numNights((int) nights)
                .subtotal(subtotal)
                .taxAmount(tax)
                .totalAmount(total)
                .advancePaid(advance)
                .balanceDue(total.subtract(advance))
                .source(dto.getSource() != null ? dto.getSource() : BookingSource.walk_in)
                .bookingRef(dto.getBookingRef())
                .specialRequests(dto.getSpecialRequests())
                .notes(dto.getNotes())
                .createdById(userId)
                .build();

        Reservation saved = reservationRepo.save(reservation);

        room.setStatus(RoomStatus.reserved);
        roomRepo.save(room);

        if (advance.compareTo(BigDecimal.ZERO) > 0) {
            FolioCharge charge = FolioCharge.builder()
                    .tenantId(tenantId)
                    .reservationId(saved.getId())
                    .description("Advance payment")
                    .amount(advance.negate())
                    .chargeType(ChargeType.advance)
                    .date(LocalDate.now(HOTEL_ZONE))
                    .build();
            folioRepo.save(charge);
        }

        return saved;
    }

    public Map<String, Object> listReservations(UUID tenantId, Map<String, Object> query) {
        UUID branchId = (UUID) query.get("branchId");
        ReservationStatus statusObj = (ReservationStatus) query.get("status");
        String status = statusObj != null ? statusObj.name() : null;

        Object fromObj = query.get("from");
        Object toObj = query.get("to");
        String search = (String) query.get("search");

        int page = query.containsKey("page") ? (int) query.get("page") : 1;
        int limit = query.containsKey("limit") ? (int) query.get("limit") : 25;
        int offset = (page - 1) * limit;

        StringBuilder sql = new StringBuilder("""
            SELECT
                r.id, r.status, r.check_in_date, r.check_out_date, r.num_nights,
                r.rate_per_night, r.total_amount, r.advance_paid, r.balance_due,
                r.num_adults, r.num_children, r.source, r.booking_ref,
                g.id AS guest_id, g.name AS guest_name, g.phone AS guest_phone, g.email AS guest_email,
                rm.id AS room_id, rm.room_number,
                rt.name AS room_type_name
            FROM hotel_reservations r
            LEFT JOIN hotel_guests g ON r.primary_guest_id = g.id
            LEFT JOIN hotel_rooms rm ON r.room_id = rm.id
            LEFT JOIN hotel_room_types rt ON rm.room_type_id = rt.id
            WHERE r.tenant_id = ?
        """);

        StringBuilder countSql = new StringBuilder("""
            SELECT COUNT(r.id)
            FROM hotel_reservations r
            LEFT JOIN hotel_guests g ON r.primary_guest_id = g.id
            LEFT JOIN hotel_rooms rm ON r.room_id = rm.id
            WHERE r.tenant_id = ?
        """);

        List<Object> params = new ArrayList<>();
        params.add(tenantId);

        if (branchId != null) {
            sql.append(" AND r.branch_id = ?");
            countSql.append(" AND r.branch_id = ?");
            params.add(branchId);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND r.status = ?");
            countSql.append(" AND r.status = ?");
            params.add(status);
        }
        if (fromObj != null && !fromObj.toString().trim().isEmpty()) {
            sql.append(" AND r.check_in_date >= CAST(? AS DATE)");
            countSql.append(" AND r.check_in_date >= CAST(? AS DATE)");
            params.add(fromObj.toString().trim());
        }
        if (toObj != null && !toObj.toString().trim().isEmpty()) {
            sql.append(" AND r.check_out_date <= CAST(? AS DATE)");
            countSql.append(" AND r.check_out_date <= CAST(? AS DATE)");
            params.add(toObj.toString().trim());
        }
        if (search != null && !search.trim().isEmpty()) {
            String searchPattern = "%" + search.trim().toLowerCase() + "%";
            String searchClause = " AND (LOWER(g.name) LIKE ? OR LOWER(g.phone) LIKE ? OR LOWER(rm.room_number) LIKE ? OR LOWER(r.booking_ref) LIKE ?)";
            sql.append(searchClause);
            countSql.append(searchClause);
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }

        Long total = jdbcTemplate.queryForObject(countSql.toString(), Long.class, params.toArray());

        sql.append(" ORDER BY r.check_in_date DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        List<Map<String, Object>> data = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", rs.getObject("id", UUID.class));
            map.put("status", rs.getString("status"));
            map.put("checkInDate", rs.getString("check_in_date"));
            map.put("checkOutDate", rs.getString("check_out_date"));
            map.put("numNights", rs.getInt("num_nights"));
            map.put("ratePerNight", rs.getBigDecimal("rate_per_night"));
            map.put("totalAmount", rs.getBigDecimal("total_amount"));
            map.put("advancePaid", rs.getBigDecimal("advance_paid"));
            map.put("balanceDue", rs.getBigDecimal("balance_due"));
            map.put("numAdults", rs.getInt("num_adults"));
            map.put("numChildren", rs.getInt("num_children"));
            map.put("source", rs.getString("source"));
            map.put("bookingRef", rs.getString("booking_ref"));

            map.put("primaryGuest", Map.of(
                    "id", rs.getObject("guest_id", UUID.class) != null ? rs.getObject("guest_id", UUID.class) : UUID.randomUUID(),
                    "name", rs.getString("guest_name") != null ? rs.getString("guest_name") : "Unknown",
                    "phone", rs.getString("guest_phone") != null ? rs.getString("guest_phone") : "No phone",
                    "email", rs.getString("guest_email") != null ? rs.getString("guest_email") : ""
            ));

            map.put("room", Map.of(
                    "id", rs.getObject("room_id", UUID.class) != null ? rs.getObject("room_id", UUID.class) : UUID.randomUUID(),
                    "roomNumber", rs.getString("room_number") != null ? rs.getString("room_number") : "Unknown",
                    "roomType", Map.of("name", rs.getString("room_type_name") != null ? rs.getString("room_type_name") : "Standard")
            ));

            return map;
        }, params.toArray());

        return Map.of(
                "data", data,
                "total", total != null ? total : 0,
                "page", page,
                "limit", limit
        );
    }

    public Reservation getReservation(UUID id, UUID tenantId) {
        return reservationRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));
    }

    @Transactional
    public Reservation checkIn(UUID id, UUID tenantId) {
        Reservation r = getReservation(id, tenantId);
        if (r.getStatus() != ReservationStatus.confirmed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot check-in active booking in status: " + r.getStatus());
        }

        r.setStatus(ReservationStatus.checked_in);
        r.setActualCheckIn(LocalDateTime.now(HOTEL_ZONE));
        reservationRepo.save(r);

        Room room = r.getRoom();
        room.setStatus(RoomStatus.occupied);
        roomRepo.save(room);

        return r;
    }

    @Transactional
    public Reservation checkOut(UUID id, UUID tenantId) {
        Reservation r = getReservation(id, tenantId);
        if (r.getStatus() != ReservationStatus.checked_in) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot check-out active booking in status: " + r.getStatus());
        }

        r.setStatus(ReservationStatus.checked_out);
        r.setActualCheckOut(LocalDateTime.now(HOTEL_ZONE));
        reservationRepo.save(r);

        Room room = r.getRoom();
        room.setStatus(RoomStatus.cleaning);
        roomRepo.save(room);

        Guest guest = r.getPrimaryGuest();
        guest.setTotalStays(guest.getTotalStays() + 1);
        guestRepo.save(guest);

        try {
            if (kafkaEnabled) {
                Map<String, String> event = new HashMap<>();
                event.put("reservationId", id.toString());
                event.put("roomId", room.getId().toString());
                event.put("tenantId", tenantId.toString());
                event.put("branchId", r.getBranchId().toString());

                String message = objectMapper.writeValueAsString(event);
                kafkaTemplate.send("hotel-checkouts", message);
                log.info("🚀 [API] Fired ReservationCheckedOutEvent to Kafka for Reservation: {}", id);
            } else {
                hotelCheckoutConsumer.processCheckoutAsync(tenantId, r.getBranchId(), room.getId(), id);
                log.info("⚡ [API] Kafka disabled - executed Direct Async Checkout tasks for Reservation {}", id);
            }
        } catch (Exception e) {
            log.error("Failed to process checkout tasks", e);
        }

        return r;
    }

    @Transactional
    public Reservation cancelReservation(UUID id, UUID tenantId, String reason, UUID userId) {
        Reservation r = getReservation(id, tenantId);
        if (List.of(ReservationStatus.checked_out, ReservationStatus.cancelled).contains(r.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reservation is already completed or cancelled");
        }

        r.setStatus(ReservationStatus.cancelled);
        r.setCancelledAt(LocalDateTime.now(HOTEL_ZONE));
        r.setCancelReason(reason);
        reservationRepo.save(r);

        Room room = r.getRoom();
        if (room.getStatus() == RoomStatus.reserved) {
            room.setStatus(RoomStatus.available);
            roomRepo.save(room);
        }

        try {
            if (kafkaEnabled) {
                Map<String, String> auditEvent = new HashMap<>();
                auditEvent.put("tenantId", tenantId.toString());
                auditEvent.put("branchId", r.getBranchId() != null ? r.getBranchId().toString() : null);
                auditEvent.put("userId", userId != null ? userId.toString() : null);
                auditEvent.put("entity", "RESERVATION");
                auditEvent.put("entityId", id.toString());
                auditEvent.put("action", "CANCEL");
                auditEvent.put("metadata", "Cancel Reason: " + reason + " | Room: " + room.getRoomNumber());

                kafkaTemplate.send("audit-logs", objectMapper.writeValueAsString(auditEvent));
                log.info("🚀 [API] Fired AuditEvent to Kafka for cancelled reservation {}", id);
            } else {
                AuditLog audit = AuditLog.builder()
                        .tenantId(tenantId)
                        .branchId(r.getBranchId())
                        .userId(userId)
                        .entity("RESERVATION")
                        .entityId(id.toString())
                        .action("CANCEL")
                        .metadata(Map.of("details", "Cancel Reason: " + reason + " | Room: " + room.getRoomNumber()))
                        .ipAddress("API-FALLBACK")
                        .userAgent("SYSTEM")
                        .build();
                auditService.log(audit);
                log.info("⚡ [API] Kafka disabled - executed Direct Async Audit Log for cancelled reservation {}", id);
            }
        } catch (Exception e) {
            log.error("Failed to log audit event", e);
        }

        return r;
    }

    public Map<String, Object> getFolio(UUID reservationId, UUID tenantId) {
        Reservation r = getReservation(reservationId, tenantId);
        List<FolioCharge> extraCharges = folioRepo.findByReservationIdAndTenantIdOrderByCreatedAtAsc(reservationId, tenantId);

        FolioCharge baseCharge = FolioCharge.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .reservationId(reservationId)
                .description("Room Charges (" + r.getNumNights() + " Nights)")
                .amount(r.getSubtotal())
                .chargeType(ChargeType.room_charge)
                .date(r.getCheckInDate())
                .createdAt(LocalDateTime.now(HOTEL_ZONE))
                .build();

        FolioCharge taxCharge = FolioCharge.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .reservationId(reservationId)
                .description("Taxes (Accommodation)")
                .amount(r.getTaxAmount())
                .chargeType(ChargeType.service)
                .date(r.getCheckInDate())
                .createdAt(LocalDateTime.now(HOTEL_ZONE))
                .build();

        List<FolioCharge> charges = new ArrayList<>();
        charges.add(baseCharge);
        charges.add(taxCharge);
        charges.addAll(extraCharges);

        BigDecimal totalCharges = charges.stream()
                .map(FolioCharge::getAmount)
                .filter(a -> a.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = charges.stream()
                .map(FolioCharge::getAmount)
                .filter(a -> a.compareTo(BigDecimal.ZERO) < 0)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal balance = totalCharges.subtract(totalPaid);

        return Map.of(
                "reservation", r,
                "charges", charges,
                "totalCharges", totalCharges,
                "totalPaid", totalPaid,
                "balance", balance
        );
    }

    @Transactional
    public FolioCharge addFolioCharge(UUID reservationId, UUID tenantId, AddFolioChargeDto dto) {
        getReservation(reservationId, tenantId);

        BigDecimal amount = dto.getAmount();
        if (dto.getChargeType() == ChargeType.advance || dto.getChargeType() == ChargeType.discount) {
            amount = amount.abs().negate();
        }

        FolioCharge charge = FolioCharge.builder()
                .tenantId(tenantId)
                .reservationId(reservationId)
                .description(dto.getDescription())
                .amount(amount)
                .chargeType(dto.getChargeType())
                .referenceId(dto.getReferenceId())
                .date(dto.getDate() != null ? LocalDate.parse(dto.getDate()) : LocalDate.now(HOTEL_ZONE))
                .build();

        return folioRepo.save(charge);
    }

    @Transactional
    public Bill generateBill(
            UUID reservationId,
            UUID tenantId,
            PaymentMethod paymentMethod,
            BigDecimal amountPaid,
            UUID shiftId
    ) {
        Map<String, Object> folio = getFolio(reservationId, tenantId);
        Reservation r = (Reservation) folio.get("reservation");
        @SuppressWarnings("unchecked")
        List<FolioCharge> charges = (List<FolioCharge>) folio.get("charges");

        Optional<Bill> existing = billRepo.findBillsWithFilters(tenantId, r.getBranchId(), null, null, BillSource.hotel, Pageable.unpaged())
                .stream()
                .filter(b -> reservationId.equals(b.getReservationId()))
                .findFirst();

        if (existing.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bill already generated for this reservation");
        }

        BigDecimal grandTotal = charges.stream()
                .map(FolioCharge::getAmount)
                .filter(a -> a.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal advances = charges.stream()
                .filter(c -> c.getChargeType() == ChargeType.advance)
                .map(FolioCharge::getAmount)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaidNow = advances.add(amountPaid != null ? amountPaid : BigDecimal.ZERO);
        BigDecimal totalTax = r.getTaxAmount();
        BigDecimal subtotal = grandTotal.subtract(totalTax);

        String today = LocalDate.now(HOTEL_ZONE).format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        Shift shift = shiftId != null ? shiftRepo.findById(shiftId).orElse(null) : null;

        Bill bill = Bill.builder()
                .tenant(tenantRepo.findById(tenantId).orElse(null))
                .branch(branchRepo.findById(r.getBranchId()).orElse(null))
                .reservationId(reservationId)
                .shift(shift)
                .source(BillSource.hotel)
                .status(grandTotal.subtract(totalPaidNow).compareTo(BigDecimal.valueOf(0.01)) <= 0 ? InvoiceStatus.paid : InvoiceStatus.issued)
                .customerName(r.getPrimaryGuest().getName())
                .customerPhone(r.getPrimaryGuest().getPhone())
                .supplyType(GstType.cgst_sgst)
                .subtotal(subtotal)
                .taxableAmount(subtotal)
                .cgstAmount(totalTax.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP))
                .sgstAmount(totalTax.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP))
                .igstAmount(BigDecimal.ZERO)
                .cessAmount(BigDecimal.ZERO)
                .totalTax(totalTax)
                .grandTotal(grandTotal)
                .paidAmount(totalPaidNow)
                .changeAmount(totalPaidNow.subtract(grandTotal).max(BigDecimal.ZERO))
                .notes("Hotel Bill for Room: " + r.getRoom().getRoomNumber())
                .build();

        int maxRetries = 3;
        Bill saved = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                long count = billRepo.countByTenantIdAndBillNumberPrefix(tenantId, "BILL-" + today + "-");
                String billNumber = String.format("BILL-%s-%05d", today, count + 1 + i);
                bill.setBillNumber(billNumber);
                bill.setInvoiceNumber(billNumber);
                saved = billRepo.saveAndFlush(bill);
                break;
            } catch (DataIntegrityViolationException e) {
                if (i == maxRetries - 1) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "High checkout concurrency. Please retry.");
                }
            }
        }

        if (amountPaid != null && amountPaid.compareTo(BigDecimal.ZERO) > 0) {
            Payment p = Payment.builder()
                    .tenant(saved.getTenant())
                    .branch(saved.getBranch())
                    .bill(saved)
                    .shift(shift)
                    .method(paymentMethod)
                    .amount(amountPaid)
                    .build();
            paymentRepo.save(p);

            FolioCharge settlement = FolioCharge.builder()
                    .tenantId(tenantId)
                    .reservationId(reservationId)
                    .description("Bill Settlement")
                    .amount(amountPaid.negate())
                    .chargeType(ChargeType.settlement)
                    .date(LocalDate.now(HOTEL_ZONE))
                    .build();
            folioRepo.save(settlement);
        }

        if (shift != null) {
            updateHotelShiftTotals(shift, saved, paymentMethod, amountPaid, advances);
        }

        if (r.getPrimaryGuest().getEmail() != null && !r.getPrimaryGuest().getEmail().isBlank()) {
            final Bill finalSaved = saved;
            CompletableFuture.runAsync(() -> {
                try {
                    emailService.sendReceipt(r.getPrimaryGuest().getEmail(), finalSaved);
                    log.info("Receipt emailed asynchronously to {}", r.getPrimaryGuest().getEmail());
                } catch (Exception e) {
                    log.error("Background email failed: {}", e.getMessage());
                }
            });
        }

        return saved;
    }

    private void updateHotelShiftTotals(Shift shift, Bill bill, PaymentMethod method, BigDecimal currentPaid, BigDecimal advances) {
        shift.setTotalSales(shift.getTotalSales().add(bill.getGrandTotal()));
        shift.setTotalOrders(shift.getTotalOrders() + 1);

        if (currentPaid != null && currentPaid.compareTo(BigDecimal.ZERO) > 0) {
            switch (method) {
                case cash -> shift.setCashSales(shift.getCashSales().add(currentPaid));
                case card -> shift.setCardSales(shift.getCardSales().add(currentPaid));
                case upi -> shift.setUpiSales(shift.getUpiSales().add(currentPaid));
                case wallet -> shift.setWalletSales(shift.getWalletSales().add(currentPaid));
                case credit -> shift.setCreditSales(shift.getCreditSales().add(currentPaid));
                case complimentary -> shift.setComplimentary(shift.getComplimentary().add(currentPaid));
            }
        }

        if (advances != null && advances.compareTo(BigDecimal.ZERO) > 0) {
            shift.setCashSales(shift.getCashSales().add(advances));
        }

        shift.setTotalCgst(shift.getTotalCgst().add(bill.getCgstAmount()));
        shift.setTotalSgst(shift.getTotalSgst().add(bill.getSgstAmount()));
        shift.setTotalIgst(shift.getTotalIgst().add(bill.getIgstAmount()));

        shiftRepo.save(shift);
    }

    public Map<String, Object> getHotelDashboardAnalytics(UUID tenantId, UUID branchId) {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        LocalDate weekAgo = today.minusDays(6);

        // 🟢 FIX 1: Modified to EXCLUDE Folio Charges originating from POS from Hotel Revenue
        String weeklySql = """
            SELECT 
              b.created_at::date::text AS date,
              COALESCE(SUM(b.grand_total), 0) - COALESCE(
                 (SELECT SUM(fc.amount) 
                  FROM hotel_folio_charges fc 
                  WHERE fc.reservation_id = b.reservation_id 
                    AND fc.description LIKE 'Restaurant POS Order %'), 0) AS revenue
            FROM bills b
            WHERE b.tenant_id = ?
              AND b.branch_id = ?
              AND b.source = 'hotel'
              AND b.status != 'void'
              AND b.created_at::date BETWEEN ? AND ?
            GROUP BY b.created_at::date, b.reservation_id
            ORDER BY b.created_at::date ASC
        """;
        List<Map<String, Object>> weeklyChartRaw = jdbcTemplate.queryForList(weeklySql, tenantId, branchId, weekAgo, today);
        
        // Group by date to combine rows if multiple bills happened on same day
        Map<String, BigDecimal> dailyRevenueMap = new LinkedHashMap<>();
        for (Map<String, Object> row : weeklyChartRaw) {
            String date = row.get("date").toString();
            BigDecimal rev = new BigDecimal(row.get("revenue").toString());
            dailyRevenueMap.put(date, dailyRevenueMap.getOrDefault(date, BigDecimal.ZERO).add(rev));
        }
        
        List<Map<String, Object>> weeklyChart = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : dailyRevenueMap.entrySet()) {
            weeklyChart.add(Map.of("date", entry.getKey(), "revenue", entry.getValue()));
        }

        BigDecimal todaySales = dailyRevenueMap.getOrDefault(today.toString(), BigDecimal.ZERO);

        long todayCheckins = reservationRepo.countByTenantIdAndBranchIdAndCheckInDateAndStatus(tenantId, branchId, today, ReservationStatus.checked_in);
        long todayCheckouts = reservationRepo.countByTenantIdAndBranchIdAndCheckOutDateAndStatus(tenantId, branchId, today, ReservationStatus.checked_out);

        Map<String, Object> occupancySummary = getOccupancySummary(tenantId, branchId);
        int occupancyRate = (int) occupancySummary.getOrDefault("occupancy_today", 0);

        BigDecimal weekSales = dailyRevenueMap.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal adr = todaySales.divide(BigDecimal.valueOf(Math.max(1, todayCheckins)), 2, RoundingMode.HALF_UP);

        long todayBills = billRepo.countByTenantIdAndBillNumberPrefix(tenantId, "BILL-" + today.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + "-");

        return Map.of(
                "todaySales", todaySales,
                "todayCheckins", todayCheckins,
                "todayCheckouts", todayCheckouts,
                "occupancyRate", occupancyRate,
                "weeklyChart", weeklyChart,
                "weekSales", weekSales,
                "adr", adr,
                "todayBills", todayBills,
                "roomStats", Map.of(
                        "available", occupancySummary.getOrDefault("available_rooms", 0),
                        "occupied", occupancySummary.getOrDefault("occupied_rooms", 0),
                        "reserved", reservationRepo.countByTenantIdAndBranchIdAndStatus(tenantId, branchId, ReservationStatus.confirmed),
                        "cleaning", occupancySummary.getOrDefault("maintenance_rooms", 0),
                        "maintenance", occupancySummary.getOrDefault("maintenance_rooms", 0)
                )
        );
    }

    public Map<String, Object> getOccupancySummary(UUID tenantId, UUID branchId) {
        String sql = """
            SELECT
              COUNT(*)::int                                                       AS total_rooms,
              COUNT(*) FILTER (WHERE status = 'occupied')::int                     AS occupied_rooms,
              COUNT(*) FILTER (WHERE status = 'available')::int                    AS available_rooms,
              COUNT(*) FILTER (WHERE status IN ('maintenance','out_of_order'))::int AS maintenance_rooms,
              COALESCE(ROUND(
                COUNT(*) FILTER (WHERE status = 'occupied')::numeric /
                NULLIF(COUNT(*),0) * 100
              ,0),0)::int                                                         AS occupancy_today
            FROM hotel_rooms
            WHERE tenant_id = ?
              AND branch_id = ?
              AND is_active = true
        """;
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, tenantId, branchId);
        if (result.isEmpty()) {
            return Map.of(
                    "total_rooms", 0,
                    "occupied_rooms", 0,
                    "available_rooms", 0,
                    "maintenance_rooms", 0,
                    "occupancy_today", 0
            );
        }
        return result.get(0);
    }

    public Map<String, Object> getGstr1Export(UUID tenantId, UUID branchId, LocalDate from, LocalDate to) {
        List<Map<String, Object>> gstData = getGstReport(tenantId, branchId, from, to);
        return Map.of(
                "version", "GSTR1_v2.0",
                "gstin", tenantRepo.findById(tenantId).map(t -> t.getGstin() != null ? t.getGstin() : "").orElse(""),
                "period", from.toString() + " to " + to.toString(),
                "b2b", List.of(),
                "b2c", gstData
        );
    }

    public List<Map<String, Object>> getRevenueReport(UUID tenantId, UUID branchId, LocalDate from, LocalDate to) {
        // 🟢 FIX 2: Added exact same POS subtraction logic for the detailed Revenue Reports page
        String sql = """
            SELECT
              b.created_at::date AS date,
              COUNT(b.id)::int   AS bookings,
              COALESCE(SUM(b.grand_total), 0) - COALESCE(
                 (SELECT SUM(fc.amount) 
                  FROM hotel_folio_charges fc 
                  WHERE fc.reservation_id = b.reservation_id 
                    AND fc.description LIKE 'Restaurant POS Order %'), 0) AS revenue,
              COALESCE(SUM(b.total_tax), 0)   AS tax,
              COALESCE(SUM(b.grand_total - b.total_tax), 0) - COALESCE(
                 (SELECT SUM(fc.amount) 
                  FROM hotel_folio_charges fc 
                  WHERE fc.reservation_id = b.reservation_id 
                    AND fc.description LIKE 'Restaurant POS Order %'), 0) AS net_revenue
            FROM bills b
            WHERE b.tenant_id = ?
              AND b.branch_id = ?
              AND b.source = 'hotel'
              AND b.status != 'void'
              AND b.created_at::date BETWEEN ? AND ?
            GROUP BY b.created_at::date, b.reservation_id
            ORDER BY b.created_at::date
        """;
        return jdbcTemplate.queryForList(sql, tenantId, branchId, from, to);
    }

    public List<Map<String, Object>> getBookingsReport(UUID tenantId, UUID branchId, LocalDate from, LocalDate to) {
        String sql = """
            SELECT
              r.id                                        AS booking_id,
              r.booking_ref                               AS booking_ref,
              g.name                                      AS guest_name,
              g.email                                     AS guest_email,
              g.phone                                     AS guest_phone,
              rm.room_number,
              r.check_in_date                             AS check_in,
              r.check_out_date                            AS check_out,
              r.num_nights                                AS nights,
              r.status,
              r.total_amount                              AS amount,
              r.balance_due,
              COALESCE(b.status, 'unpaid')          AS payment_status
            FROM hotel_reservations r
            LEFT JOIN hotel_guests   g  ON g.id  = r.primary_guest_id
            LEFT JOIN hotel_rooms    rm ON rm.id = r.room_id
            LEFT JOIN bills          b  ON b.reservation_id = r.id
            WHERE r.tenant_id  = ?
              AND r.branch_id  = ?
              AND r.check_in_date BETWEEN ? AND ?
            ORDER BY r.check_in_date DESC
        """;
        return jdbcTemplate.queryForList(sql, tenantId, branchId, from, to);
    }

    public List<Map<String, Object>> getRoomsPerformanceReport(UUID tenantId, UUID branchId, LocalDate from, LocalDate to) {
        String sql = """
            SELECT
              rm.room_number,
              rt.name                                              AS room_type,
              COUNT(r.id)::int                                     AS bookings,
              COALESCE(
                ROUND(
                  COUNT(r.id)::numeric /
                  NULLIF((?::date - ?::date + 1), 0) * 100
                ,0),0)::int                                        AS occupancy,
              COALESCE(SUM(r.total_amount),0)                      AS revenue,
              COALESCE(ROUND(AVG(r.rate_per_night)::numeric,2),0)  AS avg_rate
            FROM hotel_rooms rm
            LEFT JOIN hotel_room_types rt ON rt.id = rm.room_type_id
            LEFT JOIN hotel_reservations r
              ON r.room_id   = rm.id
             AND r.status   NOT IN ('cancelled','no_show')
             AND r.check_in_date BETWEEN ? AND ?
            WHERE rm.tenant_id = ?
              AND rm.branch_id = ?
              AND rm.is_active = true
            GROUP BY rm.id, rm.room_number, rt.name
            ORDER BY revenue DESC
        """;
        return jdbcTemplate.queryForList(sql, to, from, from, to, tenantId, branchId);
    }

    public List<Map<String, Object>> getPaymentsReport(UUID tenantId, UUID branchId, OffsetDateTime from, OffsetDateTime to) {
        String sql = """
            SELECT
              p.method,
              COUNT(p.id)::int                 AS transaction_count,
              COALESCE(SUM(p.amount),0)        AS total_amount
            FROM payments p
            JOIN bills b ON b.id = p.bill_id
            WHERE p.tenant_id  = ?
              AND b.branch_id  = ?
              AND p.created_at BETWEEN ? AND ?
            GROUP BY p.method
            ORDER BY total_amount DESC
        """;
        return jdbcTemplate.queryForList(sql, tenantId, branchId, from, to);
    }

    public List<Map<String, Object>> getGstReport(UUID tenantId, UUID branchId, LocalDate from, LocalDate to) {
        String sql = """
            SELECT
              TO_CHAR(DATE_TRUNC('month', r.check_in_date::date), 'YYYY-MM-DD') AS month,
              COUNT(r.id)::int                                                  AS total_invoices,
              COALESCE(SUM(r.total_amount - r.tax_amount),0)                      AS taxable_value,
              COALESCE(SUM(r.tax_amount / 2),0)                                   AS cgst,
              COALESCE(SUM(r.tax_amount / 2),0)                                   AS sgst,
              0                                                                   AS igst,
              COALESCE(SUM(r.tax_amount),0)                                       AS total_tax,
              COALESCE(SUM(r.total_amount),0)                                     AS gross_value
            FROM hotel_reservations r
            WHERE r.tenant_id  = ?
              AND r.branch_id  = ?
              AND r.status     NOT IN ('cancelled','no_show')
              AND r.check_in_date BETWEEN ? AND ?
            GROUP BY DATE_TRUNC('month', r.check_in_date::date)
            ORDER BY DATE_TRUNC('month', r.check_in_date::date)
        """;
        return jdbcTemplate.queryForList(sql, tenantId, branchId, from, to);
    }

    public List<Map<String, Object>> getFrontDeskReport(UUID tenantId, UUID branchId, LocalDate from, LocalDate to) {
        String sql = """
            SELECT
              u.id                                           AS staff_id,
              CONCAT(u.first_name, ' ', COALESCE(u.last_name, '')) AS staff_name,
              COUNT(r.id) FILTER (WHERE r.actual_check_in  IS NOT NULL)::int AS check_ins,
              COUNT(r.id) FILTER (WHERE r.actual_check_out IS NOT NULL)::int AS check_outs,
              COUNT(r.id)::int                               AS bookings_handled,
              COALESCE(SUM(r.total_amount),0)                AS revenue_managed,
              COALESCE(ROUND(AVG(r.total_amount)::numeric,2),0) AS avg_booking_value
            FROM hotel_reservations r
            JOIN users u ON u.id = r.created_by_id
            WHERE r.tenant_id  = ?
              AND r.branch_id  = ?
              AND r.status     NOT IN ('cancelled','no_show')
              AND r.check_in_date BETWEEN ? AND ?
            GROUP BY u.id, u.first_name, u.last_name
            ORDER BY revenue_managed DESC
        """;
        return jdbcTemplate.queryForList(sql, tenantId, branchId, from, to);
    }

    public List<HousekeepingTask> listHousekeepingTasks(UUID tenantId, UUID branchId, String date) {
        LocalDate localDate = date != null ? LocalDate.parse(date) : LocalDate.now(HOTEL_ZONE);
        return branchId != null
                ? hkRepo.findByTenantIdAndBranchIdAndScheduledForOrderByPriorityDescCreatedAtAsc(tenantId, branchId, localDate)
                : hkRepo.findByTenantIdAndScheduledForOrderByPriorityDescCreatedAtAsc(tenantId, localDate);
    }

    @Transactional
    public HousekeepingTask createHousekeepingTask(UUID tenantId, UUID branchId, CreateHkTaskDto dto) {
        Room room = roomRepo.findByIdAndTenantId(dto.getRoomId(), tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        HousekeepingTask task = HousekeepingTask.builder()
                .tenantId(tenantId)
                .branchId(branchId)
                .roomId(room.getId())
                .room(room)
                .taskType(dto.getTaskType())
                .priority(dto.getPriority() != null ? dto.getPriority() : HkPriority.normal)
                .scheduledFor(dto.getScheduledFor() != null ? LocalDate.parse(dto.getScheduledFor()) : LocalDate.now(HOTEL_ZONE))
                .notes(dto.getNotes())
                .status(HkStatus.pending)
                .build();

        return hkRepo.save(task);
    }

    @Transactional
    public HousekeepingTask updateHousekeepingTask(UUID id, UUID tenantId, HkStatus status, String notes) {
        HousekeepingTask task = hkRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Housekeeping task not found"));

        task.setStatus(status);
        if (notes != null) task.setNotes(notes);
        if (status == HkStatus.in_progress && task.getStartedAt() == null) {
            task.setStartedAt(LocalDateTime.now(HOTEL_ZONE));
        }
        if (status == HkStatus.done && task.getCompletedAt() == null) {
            task.setCompletedAt(LocalDateTime.now(HOTEL_ZONE));
        }

        HousekeepingTask saved = hkRepo.save(task);

        if (status == HkStatus.done && task.getTaskType() == HkTaskType.checkout_clean) {
            Room room = roomRepo.findByIdAndTenantId(task.getRoomId(), tenantId).orElse(null);
            if (room != null) {
                room.setStatus(RoomStatus.available);
                roomRepo.save(room);
            }
        }

        return saved;
    }

    @Transactional
    public void chargeToRoomFolio(UUID tenantId, UUID branchId, String roomNumber, BigDecimal amount, String orderNumber) {
        Room room = roomRepo.findByTenantIdAndBranchIdAndRoomNumber(tenantId, branchId, roomNumber)
                .orElseThrow(() -> new RuntimeException("Room " + roomNumber + " not found"));

        String sql = "SELECT id FROM hotel_reservations WHERE room_id = ? AND status = 'checked_in' LIMIT 1";
        List<UUID> activeResIds = jdbcTemplate.queryForList(sql, UUID.class, room.getId());

        if (activeResIds.isEmpty()) {
            throw new RuntimeException("No guest is currently checked into Room " + roomNumber);
        }

        FolioCharge charge = FolioCharge.builder()
                .tenantId(tenantId)
                .reservationId(activeResIds.get(0))
                .description("Restaurant POS Order " + (orderNumber != null ? "#" + orderNumber : ""))
                .amount(amount)
                .chargeType(ChargeType.service)
                .date(LocalDate.now(HOTEL_ZONE))
                .build();

        folioRepo.save(charge);
        log.info("Successfully charged ₹{} to Room {} for Order {}", amount, roomNumber, orderNumber);
    }

    public Map<String, Object> getDashboard(UUID tenantId, UUID branchId) {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        List<Room> rooms = roomRepo.findAllActiveRoomsByTenantAndBranch(tenantId, branchId);

        Map<String, Long> statusCounts = new HashMap<>();
        for (RoomStatus rs : RoomStatus.values()) {
            statusCounts.put(rs.name(), 0L);
        }
        for (Room r : rooms) {
            String name = r.getStatus().name();
            statusCounts.put(name, statusCounts.getOrDefault(name, 0L) + 1);
        }

        long arrivals = branchId != null
                ? reservationRepo.countByTenantIdAndBranchIdAndCheckInDateAndStatus(tenantId, branchId, today, ReservationStatus.confirmed)
                : reservationRepo.countByTenantIdAndCheckInDateAndStatus(tenantId, today, ReservationStatus.confirmed);

        long departures = branchId != null
                ? reservationRepo.countByTenantIdAndBranchIdAndCheckOutDateAndStatus(tenantId, branchId, today, ReservationStatus.checked_in)
                : reservationRepo.countByTenantIdAndCheckOutDateAndStatus(tenantId, today, ReservationStatus.checked_in);

        long inHouse = branchId != null
                ? reservationRepo.countByTenantIdAndBranchIdAndStatus(tenantId, branchId, ReservationStatus.checked_in)
                : reservationRepo.countByTenantIdAndStatus(tenantId, ReservationStatus.checked_in);

        long total = rooms.size();
        long occupied = statusCounts.getOrDefault(RoomStatus.occupied.name(), 0L);
        long occupancyPct = total > 0 ? Math.round(((double) occupied / total) * 100) : 0;

        return Map.of(
                "totalRooms", total,
                "occupancyPct", occupancyPct,
                "roomsByStatus", statusCounts,
                "arrivalsToday", arrivals,
                "departuresToday", departures,
                "inHouse", inHouse
        );
    }
}