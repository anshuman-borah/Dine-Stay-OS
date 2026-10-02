package project.EnterpriseSaas.demo.modules.kds.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.EnterpriseSaas.demo.common.enums.KdsStatus;
import project.EnterpriseSaas.demo.common.enums.OrderStatus;
import project.EnterpriseSaas.demo.common.enums.OrderType;
import project.EnterpriseSaas.demo.modules.kds.dto.KdsPendingItemDto;
import project.EnterpriseSaas.demo.modules.order.entity.Order;
import project.EnterpriseSaas.demo.modules.order.entity.OrderItem;
import project.EnterpriseSaas.demo.modules.order.event.OrderEvents;
import project.EnterpriseSaas.demo.modules.order.repository.OrderItemRepository;
import project.EnterpriseSaas.demo.modules.order.repository.OrderRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KdsService {

    private final OrderItemRepository itemRepo;
    private final OrderRepository orderRepo;
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher eventPublisher;

    private static final String KDS_BUMPED_MARKER = "__KDS_BUMPED__";

    // ── Get Pending Items for KDS ────────────────────────────────────────────

    public List<KdsPendingItemDto> getPendingItems(UUID branchId, UUID tenantId) {
        String sql = """
            SELECT
              oi.id                    AS order_item_id,
              oi.name                  AS item_name,
              oi.quantity,
              oi.notes,
              oi.kds_status,
              oi.created_at,
              oi.kds_ready_at,
              oi.menu_item_id,
              o.id                     AS order_id,
              o.order_number           AS order_order_number,
              o.order_type,
              o.status                 AS order_status,
              t.table_number           AS table_name,
              c.name                   AS category_name,
              EXTRACT(EPOCH FROM (NOW() - oi.created_at))::BIGINT AS age_seconds
            FROM order_items oi
            INNER JOIN orders o     ON o.id  = oi.order_id
            LEFT JOIN tables t      ON t.id  = o.table_id
            LEFT JOIN menu_items mi ON mi.id = oi.menu_item_id
            LEFT JOIN categories c  ON c.id  = mi.category_id
            WHERE o.branch_id = ?
              AND o.tenant_id = ?
              AND oi.kds_status IN ('pending', 'acknowledged', 'preparing', 'ready')
              AND oi.is_voided = false
              AND COALESCE(oi.void_reason, '') <> ?
              AND o.status NOT IN ('cancelled', 'billed', 'void', 'served')
            ORDER BY
              CASE oi.kds_status
                WHEN 'pending'      THEN 0
                WHEN 'acknowledged' THEN 1
                WHEN 'preparing'    THEN 2
                WHEN 'ready'        THEN 3
              END ASC,
              oi.created_at ASC
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> KdsPendingItemDto.builder()
                        .orderItemId(rs.getObject("order_item_id", UUID.class))
                        .itemName(rs.getString("item_name"))
                        .quantity(rs.getBigDecimal("quantity"))
                        .notes(rs.getString("notes"))
                        .kdsStatus(KdsStatus.valueOf(rs.getString("kds_status").toLowerCase()))
                        .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                        .kdsReadyAt(rs.getObject("kds_ready_at", OffsetDateTime.class))
                        .menuItemId(rs.getObject("menu_item_id", UUID.class))
                        .orderId(rs.getObject("order_id", UUID.class))
                        .orderOrderNumber(rs.getString("order_order_number"))
                        .orderType(OrderType.valueOf(rs.getString("order_type").toLowerCase()))
                        .orderStatus(OrderStatus.valueOf(rs.getString("order_status").toLowerCase()))
                        .tableName(rs.getString("table_name"))
                        .categoryName(rs.getString("category_name"))
                        .ageSeconds(rs.getLong("age_seconds"))
                        .build(),
                branchId, tenantId, KDS_BUMPED_MARKER
        );
    }

    // ── Update Item Status (Syncs Order Status) ──────────────────────────────

    @Transactional
    public OrderItem updateItemStatus(UUID itemId, KdsStatus status, UUID tenantId) {
        OrderItem item = itemRepo.findByIdAndTenantId(itemId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item not found"));

        item.setKdsStatus(status);
        item.setVoidReason(null);

        if (status == KdsStatus.acknowledged || status == KdsStatus.preparing) {
            if (item.getKdsAcknowledgedAt() == null) {
                item.setKdsAcknowledgedAt(OffsetDateTime.now());
            }
        }

        if (status == KdsStatus.ready) {
            item.setKdsReadyAt(OffsetDateTime.now());
        }

        OrderItem saved = itemRepo.save(item);

        // Sync parent order status so Waiter dashboard sees it
        syncParentOrderStatus(item.getOrder().getId());

        // Broadcast to all POS and KDS screens
        UUID branchId = item.getOrder().getBranch() != null ? item.getOrder().getBranch().getId() : null;
        if (branchId != null) {
            eventPublisher.publishEvent(OrderEvents.KdsItemStatusChangedEvent.builder()
                    .itemId(itemId)
                    .orderId(item.getOrder().getId())
                    .branchId(branchId)
                    .status(status)
                    .build());
        }

        log.info("KDS Item {} status updated to {}", itemId, status);
        return saved;
    }

    // ── Bump Single Item (Marks Ready for Waiter) ────────────────────────────

    @Transactional
    public OrderItem bumpItem(UUID itemId, UUID tenantId) {
        OrderItem item = itemRepo.findByIdAndTenantId(itemId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item not found"));

        item.setKdsStatus(KdsStatus.ready);
        if (item.getKdsReadyAt() == null) {
            item.setKdsReadyAt(OffsetDateTime.now());
        }
        item.setVoidReason(KDS_BUMPED_MARKER);

        OrderItem saved = itemRepo.save(item);
        syncParentOrderStatus(item.getOrder().getId());

        // Broadcast to all screens
        UUID branchId = item.getOrder().getBranch() != null ? item.getOrder().getBranch().getId() : null;
        if (branchId != null) {
            eventPublisher.publishEvent(OrderEvents.KdsItemStatusChangedEvent.builder()
                    .itemId(itemId)
                    .orderId(item.getOrder().getId())
                    .branchId(branchId)
                    .status(KdsStatus.ready)
                    .build());
        }

        return saved;
    }

    // ── Bump Entire Order (Waiter Picked Up & Served) ────────────────────────

    @Transactional
    public Map<String, Object> bumpOrderItems(UUID orderId, UUID tenantId) {
        List<OrderItem> items = itemRepo.findByOrderId(orderId);
        int bumpedCount = 0;

        for (OrderItem item : items) {
            if (Boolean.TRUE.equals(item.getIsVoided())) continue;
            item.setKdsStatus(KdsStatus.completed);
            item.setVoidReason(KDS_BUMPED_MARKER);
            bumpedCount++;
        }

        if (bumpedCount > 0) {
            itemRepo.saveAll(items);
        }

        Order order = orderRepo.findById(orderId).orElse(null);
        if (order != null && order.getStatus() != OrderStatus.billed) {
            order.setStatus(OrderStatus.served);
            order.setServedAt(OffsetDateTime.now());
            orderRepo.save(order);
            log.info("Order {} marked as SERVED", order.getOrderNumber());

            // Broadcast to all screens
            UUID branchId = order.getBranch() != null ? order.getBranch().getId() : null;
            if (branchId != null) {
                eventPublisher.publishEvent(OrderEvents.OrderStatusChangedEvent.builder()
                        .orderId(orderId)
                        .branchId(branchId)
                        .status(OrderStatus.served)
                        .build());
            }
        }

        return Map.of("bumped", bumpedCount);
    }

    // ── Recall Item ──────────────────────────────────────────────────────────

    @Transactional
    public OrderItem recallItem(UUID itemId, UUID tenantId) {
        OrderItem item = itemRepo.findByIdAndTenantId(itemId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item not found"));

        item.setKdsStatus(KdsStatus.preparing);
        item.setVoidReason(null);

        OrderItem saved = itemRepo.save(item);
        syncParentOrderStatus(item.getOrder().getId());

        UUID branchId = item.getOrder().getBranch() != null ? item.getOrder().getBranch().getId() : null;
        if (branchId != null) {
            eventPublisher.publishEvent(OrderEvents.KdsItemStatusChangedEvent.builder()
                    .itemId(itemId)
                    .orderId(item.getOrder().getId())
                    .branchId(branchId)
                    .status(KdsStatus.preparing)
                    .build());
        }

        return saved;
    }

    // ── Helper: Evaluate Overall Order Status ────────────────────────────────

    private void syncParentOrderStatus(UUID orderId) {
        Order order = orderRepo.findById(orderId).orElse(null);
        if (order == null || List.of(OrderStatus.billed, OrderStatus.cancelled).contains(order.getStatus())) {
            return;
        }

        List<OrderItem> activeItems = itemRepo.findByOrderId(orderId).stream()
                .filter(i -> !Boolean.TRUE.equals(i.getIsVoided()))
                .toList();

        if (activeItems.isEmpty()) return;

        boolean allCompleted = activeItems.stream().allMatch(i -> i.getKdsStatus() == KdsStatus.completed);
        boolean allReadyOrDone = activeItems.stream().allMatch(i ->
                i.getKdsStatus() == KdsStatus.ready ||
                        i.getKdsStatus() == KdsStatus.completed ||
                        KDS_BUMPED_MARKER.equals(i.getVoidReason())
        );
        boolean anyPreparing = activeItems.stream().anyMatch(i -> i.getKdsStatus() == KdsStatus.preparing || i.getKdsStatus() == KdsStatus.acknowledged);

        OrderStatus newStatus = order.getStatus();

        if (allCompleted) {
            newStatus = OrderStatus.served;
        } else if (allReadyOrDone) {
            newStatus = OrderStatus.ready;
        } else if (anyPreparing) {
            newStatus = OrderStatus.preparing;
        }

        if (newStatus != order.getStatus()) {
            order.setStatus(newStatus);
            if (newStatus == OrderStatus.served) order.setServedAt(OffsetDateTime.now());
            orderRepo.save(order);
            log.info("Order {} status synced to: {}", order.getOrderNumber(), newStatus);

            UUID branchId = order.getBranch() != null ? order.getBranch().getId() : null;
            if (branchId != null) {
                eventPublisher.publishEvent(OrderEvents.OrderStatusChangedEvent.builder()
                        .orderId(orderId)
                        .branchId(branchId)
                        .status(newStatus)
                        .build());
            }
        }
    }
}