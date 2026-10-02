package project.EnterpriseSaas.demo.modules.tenant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.EnterpriseSaas.demo.modules.tenant.entity.Tenant;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<Tenant> findBySlug(String slug);
}