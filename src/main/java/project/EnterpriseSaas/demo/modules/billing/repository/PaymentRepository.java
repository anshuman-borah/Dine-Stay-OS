package project.EnterpriseSaas.demo.modules.billing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.EnterpriseSaas.demo.modules.billing.entity.Payment;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @Query("SELECT p FROM Payment p WHERE p.bill.id = :billId")
    List<Payment> findByBillId(@Param("billId") UUID billId);

    @Query("SELECT p FROM Payment p WHERE p.shift.id = :shiftId")
    List<Payment> findByShiftId(@Param("shiftId") UUID shiftId);
}