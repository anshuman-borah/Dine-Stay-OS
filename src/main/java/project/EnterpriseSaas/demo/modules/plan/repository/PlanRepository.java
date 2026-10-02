package project.EnterpriseSaas.demo.modules.plan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.EnterpriseSaas.demo.modules.plan.entity.Plan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanRepository extends JpaRepository<Plan, UUID> {

    List<Plan> findByIsActiveTrueOrderByPriceMonthlyAsc();

    Optional<Plan> findByCode(String code);

    boolean existsByCode(String code);
}