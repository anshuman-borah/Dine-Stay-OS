package project.EnterpriseSaas.demo.modules.menu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.EnterpriseSaas.demo.modules.billing.entity.GstRate;
import project.EnterpriseSaas.demo.modules.billing.repository.GstRateRepository;
import project.EnterpriseSaas.demo.modules.branch.entity.Branch;
import project.EnterpriseSaas.demo.modules.branch.repository.BranchRepository;
import project.EnterpriseSaas.demo.modules.menu.dto.CreateCategoryDto;
import project.EnterpriseSaas.demo.modules.menu.dto.CreateMenuItemDto;
import project.EnterpriseSaas.demo.modules.menu.dto.UpdateCategoryDto;
import project.EnterpriseSaas.demo.modules.menu.dto.UpdateMenuItemDto;
import project.EnterpriseSaas.demo.modules.menu.entity.Category;
import project.EnterpriseSaas.demo.modules.menu.entity.MenuItem;
import project.EnterpriseSaas.demo.modules.menu.entity.MenuItemVariation;
import project.EnterpriseSaas.demo.modules.menu.repository.CategoryRepository;
import project.EnterpriseSaas.demo.modules.menu.repository.MenuItemRepository;
import project.EnterpriseSaas.demo.modules.menu.repository.MenuItemVariationRepository;
import project.EnterpriseSaas.demo.modules.tenant.entity.Tenant;
import project.EnterpriseSaas.demo.modules.tenant.repository.TenantRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuService {

    private final CategoryRepository catRepo;
    private final MenuItemRepository itemRepo;
    private final MenuItemVariationRepository variationRepo;
    private final GstRateRepository gstRepo;
    private final TenantRepository tenantRepo;
    private final BranchRepository branchRepo;

    // ── Categories ───────────────────────────────────────────────────────────

    @Cacheable(value = "categories")
    public List<Category> getCategories(UUID tenantId, UUID branchId) {
        return catRepo.findCategories(tenantId, branchId);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public Category createCategory(UUID tenantId, UUID branchId, CreateCategoryDto dto) {
        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

        Branch branch = branchId != null ? branchRepo.findById(branchId).orElse(null) : null;

        Category category = Category.builder()
                .tenant(tenant)
                .branch(branch)
                .name(dto.getName())
                .description(dto.getDescription())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .color(dto.getColor())
                .icon(dto.getIcon())
                .build();

        return catRepo.save(category);
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public Category updateCategory(UUID id, UUID tenantId, UpdateCategoryDto dto) {
        Category cat = catRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        if (dto.getName() != null) cat.setName(dto.getName());
        if (dto.getDescription() != null) cat.setDescription(dto.getDescription());
        if (dto.getSortOrder() != null) cat.setSortOrder(dto.getSortOrder());
        if (dto.getIsActive() != null) cat.setIsActive(dto.getIsActive());
        if (dto.getColor() != null) cat.setColor(dto.getColor());
        if (dto.getIcon() != null) cat.setIcon(dto.getIcon());

        return catRepo.save(cat);
    }

    @Transactional
    @CacheEvict(value = {"categories", "menuItems"}, allEntries = true)
    public Map<String, Boolean> removeCategory(UUID id, UUID tenantId) {
        Category cat = catRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        cat.setIsActive(false);
        catRepo.save(cat);
        return Map.of("success", true);
    }

    // ── Menu Items ───────────────────────────────────────────────────────────

    @Cacheable(value = "menuItems")
    public List<MenuItem> getItems(UUID tenantId, UUID branchId, UUID categoryId) {
        return itemRepo.findMenuItems(tenantId, branchId, categoryId);
    }

    public MenuItem getItem(UUID id, UUID tenantId) {
        return itemRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));
    }

    @Transactional
    @CacheEvict(value = "menuItems", allEntries = true)
    public MenuItem createItem(UUID tenantId, UUID branchId, CreateMenuItemDto dto) {
        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

        Branch branch = branchId != null ? branchRepo.findById(branchId).orElse(null) : null;
        Category category = dto.getCategoryId() != null ? catRepo.findById(dto.getCategoryId()).orElse(null) : null;

        MenuItem item = MenuItem.builder()
                .tenant(tenant)
                .branch(branch)
                .category(category)
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice() != null ? dto.getPrice() : BigDecimal.ZERO)
                .sku(dto.getSku())
                .barcode(dto.getBarcode())
                .shortCode(dto.getShortCode())
                .gstRateId(dto.getGstRateId())
                .isVeg(dto.getIsVeg() != null ? dto.getIsVeg() : true)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .imageUrl(dto.getImageUrl())
                .tags(dto.getTags() != null ? dto.getTags() : List.of())
                .build();

        return itemRepo.save(item);
    }

    @Transactional
    @CacheEvict(value = "menuItems", allEntries = true)
    public MenuItem updateItem(UUID id, UUID tenantId, UpdateMenuItemDto dto) {
        MenuItem item = getItem(id, tenantId);

        if (dto.getCategoryId() != null) {
            Category category = catRepo.findById(dto.getCategoryId()).orElse(null);
            item.setCategory(category);
        }
        if (dto.getName() != null) item.setName(dto.getName());
        if (dto.getDescription() != null) item.setDescription(dto.getDescription());
        if (dto.getPrice() != null) item.setPrice(dto.getPrice());
        if (dto.getSku() != null) item.setSku(dto.getSku());
        if (dto.getBarcode() != null) item.setBarcode(dto.getBarcode());
        if (dto.getShortCode() != null) item.setShortCode(dto.getShortCode());
        if (dto.getGstRateId() != null) item.setGstRateId(dto.getGstRateId());
        if (dto.getIsVeg() != null) item.setIsVeg(dto.getIsVeg());
        if (dto.getIsActive() != null) item.setIsActive(dto.getIsActive());
        if (dto.getSortOrder() != null) item.setSortOrder(dto.getSortOrder());
        if (dto.getImageUrl() != null) item.setImageUrl(dto.getImageUrl());
        if (dto.getTags() != null) item.setTags(dto.getTags());

        return itemRepo.save(item);
    }

    @Transactional
    @CacheEvict(value = "menuItems", allEntries = true)
    public Map<String, Boolean> removeItem(UUID id, UUID tenantId) {
        MenuItem item = getItem(id, tenantId);
        item.setIsActive(false);
        itemRepo.save(item);
        return Map.of("success", true);
    }

    // ── Variations ───────────────────────────────────────────────────────────

    public List<MenuItemVariation> getVariations(UUID menuItemId, UUID tenantId) {
        getItem(menuItemId, tenantId);
        return variationRepo.findByMenuItemIdAndTenantId(menuItemId, tenantId);
    }

    @Transactional
    @CacheEvict(value = "menuItems", allEntries = true)
    public MenuItemVariation createVariation(UUID menuItemId, UUID tenantId, MenuItemVariation variation) {
        MenuItem item = getItem(menuItemId, tenantId);
        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

        variation.setMenuItem(item);
        variation.setTenant(tenant);
        if (variation.getIsActive() == null) variation.setIsActive(true);

        return variationRepo.save(variation);
    }

    @Transactional
    @CacheEvict(value = "menuItems", allEntries = true)
    public MenuItemVariation updateVariation(UUID id, UUID tenantId, MenuItemVariation data) {
        MenuItemVariation v = variationRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Variation not found"));

        if (data.getName() != null) v.setName(data.getName());
        if (data.getPrice() != null) v.setPrice(data.getPrice());
        if (data.getCostPrice() != null) v.setCostPrice(data.getCostPrice());
        if (data.getSortOrder() != null) v.setSortOrder(data.getSortOrder());
        if (data.getIsActive() != null) v.setIsActive(data.getIsActive());

        return variationRepo.save(v);
    }

    @Transactional
    @CacheEvict(value = "menuItems", allEntries = true)
    public Map<String, Boolean> removeVariation(UUID id, UUID tenantId) {
        MenuItemVariation v = variationRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Variation not found"));
        v.setIsActive(false);
        variationRepo.save(v);
        return Map.of("success", true);
    }

    // ── GST Rates ────────────────────────────────────────────────────────────

    @Cacheable(value = "gstRates")
    public List<GstRate> getGstRates(UUID tenantId) {
        List<GstRate> rates = gstRepo.findByTenantIdAndIsActiveTrue(tenantId);
        if (rates.isEmpty()) {
            seedDefaultGstRates(tenantId);
            return gstRepo.findByTenantIdAndIsActiveTrue(tenantId);
        }
        return rates;
    }

    @Transactional
    @CacheEvict(value = "gstRates", allEntries = true)
    public GstRate createGstRate(UUID tenantId, GstRate data) {
        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

        BigDecimal rate = data.getRate() != null ? data.getRate() : BigDecimal.ZERO;
        BigDecimal halfRate = rate.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

        data.setTenant(tenant);
        data.setCgstRate(halfRate);
        data.setSgstRate(halfRate);
        data.setIgstRate(rate);
        if (data.getIsActive() == null) data.setIsActive(true);

        return gstRepo.save(data);
    }

    @Transactional
    @CacheEvict(value = "gstRates", allEntries = true)
    public GstRate updateGstRate(UUID id, UUID tenantId, GstRate data) {
        GstRate gst = gstRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "GST rate not found"));

        if (data.getName() != null) gst.setName(data.getName());
        if (data.getRate() != null) {
            BigDecimal rate = data.getRate();
            BigDecimal halfRate = rate.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            gst.setRate(rate);
            gst.setCgstRate(halfRate);
            gst.setSgstRate(halfRate);
            gst.setIgstRate(rate);
        }
        if (data.getHsnSacCode() != null) gst.setHsnSacCode(data.getHsnSacCode());
        if (data.getIsActive() != null) gst.setIsActive(data.getIsActive());

        return gstRepo.save(gst);
    }

    @Transactional
    @CacheEvict(value = "gstRates", allEntries = true)
    public Map<String, Boolean> removeGstRate(UUID id, UUID tenantId) {
        GstRate gst = gstRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "GST rate not found"));
        gst.setIsActive(false);
        gstRepo.save(gst);
        return Map.of("success", true);
    }

    @Transactional
    @CacheEvict(value = "gstRates", allEntries = true)
    public Map<String, Object> seedDefaultGstRates(UUID tenantId) {
        long count = gstRepo.countByTenantId(tenantId);
        if (count > 0) {
            return Map.of("message", "GST rates already seeded", "count", count);
        }

        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

        List<GstRate> defaultRates = List.of(
                GstRate.builder().tenant(tenant).name("Exempt").rate(BigDecimal.ZERO).cgstRate(BigDecimal.ZERO).sgstRate(BigDecimal.ZERO).igstRate(BigDecimal.ZERO).hsnSacCode("9963").isActive(true).build(),
                GstRate.builder().tenant(tenant).name("GST 5%").rate(BigDecimal.valueOf(5)).cgstRate(BigDecimal.valueOf(2.5)).sgstRate(BigDecimal.valueOf(2.5)).igstRate(BigDecimal.valueOf(5)).hsnSacCode("9963").isActive(true).build(),
                GstRate.builder().tenant(tenant).name("GST 12%").rate(BigDecimal.valueOf(12)).cgstRate(BigDecimal.valueOf(6)).sgstRate(BigDecimal.valueOf(6)).igstRate(BigDecimal.valueOf(12)).hsnSacCode("9963").isActive(true).build(),
                GstRate.builder().tenant(tenant).name("GST 18%").rate(BigDecimal.valueOf(18)).cgstRate(BigDecimal.valueOf(9)).sgstRate(BigDecimal.valueOf(9)).igstRate(BigDecimal.valueOf(18)).hsnSacCode("9963").isActive(true).build(),
                GstRate.builder().tenant(tenant).name("GST 28%").rate(BigDecimal.valueOf(28)).cgstRate(BigDecimal.valueOf(14)).sgstRate(BigDecimal.valueOf(14)).igstRate(BigDecimal.valueOf(28)).hsnSacCode("2203").isActive(true).build()
        );

        gstRepo.saveAll(defaultRates);
        return Map.of("message", "Default GST rates seeded", "count", defaultRates.size());
    }
}