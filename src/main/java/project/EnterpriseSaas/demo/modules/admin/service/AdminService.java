package project.EnterpriseSaas.demo.modules.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.EnterpriseSaas.demo.modules.plan.entity.Plan;
import project.EnterpriseSaas.demo.modules.plan.repository.PlanRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final JdbcTemplate jdbcTemplate;
    private final PlanRepository planRepo;
    private final PasswordEncoder passwordEncoder;

    // ── Platform-wide KPI Stats ──────────────────────────────────────────────

    public Map<String, Object> getStats() {
        String sqlTenants = "SELECT COUNT(*) AS total, SUM(CASE WHEN is_active THEN 1 ELSE 0 END) AS active FROM tenants WHERE slug != '_system'";
        Map<String, Object> tenantsMap = jdbcTemplate.queryForMap(sqlTenants);

        String sqlOrders = "SELECT COUNT(*)::int AS total FROM orders WHERE status = 'billed' AND created_at >= NOW() - INTERVAL '30 days'";
        Integer ordersTotal = jdbcTemplate.queryForObject(sqlOrders, Integer.class);

        String sqlRevenue = """
            WITH valid_bills AS (
                SELECT COALESCE(SUM(grand_total), 0) AS val
                FROM bills
                WHERE status NOT IN ('void', 'refunded')
                  AND date_trunc('month', created_at AT TIME ZONE 'Asia/Kolkata') = date_trunc('month', NOW() AT TIME ZONE 'Asia/Kolkata')
            ),
            folio_deductions AS (
                SELECT COALESCE(SUM(fc.amount), 0) AS val
                FROM hotel_folio_charges fc
                WHERE fc.description LIKE 'Restaurant POS Order %'
                  AND date_trunc('month', fc.created_at AT TIME ZONE 'Asia/Kolkata') = date_trunc('month', NOW() AT TIME ZONE 'Asia/Kolkata')
            )
            SELECT (SELECT val FROM valid_bills) - (SELECT val FROM folio_deductions) AS mrr
        """;
        BigDecimal mrr = jdbcTemplate.queryForObject(sqlRevenue, BigDecimal.class);

        String sqlActiveToday = "SELECT COUNT(DISTINCT tenant_id)::int AS count FROM orders WHERE status = 'billed' AND created_at AT TIME ZONE 'Asia/Kolkata' >= date_trunc('day', NOW() AT TIME ZONE 'Asia/Kolkata')";
        Integer activeToday = jdbcTemplate.queryForObject(sqlActiveToday, Integer.class);

        String sqlSubs = """
            SELECT
              SUM(CASE WHEN s.status = 'active' THEN 1 ELSE 0 END)::int AS active,
              SUM(CASE WHEN s.status = 'trial'  THEN 1 ELSE 0 END)::int AS trial,
              SUM(CASE WHEN s.status = 'past_due' OR s.status = 'cancelled' THEN 1 ELSE 0 END)::int AS churned
            FROM subscriptions s
            JOIN tenants t ON t.id = s.tenant_id
            WHERE t.slug != '_system'
        """;
        Map<String, Object> subStats = jdbcTemplate.queryForMap(sqlSubs);

        return Map.of(
                "tenants", Map.of(
                        "total", tenantsMap.getOrDefault("total", 0),
                        "active", tenantsMap.getOrDefault("active", 0)
                ),
                "subscriptions", Map.of(
                        "active", subStats.getOrDefault("active", 0),
                        "trial", subStats.getOrDefault("trial", 0),
                        "churned", subStats.getOrDefault("churned", 0)
                ),
                "orders30d", ordersTotal != null ? ordersTotal : 0,
                "activeToday", activeToday != null ? activeToday : 0,
                "mrr", mrr != null ? mrr : BigDecimal.ZERO
        );
    }

    // ── Paginated Tenant List ────────────────────────────────────────────────

    public Map<String, Object> listTenants(int page, int limit, String search) {
        int offset = (page - 1) * limit;
        boolean hasSearch = search != null && !search.isBlank();
        String searchClause = hasSearch ? "AND (t.name ILIKE ? OR t.email ILIKE ? OR t.slug ILIKE ?)" : "";

        String sqlData = String.format("""
            SELECT
              t.id, t.name, t.slug, t.email, t.phone, t.address_line1 AS address,
              t.is_active, t.created_at,
              s.status  AS sub_status,
              s.trial_ends_at,
              p.name    AS plan_name,
              p.price_monthly,
              (SELECT COUNT(*)::int FROM users   u WHERE u.tenant_id = t.id AND u.is_active) AS user_count,
              (SELECT COUNT(*)::int FROM branches b WHERE b.tenant_id = t.id)                AS branch_count,
              (SELECT COUNT(*)::int FROM orders  o WHERE o.tenant_id = t.id AND o.status = 'billed' AND o.created_at >= NOW() - INTERVAL '30 days') AS orders_30d
            FROM tenants t
            LEFT JOIN LATERAL (
              SELECT status, trial_ends_at, plan_id
              FROM subscriptions
              WHERE tenant_id = t.id
              ORDER BY created_at DESC
              LIMIT 1
            ) s ON true
            LEFT JOIN plans p ON p.id = s.plan_id
            WHERE t.slug != '_system' %s
            ORDER BY t.created_at DESC
            LIMIT ? OFFSET ?
        """, searchClause);

        List<Object> params = new ArrayList<>();
        if (hasSearch) {
            String searchPattern = "%" + search + "%";
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }
        params.add(limit);
        params.add(offset);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sqlData, params.toArray());

        String sqlCount = String.format("SELECT COUNT(*)::int FROM tenants t WHERE t.slug != '_system' %s", searchClause);
        List<Object> countParams = new ArrayList<>();
        if (hasSearch) {
            String searchPattern = "%" + search + "%";
            countParams.add(searchPattern);
            countParams.add(searchPattern);
            countParams.add(searchPattern);
        }
        Integer total = jdbcTemplate.queryForObject(sqlCount, Integer.class, countParams.toArray());

        return Map.of("data", rows, "total", total != null ? total : 0, "page", page, "limit", limit);
    }

    // ── Tenant Details ───────────────────────────────────────────────────────

    public Map<String, Object> getTenant(UUID id) {
        String sql = """
            SELECT
              t.id, t.name, t.slug, t.email, t.phone, t.gstin, t.address_line1 AS address,
              t.logo_url, t.settings, t.is_active, t.created_at, t.updated_at,
              s.status AS sub_status, s.trial_ends_at, s.current_period_end,
              p.name AS plan_name, p.price_monthly, p.max_branches, p.max_users,
              (SELECT COUNT(*)::int FROM users   u WHERE u.tenant_id = t.id AND u.is_active)    AS user_count,
              (SELECT COUNT(*)::int FROM branches b WHERE b.tenant_id = t.id)                   AS branch_count,
              (SELECT COUNT(*)::int FROM orders  o WHERE o.tenant_id = t.id AND o.status = 'billed') AS total_orders,
              (
                (SELECT COALESCE(SUM(grand_total),0) FROM bills b WHERE b.tenant_id = t.id AND b.status NOT IN ('void', 'refunded'))
                -
                (SELECT COALESCE(SUM(fc.amount),0) FROM hotel_folio_charges fc JOIN hotel_reservations r ON r.id = fc.reservation_id WHERE r.tenant_id = t.id AND fc.description LIKE 'Restaurant POS Order %')
              ) AS total_revenue
            FROM tenants t
            LEFT JOIN subscriptions s ON s.tenant_id = t.id
            LEFT JOIN plans p ON p.id = s.plan_id
            WHERE t.id = ?
        """;
        try {
            return jdbcTemplate.queryForMap(sql, id);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
        }
    }

    // ── Activate / Suspend Tenant ────────────────────────────────────────────

    @Transactional
    public Map<String, Object> setTenantActive(UUID id, boolean isActive) {
        String sql = "UPDATE tenants SET is_active = ?, updated_at = NOW() WHERE id = ?";
        int updated = jdbcTemplate.update(sql, isActive, id);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
        }
        return Map.of("success", true, "isActive", isActive);
    }

    // ── Manual Subscription Plan Override ────────────────────────────────────

    @Transactional
    public Map<String, Object> changePlan(UUID tenantId, String planCode, String status) {
        Plan plan = planRepo.findByCode(planCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found: " + planCode));

        String sql = "UPDATE subscriptions SET plan_id = ?, status = ?, updated_at = NOW() WHERE tenant_id = ?";
        jdbcTemplate.update(sql, plan.getId(), status, tenantId);

        return Map.of("success", true);
    }

    // ── Subscription Overview ────────────────────────────────────────────────

    public Map<String, Object> getSubscriptions(int page, int limit, String status, String search) {
        int offset = (page - 1) * limit;
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        params.add(limit);
        params.add(offset);

        if (status != null && !status.isBlank()) {
            params.add(status);
            conditions.add("s.status = ?" + params.size());
        }
        if (search != null && !search.isBlank()) {
            params.add("%" + search + "%");
            conditions.add("(t.name ILIKE ?" + params.size() + " OR t.email ILIKE ?" + params.size() + ")");
        }

        String whereClause = conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions);

        String sql = String.format("""
            SELECT
              t.id AS tenant_id, t.name, t.email,
              s.status      AS sub_status,
              s.trial_ends_at,
              s.current_period_start,
              s.current_period_end,
              p.name        AS plan_name,
              p.price_monthly AS mrr
            FROM subscriptions s
            JOIN tenants t ON t.id = s.tenant_id
            LEFT JOIN plans p ON p.id = s.plan_id
            %s
            ORDER BY
              CASE s.status
                WHEN 'active'   THEN 1
                WHEN 'trial'    THEN 2
                WHEN 'past_due' THEN 3
                ELSE 4
              END,
              t.name
            LIMIT ? OFFSET ?
        """, whereClause);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params.toArray());

        String sqlCount = String.format("""
            SELECT COUNT(*)::int FROM subscriptions s
            JOIN tenants t ON t.id = s.tenant_id
            %s
        """, whereClause);
        List<Object> countParams = params.subList(2, params.size());
        Integer total = jdbcTemplate.queryForObject(sqlCount, Integer.class, countParams.toArray());

        return Map.of("data", rows, "total", total != null ? total : 0, "page", page, "limit", limit);
    }

    // ── Create Tenant ────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> createTenant(Map<String, Object> dto) {
        String email = (String) dto.get("email");
        String name = (String) dto.get("name");
        String phone = (String) dto.get("phone");
        String planCode = dto.containsKey("planCode") ? (String) dto.get("planCode") : "starter";

        String checkSql = "SELECT COUNT(*)::int FROM tenants WHERE email = ?";
        Integer existingCount = jdbcTemplate.queryForObject(checkSql, Integer.class, email);
        if (existingCount != null && existingCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A business with this email already exists");
        }

        UUID tenantId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();

        String slug = name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "") + "-" + UUID.randomUUID().toString().substring(0, 4);

        // 1. Insert Tenant
        String sqlTenant = "INSERT INTO tenants (id, name, slug, email, phone, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, true, NOW(), NOW())";
        jdbcTemplate.update(sqlTenant, tenantId, name, slug, email, phone);

        // 2. Insert HQ Branch
        String sqlBranch = "INSERT INTO branches (id, tenant_id, name, code, is_hq, is_active, created_at, updated_at) VALUES (?, ?, ?, 'HQ', true, true, NOW(), NOW())";
        jdbcTemplate.update(sqlBranch, branchId, tenantId, name);

        // 3. Attach Subscription Plan
        Plan plan = planRepo.findByCode(planCode).orElse(null);
        if (plan != null) {
            String sqlSub = "INSERT INTO subscriptions (id, tenant_id, plan_id, status, trial_ends_at, created_at, updated_at) VALUES (?, ?, ?, 'trial', NOW() + INTERVAL '14 days', NOW(), NOW())";
            jdbcTemplate.update(sqlSub, subscriptionId, tenantId, plan.getId());
        }

        // 4. Create Owner Account
        if (dto.containsKey("ownerName") && dto.containsKey("ownerPassword")) {
            String ownerName = (String) dto.get("ownerName");
            String ownerPassword = (String) dto.get("ownerPassword");
            String hash = passwordEncoder.encode(ownerPassword);

            String[] nameParts = ownerName.trim().split(" ", 2);
            String firstName = nameParts[0];
            String lastName = nameParts.length > 1 ? nameParts[1] : null;

            String sqlUser = "INSERT INTO users (id, tenant_id, branch_id, email, password_hash, first_name, last_name, role, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, 'owner', true, NOW(), NOW())";
            jdbcTemplate.update(sqlUser, UUID.randomUUID(), tenantId, branchId, email, hash, firstName, lastName);
        }

        return Map.of("id", tenantId, "name", name, "slug", slug, "email", email, "branchId", branchId);
    }

    // ── Delete Tenant (Massive Bottom-Up Cascade) ────────────────────────────

    @Transactional
    public void deleteTenant(UUID id) {
        String checkSql = "SELECT slug FROM tenants WHERE id = ?";
        String slug;
        
        try {
            slug = jdbcTemplate.queryForObject(checkSql, String.class, id);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found in database.");
        }

        if ("_system".equalsIgnoreCase(slug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete the system tenant");
        }

        try {
            // 1. Deeply nested dependencies that might not have tenant_id
            try { jdbcTemplate.update("DELETE FROM hotel_folio_charges WHERE reservation_id IN (SELECT id FROM hotel_reservations WHERE tenant_id = ?)", id); } catch (Exception ignored) {}
            try { jdbcTemplate.update("DELETE FROM order_items WHERE order_id IN (SELECT id FROM orders WHERE tenant_id = ?)", id); } catch (Exception ignored) {}
            try { jdbcTemplate.update("DELETE FROM payments WHERE order_id IN (SELECT id FROM orders WHERE tenant_id = ?)", id); } catch (Exception ignored) {}

            // 2. Child Transaction Records
            safeDelete(id, "order_items"); // Just in case it has tenant_id directly!
            safeDelete(id, "payments");
            safeDelete(id, "hotel_housekeeping_tasks");
            safeDelete(id, "hotel_reservations");
            safeDelete(id, "hotel_rooms");
            
            safeDelete(id, "orders");
            safeDelete(id, "bills");
            safeDelete(id, "shifts");

            // 3. Master Data
            safeDelete(id, "inventory_items");
            safeDelete(id, "menu_items");
            safeDelete(id, "categories");

            // 4. Security & Logs
            safeDelete(id, "audit_logs");
            safeDelete(id, "password_reset_tokens");
            
            // 5. Core Infrastructure
            safeDelete(id, "users");
            safeDelete(id, "branches");
            safeDelete(id, "subscriptions");

            // 6. Final Execution
            String deleteSql = "DELETE FROM tenants WHERE id = ?";
            jdbcTemplate.update(deleteSql, id);
            
        } catch (Exception e) {
            log.error("Failed to delete tenant {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Database prevented deletion. Attached records might still exist. Error: " + e.getMessage()
            );
        }
    }

    // 🟢 BULLETPROOF HELPER: Checks if the table AND the tenant_id column exist before executing!
    private void safeDelete(UUID tenantId, String tableName) {
        String check = "SELECT EXISTS (SELECT FROM information_schema.columns WHERE table_name = ? AND column_name = 'tenant_id')";
        Boolean exists = jdbcTemplate.queryForObject(check, Boolean.class, tableName);
        
        if (Boolean.TRUE.equals(exists)) {
            jdbcTemplate.update("DELETE FROM " + tableName + " WHERE tenant_id = ?", tenantId);
        }
    }

    // ── Recent Platform Activity Log ─────────────────────────────────────────

    public Map<String, Object> getRecentActivity(int limit) {
        String sql = """
            SELECT * FROM (
                SELECT 
                    'tenant_registered' AS event_type, 
                    id::text AS id, 
                    name AS tenant_name, 
                    email AS actor_email, 
                    'Joined the platform' AS detail, 
                    created_at
                FROM tenants
                WHERE slug != '_system'

                UNION ALL

                SELECT 
                    'order_created' AS event_type, 
                    o.id::text AS id, 
                    t.name AS tenant_name, 
                    NULL AS actor_email, 
                    'Order #' || o.order_number AS detail, 
                    o.created_at
                FROM orders o
                JOIN tenants t ON t.id = o.tenant_id
                WHERE t.slug != '_system' AND o.status = 'billed'
            ) AS combined_feed
            ORDER BY created_at DESC
            LIMIT ?
        """;

        return Map.of("data", jdbcTemplate.queryForList(sql, limit), "total", jdbcTemplate.queryForList(sql, limit).size());
    }

    // ── Chart Trends (Daily Orders & Signups) ────────────────────────────────

    public List<Map<String, Object>> getOrdersTrend() {
        String sql = """
            SELECT
              TO_CHAR(DATE_TRUNC('day', created_at AT TIME ZONE 'Asia/Kolkata'), 'DD Mon') AS date,
              COUNT(*)::int AS orders,
              COALESCE(SUM(grand_total), 0) AS revenue
            FROM orders
            WHERE status = 'billed'
              AND created_at AT TIME ZONE 'Asia/Kolkata' >= NOW() AT TIME ZONE 'Asia/Kolkata' - INTERVAL '30 days'
            GROUP BY DATE_TRUNC('day', created_at AT TIME ZONE 'Asia/Kolkata')
            ORDER BY DATE_TRUNC('day', created_at AT TIME ZONE 'Asia/Kolkata')
        """;
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getSignupsTrend() {
        String sql = """
            SELECT
              TO_CHAR(DATE_TRUNC('day', created_at AT TIME ZONE 'Asia/Kolkata'), 'DD Mon') AS date,
              COUNT(*)::int AS signups
            FROM tenants
            WHERE slug != '_system'
              AND created_at AT TIME ZONE 'Asia/Kolkata' >= NOW() AT TIME ZONE 'Asia/Kolkata' - INTERVAL '30 days'
            GROUP BY DATE_TRUNC('day', created_at AT TIME ZONE 'Asia/Kolkata')
            ORDER BY DATE_TRUNC('day', created_at AT TIME ZONE 'Asia/Kolkata')
        """;
        return jdbcTemplate.queryForList(sql);
    }

    // ── Plan management ────────────────────────────────────────────────────────

    public List<Plan> listPlans() {
        return planRepo.findAll();
    }

    @Transactional
    public Plan createPlan(Plan plan) {
        if (planRepo.findByCode(plan.getCode()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Plan code already exists: " + plan.getCode());
        }
        return planRepo.save(plan);
    }

    @Transactional
    public Plan updatePlan(UUID id, Map<String, Object> data) {
        Plan plan = planRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));

        if (data.containsKey("name")) plan.setName((String) data.get("name"));
        if (data.containsKey("description")) plan.setDescription((String) data.get("description"));
        if (data.containsKey("priceMonthly")) plan.setPriceMonthly(new BigDecimal(data.get("priceMonthly").toString()));
        if (data.containsKey("maxBranches")) plan.setMaxBranches(Integer.parseInt(data.get("maxBranches").toString()));
        if (data.containsKey("maxUsers")) plan.setMaxUsers(Integer.parseInt(data.get("maxUsers").toString()));
        if (data.containsKey("maxMenuItems")) plan.setMaxMenuItems(Integer.parseInt(data.get("maxMenuItems").toString()));
        if (data.containsKey("isActive")) plan.setIsActive(Boolean.parseBoolean(data.get("isActive").toString()));

        return planRepo.save(plan);
    }

    @Transactional
    public Map<String, Object> deletePlan(UUID id) {
        Plan plan = planRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));

        String sqlCheck = "SELECT COUNT(*)::int FROM subscriptions WHERE plan_id = ?";
        Integer count = jdbcTemplate.queryForObject(sqlCheck, Integer.class, id);

        if (count != null && count > 0) {
            plan.setIsActive(false);
            planRepo.save(plan);
            return Map.of("success", true, "message", "Plan deactivated because it has active subscriptions");
        }

        planRepo.delete(plan);
        return Map.of("success", true, "message", "Plan permanently deleted");
    }

    // ── Superadmin Platform Settings ─────────────────────────────────────────

    public Map<String, Object> getSettings() {
        String sql = "SELECT settings FROM tenants WHERE slug = '_system'";
        try {
            return jdbcTemplate.queryForMap(sql);
        } catch (Exception e) {
            return Map.of();
        }
    }

    @Transactional
    public Map<String, Object> updateSettings(Map<String, Object> settings) {
        String sqlSelect = "SELECT settings FROM tenants WHERE slug = '_system'";
        Map<String, Object> systemTenant = jdbcTemplate.queryForMap(sqlSelect);

        Map<String, Object> merged = new HashMap<>(systemTenant);
        merged.putAll(settings);

        String sqlUpdate = "UPDATE tenants SET settings = CAST(? AS jsonb), updated_at = NOW() WHERE slug = '_system'";
        jdbcTemplate.update(sqlUpdate, merged);

        return Map.of("success", true, "settings", merged);
    }
}