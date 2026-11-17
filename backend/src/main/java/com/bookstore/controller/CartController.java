package com.bookstore.controller;

import com.bookstore.dto.CartDtos.AddItemRequest;
import com.bookstore.dto.CartDtos.CartResponse;
import com.bookstore.dto.CartDtos.UpdateQuantityRequest;
import com.bookstore.security.AuthPrincipal;
import com.bookstore.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse getCart(@AuthenticationPrincipal AuthPrincipal principal) {
        return cartService.getCart(principal.id());
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse addItem(@AuthenticationPrincipal AuthPrincipal principal,
                                @Valid @RequestBody AddItemRequest request) {
        return cartService.addItem(principal.id(), request);
    }

    @PatchMapping("/items/{itemId}")
    public CartResponse updateQuantity(@AuthenticationPrincipal AuthPrincipal principal,
                                       @PathVariable Long itemId,
                                       @Valid @RequestBody UpdateQuantityRequest request) {
        return cartService.updateQuantity(principal.id(), itemId, request.quantity());
    }

    @DeleteMapping("/items/{itemId}")
    public CartResponse removeItem(@AuthenticationPrincipal AuthPrincipal principal,
                                   @PathVariable Long itemId) {
        return cartService.removeItem(principal.id(), itemId);
    }
}
