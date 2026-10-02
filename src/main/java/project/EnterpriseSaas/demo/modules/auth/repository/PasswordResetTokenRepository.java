package project.EnterpriseSaas.demo.modules.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.EnterpriseSaas.demo.modules.auth.entity.PasswordResetToken;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    void deleteByUserId(UUID userId);

    Optional<PasswordResetToken> findFirstByUserIdAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            UUID userId,
            LocalDateTime now
    );
}