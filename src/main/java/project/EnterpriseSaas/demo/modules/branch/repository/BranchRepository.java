package project.EnterpriseSaas.demo.modules.branch.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.EnterpriseSaas.demo.modules.branch.entity.Branch;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BranchRepository extends JpaRepository<Branch, UUID> {

    List<Branch> findByTenant_IdAndIsActiveTrueOrderByNameAsc(UUID tenantId);

    Optional<Branch> findByIdAndTenant_Id(UUID id, UUID tenantId);

    boolean existsByTenant_IdAndCode(UUID tenantId, String code);
}