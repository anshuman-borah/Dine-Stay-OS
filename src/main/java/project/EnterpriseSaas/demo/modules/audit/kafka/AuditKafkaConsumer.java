package project.EnterpriseSaas.demo.modules.audit.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import project.EnterpriseSaas.demo.modules.audit.entity.AuditLog;
import project.EnterpriseSaas.demo.modules.audit.repository.AuditLogRepository;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditKafkaConsumer {

    private final AuditLogRepository auditLogRepo;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "audit-logs", groupId = "audit-module-group")
    public void handleAuditLog(String message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> event = objectMapper.readValue(message, Map.class);

            AuditLog logEntry = AuditLog.builder()
                    .tenantId(event.get("tenantId") != null ? UUID.fromString(event.get("tenantId")) : null)
                    .branchId(event.get("branchId") != null ? UUID.fromString(event.get("branchId")) : null)
                    .userId(event.get("userId") != null ? UUID.fromString(event.get("userId")) : null)
                    .entity(event.get("entity"))
                    .entityId(event.get("entityId"))
                    .action(event.get("action"))
                    .oldValue(stringToMap(event.get("oldValue")))   // 🟢 Fixed
                    .newValue(stringToMap(event.get("newValue")))   // 🟢 Fixed
                    .metadata(stringToMap(event.get("metadata")))   // 🟢 Fixed
                    .ipAddress("KAFKA-ASYNC")
                    .userAgent("SYSTEM")
                    .build();

            auditLogRepo.save(logEntry);
            log.info("🔐 [AUDIT] Kafka saved unbreakable log for {} action on {}", event.get("action"), event.get("entity"));

        } catch (Exception e) {
            log.error("Failed to process Kafka audit event. Error: {}", e.getMessage());
        }
    }

    // Helper method to safely convert any String into a JSONB-compatible Map
    private Map<String, Object> stringToMap(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            // First, try to parse it in case it's actually a JSON string
            return objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            // If it's just a normal string like "Reason: XYZ", wrap it in a proper Map
            return Map.of("details", (Object) value);
        }
    }
}
