package project.EnterpriseSaas.demo.modules.kafka.events;

import java.math.BigDecimal;
import java.util.UUID;

public record RoomChargeEvent(
        UUID tenantId,
        UUID branchId,
        String roomNumber,    // The POS cashier types "305"
        BigDecimal amount,    // The bill total
        String orderNumber    // E.g., "KOT-1024" for the folio description
) {}