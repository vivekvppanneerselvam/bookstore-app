package com.bookstore.dto;

import com.bookstore.model.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record OrderItemResponse(Long bookId, String title, String author, BigDecimal unitPrice, int quantity,
                                    BigDecimal lineTotal) {
    }

    public record OrderResponse(Long id, String orderNumber, String status, BigDecimal total, Instant createdAt,
                                List<OrderItemResponse> items) {

        public static OrderResponse from(Order order) {
            List<OrderItemResponse> items = order.getItems().stream()
                    .map(item -> new OrderItemResponse(item.getBookId(), item.getTitle(), item.getAuthor(),
                            item.getUnitPrice(), item.getQuantity(), item.getLineTotal()))
                    .toList();
            return new OrderResponse(order.getId(), order.getOrderNumber(), order.getStatus().name(),
                    order.getTotalAmount(), order.getCreatedAt(), items);
        }
    }
}
