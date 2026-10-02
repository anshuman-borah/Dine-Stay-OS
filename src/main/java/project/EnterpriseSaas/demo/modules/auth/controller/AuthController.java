package project.EnterpriseSaas.demo.modules.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import project.EnterpriseSaas.demo.common.dto.ApiResponse;
import project.EnterpriseSaas.demo.modules.auth.dto.*;
import project.EnterpriseSaas.demo.modules.auth.service.AuthService;
import project.EnterpriseSaas.demo.modules.auth.service.SessionService;
import project.EnterpriseSaas.demo.modules.user.dto.ChangePasswordDto;
import project.EnterpriseSaas.demo.modules.user.entity.User;
import project.EnterpriseSaas.demo.modules.user.repository.UserRepository;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SessionService sessionService;
    private final UserRepository userRepo;

    // ── Public Routes ────────────────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterTenantDto dto) {
        AuthResponse response = authService.register(dto);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginDto dto,
            HttpServletRequest request
    ) {
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        AuthResponse response = authService.login(dto, ip, userAgent);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenDto dto) {
        AuthResponse response = authService.refresh(dto.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> forgotPassword(
            @Valid @RequestBody ForgotPasswordDto dto,
            HttpServletRequest request
    ) {
        String ip = request.getRemoteAddr();
        authService.forgotPassword(dto.getEmail(), ip);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "If that email exists, a reset link has been sent")));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> resetPassword(@Valid @RequestBody ResetPasswordDto dto) {
        authService.resetPassword(dto.getUserId(), dto.getToken(), dto.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Password reset successfully. Please log in.")));
    }

    // ── Authenticated Routes ─────────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Object>> getMe(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return ResponseEntity.ok(ApiResponse.ok(authService.sanitizeUser(user)));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordDto dto
    ) {
        UUID userId = extractUserId(authentication);
        authService.changePassword(userId, dto.getCurrentPassword(), dto.getNewPassword());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Password updated successfully")));
    }

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<Object>> listSessions(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        return ResponseEntity.ok(ApiResponse.ok(sessionService.sessionExists(userId, "") ? "Active" : "Sessions"));
    }

    @DeleteMapping("/sessions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> revokeAllSessions(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Signed out", "count", 0)));
        }
        UUID userId = extractUserId(authentication);
        int count = sessionService.revokeAllSessions(userId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "message", "Signed out from " + count + " device(s)",
                "count", count
        )));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> revokeSession(
            Authentication authentication,
            @PathVariable String sessionId
    ) {
        UUID userId = extractUserId(authentication);
        sessionService.revokeSession(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Session revoked")));
    }

    private UUID extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            return userRepo.findByEmailIgnoreCaseAndIsActiveTrue(authentication.getName())
                    .map(User::getId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        }
    }
}