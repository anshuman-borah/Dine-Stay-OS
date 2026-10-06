// package project.EnterpriseSaas.demo.modules.branch.service;

// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.http.HttpStatus;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;
// import org.springframework.web.server.ResponseStatusException;
// import project.EnterpriseSaas.demo.common.enums.BranchType;
// import project.EnterpriseSaas.demo.modules.branch.dto.BranchDto;
// import project.EnterpriseSaas.demo.modules.branch.entity.Branch;
// import project.EnterpriseSaas.demo.modules.branch.repository.BranchRepository;
// import project.EnterpriseSaas.demo.modules.tenant.entity.Tenant;
// import project.EnterpriseSaas.demo.modules.tenant.repository.TenantRepository;

// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.UUID;

// @Service
// @RequiredArgsConstructor
// @Slf4j
// public class BranchService {

//     private final BranchRepository branchRepo;
//     private final TenantRepository tenantRepo;

//     public List<Branch> findAll(UUID tenantId) {
//         return branchRepo.findByTenant_IdAndIsActiveTrueOrderByNameAsc(tenantId);
//     }

//     public Branch findOne(UUID id, UUID tenantId) {
//         return branchRepo.findByIdAndTenant_Id(id, tenantId)
//                 .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
//     }

//     @Transactional
//     public Branch create(BranchDto dto, UUID tenantId) {
//         Tenant tenant = tenantRepo.findById(tenantId)
//                 .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

//         if (dto.getCode() != null && branchRepo.existsByTenant_IdAndCode(tenantId, dto.getCode())) {
//             throw new ResponseStatusException(HttpStatus.CONFLICT, "Branch code already in use");
//         }

//         Branch branch = Branch.builder()
//                 .tenant(tenant)
//                 .name(dto.getName())
//                 .code(dto.getCode() != null ? dto.getCode() : "BR-" + System.currentTimeMillis() % 10000)
//                 .type(dto.getType() != null ? dto.getType() : BranchType.restaurant)
//                 .gstin(dto.getGstin())
//                 .fssaiNo(dto.getFssaiNo())
//                 .addressLine1(dto.getAddressLine1())
//                 .city(dto.getCity())
//                 .state(dto.getState())
//                 .stateCode(dto.getStateCode())
//                 .pincode(dto.getPincode())
//                 .phone(dto.getPhone())
//                 .email(dto.getEmail())
//                 .timezone(dto.getTimezone() != null ? dto.getTimezone() : "Asia/Kolkata")
//                 .currency(dto.getCurrency() != null ? dto.getCurrency() : "INR")
//                 .isHq(Boolean.TRUE.equals(dto.getIsHq()))
//                 .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
//                 .settings(dto.getSettings() != null ? dto.getSettings() : Map.of())
//                 .build();

//         return branchRepo.save(branch);
//     }

//     @Transactional
//     public Branch update(UUID id, UUID tenantId, BranchDto dto) {
//         Branch branch = findOne(id, tenantId);

//         if (dto.getName() != null) branch.setName(dto.getName());
//         if (dto.getCode() != null) branch.setCode(dto.getCode());
//         if (dto.getType() != null) branch.setType(dto.getType());
//         if (dto.getGstin() != null) branch.setGstin(dto.getGstin());
//         if (dto.getFssaiNo() != null) branch.setFssaiNo(dto.getFssaiNo());
//         if (dto.getAddressLine1() != null) branch.setAddressLine1(dto.getAddressLine1());
//         if (dto.getCity() != null) branch.setCity(dto.getCity());
//         if (dto.getState() != null) branch.setState(dto.getState());
//         if (dto.getStateCode() != null) branch.setStateCode(dto.getStateCode());
//         if (dto.getPincode() != null) branch.setPincode(dto.getPincode());
//         if (dto.getPhone() != null) branch.setPhone(dto.getPhone());
//         if (dto.getEmail() != null) branch.setEmail(dto.getEmail());
//         if (dto.getTimezone() != null) branch.setTimezone(dto.getTimezone());
//         if (dto.getCurrency() != null) branch.setCurrency(dto.getCurrency());
//         if (dto.getIsHq() != null) branch.setIsHq(dto.getIsHq());
//         if (dto.getIsActive() != null) branch.setIsActive(dto.getIsActive());

//         if (dto.getSettings() != null) {
//             Map<String, Object> current = new HashMap<>(branch.getSettings() != null ? branch.getSettings() : Map.of());
//             current.putAll(dto.getSettings());
//             branch.setSettings(current);
//         }

