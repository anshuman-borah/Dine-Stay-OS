package project.EnterpriseSaas.demo.modules.hotel.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.EnterpriseSaas.demo.common.enums.ReservationStatus;
import project.EnterpriseSaas.demo.modules.hotel.entity.Reservation;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    @Query("SELECT r FROM Reservation r LEFT JOIN FETCH r.primaryGuest LEFT JOIN FETCH r.room rm LEFT JOIN FETCH rm.roomType WHERE r.id = :id AND r.tenantId = :tenantId")
    Optional<Reservation> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("SELECT r FROM Reservation r WHERE r.room.id = :roomId " +
            "AND r.status NOT IN :excludedStatuses " +
            "AND r.checkInDate < :checkOutDate " +
            "AND r.checkOutDate > :checkInDate")
    List<Reservation> findOverlappingReservations(
            @Param("roomId") UUID roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("excludedStatuses") Collection<ReservationStatus> excludedStatuses
    );

    @Query(value = "SELECT r FROM Reservation r " +
            "LEFT JOIN FETCH r.primaryGuest g " +
            "LEFT JOIN FETCH r.room rm " +
            "LEFT JOIN FETCH rm.roomType rt " +
            "WHERE r.tenantId = :tenantId " +
            "AND (:branchId IS NULL OR r.branchId = :branchId) " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (cast(:from as date) IS NULL OR r.checkInDate >= :from) " +
            "AND (cast(:to as date) IS NULL OR r.checkOutDate <= :to) " +
            "AND (:search IS NULL OR :search = '' OR " +
            "     LOWER(g.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(g.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(rm.roomNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(r.bookingRef) LIKE LOWER(CONCAT('%', :search, '%')))",
            countQuery = "SELECT COUNT(r) FROM Reservation r " +
                    "LEFT JOIN r.primaryGuest g " +
                    "LEFT JOIN r.room rm " +
                    "WHERE r.tenantId = :tenantId " +
                    "AND (:branchId IS NULL OR r.branchId = :branchId) " +
                    "AND (:status IS NULL OR r.status = :status) " +
                    "AND (cast(:from as date) IS NULL OR r.checkInDate >= :from) " +
                    "AND (cast(:to as date) IS NULL OR r.checkOutDate <= :to) " +
                    "AND (:search IS NULL OR :search = '' OR " +
                    "     LOWER(g.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "     LOWER(g.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "     LOWER(rm.roomNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                    "     LOWER(r.bookingRef) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Reservation> findReservationsWithFilters(
            @Param("tenantId") UUID tenantId,
            @Param("branchId") UUID branchId,
            @Param("status") ReservationStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("search") String search,
            Pageable pageable
    );

    long countByTenantIdAndCheckInDateAndStatus(UUID tenantId, LocalDate date, ReservationStatus status);
    long countByTenantIdAndBranchIdAndCheckInDateAndStatus(UUID tenantId, UUID branchId, LocalDate date, ReservationStatus status);

    long countByTenantIdAndCheckOutDateAndStatus(UUID tenantId, LocalDate date, ReservationStatus status);
    long countByTenantIdAndBranchIdAndCheckOutDateAndStatus(UUID tenantId, UUID branchId, LocalDate date, ReservationStatus status);

    long countByTenantIdAndStatus(UUID tenantId, ReservationStatus status);
    long countByTenantIdAndBranchIdAndStatus(UUID tenantId, UUID branchId, ReservationStatus status);
}