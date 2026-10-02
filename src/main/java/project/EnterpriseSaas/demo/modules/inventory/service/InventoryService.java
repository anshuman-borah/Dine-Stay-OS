//package project.EnterpriseSaas.demo.modules.inventory.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.server.ResponseStatusException;
//import project.EnterpriseSaas.demo.common.enums.TransactionType;
//import project.EnterpriseSaas.demo.modules.branch.entity.Branch;
//import project.EnterpriseSaas.demo.modules.branch.repository.BranchRepository;
//import project.EnterpriseSaas.demo.modules.inventory.dto.CreateInventoryItemDto;
//import project.EnterpriseSaas.demo.modules.inventory.dto.InventoryTransactionDto;
//import project.EnterpriseSaas.demo.modules.inventory.dto.UpdateInventoryItemDto;
//import project.EnterpriseSaas.demo.modules.inventory.entity.InventoryItem;
//import project.EnterpriseSaas.demo.modules.inventory.entity.InventoryTransaction;
//import project.EnterpriseSaas.demo.modules.inventory.repository.InventoryItemRepository;
//import project.EnterpriseSaas.demo.modules.inventory.repository.InventoryTransactionRepository;
//import project.EnterpriseSaas.demo.modules.tenant.entity.Tenant;
//import project.EnterpriseSaas.demo.modules.tenant.repository.TenantRepository;
//import project.EnterpriseSaas.demo.modules.user.entity.User;
//import project.EnterpriseSaas.demo.modules.user.repository.UserRepository;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.util.List;
//import java.util.UUID;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class InventoryService {
//
//    private final InventoryItemRepository itemRepo;
//    private final InventoryTransactionRepository txnRepo;
//    private final TenantRepository tenantRepo;
//    private final BranchRepository branchRepo;
//    private final UserRepository userRepo;
//
//    // ── Get Items ────────────────────────────────────────────────────────────
//
//    public List<InventoryItem> getItems(UUID tenantId, UUID branchId) {
//        return itemRepo.findItems(tenantId, branchId);
//    }
//
//    public InventoryItem getItem(UUID id, UUID tenantId) {
//        return itemRepo.findByIdAndTenantId(id, tenantId)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory item not found"));
//    }
//
//    // ── Create Item ──────────────────────────────────────────────────────────
//
//    @Transactional
//    public InventoryItem createItem(CreateInventoryItemDto dto, UUID tenantId, UUID branchId) {
//        Tenant tenant = tenantRepo.findById(tenantId)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
//
//        Branch branch = branchRepo.findById(branchId)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
//
//        BigDecimal costPrice = dto.getCostPrice() != null ? dto.getCostPrice() : BigDecimal.ZERO;
//
//        InventoryItem item = InventoryItem.builder()
//                .tenant(tenant)
//                .branch(branch)
//                .name(dto.getName())
//                .sku(dto.getSku())
//                .category(dto.getCategory())
//                .unit(dto.getUnit() != null ? dto.getUnit() : "piece")
//                .currentStock(dto.getCurrentStock() != null ? dto.getCurrentStock() : BigDecimal.ZERO)
//                .minStockLevel(dto.getMinStockLevel() != null ? dto.getMinStockLevel() : BigDecimal.ZERO)
//                .reorderLevel(dto.getReorderLevel() != null ? dto.getReorderLevel() : BigDecimal.ZERO)
//                .costPrice(costPrice)
//                .averageCost(costPrice)
//                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
//                .build();
//
//        return itemRepo.save(item);
//    }
//
//    // ── Update Item ──────────────────────────────────────────────────────────
//
//    @Transactional
//    public InventoryItem updateItem(UUID id, UUID tenantId, UpdateInventoryItemDto dto) {
//        InventoryItem item = getItem(id, tenantId);
//
//        if (dto.getName() != null) item.setName(dto.getName());
//        if (dto.getSku() != null) item.setSku(dto.getSku());
//        if (dto.getCategory() != null) item.setCategory(dto.getCategory());
//        if (dto.getUnit() != null) item.setUnit(dto.getUnit());
//        if (dto.getMinStockLevel() != null) item.setMinStockLevel(dto.getMinStockLevel());
//        if (dto.getReorderLevel() != null) item.setReorderLevel(dto.getReorderLevel());
//        if (dto.getCostPrice() != null) item.setCostPrice(dto.getCostPrice());
//        if (dto.getIsActive() != null) item.setIsActive(dto.getIsActive());
//
//        return itemRepo.save(item);
//    }
//
//    // ── Record Transaction ───────────────────────────────────────────────────
//
//    @Transactional
//    public InventoryTransaction recordTransaction(
//            UUID tenantId,
//            UUID branchId,
//            InventoryTransactionDto dto,
//            UUID userId
//    ) {
//        InventoryItem item = itemRepo.findByIdAndTenantIdForUpdate(dto.getItemId(), tenantId)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
//
//        BigDecimal currentStock = item.getCurrentStock();
//        BigDecimal qty = dto.getQuantity();
//        boolean isOutgoing = (dto.getType() == TransactionType.sale || dto.getType() == TransactionType.waste);
//
//        BigDecimal newStock = isOutgoing
//                ? currentStock.subtract(qty)
//                : currentStock.add(qty);
//
//        if (isOutgoing && newStock.compareTo(BigDecimal.ZERO) < 0) {
//            throw new ResponseStatusException(
//                    HttpStatus.BAD_REQUEST,
//                    "Insufficient stock. Available: " + currentStock
//            );
//        }
//
//        BigDecimal unitCost = dto.getUnitCost() != null ? dto.getUnitCost() : BigDecimal.ZERO;
//        BigDecimal newAvgCost = item.getAverageCost();
//        BigDecimal newCostPrice = item.getCostPrice();
//
//        // Moving Average Cost Formula on Purchase / Inbound Stock
//        if (dto.getType() == TransactionType.purchase && unitCost.compareTo(BigDecimal.ZERO) > 0) {
//            BigDecimal totalStock = currentStock.add(qty);
//            if (totalStock.compareTo(BigDecimal.ZERO) > 0) {
//                BigDecimal currentTotalValue = item.getAverageCost().multiply(currentStock);
//                BigDecimal incomingTotalValue = unitCost.multiply(qty);
//                newAvgCost = currentTotalValue.add(incomingTotalValue).divide(totalStock, 4, RoundingMode.HALF_UP);
//            }
//            newCostPrice = unitCost;
//        }
//
//        item.setCurrentStock(newStock);
//        item.setAverageCost(newAvgCost);
//        item.setCostPrice(newCostPrice);
//        itemRepo.save(item);
//
//        User user = userId != null ? userRepo.findById(userId).orElse(null) : null;
//
//        InventoryTransaction txn = InventoryTransaction.builder()
//                .tenant(item.getTenant())
//                .branch(item.getBranch())
//                .inventoryItem(item)
//                .type(dto.getType())
//                .quantity(qty)
//                .unitCost(unitCost)
//                .totalCost(qty.multiply(unitCost))
//                .balanceAfter(newStock)
//                .notes(dto.getNotes())
//                .createdBy(user)
//                .build();
//
//        return txnRepo.save(txn);
//    }
//
//    // ── Get Ledger (Transaction History) ─────────────────────────────────────
//
//    public List<InventoryTransaction> getLedger(UUID itemId, UUID tenantId, int limit) {
//        PageRequest pageRequest = PageRequest.of(0, Math.min(limit, 100));
//        return txnRepo.findLedger(itemId, tenantId, pageRequest);
//    }
//
//    // ── Get Low Stock Alerts ─────────────────────────────────────────────────
//
//    public List<InventoryItem> getLowStockAlerts(UUID tenantId, UUID branchId) {
//        return itemRepo.findLowStockItems(tenantId, branchId);
//    }
//}

