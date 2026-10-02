package project.EnterpriseSaas.demo.modules.audit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import project.EnterpriseSaas.demo.modules.audit.dto.AuditQueryDto;
import project.EnterpriseSaas.demo.modules.audit.entity.AuditLog;
import project.EnterpriseSaas.demo.modules.audit.repository.AuditLogRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditRepo;

    // ── Asynchronous non-blocking log ingestion ──────────────────────────────

    @Async
    public void log(AuditLog entry) {
        try {
            if (entry.getMetadata() == null) {
                entry.setMetadata(Map.of());
            }
            auditRepo.save(entry);
        } catch (Exception e) {
            // Audit logging must never fail the main request execution
            log.warn("Failed to write audit log entry: {}", e.getMessage());
        }
    }

    // ── Find By Entity (Order, Bill, Shift, etc.) ────────────────────────────

    public List<AuditLog> findByEntity(String entity, String entityId, UUID tenantId) {
        PageRequest pageRequest = PageRequest.of(0, 50);
        return auditRepo.findByEntityAndEntityIdAndTenantIdOrderByCreatedAtDesc(entity, entityId, tenantId, pageRequest);
    }

    // ── Find By User (Staff actions) ─────────────────────────────────────────

    public List<AuditLog> findByUser(UUID userId, UUID tenantId, int limit) {
        PageRequest pageRequest = PageRequest.of(0, Math.min(limit, 200));
        return auditRepo.findByUserIdAndTenantIdOrderByCreatedAtDesc(userId, tenantId, pageRequest);
    }

    // ── Find By Tenant with Filters (Paginated) ──────────────────────────────

    public Map<String, Object> findByTenant(UUID tenantId, AuditQueryDto query) {
        int page = query.getPage() != null && query.getPage() > 0 ? query.getPage() : 1;
        int limit = query.getLimit() != null && query.getLimit() > 0 ? query.getLimit() : 100;

        // Provide extreme fallback dates so the SQL BETWEEN query never receives a NULL parameter
        LocalDateTime fromDt = query.getFrom() != null ? query.getFrom().atStartOfDay() : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime toDt = query.getTo() != null ? query.getTo().atTime(23, 59, 59, 999999999) : LocalDateTime.of(2100, 12, 31, 23, 59, 59);

        PageRequest pageRequest = PageRequest.of(page - 1, limit);
        Page<AuditLog> auditPage = auditRepo.findAuditLogsWithFilters(
                tenantId,
                query.getEntity(),
                query.getAction(),
                query.getUserId(),
                fromDt,
                toDt,
                pageRequest
        );

        return Map.of(
                "data", auditPage.getContent(),
                "total", auditPage.getTotalElements(),
                "page", page,
                "limit", limit
        );
    }
}