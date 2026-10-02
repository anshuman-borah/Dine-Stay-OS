package project.EnterpriseSaas.demo.modules.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import project.EnterpriseSaas.demo.common.enums.KdsStatus;
import project.EnterpriseSaas.demo.common.enums.OrderStatus;
import project.EnterpriseSaas.demo.modules.order.entity.Order;

import java.util.UUID;

public class OrderEvents {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderCreatedEvent {
        private UUID branchId;
        private Order order;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderStatusChangedEvent {
        private UUID orderId;
        private UUID branchId;
        private OrderStatus status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemsAddedEvent {
        private UUID orderId;
        private UUID branchId;
        private String orderNumber;
        private Object items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KdsItemStatusChangedEvent {
        private UUID itemId;
        private UUID orderId;
        private UUID branchId;
        private KdsStatus status;
    }
}