//         return branchRepo.save(branch);
//     }
// }
package project.EnterpriseSaas.demo.modules.branch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.EnterpriseSaas.demo.common.enums.BranchType;
import project.EnterpriseSaas.demo.modules.branch.dto.BranchDto;
import project.EnterpriseSaas.demo.modules.branch.entity.Branch;
import project.EnterpriseSaas.demo.modules.branch.repository.BranchRepository;
import project.EnterpriseSaas.demo.modules.tenant.entity.Tenant;
import project.EnterpriseSaas.demo.modules.tenant.repository.TenantRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BranchService {

    private final BranchRepository branchRepo;
    private final TenantRepository tenantRepo;

    public List<Branch> findAll(UUID tenantId) {
        return branchRepo.findByTenant_IdAndIsActiveTrueOrderByNameAsc(tenantId);
    }

    public Branch findOne(UUID id, UUID tenantId) {
        return branchRepo.findByIdAndTenant_Id(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    }

    @Transactional
    public Branch create(BranchDto dto, UUID tenantId) {
        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

        if (dto.getCode() != null && branchRepo.existsByTenant_IdAndCode(tenantId, dto.getCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Branch code already in use");
        }

        Branch branch = Branch.builder()
                .tenant(tenant)
                .name(dto.getName())
                .code(dto.getCode() != null ? dto.getCode() : "BR-" + System.currentTimeMillis() % 10000)
                .type(dto.getType() != null ? dto.getType() : BranchType.restaurant)
                .gstin(dto.getGstin())
                .fssaiNo(dto.getFssaiNo())
                .addressLine1(dto.getAddressLine1())
                .city(dto.getCity())
                .state(dto.getState())
                .stateCode(dto.getStateCode())
                .pincode(dto.getPincode())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .timezone(dto.getTimezone() != null ? dto.getTimezone() : "Asia/Kolkata")
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "INR")
                .isHq(Boolean.TRUE.equals(dto.getIsHq()))
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .settings(dto.getSettings() != null ? dto.getSettings() : Map.of())
                .build();

        return branchRepo.save(branch);
    }

    @Transactional
    public Branch update(UUID id, UUID tenantId, BranchDto dto) {
        Branch branch = findOne(id, tenantId);

        if (dto.getName() != null) branch.setName(dto.getName());
        if (dto.getCode() != null) branch.setCode(dto.getCode());
        if (dto.getType() != null) branch.setType(dto.getType());
        if (dto.getGstin() != null) branch.setGstin(dto.getGstin());
        if (dto.getFssaiNo() != null) branch.setFssaiNo(dto.getFssaiNo());
        if (dto.getAddressLine1() != null) branch.setAddressLine1(dto.getAddressLine1());
        if (dto.getCity() != null) branch.setCity(dto.getCity());
        if (dto.getState() != null) branch.setState(dto.getState());
        if (dto.getStateCode() != null) branch.setStateCode(dto.getStateCode());
        if (dto.getPincode() != null) branch.setPincode(dto.getPincode());
        if (dto.getPhone() != null) branch.setPhone(dto.getPhone());
        if (dto.getEmail() != null) branch.setEmail(dto.getEmail());
        if (dto.getTimezone() != null) branch.setTimezone(dto.getTimezone());
        if (dto.getCurrency() != null) branch.setCurrency(dto.getCurrency());
        if (dto.getIsHq() != null) branch.setIsHq(dto.getIsHq());
        if (dto.getIsActive() != null) branch.setIsActive(dto.getIsActive());

        if (dto.getSettings() != null) {
            Map<String, Object> current = new HashMap<>(branch.getSettings() != null ? branch.getSettings() : Map.of());
            current.putAll(dto.getSettings());
            branch.setSettings(current);
        }

        return branchRepo.save(branch);
    }

    // ── Safe Soft Delete ─────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> delete(UUID id, UUID tenantId) {
        Branch branch = findOne(id, tenantId);
        
        if (branch.getIsHq()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete the Headquarters branch.");
        }

        //  Preserve historical financial data by keeping the branch in the DB
        branch.setIsActive(false);
        branchRepo.save(branch);

        log.info("Soft deleted branch {} for tenant {}", id, tenantId);
        return Map.of("success", true, "message", "Branch deactivated successfully");
    }
}