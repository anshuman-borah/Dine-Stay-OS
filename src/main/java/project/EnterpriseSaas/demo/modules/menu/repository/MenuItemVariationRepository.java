package project.EnterpriseSaas.demo.modules.menu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project.EnterpriseSaas.demo.modules.menu.entity.MenuItemVariation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MenuItemVariationRepository extends JpaRepository<MenuItemVariation, UUID> {

    @Query("SELECT v FROM MenuItemVariation v WHERE v.menuItem.id = :menuItemId " +
            "AND v.tenant.id = :tenantId AND v.isActive = true " +
            "ORDER BY v.sortOrder ASC, v.name ASC")
    List<MenuItemVariation> findByMenuItemIdAndTenantId(
            @Param("menuItemId") UUID menuItemId,
            @Param("tenantId") UUID tenantId
    );

    @Query("SELECT v FROM MenuItemVariation v WHERE v.id = :id AND v.tenant.id = :tenantId")
    Optional<MenuItemVariation> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
}