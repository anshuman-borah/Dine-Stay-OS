package project.EnterpriseSaas.demo.modules.core.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledJobs {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Runs every day at 12:05 AM
     * Expires trial periods and marks overdue subscriptions.
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void processSubscriptions() {
        log.info("Running daily subscription checks...");

        // 1. Expire Trials
        String expireTrialsSql = """
            UPDATE subscriptions 
            SET status = 'past_due', updated_at = NOW() 
            WHERE status = 'trial' AND trial_ends_at < NOW()
        """;
        int trialsExpired = jdbcTemplate.update(expireTrialsSql);
        if (trialsExpired > 0) log.info("Expired {} trial subscriptions", trialsExpired);

        // 2. Expire Active Subscriptions that missed payment
        String expireActiveSql = """
            UPDATE subscriptions 
            SET status = 'past_due', updated_at = NOW() 
            WHERE status = 'active' AND current_period_end < NOW()
        """;
        int subsExpired = jdbcTemplate.update(expireActiveSql);
        if (subsExpired > 0) log.info("Marked {} subscriptions as past_due", subsExpired);
    }

    /**
     * Runs every day at 3:00 AM
     * Force-closes any POS shifts that have been open for more than 24 hours.
     * This prevents reporting bugs if a cashier forgets to click "Close Shift".
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void autoCloseOrphanedShifts() {
        log.info("Checking for orphaned POS shifts...");

        String sql = """
            UPDATE shifts 
            SET status = 'closed', 
                closed_at = NOW(), 
                notes = CONCAT(COALESCE(notes, ''), ' [AUTO-CLOSED BY SYSTEM]'),
                updated_at = NOW()
            WHERE status = 'open' AND opened_at < NOW() - INTERVAL '24 hours'
        """;
        int closedShifts = jdbcTemplate.update(sql);
        if (closedShifts > 0) log.info("Auto-closed {} orphaned shifts", closedShifts);
    }

    /**
     * Runs every day at 1:00 AM
     * Auto-generates Housekeeping tasks for occupied hotel rooms.
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void generateHousekeepingTasks() {
        log.info("Generating daily housekeeping tasks...");

        String sql = """
            INSERT INTO hotel_housekeeping_tasks 
            (id, tenant_id, branch_id, room_id, task_type, status, scheduled_for, created_at, updated_at)
            SELECT 
                gen_random_uuid(), tenant_id, branch_id, id, 'daily_cleaning', 'pending', CURRENT_DATE, NOW(), NOW()
            FROM hotel_rooms
            WHERE status = 'occupied' AND is_active = true
            ON CONFLICT DO NOTHING
        """;
        int tasksGenerated = jdbcTemplate.update(sql);
        if (tasksGenerated > 0) log.info("Generated {} daily housekeeping tasks", tasksGenerated);
    }
}