package project.EnterpriseSaas.demo.modules.table.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.EnterpriseSaas.demo.common.enums.TableStatus;
import project.EnterpriseSaas.demo.modules.branch.entity.Branch;
import project.EnterpriseSaas.demo.modules.branch.repository.BranchRepository;
import project.EnterpriseSaas.demo.modules.table.dto.CreateSectionDto;
import project.EnterpriseSaas.demo.modules.table.dto.CreateTableDto;
import project.EnterpriseSaas.demo.modules.table.dto.UpdateTableDto;
import project.EnterpriseSaas.demo.modules.table.entity.Table;
import project.EnterpriseSaas.demo.modules.table.entity.TableSection;
import project.EnterpriseSaas.demo.modules.table.repository.TableRepository;
import project.EnterpriseSaas.demo.modules.table.repository.TableSectionRepository;
import project.EnterpriseSaas.demo.modules.tenant.entity.Tenant;
import project.EnterpriseSaas.demo.modules.tenant.repository.TenantRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TableService {

    private final TableRepository tableRepo;
    private final TableSectionRepository sectionRepo;
    private final TenantRepository tenantRepo;
    private final BranchRepository branchRepo;

    // ── Tables ───────────────────────────────────────────────────────────────

    public List<Table> findAll(UUID branchId, UUID tenantId) {
        return tableRepo.findAllByBranchAndTenant(branchId, tenantId);
    }

    public Table findOne(UUID id, UUID tenantId) {
        return tableRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    }

    @Transactional
    public Table create(CreateTableDto dto, UUID tenantId, UUID branchId) {
        String tableNum = dto.getEffectiveTableNumber();
        if (tableNum == null || tableNum.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Table name or number is required");
        }

        String cleanedNum = tableNum.trim();
        if (tableRepo.existsByBranch_IdAndTableNumberIgnoreCaseAndIsActiveTrue(branchId, cleanedNum)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Table '" + cleanedNum + "' already exists in this branch");
        }

        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

        Branch branch = branchRepo.findById(branchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));

        TableSection section = dto.getSectionId() != null
                ? sectionRepo.findById(dto.getSectionId()).orElse(null)
                : null;

        TableStatus tableStatus = TableStatus.available;
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            try {
                tableStatus = TableStatus.valueOf(dto.getStatus().toLowerCase());
            } catch (IllegalArgumentException ignored) {}
        }

        Table table = Table.builder()
                .tenant(tenant)
                .branch(branch)
                .section(section)
                .tableNumber(cleanedNum)
                .status(tableStatus)
                .capacity(dto.getCapacity() != null ? dto.getCapacity() : 4)
                .qrCode(dto.getQrCode())
                .posX(dto.getPosX() != null ? dto.getPosX().intValue() : null)
                .posY(dto.getPosY() != null ? dto.getPosY().intValue() : null)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        return tableRepo.save(table);
    }

    @Transactional
    public Table update(UUID id, UUID tenantId, UpdateTableDto dto) {
        Table table = findOne(id, tenantId);

        String tableNum = dto.getEffectiveTableNumber();
        if (tableNum != null && !tableNum.isBlank()) {
            String cleanedNum = tableNum.trim();
            if (!cleanedNum.equalsIgnoreCase(table.getTableNumber()) &&
                    tableRepo.existsByBranch_IdAndTableNumberIgnoreCaseAndIsActiveTrue(table.getBranch().getId(), cleanedNum)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Table '" + cleanedNum + "' already exists in this branch");
            }
            table.setTableNumber(cleanedNum);
        }

        if (dto.getCapacity() != null) table.setCapacity(dto.getCapacity());
        if (dto.getIsActive() != null) table.setIsActive(dto.getIsActive());
        if (dto.getQrCode() != null) table.setQrCode(dto.getQrCode());
        if (dto.getPosX() != null) table.setPosX(dto.getPosX().intValue());
        if (dto.getPosY() != null) table.setPosY(dto.getPosY().intValue());

        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            try {
                TableStatus newStatus = TableStatus.valueOf(dto.getStatus().toLowerCase());
                table.setStatus(newStatus);
                log.info("Updated table {} status to {}", table.getTableNumber(), newStatus);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid table status string received: {}", dto.getStatus());
            }
        }

        if (dto.getSectionId() != null) {
            TableSection section = sectionRepo.findById(dto.getSectionId()).orElse(null);
            table.setSection(section);
        }

        return tableRepo.save(table);
    }

    @Transactional
    public Map<String, Boolean> remove(UUID id) {
        Table table = tableRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
        table.setIsActive(false);
        tableRepo.save(table);
        return Map.of("success", true);
    }

    // ── Sections ─────────────────────────────────────────────────────────────

    public List<TableSection> findAllSections(UUID branchId, UUID tenantId) {
        return sectionRepo.findAllByBranchAndTenant(branchId, tenantId);
    }

    public TableSection findOneSection(UUID id, UUID tenantId) {
        return sectionRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));
    }

    @Transactional
    public TableSection createSection(CreateSectionDto dto, UUID tenantId, UUID branchId) {
        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

        Branch branch = branchRepo.findById(branchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));

        TableSection section = TableSection.builder()
                .tenant(tenant)
                .branch(branch)
                .name(dto.getName())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        return sectionRepo.save(section);
    }

    @Transactional
    public TableSection updateSection(UUID id, UUID tenantId, CreateSectionDto dto) {
        TableSection section = findOneSection(id, tenantId);

        if (dto.getName() != null) section.setName(dto.getName());
        if (dto.getSortOrder() != null) section.setSortOrder(dto.getSortOrder());
        if (dto.getIsActive() != null) section.setIsActive(dto.getIsActive());

        return sectionRepo.save(section);
    }

    @Transactional
    public Map<String, Boolean> removeSection(UUID id, UUID tenantId) {
        long tableCount = tableRepo.countBySectionIdAndTenantId(id, tenantId);
        if (tableCount > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Move or reassign " + tableCount + " table(s) before deleting this section."
            );
        }

        TableSection section = findOneSection(id, tenantId);
        section.setIsActive(false);
        sectionRepo.save(section);
        return Map.of("success", true);
    }
}