package project.EnterpriseSaas.demo.modules.shift.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.EnterpriseSaas.demo.modules.shift.entity.ShiftDenomination;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShiftDenominationRepository extends JpaRepository<ShiftDenomination, UUID> {

    @Query("SELECT d FROM ShiftDenomination d WHERE d.shift.id = :shiftId")
    List<ShiftDenomination> findByShiftId(@Param("shiftId") UUID shiftId);
}