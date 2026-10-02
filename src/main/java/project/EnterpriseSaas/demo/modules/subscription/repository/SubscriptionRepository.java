package project.EnterpriseSaas.demo.modules.subscription.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.EnterpriseSaas.demo.modules.subscription.entity.Subscription;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    // Safely replaces the raw HQL LIMIT 1 query and prevents N+1 issues by eagerly fetching the plan
    @EntityGraph(attributePaths = {"plan"})
    Optional<Subscription> findFirstByTenant_IdOrderByCreatedAtDesc(UUID tenantId);

    Optional<Subscription> findByTenant_Id(UUID tenantId);
}