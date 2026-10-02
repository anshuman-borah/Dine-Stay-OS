package project.EnterpriseSaas.demo.modules.tenant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import project.EnterpriseSaas.demo.common.enums.TaxRegime;
import project.EnterpriseSaas.demo.modules.tenant.dto.UpdateTenantDto;
import project.EnterpriseSaas.demo.modules.tenant.entity.Tenant;
import project.EnterpriseSaas.demo.modules.tenant.repository.TenantRepository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepo;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public Tenant findById(UUID id) {
        return tenantRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
    }

    public Tenant findBySlug(String slug) {
        // FIXED: Now queries the database directly instead of loading all rows into RAM
        return tenantRepo.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
    }

    @Transactional
    public Tenant update(UUID id, UpdateTenantDto dto) {
        Tenant tenant = findById(id);

        if (dto.getName() != null) tenant.setName(dto.getName());
        if (dto.getGstin() != null) tenant.setGstin(dto.getGstin().isBlank() ? null : dto.getGstin());
        if (dto.getPan() != null) tenant.setPan(dto.getPan().isBlank() ? null : dto.getPan());
        if (dto.getFssaiNo() != null) tenant.setFssaiNo(dto.getFssaiNo().isBlank() ? null : dto.getFssaiNo());
        if (dto.getAddressLine1() != null) tenant.setAddressLine1(dto.getAddressLine1());
        if (dto.getAddressLine2() != null) tenant.setAddressLine2(dto.getAddressLine2());
        if (dto.getCity() != null) tenant.setCity(dto.getCity());
        if (dto.getState() != null) tenant.setState(dto.getState());
        if (dto.getStateCode() != null) tenant.setStateCode(dto.getStateCode());
        if (dto.getPincode() != null) tenant.setPincode(dto.getPincode());
        if (dto.getCountry() != null) tenant.setCountry(dto.getCountry());
        if (dto.getEmail() != null) tenant.setEmail(dto.getEmail());
        if (dto.getPhone() != null) tenant.setPhone(dto.getPhone());

        // Ensure UI can clear the logo
        if (dto.getLogoUrl() != null) tenant.setLogoUrl(dto.getLogoUrl().isBlank() ? null : dto.getLogoUrl());

        // FIX: Directly set the enum, since Spring Boot already parsed it for us!
        if (dto.getTaxRegime() != null) {
            tenant.setTaxRegime(dto.getTaxRegime());
        }

        // Deep merge settings JSON
        if (dto.getSettings() != null) {
            Map<String, Object> current = new HashMap<>(tenant.getSettings() != null ? tenant.getSettings() : Map.of());
            current.putAll(dto.getSettings());
            tenant.setSettings(current);
        }

        return tenantRepo.save(tenant);
    }

    // ── Razorpay Integration ──────────────────────────────────────────────────

    public Map<String, Object> getRazorpayStatus(UUID tenantId) {
        Tenant tenant = findById(tenantId);
        Map<String, Object> settings = tenant.getSettings() != null ? tenant.getSettings() : Map.of();

        @SuppressWarnings("unchecked")
        Map<String, Object> rzp = settings.containsKey("razorpay") && settings.get("razorpay") instanceof Map
                ? (Map<String, Object>) settings.get("razorpay")
                : Map.of();

        Map<String, Object> result = new HashMap<>();
        result.put("connected", Boolean.TRUE.equals(rzp.get("connected")));
        result.put("liveMode", Boolean.TRUE.equals(rzp.get("liveMode")));
        result.put("keyId", rzp.get("keyId"));
        result.put("connectedAt", rzp.get("connectedAt"));
        return result;
    }

    @Transactional
    public Map<String, Object> saveRazorpayKeys(UUID tenantId, String keyId, String keySecret) {
        boolean valid = verifyRazorpayKeys(keyId, keySecret);
        if (!valid) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid Razorpay credentials. Please check your Key ID and Secret and try again."
            );
        }

        Tenant tenant = findById(tenantId);
        Map<String, Object> existingSettings = new HashMap<>(tenant.getSettings() != null ? tenant.getSettings() : Map.of());
        boolean liveMode = keyId.startsWith("rzp_live_");
        String now = Instant.now().toString();

        Map<String, Object> rzpConfig = new HashMap<>();
        rzpConfig.put("keyId", keyId);
        rzpConfig.put("keySecret", keySecret);
        rzpConfig.put("connected", true);
        rzpConfig.put("liveMode", liveMode);
        rzpConfig.put("connectedAt", now);

        existingSettings.put("razorpay", rzpConfig);
        tenant.setSettings(existingSettings);
        tenantRepo.save(tenant);

        return Map.of(
                "connected", true,
                "liveMode", liveMode,
                "keyId", keyId,
                "connectedAt", now
        );
    }

    @Transactional
    public Map<String, Object> disconnectRazorpay(UUID tenantId) {
        Tenant tenant = findById(tenantId);
        Map<String, Object> existingSettings = new HashMap<>(tenant.getSettings() != null ? tenant.getSettings() : Map.of());

        Map<String, Object> rzpConfig = new HashMap<>();
        rzpConfig.put("connected", false);
        rzpConfig.put("keyId", null);
        rzpConfig.put("keySecret", null);
        rzpConfig.put("liveMode", false);

        existingSettings.put("razorpay", rzpConfig);
        tenant.setSettings(existingSettings);
        tenantRepo.save(tenant);

        return Map.of("connected", false);
    }

    private boolean verifyRazorpayKeys(String keyId, String keySecret) {
        try {
            String auth = Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.razorpay.com/v1/payments?count=1"))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() != 401 && response.statusCode() != 403;
        } catch (Exception e) {
            log.error("Failed to verify Razorpay keys: {}", e.getMessage());
            return false;
        }
    }
}