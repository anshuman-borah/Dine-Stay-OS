package project.EnterpriseSaas.demo.modules.audit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import project.EnterpriseSaas.demo.modules.audit.repository.AuditLogRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditCleanupService {

    private final AuditLogRepository auditLogRepo;

    // Runs automatically every day at 3:00 AM
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOldAuditLogs() {
        log.info("🧹 Starting daily audit log cleanup...");

        // Calculate the date 30 days ago
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        // Delete all logs older than 30 days to save Supabase storage!
        auditLogRepo.deleteLogsOlderThan(thirtyDaysAgo);

        log.info("✨ Successfully cleaned up audit logs older than 30 days.");
    }
}