package project.EnterpriseSaas.demo.modules.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.EnterpriseSaas.demo.common.enums.TransactionType;
import project.EnterpriseSaas.demo.modules.branch.entity.Branch;
import project.EnterpriseSaas.demo.modules.branch.repository.BranchRepository;
import project.EnterpriseSaas.demo.modules.inventory.dto.CreateInventoryItemDto;
import project.EnterpriseSaas.demo.modules.inventory.dto.InventoryTransactionDto;
import project.EnterpriseSaas.demo.modules.inventory.dto.UpdateInventoryItemDto;
import project.EnterpriseSaas.demo.modules.inventory.entity.InventoryItem;
import project.EnterpriseSaas.demo.modules.inventory.entity.InventoryTransaction;
import project.EnterpriseSaas.demo.modules.inventory.repository.InventoryItemRepository;
import project.EnterpriseSaas.demo.modules.inventory.repository.InventoryTransactionRepository;
import project.EnterpriseSaas.demo.modules.tenant.entity.Tenant;
import project.EnterpriseSaas.demo.modules.tenant.repository.TenantRepository;
import project.EnterpriseSaas.demo.modules.user.entity.User;
import project.EnterpriseSaas.demo.modules.user.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryItemRepository itemRepo;
    private final InventoryTransactionRepository txnRepo;
    private final TenantRepository tenantRepo;
    private final BranchRepository branchRepo;
    private final UserRepository userRepo;

    // ... (getItems, getItem, createItem, updateItem, recordTransaction, getLedger, getLowStockAlerts omitted for brevity)
    public List<InventoryItem> getItems(UUID tenantId, UUID branchId) {
        return itemRepo.findItems(tenantId, branchId);
    }

    public InventoryItem getItem(UUID id, UUID tenantId) {
        return itemRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory item not found"));
    }

    @Transactional
    public InventoryItem createItem(CreateInventoryItemDto dto, UUID tenantId, UUID branchId) {
        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

        Branch branch = branchRepo.findById(branchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));

        BigDecimal costPrice = dto.getCostPrice() != null ? dto.getCostPrice() : BigDecimal.ZERO;

        InventoryItem item = InventoryItem.builder()
                .tenant(tenant)
                .branch(branch)
                .name(dto.getName())
                .sku(dto.getSku())
                .category(dto.getCategory())
                .unit(dto.getUnit() != null ? dto.getUnit() : "piece")
                .currentStock(dto.getCurrentStock() != null ? dto.getCurrentStock() : BigDecimal.ZERO)
                .minStockLevel(dto.getMinStockLevel() != null ? dto.getMinStockLevel() : BigDecimal.ZERO)
                .reorderLevel(dto.getReorderLevel() != null ? dto.getReorderLevel() : BigDecimal.ZERO)
                .costPrice(costPrice)
                .averageCost(costPrice)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        return itemRepo.save(item);
    }

    @Transactional
    public InventoryItem updateItem(UUID id, UUID tenantId, UpdateInventoryItemDto dto) {
        InventoryItem item = getItem(id, tenantId);

        if (dto.getName() != null) item.setName(dto.getName());
        if (dto.getSku() != null) item.setSku(dto.getSku());
        if (dto.getCategory() != null) item.setCategory(dto.getCategory());
        if (dto.getUnit() != null) item.setUnit(dto.getUnit());
        if (dto.getMinStockLevel() != null) item.setMinStockLevel(dto.getMinStockLevel());
        if (dto.getReorderLevel() != null) item.setReorderLevel(dto.getReorderLevel());
        if (dto.getCostPrice() != null) item.setCostPrice(dto.getCostPrice());
        if (dto.getIsActive() != null) item.setIsActive(dto.getIsActive());

        return itemRepo.save(item);
    }

    @Transactional
    public InventoryTransaction recordTransaction(
            UUID tenantId,
            UUID branchId,
            InventoryTransactionDto dto,
            UUID userId
    ) {
        InventoryItem item = itemRepo.findByIdAndTenantIdForUpdate(dto.getItemId(), tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

        BigDecimal currentStock = item.getCurrentStock();
        BigDecimal qty = dto.getQuantity();
        boolean isOutgoing = (dto.getType() == TransactionType.sale || dto.getType() == TransactionType.waste);

        BigDecimal newStock = isOutgoing
                ? currentStock.subtract(qty)
                : currentStock.add(qty);

        if (isOutgoing && newStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Insufficient stock. Available: " + currentStock
            );
        }

        BigDecimal unitCost = dto.getUnitCost() != null ? dto.getUnitCost() : BigDecimal.ZERO;
        BigDecimal newAvgCost = item.getAverageCost();
        BigDecimal newCostPrice = item.getCostPrice();

        // Moving Average Cost Formula on Purchase / Inbound Stock
        if (dto.getType() == TransactionType.purchase && unitCost.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalStock = currentStock.add(qty);
            if (totalStock.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal currentTotalValue = item.getAverageCost().multiply(currentStock);
                BigDecimal incomingTotalValue = unitCost.multiply(qty);
                newAvgCost = currentTotalValue.add(incomingTotalValue).divide(totalStock, 4, RoundingMode.HALF_UP);
            }
            newCostPrice = unitCost;
        }

        item.setCurrentStock(newStock);
        item.setAverageCost(newAvgCost);
        item.setCostPrice(newCostPrice);
        itemRepo.save(item);

        User user = userId != null ? userRepo.findById(userId).orElse(null) : null;

        InventoryTransaction txn = InventoryTransaction.builder()
                .tenant(item.getTenant())
                .branch(item.getBranch())
                .inventoryItem(item)
                .type(dto.getType())
                .quantity(qty)
                .unitCost(unitCost)
                .totalCost(qty.multiply(unitCost))
                .balanceAfter(newStock)
                .notes(dto.getNotes())
                .createdBy(user)
                .build();

        return txnRepo.save(txn);
    }

    public List<InventoryTransaction> getLedger(UUID itemId, UUID tenantId, int limit) {
        PageRequest pageRequest = PageRequest.of(0, Math.min(limit, 100));
        return txnRepo.findLedger(itemId, tenantId, pageRequest);
    }

    public List<InventoryItem> getLowStockAlerts(UUID tenantId, UUID branchId) {
        return itemRepo.findLowStockItems(tenantId, branchId);
    }

    // 🟢 NEW: Deduct minibar inventory automatically during Kafka checkout
    @Transactional
    public void deductMinibarItems(UUID tenantId, UUID branchId, List<project.EnterpriseSaas.demo.modules.hotel.entity.FolioCharge> charges) {
        for (project.EnterpriseSaas.demo.modules.hotel.entity.FolioCharge charge : charges) {
            if (charge.getChargeType() == project.EnterpriseSaas.demo.common.enums.ChargeType.minibar) {
                // Find inventory item by exact name (assuming minibar descriptions match inventory names)
                List<InventoryItem> items = itemRepo.findItems(tenantId, branchId);
                for (InventoryItem item : items) {
                    if (item.getName().equalsIgnoreCase(charge.getDescription()) && item.getCurrentStock().compareTo(BigDecimal.ONE) >= 0) {

                        item.setCurrentStock(item.getCurrentStock().subtract(BigDecimal.ONE));
                        itemRepo.save(item);

                        InventoryTransaction txn = InventoryTransaction.builder()
                                .tenant(item.getTenant())
                                .branch(item.getBranch())
                                .inventoryItem(item)
                                .type(TransactionType.sale)
                                .quantity(BigDecimal.ONE)
                                .unitCost(item.getAverageCost())
                                .totalCost(item.getAverageCost())
                                .balanceAfter(item.getCurrentStock())
                                .notes("Auto-deducted during checkout for minibar consumption")
                                .build();
                        txnRepo.save(txn);

                        log.info("Deducted 1 unit of {} from inventory for minibar charge.", item.getName());
                        break;
                    }
                }
            }
        }
    }
}