package project.EnterpriseSaas.demo.modules.order.gateway;

import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import project.EnterpriseSaas.demo.modules.order.event.OrderEvents.*;
import project.EnterpriseSaas.demo.security.JwtService;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrdersGateway {

    private final SocketIOServer server;
    private final JwtService jwtService;

    // 🟢 NEW: Spring's Native WebSocket Broadcaster
    private final SimpMessagingTemplate messagingTemplate;

    private SocketIONamespace ordersNamespace;

    @PostConstruct
    public void start() {
        ordersNamespace = server.addNamespace("/orders");

        // ── 1. Authenticate connection with JWT ───────────────────────────────
        ordersNamespace.addConnectListener((client) -> {
            String token = client.getHandshakeData().getSingleUrlParam("token");
            if (token == null || token.isBlank()) {
                Object authObj = client.getHandshakeData().getHttpHeaders().get("Authorization");
                if (authObj != null) {
                    String authStr = authObj.toString();
                    if (authStr.startsWith("Bearer ")) token = authStr.substring(7);
                }
            }

            if (token == null || !jwtService.isAccessTokenValid(token)) {
                log.warn("[WS] Invalid or missing token from client: {} — Disconnecting", client.getSessionId());
                client.disconnect();
                return;
            }

            String tenantId = jwtService.extractTenantId(token);
            client.set("tenantId", tenantId);
            log.info("[WS] Connected: {} for tenant {}", client.getSessionId(), tenantId);
        });

        ordersNamespace.addDisconnectListener((client) -> {
            log.info("[WS] Disconnected: {}", client.getSessionId());
        });

        // ── 2. Handle 'join:branch' Room Subscription ────────────────────────
        ordersNamespace.addEventListener("join:branch", Map.class, (client, data, ackSender) -> {
            String branchId = (String) data.get("branchId");
            if (branchId != null) {
                String roomName = "branch:" + branchId;
                client.joinRoom(roomName);
                log.info("[WS] Client {} joined room {}", client.getSessionId(), roomName);
            }
        });

        // ── 3. Handle 'join:kds' Room Subscription ───────────────────────────
        ordersNamespace.addEventListener("join:kds", Map.class, (client, data, ackSender) -> {
            String branchId = (String) data.get("branchId");
            String displayId = (String) data.get("displayId");
            if (branchId != null && displayId != null) {
                client.joinRoom("kds:" + branchId + ":" + displayId);
            }
        });

        server.start();
        log.info("🚀 Socket.IO Server successfully started on port 4001 (/orders namespace)");
    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            server.stop();
        }
    }

    // ── Broadcast Event Listeners (DUAL BROADCASTING) ───────────────────────

    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        String branchId = event.getBranchId().toString();
        String room = "branch:" + branchId;

        Map<String, Object> kdsPayload = Map.of(
                "orderId", event.getOrder().getId(),
                "orderNumber", event.getOrder().getOrderNumber(),
                "items", event.getOrder().getItems(),
                "tableId", event.getOrder().getTableId() != null ? event.getOrder().getTableId().toString() : ""
        );

        // 1. Old Socket.IO Broadcast
        ordersNamespace.getRoomOperations(room).sendEvent("order:created", event.getOrder());
        ordersNamespace.getRoomOperations(room).sendEvent("kds:newItems", kdsPayload);

        // 2. 🟢 New STOMP Native Broadcast
        messagingTemplate.convertAndSend("/topic/branch/" + branchId + "/order-created", event.getOrder());
        messagingTemplate.convertAndSend("/topic/branch/" + branchId + "/kds-newItems", kdsPayload);

        log.info("[WS Broadcast] order:created and kds:newItems sent to room {} (Dual Broadcast)", room);
    }

    @EventListener
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        String branchId = event.getBranchId().toString();
        String room = "branch:" + branchId;

        Map<String, Object> payload = Map.of(
                "orderId", event.getOrderId(),
                "status", event.getStatus().name(),
                "branchId", branchId
        );

        // 1. Old Socket.IO
        ordersNamespace.getRoomOperations(room).sendEvent("order:statusChanged", payload);
        // 2. 🟢 New STOMP Native
        messagingTemplate.convertAndSend("/topic/branch/" + branchId + "/order-statusChanged", payload);

        log.info("[WS Broadcast] order:statusChanged sent to room {} (Dual Broadcast)", room);
    }

    @EventListener
    public void onOrderItemsAdded(OrderItemsAddedEvent event) {
        String branchId = event.getBranchId().toString();
        String room = "branch:" + branchId;

        // 1. Old Socket.IO
        ordersNamespace.getRoomOperations(room).sendEvent("order:itemsAdded", event);
        ordersNamespace.getRoomOperations(room).sendEvent("kds:newItems", event);

        // 2. 🟢 New STOMP Native
        messagingTemplate.convertAndSend("/topic/branch/" + branchId + "/order-itemsAdded", event);
        messagingTemplate.convertAndSend("/topic/branch/" + branchId + "/kds-newItems", event);

        log.info("[WS Broadcast] order:itemsAdded sent to room {} (Dual Broadcast)", room);
    }

    @EventListener
    public void onKdsItemStatusChanged(KdsItemStatusChangedEvent event) {
        String branchId = event.getBranchId().toString();
        String room = "branch:" + branchId;

        Map<String, Object> payload = Map.of(
                "itemId", event.getItemId(),
                "orderId", event.getOrderId(),
                "status", event.getStatus().name(),
                "branchId", branchId
        );

        // 1. Old Socket.IO
        ordersNamespace.getRoomOperations(room).sendEvent("kds:itemStatusChanged", payload);
        // 2. 🟢 New STOMP Native
        messagingTemplate.convertAndSend("/topic/branch/" + branchId + "/kds-itemStatusChanged", payload);

        log.info("[WS Broadcast] kds:itemStatusChanged sent to room {} (Dual Broadcast)", room);
    }

    public void broadcastHousekeepingUpdate(String branchId, String eventName) {
        String room = "branch:" + branchId;
        Map<String, Object> payload = Map.of("timestamp", System.currentTimeMillis());

        // 1. Old Socket.IO
        ordersNamespace.getRoomOperations(room).sendEvent(eventName, payload);

        // 2. 🟢 New STOMP Native (Replacing colons with dashes for clean STOMP URLs)
        String stompEndpoint = eventName.replace(":", "-");
        messagingTemplate.convertAndSend("/topic/branch/" + branchId + "/" + stompEndpoint, payload);

        log.info("[WS Broadcast] {} sent to room {} (Dual Broadcast)", eventName, room);
    }
}