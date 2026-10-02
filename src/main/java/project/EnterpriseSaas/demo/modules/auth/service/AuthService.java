package project.EnterpriseSaas.demo.modules.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.EnterpriseSaas.demo.common.enums.SubscriptionStatus;
import project.EnterpriseSaas.demo.common.enums.UserRole;
import project.EnterpriseSaas.demo.modules.auth.dto.AuthResponse;
import project.EnterpriseSaas.demo.modules.auth.dto.LoginDto;
import project.EnterpriseSaas.demo.modules.auth.dto.RegisterTenantDto;
import project.EnterpriseSaas.demo.modules.auth.entity.PasswordResetToken;
import project.EnterpriseSaas.demo.modules.auth.repository.PasswordResetTokenRepository;
import project.EnterpriseSaas.demo.modules.branch.entity.Branch;
import project.EnterpriseSaas.demo.modules.branch.repository.BranchRepository;
import project.EnterpriseSaas.demo.modules.plan.entity.Plan;
import project.EnterpriseSaas.demo.modules.plan.repository.PlanRepository;
import project.EnterpriseSaas.demo.modules.subscription.entity.Subscription;
import project.EnterpriseSaas.demo.modules.subscription.repository.SubscriptionRepository;
import project.EnterpriseSaas.demo.modules.tenant.entity.Tenant;
import project.EnterpriseSaas.demo.modules.tenant.repository.TenantRepository;
import project.EnterpriseSaas.demo.modules.user.entity.User;
import project.EnterpriseSaas.demo.modules.user.repository.UserRepository;
import project.EnterpriseSaas.demo.security.JwtService;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepo;
    private final TenantRepository tenantRepo;
    private final PlanRepository planRepo;
    private final SubscriptionRepository subRepo;
    private final BranchRepository branchRepo;
    private final PasswordResetTokenRepository prtRepo;
    private final JwtService jwtService;
    private final SessionService sessionService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.superadmin.email:superadmin@dinestay.app}")
    private String superadminEmail;

    @Value("${app.url:http://localhost:3000}")
    private String appUrl;

    // ── Register ─────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse register(RegisterTenantDto dto) {
        String email = dto.getEmail().trim().toLowerCase();

        if (tenantRepo.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        String planCode = dto.getPlanCode() != null ? dto.getPlanCode() : "starter";
        Plan plan = planRepo.findByCode(planCode).orElse(null);

        // CLEAN DISCORD-STYLE SLUG: e.g. "spice-garden-8291"
        String baseSlug = dto.getBusinessName().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        String slug = baseSlug + "-" + (1000 + new SecureRandom().nextInt(9000));

        Tenant tenant = Tenant.builder()
                .name(dto.getBusinessName())
                .slug(slug)
                .email(email)
                .phone(dto.getPhone())
                .settings(Map.of())
                .build();
        tenant = tenantRepo.save(tenant);

        Branch branch = Branch.builder()
                .tenant(tenant)
                .name(dto.getBusinessName())
                .code("HQ")
                .isHq(true)
                .settings(Map.of())
                .build();
        branch = branchRepo.save(branch);

        User user = User.builder()
                .tenant(tenant)
                .branch(branch)
                .email(email)
                .phone(dto.getPhone())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .firstName(dto.getBusinessName())
                .role(UserRole.owner)
                .isActive(true)
                .settings(Map.of())
                .permissions(Map.of())
                .build();
        user = userRepo.save(user);

        OffsetDateTime trialEnd = OffsetDateTime.now().plusDays(14);
        Subscription subscription = Subscription.builder()
                .tenant(tenant)
                .plan(plan)
                .status(SubscriptionStatus.trial)
                .trialEndsAt(trialEnd)
                .metadata(Map.of())
                .build();
        subRepo.save(subscription);

        String sessionId = sessionService.createSession(user.getId(), "pending", null, null);
        String accessToken = jwtService.generateAccessToken(
                user.getId().toString(),
                user.getEmail(),
                tenant.getId().toString(),
                branch.getId().toString(),
                user.getRole().name(),
                sessionId
        );
        String refreshToken = jwtService.generateRefreshToken(user.getId().toString(), user.getEmail());
        sessionService.rotateSession(user.getId(), sessionId, refreshToken);

        user.setRefreshToken(passwordEncoder.encode(refreshToken));
        userRepo.save(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(sanitizeUser(user))
                .build();
    }

    // ── Login ────────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginDto dto, String ip, String userAgent) {
        String email = dto.getEmail() != null ? dto.getEmail().trim() : null;
        String phone = dto.getPhone() != null ? dto.getPhone().trim() : null;

        log.info("Login request received for email: '{}', phone: '{}'", email, phone);

        // Superadmin Login Check
        if (email != null && email.equalsIgnoreCase(superadminEmail)) {
            Optional<User> saOpt = userRepo.findByEmailIgnoreCaseAndRoleAndIsActiveTrue(email, UserRole.superadmin);
            if (saOpt.isPresent()) {
                User saUser = saOpt.get();
                if (dto.getPassword() == null || !passwordEncoder.matches(dto.getPassword(), saUser.getPasswordHash())) {
                    log.warn("Superadmin password mismatch for: {}", email);
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
                }

                saUser.setLastLoginAt(OffsetDateTime.now());
                String sessionId = sessionService.createSession(saUser.getId(), "pending", ip, userAgent);
                String accessToken = jwtService.generateAccessToken(
                        saUser.getId().toString(),
                        saUser.getEmail(),
                        "superadmin",
                        null,
                        saUser.getRole().name(),
                        sessionId
                );
                String refreshToken = jwtService.generateRefreshToken(saUser.getId().toString(), saUser.getEmail());
                sessionService.rotateSession(saUser.getId(), sessionId, refreshToken);

                saUser.setRefreshToken(passwordEncoder.encode(refreshToken));
                userRepo.save(saUser);

                log.info("Superadmin logged in successfully: {}", email);
                return AuthResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .user(sanitizeUser(saUser))
                        .build();
            }
        }

        if (email == null && phone == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email or phone required");
        }

        // Resolve Tenant ID from Slug if provided
        UUID resolvedTenantId = dto.getTenantId();
        if (resolvedTenantId == null && dto.getTenantSlug() != null && !dto.getTenantSlug().isBlank()) {
            Tenant t = tenantRepo.findBySlug(dto.getTenantSlug().trim().toLowerCase())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Workspace Code"));
            resolvedTenantId = t.getId();
        }

        Optional<User> userOpt;
        if (resolvedTenantId != null) {
            userOpt = email != null
                    ? userRepo.findByEmailIgnoreCaseAndTenant_IdAndIsActiveTrue(email, resolvedTenantId)
                    : userRepo.findByPhoneAndTenant_IdAndIsActiveTrue(phone, resolvedTenantId);
        } else {
            userOpt = email != null
                    ? userRepo.findByEmailIgnoreCaseAndIsActiveTrue(email)
                    : userRepo.findByPhoneAndIsActiveTrue(phone);
        }

        User user = userOpt.orElseThrow(() -> {
            log.warn("User not found in DB for email: '{}' / phone: '{}'", email, phone);
            return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        });

        boolean valid = false;
        // 1. Try PIN login if a PIN is provided and the user has a PIN set
        if (dto.getPin() != null && !dto.getPin().isBlank()) {
            if (user.getPin() != null && !user.getPin().isBlank()) {
                valid = passwordEncoder.matches(dto.getPin(), user.getPin());
            }
        }
        // 2. Fallback to password login
        else if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            valid = passwordEncoder.matches(dto.getPassword(), user.getPasswordHash());
        }

        if (!valid) {
            log.warn("Password/PIN mismatch for user: {}", user.getEmail());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        user.setLastLoginAt(OffsetDateTime.now());
        UUID branchId = dto.getBranchId() != null
                ? dto.getBranchId()
                : (user.getBranch() != null ? user.getBranch().getId() : null);
        String jwtTenantId = user.getRole() == UserRole.superadmin
                ? "superadmin"
                : (user.getTenant() != null ? user.getTenant().getId().toString() : null);

        String sessionId = sessionService.createSession(user.getId(), "pending", ip, userAgent);
        String accessToken = jwtService.generateAccessToken(
                user.getId().toString(),
                user.getEmail(),
                jwtTenantId,
                branchId != null ? branchId.toString() : null,
                user.getRole().name(),
                sessionId
        );
        String refreshToken = jwtService.generateRefreshToken(user.getId().toString(), user.getEmail());
        sessionService.rotateSession(user.getId(), sessionId, refreshToken);

        user.setRefreshToken(passwordEncoder.encode(refreshToken));
        userRepo.save(user);

        log.info("User logged in successfully: {} (Role: {})", user.getEmail() != null ? user.getEmail() : user.getPhone(), user.getRole());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(sanitizeUser(user))
                .build();
    }

    // ── Refresh ──────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        try {
            String userIdStr = jwtService.extractUserId(refreshToken);
            UUID userId = UUID.fromString(userIdStr);
            String sessionId = jwtService.extractSessionId(refreshToken);
            String tenantId = jwtService.extractTenantId(refreshToken);
            String branchIdStr = jwtService.extractBranchId(refreshToken);

            if (sessionId != null) {
                boolean valid = sessionService.validateSession(userId, sessionId, refreshToken);
                if (!valid) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session expired or revoked");

                User user = userRepo.findById(userId)
                        .filter(User::getIsActive)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

                String newAccessToken = jwtService.generateAccessToken(
                        user.getId().toString(),
                        user.getEmail(),
                        tenantId,
                        branchIdStr,
                        user.getRole().name(),
                        sessionId
                );
                String newRefreshToken = jwtService.generateRefreshToken(user.getId().toString(), user.getEmail());
                sessionService.rotateSession(user.getId(), sessionId, newRefreshToken);

                user.setRefreshToken(passwordEncoder.encode(newRefreshToken));
                userRepo.save(user);

                return AuthResponse.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(newRefreshToken)
                        .build();
            }

            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
    }

    // ── Password Reset ───────────────────────────────────────────────────────

    @Transactional
    public void forgotPassword(String email, String ip) {
        Optional<User> userOpt = userRepo.findByEmailIgnoreCaseAndIsActiveTrue(email.trim());
        if (userOpt.isEmpty()) return;

        User user = userOpt.get();
        prtRepo.deleteByUserId(user.getId());

        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        String rawToken = HexFormat.of().formatHex(randomBytes);
        String tokenHash = passwordEncoder.encode(rawToken);

        PasswordResetToken prt = PasswordResetToken.builder()
                .userId(user.getId())
                .tenantId(user.getTenant() != null ? user.getTenant().getId() : null)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .ipAddress(ip)
                .build();
        prtRepo.save(prt);

        log.info("Password reset link generated for user {}: {}/reset-password?token={}&userId={}",
                user.getId(), appUrl, rawToken, user.getId());
    }

    @Transactional
    public void resetPassword(UUID userId, String rawToken, String newPassword) {
        User user = userRepo.findById(userId)
                .filter(User::getIsActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset link"));

        PasswordResetToken prt = prtRepo.findFirstByUserIdAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                userId, LocalDateTime.now()
        ).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset link has expired or already been used"));

        if (!passwordEncoder.matches(rawToken, prt.getTokenHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reset link");
        }

        prt.setUsedAt(LocalDateTime.now());
        prtRepo.save(prt);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setRefreshToken(null);
        userRepo.save(user);

        sessionService.revokeAllSessions(userId);
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepo.findById(userId)
                .filter(User::getIsActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect current password");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepo.save(user);
    }

    public Map<String, Object> sanitizeUser(User user) {
        Map<String, Object> safe = new HashMap<>();
        safe.put("id", user.getId());
        safe.put("tenantId", user.getTenant() != null ? user.getTenant().getId() : null);

        // --- ADDED THE TWO MISSING FIELDS HERE! ---
        safe.put("tenantName", user.getTenant() != null ? user.getTenant().getName() : null);
        safe.put("tenantSlug", user.getTenant() != null ? user.getTenant().getSlug() : null);

        safe.put("branchId", user.getBranch() != null ? user.getBranch().getId() : null);
        safe.put("email", user.getEmail());
        safe.put("phone", user.getPhone());
        safe.put("firstName", user.getFirstName());
        safe.put("lastName", user.getLastName());
        safe.put("role", user.getRole());
        safe.put("isActive", user.getIsActive());
        safe.put("createdAt", user.getCreatedAt());
        return safe;
    }
}