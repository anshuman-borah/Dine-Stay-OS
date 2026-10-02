package project.EnterpriseSaas.demo.modules.subscription.service;

import org.springframework.cache.annotation.Cacheable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.EnterpriseSaas.demo.common.enums.SubscriptionStatus;
import project.EnterpriseSaas.demo.modules.plan.entity.Plan;
import project.EnterpriseSaas.demo.modules.plan.repository.PlanRepository;
import project.EnterpriseSaas.demo.modules.subscription.entity.Subscription;
import project.EnterpriseSaas.demo.modules.subscription.repository.SubscriptionRepository;
import project.EnterpriseSaas.demo.modules.tenant.entity.Tenant;
import project.EnterpriseSaas.demo.modules.tenant.repository.TenantRepository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final PlanRepository planRepo;
    private final SubscriptionRepository subRepo;
    private final TenantRepository tenantRepo;

    // ── Get Public Plans ─────────────────────────────────────────────────────
    @Cacheable(value = "subscriptionPlans")
    public List<Plan> getPlans() {
        return planRepo.findByIsActiveTrueOrderByPriceMonthlyAsc();
    }

    // ── Get Current Tenant Subscription ──────────────────────────────────────

    public Subscription getSubscription(UUID tenantId) {
        return subRepo.findFirstByTenant_IdOrderByCreatedAtDesc(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found"));
    }

    // ── Check Feature Enabled ────────────────────────────────────────────────

    public boolean isFeatureEnabled(UUID tenantId, String feature) {
        Subscription sub = subRepo.findFirstByTenant_IdOrderByCreatedAtDesc(tenantId).orElse(null);
        if (sub == null || sub.getStatus() == SubscriptionStatus.cancelled) {
            return false;
        }
        if (sub.getStatus() == SubscriptionStatus.trial) {
            return true;
        }

        List<String> features = sub.getPlan() != null ? sub.getPlan().getFeatures() : List.of();
        return features.contains("all") || features.contains(feature);
    }

    // ── Check Limits ─────────────────────────────────────────────────────────

    public Map<String, Object> checkLimits(UUID tenantId) {
        Subscription sub = subRepo.findFirstByTenant_IdOrderByCreatedAtDesc(tenantId).orElse(null);
        if (sub == null) return null;

        Plan plan = sub.getPlan();
        return Map.of(
                "planCode", plan != null ? plan.getCode() : "trial",
                "planName", plan != null ? plan.getName() : "Free Trial",
                "maxBranches", plan != null ? plan.getMaxBranches() : 1,
                "maxUsers", plan != null ? plan.getMaxUsers() : 5,
                "maxMenuItems", plan != null ? plan.getMaxMenuItems() : 100,
                "status", sub.getStatus(),
                "trialEndsAt", sub.getTrialEndsAt() != null ? sub.getTrialEndsAt() : ""
        );
    }

    // ── Upgrade Subscription ─────────────────────────────────────────────────

    @Transactional
    public Subscription upgradeSubscription(UUID tenantId, String planCode, Map<String, Object> paymentDetails) {
        Plan plan = planRepo.findByCode(planCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Selected subscription plan not found"));

        Subscription sub = subRepo.findFirstByTenant_IdOrderByCreatedAtDesc(tenantId).orElse(null);

        if (sub == null) {
            Tenant tenant = tenantRepo.findById(tenantId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
            sub = Subscription.builder()
                    .tenant(tenant)
                    .build();
        }

        sub.setPlan(plan);
        sub.setStatus(SubscriptionStatus.active);
        sub.setCurrentPeriodStart(OffsetDateTime.now());
        sub.setCurrentPeriodEnd(OffsetDateTime.now().plusDays(30)); // 30-day cycle

        Map<String, Object> meta = new HashMap<>(sub.getMetadata() != null ? sub.getMetadata() : Map.of());
        meta.put("upgradedAt", Instant.now().toString());
        if (paymentDetails != null) {
            meta.put("lastPayment", paymentDetails);
        }
        sub.setMetadata(meta);

        return subRepo.save(sub);
    }
}