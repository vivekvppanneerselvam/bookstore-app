package com.bookstore.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class CartDtos {

    private CartDtos() {
    }

    public record AddItemRequest(@NotNull Long bookId,
                                 @Min(1) @Max(10) int quantity) {
    }

    public record UpdateQuantityRequest(@Min(1) @Max(10) int quantity) {
    }

    public record CartItemResponse(Long id, Long bookId, String title, String author, BigDecimal unitPrice,
                                   int quantity, BigDecimal subtotal) {
    }

    public record CartResponse(Long id, List<CartItemResponse> items, int itemCount, BigDecimal total) {
    }
}
