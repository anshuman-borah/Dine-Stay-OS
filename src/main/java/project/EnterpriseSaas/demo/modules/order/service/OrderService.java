package project.EnterpriseSaas.demo.modules.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.EnterpriseSaas.demo.common.enums.OrderStatus;
import project.EnterpriseSaas.demo.common.enums.OrderType;
import project.EnterpriseSaas.demo.common.enums.TableStatus;
import project.EnterpriseSaas.demo.modules.billing.entity.GstRate;
import project.EnterpriseSaas.demo.modules.billing.repository.GstRateRepository;
import project.EnterpriseSaas.demo.modules.branch.entity.Branch;
import project.EnterpriseSaas.demo.modules.branch.repository.BranchRepository;
import project.EnterpriseSaas.demo.modules.menu.entity.MenuItem;
import project.EnterpriseSaas.demo.modules.menu.entity.MenuItemVariation;
import project.EnterpriseSaas.demo.modules.menu.repository.MenuItemRepository;
import project.EnterpriseSaas.demo.modules.menu.repository.MenuItemVariationRepository;
import project.EnterpriseSaas.demo.modules.order.dto.AddOrderItemDto;
import project.EnterpriseSaas.demo.modules.order.dto.ApplyDiscountDto;
import project.EnterpriseSaas.demo.modules.order.dto.CreateOrderDto;
import project.EnterpriseSaas.demo.modules.order.entity.Order;
import project.EnterpriseSaas.demo.modules.order.entity.OrderItem;
import project.EnterpriseSaas.demo.modules.order.event.OrderEvents;
import project.EnterpriseSaas.demo.modules.order.repository.OrderItemRepository;
import project.EnterpriseSaas.demo.modules.order.repository.OrderRepository;
import project.EnterpriseSaas.demo.modules.shift.entity.Shift;
import project.EnterpriseSaas.demo.modules.shift.repository.ShiftRepository;
import project.EnterpriseSaas.demo.modules.table.entity.Table;
import project.EnterpriseSaas.demo.modules.table.repository.TableRepository;
import project.EnterpriseSaas.demo.modules.tenant.entity.Tenant;
import project.EnterpriseSaas.demo.modules.tenant.repository.TenantRepository;
import project.EnterpriseSaas.demo.modules.user.entity.User;
import project.EnterpriseSaas.demo.modules.user.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final MenuItemRepository menuRepo;
    private final MenuItemVariationRepository variationRepo;
    private final GstRateRepository gstRepo;
    private final TableRepository tableRepo;
    private final TenantRepository tenantRepo;
    private final BranchRepository branchRepo;
    private final UserRepository userRepo;
    private final ShiftRepository shiftRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    // ── KAFKA CONSUMER ───────────────────────────────────────────────────────

    @KafkaListener(topics = "new-orders-topic", groupId = "dinestay-order-group")
    @Transactional
    public void consumeNewOrderEvent(String message) {
        try {
            log.info("📥 [KAFKA] Picked up new order creation task");

            JsonNode root = objectMapper.readTree(message);
            UUID tenantId = UUID.fromString(root.get("tenantId").asText());
            UUID branchId = UUID.fromString(root.get("branchId").asText());

            // Reconstruct the DTO
            CreateOrderDto dto = objectMapper.treeToValue(root.get("dto"), CreateOrderDto.class);

            // Execute the heavy database logic
            createOrderInternal(dto, tenantId, branchId);

            log.info("✅ [KAFKA] Successfully processed and saved new order");
        } catch (Exception e) {
            log.error("❌ [KAFKA] Failed to process order event: {}", e.getMessage(), e);
        }
    }

    // ── Create Order (Internal Logic) ────────────────────────────────────────

    private Order createOrderInternal(CreateOrderDto dto, UUID tenantId, UUID branchId) {
        if (dto.getOfflineId() != null) {
            Optional<Order> existing = orderRepo.findByOfflineIdAndTenantId(dto.getOfflineId(), tenantId);
            if (existing.isPresent()) {
                log.info("Idempotent offline order replay: returning existing order {}", existing.get().getId());
                return existing.get();
            }
        }

        Tenant tenant = tenantRepo.findById(tenantId).orElseThrow();
        Branch branch = branchRepo.findById(branchId).orElseThrow();

        Table table = dto.getTableId() != null ? tableRepo.findById(dto.getTableId()).orElse(null) : null;
        User waiter = dto.getWaiterId() != null ? userRepo.findById(dto.getWaiterId()).orElse(null) : null;
        Shift shift = dto.getShiftId() != null ? shiftRepo.findById(dto.getShiftId()).orElse(null) : null;

        String orderNumber = generateOrderNumber(branchId);

        Order order = Order.builder()
                .tenant(tenant)
                .branch(branch)
                .table(table)
                .shift(shift)
                .waiter(waiter)
                .orderNumber(orderNumber)
                .type(dto.getEffectiveOrderType() != null ? dto.getEffectiveOrderType() : OrderType.dine_in)
                .covers(dto.getEffectiveCovers() != null ? dto.getEffectiveCovers() : 1)
                .customerName(dto.getCustomerName())
                .customerPhone(dto.getCustomerPhone())
                .customerGstin(dto.getCustomerGstin())
                .customerAddress(dto.getCustomerAddress())
                .status(OrderStatus.placed)
                .placedAt(OffsetDateTime.now())
                .offlineId(dto.getOfflineId())
                .isComplimentary(Boolean.TRUE.equals(dto.getIsComplimentary()))
                .isSalesReturn(Boolean.TRUE.equals(dto.getIsSalesReturn()))
                .notes(dto.getNotes())
                .build();

        Order saved = orderRepo.save(order);

        if (table != null) {
            table.setStatus(TableStatus.occupied);
            tableRepo.save(table);
        }

        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            List<AddOrderItemDto> addItems = dto.getItems().stream().map(i -> AddOrderItemDto.builder()
                    .menuItemId(i.getMenuItemId())
                    .quantity(i.getQuantity())
                    .variationId(i.getVariationId())
                    .notes(i.getNotes())
                    .modifiers(i.getModifiers())
                    .build()
            ).toList();

            addItems(saved.getId(), addItems, tenantId);
        }

        Order result = findOne(saved.getId(), tenantId);

        // Broadcast to all POS and KDS screens in the branch
        eventPublisher.publishEvent(OrderEvents.OrderCreatedEvent.builder()
                .branchId(branchId)
                .order(result)
                .build());

        return result;
    }

    // ── Original Public Methods ──────────────────────────────────────────────

    // (We keep this one public just in case other internal services need to create an order synchronously)
    @Transactional
    public Order createOrder(CreateOrderDto dto, UUID tenantId, UUID branchId) {
        return createOrderInternal(dto, tenantId, branchId);
    }

    // ── Add Items (KOT) ──────────────────────────────────────────────────────

    @Transactional
    public Order addItems(UUID orderId, List<AddOrderItemDto> items, UUID tenantId) {
        Order order = findOne(orderId, tenantId);
        if (List.of(OrderStatus.billed, OrderStatus.cancelled).contains(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot modify a billed or cancelled order");
        }

        Tenant tenant = order.getTenant();
        List<OrderItem> orderItems = new ArrayList<>();

        for (AddOrderItemDto itemDto : items) {
            MenuItem menu = menuRepo.findById(itemDto.getMenuItemId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found: " + itemDto.getMenuItemId()));

            MenuItemVariation variation = itemDto.getVariationId() != null
                    ? variationRepo.findById(itemDto.getVariationId()).orElse(null)
                    : null;

            GstRate gst = menu.getGstRateId() != null ? gstRepo.findById(menu.getGstRateId()).orElse(null) : null;

            BigDecimal unitPrice = (variation != null && variation.getPrice() != null)
                    ? variation.getPrice()
                    : (menu.getPrice() != null ? menu.getPrice() : BigDecimal.ZERO);

            BigDecimal costPrice = (variation != null && variation.getCostPrice() != null)
                    ? variation.getCostPrice()
                    : (menu.getCostPrice() != null ? menu.getCostPrice() : BigDecimal.ZERO);

            BigDecimal qty = BigDecimal.valueOf(itemDto.getQuantity());
            BigDecimal lineSubtotal = unitPrice.multiply(qty);

            BigDecimal gstRateVal = gst != null ? gst.getRate() : BigDecimal.ZERO;
            BigDecimal halfRate = gstRateVal.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            BigDecimal cgstAmt = lineSubtotal.multiply(halfRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal sgstAmt = lineSubtotal.multiply(halfRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal totalTax = cgstAmt.add(sgstAmt);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .tenant(tenant)
                    .menuItem(menu)
                    .variation(variation)
                    .variationName(variation != null ? variation.getName() : null)
                    .name(menu.getName())
                    .sku(menu.getSku())
                    .quantity(qty)
                    .unitPrice(unitPrice)
                    .costPrice(costPrice)
                    .isVeg(menu.getIsVeg() != null ? menu.getIsVeg() : true)
                    .notes(itemDto.getNotes())
                    .gstRate(gstRateVal)
                    .cgstRate(halfRate)
                    .sgstRate(halfRate)
                    .igstRate(BigDecimal.ZERO)
                    .taxableAmount(lineSubtotal)
                    .cgstAmount(cgstAmt)
                    .sgstAmount(sgstAmt)
                    .igstAmount(BigDecimal.ZERO)
                    .cessAmount(BigDecimal.ZERO)
                    .lineTotal(lineSubtotal.add(totalTax))
                    .modifiers(itemDto.getModifiers() != null ? itemDto.getModifiers() : List.of())
                    .build();

            orderItems.add(orderItem);
        }

        orderItemRepo.saveAll(orderItems);
        Order result = recalculateTotals(orderId);

        // Broadcast new items to all KDS and POS screens
        eventPublisher.publishEvent(OrderEvents.OrderItemsAddedEvent.builder()
                .orderId(orderId)
                .branchId(result.getBranchId())
                .orderNumber(result.getOrderNumber())
                .items(orderItems)
                .build());

        return result;
    }

    // ── Update Order Status ──────────────────────────────────────────────────

    @Transactional
    public Order updateStatus(UUID orderId, OrderStatus status, UUID tenantId) {
        Order order = findOne(orderId, tenantId);
        order.setStatus(status);
        if (status == OrderStatus.placed) order.setPlacedAt(OffsetDateTime.now());
        if (status == OrderStatus.served) order.setServedAt(OffsetDateTime.now());
        if (status == OrderStatus.billed) order.setBilledAt(OffsetDateTime.now());
        Order saved = orderRepo.save(order);

        // Broadcast status change to all screens
        eventPublisher.publishEvent(OrderEvents.OrderStatusChangedEvent.builder()
                .orderId(orderId)
                .branchId(saved.getBranchId())
                .status(status)
                .build());

        return saved;
    }

    // ── Apply Discount ───────────────────────────────────────────────────────

    @Transactional
    public Order applyDiscount(UUID orderId, ApplyDiscountDto dto, UUID tenantId) {
        Order order = findOne(orderId, tenantId);
        if (dto.getDiscountPercent() != null) {
            order.setDiscountPercent(dto.getDiscountPercent());
        }
        orderRepo.save(order);
        return recalculateTotals(orderId);
    }

    // ── Void Item ────────────────────────────────────────────────────────────

    @Transactional
    public Order voidItem(UUID orderItemId, String reason, UUID tenantId) {
        OrderItem item = orderItemRepo.findByIdAndTenantId(orderItemId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item not found"));

        item.setIsVoided(true);
        item.setVoidReason(reason);
        orderItemRepo.save(item);

        return recalculateTotals(item.getOrder().getId());
    }

    // ── Find All Orders (Safe Status Parsing) ────────────────────────────────

    public List<Order> findAll(UUID branchId, UUID tenantId, String statusStr, int limit) {
        PageRequest pageRequest = PageRequest.of(0, Math.min(limit, 100));
        if (statusStr != null && !statusStr.isBlank()) {
            List<OrderStatus> statuses = Arrays.stream(statusStr.split(","))
                    .map(String::trim)
                    .map(s -> {
                        try {
                            return OrderStatus.valueOf(s.toLowerCase());
                        } catch (Exception e) {
                            log.debug("Ignoring unknown order status: {}", s);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();

            if (!statuses.isEmpty()) {
                return orderRepo.findByBranchAndTenantAndStatusIn(branchId, tenantId, statuses, pageRequest);
            }
        }
        return orderRepo.findByBranchAndTenant(branchId, tenantId, pageRequest);
    }

    public Order findOne(UUID id, UUID tenantId) {
        return orderRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    // ── Recalculate Totals ───────────────────────────────────────────────────

    @Transactional
    public Order recalculateTotals(UUID orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        List<OrderItem> items = orderItemRepo.findByOrderId(orderId);
        List<OrderItem> activeItems = items.stream().filter(i -> !Boolean.TRUE.equals(i.getIsVoided())).toList();

        BigDecimal subtotal = activeItems.stream()
                .map(i -> i.getUnitPrice().multiply(i.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountAmt = BigDecimal.ZERO;
        if (order.getDiscountPercent() != null && order.getDiscountPercent().compareTo(BigDecimal.ZERO) > 0) {
            discountAmt = subtotal.multiply(order.getDiscountPercent()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        BigDecimal discountRatio = subtotal.compareTo(BigDecimal.ZERO) > 0
                ? discountAmt.divide(subtotal, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal taxable = subtotal.subtract(discountAmt);
        BigDecimal scale = BigDecimal.ONE.subtract(discountRatio);

        BigDecimal cgst = activeItems.stream()
                .map(OrderItem::getCgstAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(scale);

        BigDecimal sgst = activeItems.stream()
                .map(OrderItem::getSgstAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(scale);

        BigDecimal totalTax = cgst.add(sgst);
        BigDecimal rawTotal = taxable.add(totalTax);
        BigDecimal rounded = rawTotal.setScale(0, RoundingMode.HALF_UP);
        BigDecimal roundOff = rounded.subtract(rawTotal);

        order.setSubtotal(subtotal);
        order.setDiscountAmount(discountAmt);
        order.setTaxableAmount(taxable);
        order.setCgstAmount(cgst);
        order.setSgstAmount(sgst);
        order.setIgstAmount(BigDecimal.ZERO);
        order.setCessAmount(BigDecimal.ZERO);
        order.setTotalTax(totalTax);
        order.setRoundOff(roundOff);
        order.setGrandTotal(rounded);

        return orderRepo.save(order);
    }

    private String generateOrderNumber(UUID branchId) {
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String prefix = "ORD-" + today + "-";
        long count = orderRepo.countByBranchIdAndOrderNumberPrefix(branchId, prefix);
        return prefix + String.format("%04d", count + 1);
    }
}